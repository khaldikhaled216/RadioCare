package com.radiocare.service;

import com.radiocare.model.FactureMedicale;
import com.radiocare.model.Patient;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.ModePaiement;
import com.radiocare.model.enums.StatutPaiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class FacturationServiceTest {
    @TempDir Path dir;
    ServiceRegistry reg;

    @BeforeEach
    void init() { reg = Fixture.creer(dir); }

    @Test
    void calculTvaEtTiersPayant() {
        FacturationService.Montants m = FacturationService.calculerMontants(280, 0.7);
        assertEquals(280.0, m.ht());
        assertEquals(53.2, m.tva(), 1e-9);          // 19 %
        assertEquals(333.2, m.ttc(), 1e-9);
        assertEquals(233.24, m.partAssurance(), 1e-9);
        assertEquals(99.96, m.partPatient(), 1e-9);
        assertEquals(m.ttc(), m.partAssurance() + m.partPatient(), 1e-9);
    }

    @Test
    void patientNonAssurePaieLaTotalite() {
        FacturationService.Montants m = FacturationService.calculerMontants(35, 0);
        assertEquals(41.65, m.ttc(), 1e-9);
        assertEquals(0, m.partAssurance());
        assertEquals(41.65, m.partPatient(), 1e-9);
    }

    private FactureMedicale factureRealisee() {
        RendezVous r = rdvPasse(reg, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30);
        return reg.rendezVous.realiser(r.getIdRdv());
    }

    @Test
    void realisationGenereLaFactureEtDebiteLeSolde() {
        FactureMedicale f = factureRealisee();
        assertEquals(280, f.getMontantHT());
        assertEquals(0.19, f.getTauxTVA());
        assertEquals(333.2, f.getMontantTTC(), 1e-9);
        assertEquals(99.96, f.getPartPatient(), 1e-9);
        assertEquals(StatutPaiement.EN_ATTENTE, f.getStatutPaiement());
        assertEquals(-99.96, reg.patients.trouver(PATIENT).getSoldeCompte(), 1e-9);
        assertThrows(ValidationException.class, () -> reg.facturation.genererFacture(reg.store.rendezVous.findById(f.getIdRdv()).orElseThrow(), LocalDate.now()));
    }

    @Test
    void paiementsPartielsPuisTotalMettentAJourSoldeEtStatut() {
        FactureMedicale f = factureRealisee();
        reg.facturation.enregistrerPaiement(f.getNumFacture(), LocalDate.now(), 50, ModePaiement.ESPECES, "rec-000123");
        FactureMedicale apres = reg.facturation.trouverFacture(f.getNumFacture());
        assertEquals(49.96, apres.getResteAPayer(), 1e-9);
        assertEquals(StatutPaiement.EN_ATTENTE, apres.getStatutPaiement());
        assertEquals(-49.96, reg.patients.trouver(PATIENT).getSoldeCompte(), 1e-9);
        assertEquals("REC-000123", reg.facturation.listerPaiements().get(0).getReferenceTextuellePreuve()); // normalisée

        reg.facturation.enregistrerPaiement(f.getNumFacture(), LocalDate.now(), 49.96, ModePaiement.CARTE, "CB-778899");
        assertEquals(StatutPaiement.REGLEE, reg.facturation.trouverFacture(f.getNumFacture()).getStatutPaiement());
        assertEquals(0, reg.patients.trouver(PATIENT).getSoldeCompte(), 1e-9);

        PatientService.BilanPatient b = reg.patients.bilan(PATIENT);
        assertEquals(333.2, b.totalActesTTC(), 1e-9);
        assertEquals(233.24, b.partAssurance(), 1e-9);
        assertEquals(99.96, b.dejaRegle(), 1e-9);
        assertEquals(0, b.resteAPayer(), 1e-9);
    }

    @Test
    void tropPercuEtPreuveInvalideRefuses() {
        FactureMedicale f = factureRealisee();
        assertThrows(ValidationException.class, () -> reg.facturation.enregistrerPaiement(f.getNumFacture(), LocalDate.now(), 1000, ModePaiement.ESPECES, "REC-1234"));
        assertThrows(ValidationException.class, () -> reg.facturation.enregistrerPaiement(f.getNumFacture(), LocalDate.now(), 10, ModePaiement.CHEQUE, "REC-1234"));
        assertThrows(ValidationException.class, () -> reg.facturation.enregistrerPaiement(f.getNumFacture(), LocalDate.now(), 10, ModePaiement.CHEQUE, "chèque n°12"));
        assertEquals(0, reg.store.paiements.count());
    }

    @Test
    void annulationDUnPaiementRetablitLesMontants() {
        FactureMedicale f = factureRealisee();
        var p = reg.facturation.enregistrerPaiement(f.getNumFacture(), LocalDate.now(), 99.96, ModePaiement.VIREMENT, "VIR-2026-001");
        reg.facturation.supprimerPaiement(p.getId());
        assertEquals(99.96, reg.facturation.trouverFacture(f.getNumFacture()).getResteAPayer(), 1e-9);
        assertEquals(-99.96, reg.patients.trouver(PATIENT).getSoldeCompte(), 1e-9);
        assertThrows(ValidationException.class, () -> {
            reg.facturation.enregistrerPaiement(f.getNumFacture(), LocalDate.now(), 1, ModePaiement.ESPECES, "REC-1");
            reg.facturation.supprimerFacture(f.getNumFacture());
        });
    }

    @Test
    void factureEchueImpayeePasseEnRetard() {
        FactureMedicale f = factureRealisee();
        assertEquals(StatutPaiement.EN_ATTENTE, reg.facturation.statutA(f, f.getDateEcheance()));
        assertEquals(StatutPaiement.RETARD, reg.facturation.statutA(f, f.getDateEcheance().plusDays(1)));
        reg.facturation.actualiserStatuts(f.getDateEcheance().plusDays(1));
        assertEquals(1, reg.facturation.facturesEnRetard().size());
    }

    @Test
    void patientDebiteurEstBloqueParLeMoteur() {
        Patient p = reg.patients.trouver(PATIENT2); // non assuré
        for (int i = 0; i < 2; i++) {
            RendezVous r = rdvPasse(reg, "0" + (8 + i) + ":00", PATIENT2, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45);
            reg.rendezVous.realiser(r.getIdRdv()); // 2 × 452,2 TND
        }
        assertTrue(reg.patients.trouver(p.getCin()).getSoldeCompte() < -500);
        assertThrows(com.radiocare.service.conflit.ConflitException.class,
                () -> reg.rendezVous.planifier(rdv(lundiProchain(), "09:00", PATIENT2, MAN_RX, SALLE_RX, EXA_RX, 10)));
    }
}
