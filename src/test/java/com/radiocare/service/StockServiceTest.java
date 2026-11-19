package com.radiocare.service;

import com.radiocare.model.CommandeFournisseur;
import com.radiocare.model.ConsommableMedical;
import com.radiocare.model.enums.CategorieConsommable;
import com.radiocare.model.enums.StatutLivraison;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;

import static com.radiocare.service.Fixture.*;
import static org.junit.jupiter.api.Assertions.*;

class StockServiceTest {
    @TempDir Path dir;
    ServiceRegistry reg;
    StockService stock;

    @BeforeEach
    void init() { reg = Fixture.creer(dir); stock = reg.stock; }

    @Test
    void consommationEtRefusSiInsuffisantOuPerime() {
        stock.consommer(GADO, 3, LocalDate.now());
        assertEquals(2, reg.store.consommables.findById(GADO).orElseThrow().getQuantiteEnStock());
        assertThrows(ValidationException.class, () -> stock.consommer(GADO, 3, LocalDate.now()));
        assertThrows(ValidationException.class, () -> stock.consommer(GADO, 1, LocalDate.now().plusYears(2)));
    }

    @Test
    void alertesStockCritiqueEtPeremption() {
        assertTrue(stock.alertesStockCritique().isEmpty());          // 5 >= seuil 2
        stock.consommer(GADO, 4, LocalDate.now());                    // 1 < 2
        assertEquals(1, stock.alertesStockCritique().size());
        assertEquals(3, stock.quantiteSuggeree(stock.alertesStockCritique().get(0))); // 2×2 - 1
        stock.creerConsommable(new ConsommableMedical(null, "Gel", CategorieConsommable.GEL_ECHO, 50, 10, 4.5, LocalDate.now().plusDays(10)));
        assertEquals(1, stock.alertesPeremption(LocalDate.now()).size());
    }

    @Test
    void stockPrevisionnelTientCompteDesReservationsEtLivraisons() {
        var lundi = lundiProchain();
        assertEquals(5, stock.stockPrevisionnel(GADO, lundi, null));
        reg.rendezVous.planifier(rdv(lundi, "09:00", PATIENT, MAN_IRM, SALLE_IRM, EXA_IRM_INJ, 45));
        assertEquals(4, stock.stockPrevisionnel(GADO, lundi, null));
        assertEquals(5, stock.stockPrevisionnel(GADO, lundi.minusDays(1), null)); // réservation postérieure
        stock.passerCommande(new CommandeFournisseur(null, GADO, "MedImaging", 20, LocalDate.now(), lundi, 0, "FF-MED-2026-01"));
        assertEquals(24, stock.stockPrevisionnel(GADO, lundi, null));
    }

    @Test
    void cycleDeVieDUneCommandeFournisseur() {
        CommandeFournisseur c = stock.passerCommande(new CommandeFournisseur(null, GADO, "MedImaging", 20, LocalDate.now(), null, 0, "ff-med-2026-02"));
        assertEquals(900, c.getMontantTotal(), 1e-9);              // 20 × 45 calculé automatiquement
        assertEquals("FF-MED-2026-02", c.getRefFactureTextuelle());
        assertEquals(900, stock.depensesReapprovisionnement(YearMonth.now()), 1e-9);
        LocalDate peremption = LocalDate.now().plusYears(2);
        stock.receptionnerCommande(c.getId(), peremption);
        ConsommableMedical g = reg.store.consommables.findById(GADO).orElseThrow();
        assertEquals(25, g.getQuantiteEnStock());
        assertEquals(peremption, g.getDatePeremption());
        assertEquals(StatutLivraison.LIVREE, reg.store.commandes.findById(c.getId()).orElseThrow().getStatutLivraison());
        assertThrows(ValidationException.class, () -> stock.receptionnerCommande(c.getId(), null));
        assertThrows(ValidationException.class, () -> stock.supprimerCommande(c.getId()));

        CommandeFournisseur c2 = stock.passerCommande(new CommandeFournisseur(null, GADO, "X", 5, LocalDate.now(), null, 100, "FF-X-01"));
        stock.annulerCommande(c2.getId());
        assertEquals(900, stock.depensesReapprovisionnement(YearMonth.now()), 1e-9); // annulée exclue
    }

    @Test
    void consommableUtiliseNePeutEtreSupprime() {
        assertThrows(ValidationException.class, () -> stock.supprimerConsommable(GADO));
    }
}
