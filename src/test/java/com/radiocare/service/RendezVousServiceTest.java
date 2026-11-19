package com.radiocare.service;

import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.StatutRdv;
import com.radiocare.model.enums.StatutEquipement;
import com.radiocare.service.conflit.ConflitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalTime;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class RendezVousServiceTest {
    @TempDir Path dir;
    ServiceRegistry reg;

    @BeforeEach
    void init() { reg = Fixture.creer(dir); }

    @Test
    void cycleDeVieCompletDUnExamenInjecte() {
        RendezVous r = rdvPasse(reg, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45);
        reg.rendezVous.demarrer(r.getIdRdv(), LocalTime.of(8, 50), LocalTime.of(9, 5));
        assertEquals(StatutRdv.EN_COURS, reg.rendezVous.trouver(r.getIdRdv()).getStatut());
        reg.rendezVous.realiser(r.getIdRdv());

        RendezVous fait = reg.rendezVous.trouver(r.getIdRdv());
        assertEquals(StatutRdv.REALISE, fait.getStatut());
        assertEquals(15, fait.getAttenteMinutes());
        assertEquals(4, reg.store.consommables.findById(GADO).orElseThrow().getQuantiteEnStock());   // 5 - 1
        var eq = reg.store.equipements.findById(EQ_IRM).orElseThrow();
        assertEquals(1000.75, eq.getHeuresCumulees(), 1e-9);
        assertEquals(99.25, eq.getHeuresRestantesMaintenance(), 1e-9);
        assertTrue(reg.patients.trouver(PATIENT).getHistoriqueExamensIds().contains(r.getIdRdv()));
        assertEquals(1, reg.store.factures.count());
        assertThrows(ValidationException.class, () -> reg.rendezVous.realiser(r.getIdRdv()));
        assertThrows(ValidationException.class, () -> reg.rendezVous.supprimer(r.getIdRdv()));
    }

    @Test
    void realisationRefuseeSiEquipementHorsServiceOuStockReelVide() {
        RendezVous r = rdvPasse(reg, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45);
        reg.equipements.changerStatut(EQ_IRM, StatutEquipement.EN_PANNE);
        assertThrows(ConflitException.class, () -> reg.rendezVous.realiser(r.getIdRdv()));
        reg.equipements.changerStatut(EQ_IRM, StatutEquipement.EN_SERVICE);
        var c = reg.store.consommables.findById(GADO).orElseThrow();
        c.setQuantiteEnStock(0);
        reg.store.consommables.update(c);
        assertThrows(ConflitException.class, () -> reg.rendezVous.realiser(r.getIdRdv()));
        assertEquals(0, reg.store.factures.count());
        assertEquals(StatutRdv.PLANIFIE, reg.rendezVous.trouver(r.getIdRdv()).getStatut());
    }

    @Test
    void unExamenFuturNePeutPasEtreRealise() {
        RendezVous r = reg.rendezVous.planifier(rdv(lundiProchain(), "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30));
        assertThrows(ValidationException.class, () -> reg.rendezVous.realiser(r.getIdRdv()));
        assertThrows(ValidationException.class, () -> reg.rendezVous.demarrer(r.getIdRdv(), LocalTime.of(9, 0), LocalTime.of(9, 0)));
    }

    @Test
    void crudEtPlanningHebdomadaire() {
        RendezVous r = reg.rendezVous.planifier(rdv(lundiProchain(), "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 0)); // durée par défaut
        assertEquals(30, r.getDureeMinutes());
        var ligne = reg.rendezVous.planningSemaine(lundiProchain().plusDays(2)).get(0);
        assertEquals("09:00 - 09:30", ligne.heure());
        assertEquals("Amel Khemiri", ligne.patient());
        assertEquals("Salle IRM", ligne.salle());
        assertEquals("Dr Sana Trabelsi", ligne.medecin());
        assertEquals("Amine Ferchichi", ligne.manipulateur());
        assertTrue(ligne.equipement().contains("Siemens"));
        reg.rendezVous.annuler(r.getIdRdv());
        assertThrows(ValidationException.class, () -> reg.rendezVous.annuler(r.getIdRdv()));
        reg.rendezVous.supprimer(r.getIdRdv());
        assertEquals(0, reg.store.rendezVous.count());
    }
}
