package com.radiocare.service;

import com.radiocare.model.enums.ModePaiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class StatistiqueServiceTest {
    @TempDir Path dir;
    ServiceRegistry reg;
    LocalDate jour;

    @BeforeEach
    void init() {
        reg = Fixture.creer(dir.resolve("data"));
        jour = jourOuvrePasse();
        var r = rdvPasse(reg, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45);
        reg.rendezVous.demarrer(r.getIdRdv(), LocalTime.of(8, 50), LocalTime.of(9, 10));
        var f = reg.rendezVous.realiser(r.getIdRdv());
        reg.facturation.enregistrerPaiement(f.getNumFacture(), jour, f.getPartPatient(), ModePaiement.CARTE, "CB-1234");
    }

    @Test
    void occupationAttenteEtRentabilite() {
        var occ = reg.statistiques.occupationSalles(jour, jour);
        var irm = occ.stream().filter(o -> o.id().equals(SALLE_IRM)).findFirst().orElseThrow();
        assertEquals(45, irm.minutesReservees());
        assertEquals(ParametresCentre.minutesOuvertureParJour(), irm.minutesDisponibles());
        assertEquals(Math.round(100.0 * 45 / 660 * 1000) / 1000.0, irm.taux(), 1e-9);
        assertEquals(20, reg.statistiques.attenteMoyenneMinutes(jour, jour), 1e-9);
        var rent = reg.statistiques.rentabiliteParExamen(jour, jour).get(0);
        assertEquals("IRM-CER-INJ", rent.codeActe());
        assertEquals(380 - 45, rent.marge(), 1e-9);
    }

    @Test
    void bilanMensuelEtTvaCollectee() {
        var b = reg.comptabilite.bilan(YearMonth.from(jour));
        assertEquals(380, b.chiffreAffairesHT(), 1e-9);
        assertEquals(72.2, b.tvaCollectee(), 1e-9);
        assertEquals(452.2, b.totalRecettes(), 1e-9);        // patient + assurance = TTC
        assertTrue(b.honorairesMedecins() > 0);
        assertEquals(b.totalRecettes() - b.totalDepenses(), b.resultat(), 1e-6);
    }

    @Test
    void exportJsonEtTexte() throws Exception {
        var fichiers = reg.statistiques.exporter(YearMonth.from(jour), dir.resolve("exports"));
        assertEquals(2, fichiers.size());
        String json = Files.readString(fichiers.get(0));
        assertTrue(json.contains("\"occupationSalles\""), json.substring(0, Math.min(300, json.length())));
        assertTrue(json.contains("IRM-CER-INJ"));
        assertTrue(Files.readString(fichiers.get(1)).contains("RAPPORT D'ACTIVITÉ"));
    }

    @Test
    void conflitsComptesParType() {
        assertThrows(Exception.class, () -> reg.rendezVous.planifier(rdv(lundiProchain(), "09:00", PATIENT, MAN_RX, SALLE_IRM, EXA_IRM, 30)));
        long n = reg.statistiques.conflitsParType(LocalDate.now(), LocalDate.now()).get(com.radiocare.model.enums.TypeConflit.HABILITATION.getLibelle());
        assertEquals(1, n);
    }
}
