package com.radiocare.view.fx.pages;

import com.radiocare.model.CommandeFournisseur;
import com.radiocare.model.ConsommableMedical;
import com.radiocare.model.enums.CategorieConsommable;
import com.radiocare.model.enums.StatutLivraison;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Formulaire;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/** F8 : stocks de consommables, alertes et commandes fournisseurs. */
public class StockPage extends Page {
    private int onglet = 0;

    public StockPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Stocks de consommables & commandes"; }

    @Override
    public Node construire() {
        TabPane tabs = new TabPane(new Tab("Consommables", consommables()), new Tab("Commandes fournisseurs", commandes()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(onglet);
        tabs.getSelectionModel().selectedIndexProperty().addListener((o, a, n) -> onglet = n.intValue());
        return tabs;
    }

    private Node consommables() {
        LocalDate limite = LocalDate.now().plusDays(30);
        List<ConsommableMedical> liste = ctx.stock.consommables();
        TableView<ConsommableMedical> t = Ui.table(liste);
        Ui.col(t, "ID", ConsommableMedical::getId, 80);
        Ui.col(t, "Désignation", ConsommableMedical::getDesignation, 220);
        Ui.col(t, "Catégorie", c -> c.getCategorie().getLibelle(), 190);
        Ui.col(t, "Stock", ConsommableMedical::getQuantiteEnStock, 60);
        Ui.col(t, "Seuil", ConsommableMedical::getSeuilAlerte, 60);
        Ui.col(t, "Prix unitaire HT", c -> Ui.tnd(c.getPrixUnitaireHT()), 110);
        Ui.col(t, "Valeur", c -> Ui.tnd(c.getValeurStock()), 110);
        Ui.col(t, "Péremption", c -> Ui.date(c.getDatePeremption()), 90);
        Ui.colPastille(t, "État", c -> c.isPerimeA(LocalDate.now()) ? "Périmé" : c.isStockCritique() ? "Stock critique"
                        : c.expireAvant(limite) ? "Péremption proche" : "OK",
                c -> c.isPerimeA(LocalDate.now()) || c.isStockCritique() ? "p-rouge" : c.expireAvant(limite) ? "p-orange" : "p-vert", 130);

        long critiques = ctx.stock.stockCritique().size();
        long peremption = ctx.stock.peremptions().size();
        double valeur = liste.stream().mapToDouble(ConsommableMedical::getValeurStock).sum();
        HBox kpis = new HBox(12, Ui.kpi("Valeur du stock", Ui.tnd(valeur), liste.size() + " références", "kpi-accent"),
                Ui.kpi("Stock critique", String.valueOf(critiques), "sous le seuil d'alerte", critiques > 0 ? "kpi-danger" : "kpi-accent"),
                Ui.kpi("Péremption < 30 jours", String.valueOf(peremption), "à surveiller", peremption > 0 ? "kpi-alerte" : "kpi-accent"));
        return new VBox(12, kpis, new VBox(Ui.barre(Ui.primaire("+ Nouveau consommable", () -> formConso(null)),
                Ui.bouton("Modifier / inventaire", () -> Ui.executer(() -> formConso(Ui.selection(t, "un consommable")))),
                Ui.bouton("Stock prévisionnel", () -> Ui.executer(() -> previsionnel(Ui.selection(t, "un consommable")))),
                Ui.bouton("Commander", () -> Ui.executer(() -> commander(Ui.selection(t, "un consommable")))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    ConsommableMedical c = Ui.selection(t, "un consommable");
                    if (Ui.confirmer("Supprimer " + c.getDesignation() + " ?")) { ctx.stock.supprimer(c.getId()); ctx.rafraichir(); }
                }))), t));
    }

    private void formConso(ConsommableMedical ex) {
        Formulaire f = new Formulaire(ex == null ? "Nouveau consommable" : "Modifier " + ex.getDesignation());
        TextField des = f.texte("Désignation", ex == null ? null : ex.getDesignation());
        ComboBox<CategorieConsommable> cat = f.choix("Catégorie", List.of(CategorieConsommable.values()), ex == null ? null : ex.getCategorie(), CategorieConsommable::getLibelle);
        TextField qte = f.nombre("Quantité en stock", ex == null ? 0.0 : ex.getQuantiteEnStock());
        TextField seuil = f.nombre("Seuil d'alerte", ex == null ? 10.0 : ex.getSeuilAlerte());
        TextField prix = f.nombre("Prix unitaire HT (TND)", ex == null ? null : ex.getPrixUnitaireHT());
        DatePicker per = f.date("Date de péremption", ex == null ? null : ex.getDatePeremption());
        if (f.afficher(() -> {
            ConsommableMedical c = new ConsommableMedical();
            if (ex != null) c.setIdProduit(ex.getId());
            c.setDesignation(des.getText().trim());
            c.setCategorie(Formulaire.valeur(cat, "Catégorie"));
            c.setQuantiteEnStock(Formulaire.entier(qte, "Quantité"));
            c.setSeuilAlerte(Formulaire.entier(seuil, "Seuil d'alerte"));
            c.setPrixUnitaireHT(Formulaire.decimal(prix, "Prix unitaire"));
            c.setDatePeremption(Formulaire.dateOuNull(per));
            if (ex == null) ctx.stock.ajouter(c); else ctx.stock.modifier(c);
        })) { ctx.notifier("Consommable enregistré."); ctx.rafraichir(); }
    }

    private void previsionnel(ConsommableMedical c) {
        Formulaire f = new Formulaire("Stock prévisionnel — " + c.getDesignation());
        DatePicker d = f.date("À la date du", LocalDate.now().plusDays(7));
        f.note("Prévisionnel = stock (si non périmé) + livraisons attendues − réservations des RDV planifiés.");
        f.afficher(() -> {
            LocalDate date = Formulaire.date(d, "Date");
            Ui.info("Stock prévisionnel", c.getDesignation() + "\n\nStock actuel : " + c.getQuantiteEnStock()
                    + "\nStock prévisionnel au " + Ui.date(date) + " : " + ctx.stock.stockPrevisionnel(c.getId(), date));
        });
    }

    private void commander(ConsommableMedical c) {
        Formulaire f = new Formulaire("Commande fournisseur — " + c.getDesignation());
        TextField fournisseur = f.texte("Fournisseur", null);
        TextField qte = f.nombre("Quantité", (double) Math.max(1, ctx.stock.quantiteSuggeree(c)));
        DatePicker date = f.date("Date de commande", LocalDate.now());
        DatePicker livraison = f.date("Livraison prévue", LocalDate.now().plusDays(5));
        TextField montant = f.nombre("Montant total (TND, vide = calculé)", null);
        TextField ref = f.texte("Réf. bon / facture fournisseur", null);
        f.note("Référence normée, ex. FF-PHARMADIS-2026-0042");
        if (f.afficher(() -> {
            CommandeFournisseur cmd = new CommandeFournisseur();
            cmd.setIdProduit(c.getId());
            cmd.setNomFournisseur(fournisseur.getText().trim());
            cmd.setQuantiteCommandee(Formulaire.entier(qte, "Quantité"));
            cmd.setDateCommande(Formulaire.date(date, "Date de commande"));
            cmd.setDateLivraisonPrevue(Formulaire.dateOuNull(livraison));
            cmd.setMontantTotal(montant.getText().isBlank() ? 0 : Formulaire.decimal(montant, "Montant"));
            cmd.setRefFactureTextuelle(ref.getText());
            ctx.stock.commander(cmd);
        })) { onglet = 1; ctx.notifier("Commande enregistrée (dépense intégrée à la comptabilité)."); ctx.rafraichir(); }
    }

    private Node commandes() {
        TableView<CommandeFournisseur> t = Ui.table(ctx.stock.commandes());
        Ui.col(t, "ID", CommandeFournisseur::getId, 80);
        Ui.col(t, "Produit", CommandeFournisseur::getIdProduit, 80);
        Ui.col(t, "Fournisseur", CommandeFournisseur::getNomFournisseur, 150);
        Ui.col(t, "Quantité", CommandeFournisseur::getQuantiteCommandee, 70);
        Ui.col(t, "Commandée le", c -> Ui.date(c.getDateCommande()), 100);
        Ui.col(t, "Livraison prévue", c -> Ui.date(c.getDateLivraisonPrevue()), 110);
        Ui.col(t, "Montant", c -> Ui.tnd(c.getMontantTotal()), 110);
        Ui.col(t, "Réf. facture", CommandeFournisseur::getRefFactureTextuelle, 170);
        Ui.colPastille(t, "Statut", c -> c.getStatutLivraison().getLibelle(), c -> switch (c.getStatutLivraison()) {
            case EN_COURS -> "p-bleu";
            case LIVREE -> "p-vert";
            case ANNULEE -> "p-gris";
        }, 90);
        return new VBox(Ui.barre(
                Ui.primaire("Réceptionner", () -> Ui.executer(() -> receptionner(Ui.selection(t, "une commande")))),
                Ui.bouton("Annuler la commande", () -> Ui.executer(() -> {
                    CommandeFournisseur c = Ui.selection(t, "une commande");
                    if (Ui.confirmer("Annuler la commande " + c.getId() + " ?")) { ctx.stock.annuler(c.getId()); ctx.rafraichir(); }
                })),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    CommandeFournisseur c = Ui.selection(t, "une commande");
                    if (Ui.confirmer("Supprimer la commande " + c.getId() + " ?")) { ctx.stock.supprimerCommande(c.getId()); ctx.rafraichir(); }
                }))), t);
    }

    private void receptionner(CommandeFournisseur c) {
        if (c.getStatutLivraison() != StatutLivraison.EN_COURS) throw new IllegalArgumentException("Cette commande n'est plus en cours.");
        Formulaire f = new Formulaire("Réception de la commande " + c.getId());
        f.note(c.getQuantiteCommandee() + " unité(s) de " + c.getIdProduit() + " seront ajoutées au stock.");
        DatePicker per = f.date("Péremption du lot livré", null);
        if (f.afficher(() -> ctx.stock.receptionner(c.getId(), Formulaire.dateOuNull(per)))) {
            ctx.notifier("Commande réceptionnée, stock mis à jour.");
            ctx.rafraichir();
        }
    }
}
