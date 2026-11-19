package com.radiocare.controller;

import com.radiocare.model.CommandeFournisseur;
import com.radiocare.model.ConsommableMedical;
import com.radiocare.service.StockService;

import java.time.LocalDate;
import java.util.List;

/** Coordination Vue ↔ Service pour F8 (stocks et commandes fournisseurs). */
public class StockController {
    private final StockService service;

    public StockController(StockService service) { this.service = service; }

    public List<ConsommableMedical> consommables() { return service.listerConsommables(); }
    public ConsommableMedical ajouter(ConsommableMedical c) { c.setIdProduit(null); return service.creerConsommable(c); }
    public ConsommableMedical modifier(ConsommableMedical c) { return service.modifierConsommable(c); }
    public void supprimer(String id) { service.supprimerConsommable(id); }
    public List<ConsommableMedical> stockCritique() { return service.alertesStockCritique(); }
    public List<ConsommableMedical> peremptions() { return service.alertesPeremption(LocalDate.now()); }
    public int quantiteSuggeree(ConsommableMedical c) { return service.quantiteSuggeree(c); }
    public int stockPrevisionnel(String id, LocalDate date) { return service.stockPrevisionnel(id, date, null); }

    public List<CommandeFournisseur> commandes() { return service.listerCommandes(); }
    public CommandeFournisseur commander(CommandeFournisseur c) { c.setIdCommande(null); return service.passerCommande(c); }
    public CommandeFournisseur modifierCommande(CommandeFournisseur c) { return service.modifierCommande(c); }
    public CommandeFournisseur receptionner(String id, LocalDate peremption) { return service.receptionnerCommande(id, peremption); }
    public CommandeFournisseur annuler(String id) { return service.annulerCommande(id); }
    public void supprimerCommande(String id) { service.supprimerCommande(id); }
}
