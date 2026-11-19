package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.CommandeFournisseur;
import com.radiocare.model.ConsommableMedical;
import com.radiocare.model.ExamenMedical;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.StatutLivraison;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/** F8 : gestion des stocks de consommables médicaux, alertes et commandes fournisseurs. */
public class StockService {
    private final DataStore store;

    public StockService(DataStore store) { this.store = store; }

    // ----------------------------------------------------------------- Consommables (CRUD)
    public List<ConsommableMedical> listerConsommables() { return store.consommables.findAll(); }

    public ConsommableMedical creerConsommable(ConsommableMedical c) {
        if (c.getIdProduit() == null || c.getIdProduit().isBlank()) c.setIdProduit(store.consommables.nextId());
        valider(c);
        return store.consommables.create(c);
    }

    public ConsommableMedical modifierConsommable(ConsommableMedical c) {
        valider(c);
        return store.consommables.update(c);
    }

    public void supprimerConsommable(String id) {
        if (store.examens.findAll().stream().anyMatch(e -> id.equals(e.getIdConsommableRequis())))
            throw new ValidationException("Ce consommable est utilisé par un examen du catalogue : suppression impossible.");
        if (store.commandes.findAll().stream().anyMatch(c -> id.equals(c.getIdProduit())))
            throw new ValidationException("Ce consommable possède des commandes fournisseurs : suppression impossible.");
        store.consommables.delete(id);
    }

    private void valider(ConsommableMedical c) {
        Validateur.obligatoire(c.getDesignation(), "Désignation");
        Validateur.nonNul(c.getCategorie(), "Catégorie");
        Validateur.positifOuNul(c.getQuantiteEnStock(), "Quantité en stock");
        Validateur.positifOuNul(c.getSeuilAlerte(), "Seuil d'alerte");
        Validateur.positif(c.getPrixUnitaireHT(), "Prix unitaire HT");
    }

    // ----------------------------------------------------------------- Mouvements
    /** Décrémente le stock lors de la réalisation d'un examen ; refuse si stock réel insuffisant ou lot périmé. */
    public void consommer(String idProduit, int quantite, LocalDate date) {
        ConsommableMedical c = store.consommables.findById(idProduit)
                .orElseThrow(() -> new ValidationException("Consommable inconnu : " + idProduit));
        if (c.isPerimeA(date))
            throw new ValidationException("Le lot de « " + c.getDesignation() + " » est périmé depuis le " + c.getDatePeremption() + ".");
        if (c.getQuantiteEnStock() < quantite)
            throw new ValidationException("Stock insuffisant de « " + c.getDesignation() + " » : " + c.getQuantiteEnStock() + " disponible(s), " + quantite + " requis.");
        c.setQuantiteEnStock(c.getQuantiteEnStock() - quantite);
        store.consommables.update(c);
    }

    /**
     * Stock prévisionnel d'un produit à une date :
     * stock actuel (0 si le lot est périmé à cette date)
     * + commandes en cours livrées au plus tard à cette date
     * − quantités réservées par les RDV actifs (planifiés / en cours) datés au plus tard à cette date.
     *
     * @param idRdvExclu RDV à ignorer (cas d'une modification), peut être null
     */
    public int stockPrevisionnel(String idProduit, LocalDate date, String idRdvExclu) {
        ConsommableMedical c = store.consommables.findById(idProduit).orElse(null);
        if (c == null) return 0;
        int stock = c.isPerimeA(date) ? 0 : c.getQuantiteEnStock();
        int livraisons = store.commandes.findWhere(cmd -> idProduit.equals(cmd.getIdProduit())
                        && cmd.getStatutLivraison() == StatutLivraison.EN_COURS
                        && cmd.getDateLivraisonPrevue() != null && !cmd.getDateLivraisonPrevue().isAfter(date)
                        && (cmd.getDatePeremptionLot() == null || !cmd.getDatePeremptionLot().isBefore(date)))
                .stream().mapToInt(CommandeFournisseur::getQuantiteCommandee).sum();
        int reserve = 0;
        for (RendezVous r : store.rendezVous.findWhere(r -> r.isActif() && !r.getDate().isAfter(date)
                && !Objects.equals(r.getIdRdv(), idRdvExclu))) {
            ExamenMedical ex = store.examens.findById(r.getIdExamen()).orElse(null);
            if (ex != null && idProduit.equals(ex.getIdConsommableRequis())) reserve += ex.getQuantiteConsommable();
        }
        return stock + livraisons - reserve;
    }

    // ----------------------------------------------------------------- Alertes
    public List<ConsommableMedical> alertesStockCritique() {
        return store.consommables.findWhere(ConsommableMedical::isStockCritique);
    }

    /** Produits périmés ou périmant dans le délai d'alerte. */
    public List<ConsommableMedical> alertesPeremption(LocalDate aujourdHui) {
        LocalDate limite = aujourdHui.plusDays(ParametresCentre.DELAI_ALERTE_PEREMPTION_JOURS);
        return store.consommables.findWhere(c -> c.expireAvant(limite));
    }

    /** Quantité de réapprovisionnement suggérée (remonter à deux fois le seuil d'alerte). */
    public int quantiteSuggeree(ConsommableMedical c) {
        return Math.max(0, 2 * c.getSeuilAlerte() - c.getQuantiteEnStock());
    }

    // ----------------------------------------------------------------- Commandes fournisseurs
    public List<CommandeFournisseur> listerCommandes() { return store.commandes.findAll(); }

    public CommandeFournisseur passerCommande(CommandeFournisseur cmd) {
        if (cmd.getIdCommande() == null || cmd.getIdCommande().isBlank()) cmd.setIdCommande(store.commandes.nextId());
        validerCommande(cmd);
        cmd.setStatutLivraison(StatutLivraison.EN_COURS);
        return store.commandes.create(cmd);
    }

    public CommandeFournisseur modifierCommande(CommandeFournisseur cmd) {
        CommandeFournisseur existante = store.commandes.findById(cmd.getIdCommande())
                .orElseThrow(() -> new ValidationException("Commande inconnue."));
        if (existante.getStatutLivraison() != StatutLivraison.EN_COURS)
            throw new ValidationException("Seule une commande en cours peut être modifiée.");
        cmd.setStatutLivraison(StatutLivraison.EN_COURS);
        validerCommande(cmd);
        return store.commandes.update(cmd);
    }

    private void validerCommande(CommandeFournisseur cmd) {
        ConsommableMedical produit = store.consommables.findById(cmd.getIdProduit())
                .orElseThrow(() -> new ValidationException("Produit inconnu."));
        Validateur.obligatoire(cmd.getNomFournisseur(), "Fournisseur");
        Validateur.positif(cmd.getQuantiteCommandee(), "Quantité commandée");
        Validateur.nonNul(cmd.getDateCommande(), "Date de commande");
        if (cmd.getDateLivraisonPrevue() != null && cmd.getDateLivraisonPrevue().isBefore(cmd.getDateCommande()))
            throw new ValidationException("La date de livraison prévue précède la date de commande.");
        if (cmd.getMontantTotal() <= 0) cmd.setMontantTotal(Validateur.arrondi(cmd.getQuantiteCommandee() * produit.getPrixUnitaireHT()));
        cmd.setRefFactureTextuelle(Validateur.reference(cmd.getRefFactureTextuelle(), "Référence facture fournisseur"));
    }

    /** Réception : réintégration de la quantité en stock (et mise à jour éventuelle de la péremption). */
    public CommandeFournisseur receptionnerCommande(String idCommande, LocalDate datePeremptionLot) {
        CommandeFournisseur cmd = store.commandes.findById(idCommande).orElseThrow(() -> new ValidationException("Commande inconnue."));
        if (cmd.getStatutLivraison() != StatutLivraison.EN_COURS)
            throw new ValidationException("La commande " + idCommande + " est déjà " + cmd.getStatutLivraison().getLibelle().toLowerCase() + ".");
        ConsommableMedical c = store.consommables.findById(cmd.getIdProduit()).orElseThrow(() -> new ValidationException("Produit inconnu."));
        boolean ancienLotPerime = c.isPerimeA(LocalDate.now());
        c.setQuantiteEnStock((ancienLotPerime ? 0 : c.getQuantiteEnStock()) + cmd.getQuantiteCommandee());
        LocalDate peremption = datePeremptionLot != null ? datePeremptionLot : cmd.getDatePeremptionLot();
        if (peremption != null) c.setDatePeremption(peremption);
        store.consommables.update(c);
        cmd.setDatePeremptionLot(peremption);
        cmd.setStatutLivraison(StatutLivraison.LIVREE);
        return store.commandes.update(cmd);
    }

    public CommandeFournisseur annulerCommande(String idCommande) {
        CommandeFournisseur cmd = store.commandes.findById(idCommande).orElseThrow(() -> new ValidationException("Commande inconnue."));
        if (cmd.getStatutLivraison() != StatutLivraison.EN_COURS)
            throw new ValidationException("Seule une commande en cours peut être annulée.");
        cmd.setStatutLivraison(StatutLivraison.ANNULEE);
        return store.commandes.update(cmd);
    }

    public void supprimerCommande(String idCommande) {
        CommandeFournisseur cmd = store.commandes.findById(idCommande).orElseThrow(() -> new ValidationException("Commande inconnue."));
        if (cmd.getStatutLivraison() == StatutLivraison.LIVREE)
            throw new ValidationException("Une commande livrée est intégrée à la comptabilité et ne peut être supprimée.");
        store.commandes.delete(idCommande);
    }

    /** Dépenses de réapprovisionnement du mois (commandes non annulées, par date de commande) — intégrées au F5. */
    public double depensesReapprovisionnement(YearMonth mois) {
        return store.commandes.findWhere(c -> c.getStatutLivraison() != StatutLivraison.ANNULEE
                && YearMonth.from(c.getDateCommande()).equals(mois)).stream().mapToDouble(CommandeFournisseur::getMontantTotal).sum();
    }
}
