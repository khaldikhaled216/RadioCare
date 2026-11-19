package com.radiocare.controller;

import com.radiocare.model.FactureMedicale;
import com.radiocare.model.Paiement;
import com.radiocare.model.enums.ModePaiement;
import com.radiocare.service.ComptabiliteService;
import com.radiocare.service.FacturationService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Coordination Vue ↔ Services pour F5 (facturation, paiements, bilan financier). */
public class FacturationController {
    private final FacturationService facturation;
    private final ComptabiliteService comptabilite;

    public FacturationController(FacturationService facturation, ComptabiliteService comptabilite) {
        this.facturation = facturation;
        this.comptabilite = comptabilite;
    }

    public List<FactureMedicale> factures() { facturation.actualiserStatuts(LocalDate.now()); return facturation.listerFactures(); }
    public FactureMedicale facture(String num) { return facturation.trouverFacture(num); }
    public void supprimerFacture(String num) { facturation.supprimerFacture(num); }
    public List<Paiement> paiements() { return facturation.listerPaiements(); }
    public List<Paiement> paiementsDe(String num) { return facturation.paiementsDeFacture(num); }
    public Paiement payer(String num, LocalDate date, double montant, ModePaiement mode, String preuve) {
        return facturation.enregistrerPaiement(num, date, montant, mode, preuve);
    }
    public void supprimerPaiement(String id) { facturation.supprimerPaiement(id); }

    public ComptabiliteService.BilanMensuel bilan(YearMonth mois) { return comptabilite.bilan(mois); }
    public List<ComptabiliteService.BilanMensuel> historique(YearMonth fin, int n) { return comptabilite.historique(fin, n); }
}
