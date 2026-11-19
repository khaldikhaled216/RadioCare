package com.radiocare.service;

import com.radiocare.model.*;
import com.radiocare.model.enums.Habilitation;
import com.radiocare.model.enums.StatutDispo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class PersonnelServiceTest {
    @TempDir Path dir;
    ServiceRegistry reg;

    @BeforeEach
    void init() { reg = Fixture.creer(dir); }

    @Test
    void calculPolymorpheDesRemunerations() {
        ActiviteMensuelle a = new ActiviteMensuelle("2026-11", 3, 1000, 10, 8);
        List<PersonnelMedical> personnel = List.of(reg.store.medecins.findById(MED).orElseThrow(), reg.store.manipulateurs.findById(MAN_IRM).orElseThrow());
        assertEquals(300.0, personnel.get(0).calculerRemuneration(a), 1e-9);            // 30 % × 1000
        assertEquals(20 * 10 + 20 * 1.5 * 8, personnel.get(1).calculerRemuneration(a), 1e-9); // 440
    }

    @Test
    void remunerationsCalculeesDepuisLesActesRealises() {
        RendezVous r1 = rdvPasse(reg, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30);       // 280 HT, 0,5 h
        RendezVous r2 = rdvPasse(reg, "10:00", PATIENT2, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45);  // 380 HT, 0,75 h
        reg.rendezVous.realiser(r1.getIdRdv());
        reg.rendezVous.realiser(r2.getIdRdv());
        YearMonth mois = YearMonth.from(jourOuvrePasse());
        reg.personnel.saisirHeuresGarde(MAN_IRM, mois, 12);

        MedecinRadiologue med = reg.store.medecins.findById(MED).orElseThrow();
        ActiviteMensuelle am = reg.personnel.activite(med, mois);
        assertEquals(2, am.nombreActes());
        assertEquals(660, am.montantActesHT(), 1e-9);
        assertEquals(198, reg.personnel.remuneration(med, mois), 1e-9);                     // 30 % × 660

        ManipulateurRadio man = reg.store.manipulateurs.findById(MAN_IRM).orElseThrow();
        assertEquals(20 * 1.25 + 20 * 1.5 * 12, reg.personnel.remuneration(man, mois), 1e-9); // 25 + 360
        assertEquals(0, reg.personnel.remuneration(reg.store.manipulateurs.findById(MAN_RX).orElseThrow(), mois));
        assertEquals(3, reg.personnel.etatRemunerations(mois).size()); // 1 médecin + 2 manipulateurs
    }

    @Test
    void crudEtValidationsDuPersonnel() {
        ManipulateurRadio m = new ManipulateurRadio(null, "09876543", "Khelifi", "Yassine", EnumSet.of(Habilitation.SCANNER), 16.5, StatutDispo.DISPONIBLE);
        reg.personnel.creerManipulateur(m);
        assertEquals("MAN-0003", m.getId());
        ManipulateurRadio doublonCin = new ManipulateurRadio(null, "09876543", "X", "Y", EnumSet.of(Habilitation.IRM), 10, StatutDispo.DISPONIBLE);
        assertThrows(ValidationException.class, () -> reg.personnel.creerManipulateur(doublonCin));
        ManipulateurRadio sansHab = new ManipulateurRadio(null, "07777777", "X", "Y", EnumSet.noneOf(Habilitation.class), 10, StatutDispo.DISPONIBLE);
        assertThrows(ValidationException.class, () -> reg.personnel.creerManipulateur(sansHab));
        reg.personnel.supprimerManipulateur("MAN-0003");

        reg.rendezVous.planifier(rdv(lundiProchain(), "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30));
        assertThrows(ValidationException.class, () -> reg.personnel.supprimerManipulateur(MAN_IRM));
        assertThrows(ValidationException.class, () -> reg.personnel.supprimerMedecin(MED));
    }
}
