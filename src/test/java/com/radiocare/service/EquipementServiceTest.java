package com.radiocare.service;

import com.radiocare.model.EquipementImagerie;
import com.radiocare.model.SalleRadiologie;
import com.radiocare.model.enums.StatutEquipement;
import com.radiocare.model.enums.TypeEquipement;
import com.radiocare.model.enums.TypeSalle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class EquipementServiceTest {
    @TempDir Path dir;
    ServiceRegistry reg;
    EquipementService svc;

    @BeforeEach
    void init() { reg = Fixture.creer(dir); svc = reg.equipements; }

    @Test
    void decompteDHeuresEtAlerteDeMaintenance() {
        // seuil = max(50 h, 10 % de 500 h) = 50 h ; restant = 100 h
        assertFalse(svc.estEnAlerteMaintenance(svc.trouverEquipement(EQ_IRM)));
        svc.enregistrerUtilisation(EQ_IRM, 50);
        EquipementImagerie e = svc.trouverEquipement(EQ_IRM);
        assertEquals(1050, e.getHeuresCumulees());
        assertEquals(50, e.getHeuresRestantesMaintenance());
        assertTrue(svc.estEnAlerteMaintenance(e));
        assertTrue(reg.alertes.alertes(LocalDate.now()).stream().anyMatch(a -> a.categorie().equals("Maintenance")));
    }

    @Test
    void potentielEpuisePasseEnMaintenance() {
        svc.enregistrerUtilisation(EQ_IRM, 120);
        EquipementImagerie e = svc.trouverEquipement(EQ_IRM);
        assertEquals(0, e.getHeuresRestantesMaintenance());
        assertEquals(StatutEquipement.EN_MAINTENANCE, e.getStatut());
    }

    @Test
    void maintenancePreventiveRemetLeCompteurEtComptabiliseLeCout() {
        svc.enregistrerUtilisation(EQ_IRM, 120);
        svc.enregistrerMaintenance(EQ_IRM, LocalDate.now(), true, "Révision", "bi-siemens-2026/01", 2500);
        EquipementImagerie e = svc.trouverEquipement(EQ_IRM);
        assertEquals(500, e.getHeuresRestantesMaintenance());
        assertEquals(StatutEquipement.EN_SERVICE, e.getStatut());
        assertEquals("BI-SIEMENS-2026/01", e.getInterventions().get(0).getRefPieceJustificative());
        assertEquals(2500, svc.depensesMaintenance(YearMonth.now()));
        assertThrows(ValidationException.class, () -> svc.enregistrerMaintenance(EQ_IRM, LocalDate.now(), true, "x", "pas une ref!", 1));
    }

    @Test
    void rapportDUtilisationIntegreLesHeuresPlanifiees() {
        reg.rendezVous.planifier(rdv(lundiProchain(), "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM, 30));
        var ligne = svc.rapportUtilisation().stream().filter(l -> l.equipement().getId().equals(EQ_IRM)).findFirst().orElseThrow();
        assertEquals(0.5, ligne.heuresPlanifiees(), 1e-9);
        assertEquals(20.0, ligne.pourcentagePotentiel(), 1e-9);
        assertEquals(SALLE_IRM, ligne.salle());
    }

    @Test
    void salleRefuseUnEquipementIncompatibleOuDejaAttribue() {
        SalleRadiologie s = new SalleRadiologie(null, "Salle Scanner", 0, TypeSalle.SALLE_SCANNER, EQ_IRM, null, 2.5);
        assertThrows(ValidationException.class, () -> svc.creerSalle(s));
        EquipementImagerie scan = svc.creerEquipement(new EquipementImagerie(null, TypeEquipement.SCANNER_CT, "GE", "CT", 0, 400, 400));
        s.setEquipementAttribueId(scan.getId());
        svc.creerSalle(s);
        SalleRadiologie s2 = new SalleRadiologie(null, "Salle Scanner 2", 0, TypeSalle.SALLE_SCANNER, scan.getId(), null, 2.5);
        assertThrows(ValidationException.class, () -> svc.creerSalle(s2));
        assertThrows(ValidationException.class, () -> svc.supprimerEquipement(scan.getId()));
    }

    @Test
    void conformiteTechniqueDuBlindage() {
        EquipementImagerie scan = svc.creerEquipement(new EquipementImagerie(null, TypeEquipement.SCANNER_CT, "GE", "CT", 0, 400, 400));
        SalleRadiologie s = svc.creerSalle(new SalleRadiologie(null, "Scanner", 0, TypeSalle.SALLE_SCANNER, scan.getId(), null, 1.0));
        assertTrue(svc.verifierConformite(s).stream().anyMatch(a -> a.contains("Blindage")));
        s.setEpaisseurPlombMm(2.5);
        svc.modifierSalle(s);
        assertTrue(svc.verifierConformite(s).isEmpty());
    }

    @Test
    void validationDesDonneesEquipement() {
        assertThrows(ValidationException.class, () -> svc.creerEquipement(new EquipementImagerie(null, TypeEquipement.IRM, "", "x", 0, 10, 10)));
        assertThrows(ValidationException.class, () -> svc.creerEquipement(new EquipementImagerie(null, TypeEquipement.IRM, "GE", "x", 0, 600, 500)));
    }
}
