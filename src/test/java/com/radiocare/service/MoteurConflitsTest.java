package com.radiocare.service;

import com.radiocare.model.*;
import com.radiocare.model.enums.*;
import com.radiocare.service.conflit.Conflit;
import com.radiocare.service.conflit.ConflitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class MoteurConflitsTest {
    @TempDir Path dir;
    ServiceRegistry reg;
    LocalDate lundi;

    @BeforeEach
    void init() {
        reg = Fixture.creer(dir);
        lundi = lundiProchain();
    }

    private List<TypeConflit> types(RendezVous r) {
        return reg.rendezVous.simuler(r).stream().map(Conflit::type).distinct().toList();
    }

    @Test
    void creneauLibreSansConflit() {
        assertTrue(reg.rendezVous.simuler(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)).isEmpty());
        RendezVous cree = reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30));
        assertEquals(StatutRdv.PLANIFIE, cree.getStatut());
        assertEquals(0, reg.store.journalConflits.count());
    }

    @Test
    void conflit1_doubleReservationDeSalle() {
        reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30));
        // autre patient, autre manip habilité ? même salle, créneau chevauchant 09:15-09:45
        ManipulateurRadio m2 = new ManipulateurRadio("MAN-0003", "04567890", "Bouazizi", "Rim", java.util.EnumSet.of(Habilitation.IRM), 17, StatutDispo.DISPONIBLE);
        reg.store.manipulateurs.create(m2);
        MedecinRadiologue med2 = new MedecinRadiologue("MED-0002", "05678901", "Jaziri", "Karim", Specialite.GENERALE, 25, DisponibiliteHebdo.semaineComplete());
        reg.store.medecins.create(med2);
        RendezVous r = rdv(lundi, "09:15", PATIENT2, "MAN-0003", SALLE_IRM, EXA_IRM, 30);
        r.setIdMedecin("MED-0002");
        assertEquals(List.of(TypeConflit.DOUBLE_RESERVATION), types(r));
        // créneau adjacent (09:30) : pas de chevauchement
        r.setHeureDebut(java.time.LocalTime.of(9, 30));
        assertTrue(types(r).isEmpty());
    }

    @Test
    void conflit1_equipementEnPanneOuIncompatible() {
        reg.equipements.changerStatut(EQ_IRM, StatutEquipement.EN_PANNE);
        assertTrue(types(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)).contains(TypeConflit.DOUBLE_RESERVATION));
        // examen IRM dans la salle de radiographie
        assertTrue(types(rdv(lundi, "11:00", PATIENT, MAN_IRM, SALLE_RX, EXA_IRM, 30)).contains(TypeConflit.DOUBLE_RESERVATION));
    }

    @Test
    void conflit2_chevauchementAgendaMedecinEtManipulateur() {
        reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30));
        // même médecin, même manip, autre salle (RX) : Amine n'a pas l'habilitation RX -> on utilise Nour mais même médecin
        List<TypeConflit> t = types(rdv(lundi, "09:10", PATIENT2, MAN_RX, SALLE_RX, EXA_RX, 10));
        assertEquals(List.of(TypeConflit.CHEVAUCHEMENT_AGENDA), t);
    }

    @Test
    void conflit2_medecinHorsPlanningOuManipEnConge() {
        MedecinRadiologue m = reg.store.medecins.findById(MED).orElseThrow();
        m.setDispoPlanning(new DisponibiliteHebdo(java.util.EnumSet.of(java.time.DayOfWeek.TUESDAY), java.time.LocalTime.of(8, 0), java.time.LocalTime.of(12, 0)));
        reg.store.medecins.update(m);
        assertTrue(types(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)).contains(TypeConflit.CHEVAUCHEMENT_AGENDA));

        ManipulateurRadio man = reg.store.manipulateurs.findById(MAN_RX).orElseThrow();
        man.setStatutDispo(StatutDispo.EN_CONGE);
        reg.store.manipulateurs.update(man);
        String msg = reg.rendezVous.simuler(rdv(lundi.plusDays(1), "09:00", PATIENT, MAN_RX, SALLE_RX, EXA_RX, 10)).toString();
        assertTrue(msg.contains("en congé"), msg);
    }

    @Test
    void conflit3_depassementDuPotentielDHeures() {
        EquipementImagerie eq = reg.store.equipements.findById(EQ_IRM).orElseThrow();
        eq.setHeuresRestantesMaintenance(0.75); // 45 minutes
        reg.store.equipements.update(eq);
        reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)); // 0.5 h engagée
        List<TypeConflit> t = types(rdv(lundi, "10:00", PATIENT2, MAN_IRM, SALLE_IRM, EXA_IRM, 30)); // 0.5 + 0.5 > 0.75
        assertEquals(List.of(TypeConflit.POTENTIEL_HEURES), t);
        assertTrue(types(rdv(lundi, "10:00", PATIENT2, MAN_IRM, SALLE_IRM, EXA_IRM, 15)).isEmpty()); // 0.75 exactement
    }

    @Test
    void conflit4_habilitationManquante() {
        assertEquals(List.of(TypeConflit.HABILITATION), types(rdv(lundi, "09:00", PATIENT, MAN_RX, SALLE_IRM, EXA_IRM, 30)));
    }

    @Test
    void conflit5_contreIndicationMajeure() {
        Patient p = reg.store.patients.findById(PATIENT).orElseThrow();
        p.getContreIndications().add(ContreIndication.PACEMAKER);
        reg.store.patients.update(p);
        assertEquals(List.of(TypeConflit.BLOCAGE_PATIENT), types(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)));
        // le pacemaker n'interdit pas une radiographie
        assertTrue(types(rdv(lundi, "09:00", PATIENT, MAN_RX, SALLE_RX, EXA_RX, 10)).isEmpty());
    }

    @Test
    void conflit5_allergieAuProduitInjecte() {
        Patient p = reg.store.patients.findById(PATIENT).orElseThrow();
        p.getContreIndications().add(ContreIndication.ALLERGIE_GADOLINIUM);
        reg.store.patients.update(p);
        assertTrue(types(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45)).contains(TypeConflit.BLOCAGE_PATIENT));
        assertTrue(types(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)).isEmpty());
    }

    @Test
    void conflit5_soldeDebiteurTropEleve() {
        Patient p = reg.store.patients.findById(PATIENT).orElseThrow();
        p.setSoldeCompte(-ParametresCentre.SEUIL_SOLDE_DEBITEUR - 0.01);
        reg.store.patients.update(p);
        assertEquals(List.of(TypeConflit.BLOCAGE_PATIENT), types(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)));
        p.setSoldeCompte(-ParametresCentre.SEUIL_SOLDE_DEBITEUR);
        reg.store.patients.update(p);
        assertTrue(types(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)).isEmpty());
    }

    @Test
    void conflit6_ruptureDeStockPrevisionnel() {
        ConsommableMedical c = reg.store.consommables.findById(GADO).orElseThrow();
        c.setQuantiteEnStock(1);
        reg.store.consommables.update(c);
        reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45)); // réserve la seule dose
        assertEquals(List.of(TypeConflit.RUPTURE_STOCK), types(rdv(lundi.plusDays(1), "09:00", PATIENT2, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45)));
    }

    @Test
    void conflit6_lotPerimeALaDateDeLExamen() {
        ConsommableMedical c = reg.store.consommables.findById(GADO).orElseThrow();
        c.setDatePeremption(lundi.minusDays(1));
        reg.store.consommables.update(c);
        assertEquals(List.of(TypeConflit.RUPTURE_STOCK), types(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45)));
    }

    @Test
    void conflit6_livraisonAttendueRetablitLeStock() {
        ConsommableMedical c = reg.store.consommables.findById(GADO).orElseThrow();
        c.setQuantiteEnStock(0);
        reg.store.consommables.update(c);
        RendezVous r = rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45);
        assertFalse(types(r).isEmpty());
        reg.stock.passerCommande(new CommandeFournisseur(null, GADO, "Fournisseur", 10, LocalDate.now(), lundi.minusDays(1), 0, "FF-TEST-0001"));
        assertTrue(types(r).isEmpty());
    }

    @Test
    void plusieursConflitsSontCumulesJournalisesEtBloquants() {
        Patient p = reg.store.patients.findById(PATIENT).orElseThrow();
        p.getContreIndications().add(ContreIndication.PACEMAKER);
        reg.store.patients.update(p);
        ConflitException ex = assertThrows(ConflitException.class,
                () -> reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_RX, SALLE_IRM, EXA_IRM, 30)));
        assertEquals(2, ex.getConflits().size());
        assertEquals(2, reg.store.journalConflits.count());
        assertEquals(0, reg.store.rendezVous.count());
    }

    @Test
    void modificationNeSeConfrontePasASoiMeme() {
        RendezVous r = reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30));
        RendezVous m = rdv(lundi, "09:15", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30);
        m.setIdRdv(r.getIdRdv());
        assertDoesNotThrow(() -> reg.rendezVous.modifier(m));
        assertEquals(java.time.LocalTime.of(9, 15), reg.store.rendezVous.findById(r.getIdRdv()).orElseThrow().getHeureDebut());
    }

    @Test
    void annulationLibereLeCreneau() {
        RendezVous r = reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30));
        assertFalse(types(rdv(lundi, "09:00", PATIENT2, MAN_IRM, SALLE_IRM, EXA_IRM, 30)).isEmpty());
        reg.rendezVous.annuler(r.getIdRdv());
        assertTrue(types(rdv(lundi, "09:00", PATIENT2, MAN_IRM, SALLE_IRM, EXA_IRM, 30)).isEmpty());
    }

    @Test
    void validationsDeSaisieAvantMoteur() {
        assertThrows(ValidationException.class, () -> reg.rendezVous.planifier(rdv(LocalDate.now().minusDays(7), "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)));
        assertThrows(ValidationException.class, () -> reg.rendezVous.planifier(rdv(lundi, "18:45", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)));
        assertThrows(ValidationException.class, () -> reg.rendezVous.planifier(rdv(lundi.minusDays(1), "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30)));
        assertThrows(ValidationException.class, () -> reg.rendezVous.planifier(rdv(lundi, "09:00", "00000000", MAN_IRM, SALLE_IRM, EXA_IRM, 30)));
    }
}
