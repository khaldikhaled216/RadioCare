package com.radiocare.view;

import com.radiocare.controller.StockController;
import com.radiocare.model.CommandeFournisseur;
import com.radiocare.model.ConsommableMedical;
import com.radiocare.model.enums.CategorieConsommable;
import com.radiocare.model.enums.StatutLivraison;

import java.time.LocalDate;
import java.util.List;

import static com.radiocare.view.Console.*;

/** Vue console F8 : stocks de consommables médicaux et commandes fournisseurs. */
public class StockView {
    private final Console console;
    private final StockController ctrl;

    public StockView(Console console, StockController ctrl) {
        this.console = console;
        this.ctrl = ctrl;
    }

    public void afficher() {
        new Menu(console, "STOCKS DE CONSOMMABLES & RÉAPPROVISIONNEMENT (F8)")
                .option("État du stock", this::lister)
                .option("Alertes : stock critique et péremption", this::alertes)
                .option("Stock prévisionnel d'un produit à une date", this::previsionnel)
                .option("Ajouter un consommable", () -> saisir(null))
                .option("Modifier un consommable (inventaire)", () -> saisir(selectionner()))
                .option("Supprimer un consommable", this::supprimer)
                .option("Lister les commandes fournisseurs", this::listerCommandes)
                .option("Passer une commande fournisseur", this::commander)
                .option("Réceptionner une commande (réintégration en stock)", this::receptionner)
                .option("Annuler une commande", this::annuler)
                .option("Supprimer une commande", this::supprimerCommande)
                .executer();
    }

    private void lister() {
        LocalDate limite = LocalDate.now().plusDays(30);
        Tableau.de(ctrl.consommables())
                .col("ID", ConsommableMedical::getId).col("Désignation", ConsommableMedical::getDesignation, 32)
                .col("Catégorie", c -> c.getCategorie().getLibelle())
                .colD("Stock", ConsommableMedical::getQuantiteEnStock).colD("Seuil", ConsommableMedical::getSeuilAlerte)
                .colD("PU HT", c -> tnd(c.getPrixUnitaireHT())).colD("Valeur", c -> tnd(c.getValeurStock()))
                .col("Péremption", c -> date(c.getDatePeremption()))
                .col("État", c -> c.isPerimeA(LocalDate.now()) ? console.rouge("PÉRIMÉ")
                        : c.isStockCritique() ? console.rouge("CRITIQUE") : c.expireAvant(limite) ? console.jaune("péremption proche") : console.vert("OK"))
                .afficher(console);
    }

    private void alertes() {
        List<ConsommableMedical> crit = ctrl.stockCritique();
        console.sousTitre("Stock critique (quantité < seuil d'alerte)");
        Tableau.de(crit).col("ID", ConsommableMedical::getId).col("Désignation", ConsommableMedical::getDesignation)
                .colD("Stock", ConsommableMedical::getQuantiteEnStock).colD("Seuil", ConsommableMedical::getSeuilAlerte)
                .colD("Réappro. suggéré", ctrl::quantiteSuggeree).afficher(console);
        console.sousTitre("Péremption (périmés ou dans les 30 jours)");
        Tableau.de(ctrl.peremptions()).col("ID", ConsommableMedical::getId).col("Désignation", ConsommableMedical::getDesignation)
                .col("Péremption", c -> date(c.getDatePeremption())).colD("Stock", ConsommableMedical::getQuantiteEnStock).afficher(console);
        console.pause();
    }

    private ConsommableMedical selectionner() {
        return Selection.parRecherche(console, "Consommable", ctrl.consommables(), ConsommableMedical::getId, ConsommableMedical::getDesignation);
    }

    private void previsionnel() {
        ConsommableMedical c = selectionner();
        LocalDate d = console.lireDate("À la date du", LocalDate.now().plusDays(7));
        int p = ctrl.stockPrevisionnel(c.getId(), d);
        console.champ("Stock actuel", c.getQuantiteEnStock());
        console.champ("Stock prévisionnel au " + date(d), p <= 0 ? console.rouge(String.valueOf(p)) : String.valueOf(p));
        console.info("Prévisionnel = stock (si non périmé) + livraisons attendues − réservations des RDV planifiés.");
    }

    private void saisir(ConsommableMedical ex) {
        ConsommableMedical c = new ConsommableMedical();
        if (ex != null) {
            c.setIdProduit(ex.getId()); c.setDesignation(ex.getDesignation()); c.setCategorie(ex.getCategorie());
            c.setQuantiteEnStock(ex.getQuantiteEnStock()); c.setSeuilAlerte(ex.getSeuilAlerte());
            c.setPrixUnitaireHT(ex.getPrixUnitaireHT()); c.setDatePeremption(ex.getDatePeremption());
            console.info("Entrée = conserver la valeur actuelle.");
        }
        c.setDesignation(console.lireTexte("Désignation", c.getDesignation()));
        c.setCategorie(console.choisirEnum("Catégorie", CategorieConsommable.class, c.getCategorie()));
        c.setQuantiteEnStock(console.lireEntier("Quantité en stock", c.getQuantiteEnStock(), 0, 1_000_000));
        c.setSeuilAlerte(console.lireEntier("Seuil d'alerte", ex == null ? 10 : c.getSeuilAlerte(), 0, 1_000_000));
        c.setPrixUnitaireHT(console.lireDecimal("Prix unitaire HT (TND)", ex == null ? null : c.getPrixUnitaireHT(), 0.001, 1_000_000));
        c.setDatePeremption(console.lireDateOptionnelle("Date de péremption", c.getDatePeremption()));
        if (ex == null) ctrl.ajouter(c); else ctrl.modifier(c);
        console.succes("Consommable " + c.getId() + " enregistré.");
        if (c.isStockCritique()) console.avertissement("Stock sous le seuil d'alerte : pensez à passer commande.");
    }

    private void supprimer() {
        ConsommableMedical c = selectionner();
        if (console.confirmer("Supprimer " + c.getDesignation() + " ?", false)) { ctrl.supprimer(c.getId()); console.succes("Consommable supprimé."); }
    }

    private void listerCommandes() {
        Tableau.de(ctrl.commandes())
                .col("ID", CommandeFournisseur::getId).col("Produit", CommandeFournisseur::getIdProduit)
                .col("Fournisseur", CommandeFournisseur::getNomFournisseur).colD("Qté", CommandeFournisseur::getQuantiteCommandee)
                .col("Commandée le", c -> date(c.getDateCommande())).col("Livraison prévue", c -> date(c.getDateLivraisonPrevue()))
                .colD("Montant", c -> tnd(c.getMontantTotal())).col("Réf. facture", CommandeFournisseur::getRefFactureTextuelle)
                .col("Statut", c -> c.getStatutLivraison().getLibelle())
                .afficher(console);
    }

    private void commander() {
        ConsommableMedical c = selectionner();
        CommandeFournisseur cmd = new CommandeFournisseur();
        cmd.setIdProduit(c.getId());
        cmd.setNomFournisseur(console.lireTexte("Fournisseur"));
        cmd.setQuantiteCommandee(console.lireEntier("Quantité", Math.max(1, ctrl.quantiteSuggeree(c)), 1, 1_000_000));
        cmd.setDateCommande(console.lireDate("Date de commande", LocalDate.now()));
        cmd.setDateLivraisonPrevue(console.lireDateOptionnelle("Date de livraison prévue", cmd.getDateCommande().plusDays(5)));
        double defaut = cmd.getQuantiteCommandee() * c.getPrixUnitaireHT();
        cmd.setMontantTotal(console.lireDecimal("Montant total (TND)", Math.round(defaut * 1000) / 1000.0, 0.001, 100_000_000));
        cmd.setRefFactureTextuelle(console.lireTexte("Référence normée du bon / de la facture fournisseur (ex. FF-PHARMA-2026-0042)"));
        ctrl.commander(cmd);
        console.succes("Commande " + cmd.getId() + " enregistrée (dépense intégrée à la comptabilité du mois).");
    }

    private CommandeFournisseur selectionnerCommande(boolean enCours) {
        List<CommandeFournisseur> l = ctrl.commandes().stream().filter(c -> !enCours || c.getStatutLivraison() == StatutLivraison.EN_COURS).toList();
        return Selection.parRecherche(console, "Commande", l, CommandeFournisseur::getId,
                c -> c.getNomFournisseur() + " — " + c.getQuantiteCommandee() + " × " + c.getIdProduit() + " (" + c.getStatutLivraison().getLibelle() + ")");
    }

    private void receptionner() {
        CommandeFournisseur c = selectionnerCommande(true);
        LocalDate p = console.lireDateOptionnelle("Date de péremption du lot livré", null);
        ctrl.receptionner(c.getId(), p);
        console.succes("Commande réceptionnée : +" + c.getQuantiteCommandee() + " en stock.");
    }

    private void annuler() {
        CommandeFournisseur c = selectionnerCommande(true);
        if (console.confirmer("Annuler la commande " + c.getId() + " ?", false)) { ctrl.annuler(c.getId()); console.succes("Commande annulée."); }
    }

    private void supprimerCommande() {
        CommandeFournisseur c = selectionnerCommande(false);
        if (console.confirmer("Supprimer la commande " + c.getId() + " ?", false)) { ctrl.supprimerCommande(c.getId()); console.succes("Commande supprimée."); }
    }
}
