package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.enums.StatutRdv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataServiceTest {
    @TempDir Path dir;

    @Test
    void genereUnJeuCoherentEtRechargeable() {
        ServiceRegistry reg = ServiceRegistry.ouvrir(dir, true);
        DataStore s = reg.store;
        assertEquals(52, s.patients.count());
        assertTrue(s.rendezVous.findWhere(r -> r.getStatut() == StatutRdv.REALISE).size() > 50);
        assertTrue(s.rendezVous.findWhere(r -> r.getStatut() == StatutRdv.PLANIFIE).size() > 5);
        assertTrue(s.factures.count() > 50);
        assertTrue(s.paiements.count() > 20);
        assertTrue(s.comptesRendus.count() > 30);
        assertTrue(s.teleExpertises.count() >= 2);
        assertTrue(s.journalConflits.count() >= 5, "les tentatives conflictuelles doivent être journalisées");
        assertTrue(reg.patients.trouver("10293847").getSoldeCompte() < -ParametresCentre.SEUIL_SOLDE_DEBITEUR);
        assertFalse(reg.alertes.alertes(LocalDate.now()).isEmpty());
        assertTrue(reg.comptabilite.bilan(YearMonth.now().minusMonths(1)).totalRecettes() > 0);

        // rechargement depuis le disque : mêmes volumes, aucun avertissement
        ServiceRegistry recharge = ServiceRegistry.ouvrir(dir, true);
        assertEquals(s.rendezVous.count(), recharge.store.rendezVous.count());
        assertEquals(s.factures.count(), recharge.store.factures.count());
        assertTrue(recharge.store.getAvertissements().isEmpty(), recharge.store.getAvertissements().toString());
        System.out.printf("RDV=%d factures=%d paiements=%d CR=%d conflits=%d%n", s.rendezVous.count(), s.factures.count(),
                s.paiements.count(), s.comptesRendus.count(), s.journalConflits.count());
    }
}
