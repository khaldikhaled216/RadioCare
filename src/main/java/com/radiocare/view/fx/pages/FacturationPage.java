package com.radiocare.view.fx.pages;

import com.radiocare.model.FactureMedicale;
import com.radiocare.model.Paiement;
import com.radiocare.model.enums.ModePaiement;
import com.radiocare.model.enums.StatutPaiement;
import com.radiocare.service.ComptabiliteService.BilanMensuel;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Formulaire;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** F5 : factures (TVA 19 %, tiers payant), paiements et bilan financier mensuel. */
public class FacturationPage extends Page {
    private int onglet = 0;
    private String filtre = "Toutes";
    private YearMonth moisBilan = YearMonth.now();

    public FacturationPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Facturation, paiements & bilan financier"; }

    @Override
    public Node construire() {
        TabPane tabs = new TabPane(new Tab("Factures", factures()), new Tab("Paiements", paiements()), new Tab("Bilan financier", bilan()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(onglet);
        tabs.getSelectionModel().selectedIndexProperty().addListener((o, a, n) -> onglet = n.intValue());
        return tabs;
    }

    private String couleur(StatutPaiement s) {
        return switch (s) {
            case REGLEE -> "p-vert";
            case EN_ATTENTE -> "p-orange";
            case RETARD -> "p-rouge";
        };
    }

    private Node factures() {
        List<FactureMedicale> toutes = ctx.facturation.factures();
        List<FactureMedicale> liste = switch (filtre) {
            case "Impayées" -> toutes.stream().filter(f -> f.getStatutPaiement() != StatutPaiement.REGLEE).toList();
            case "En retard" -> toutes.stream().filter(f -> f.getStatutPaiement() == StatutPaiement.RETARD).toList();
            default -> toutes;
        };
        TableView<FactureMedicale> t = Ui.table(liste);
        Ui.col(t, "N° facture", FactureMedicale::getNumFacture, 90);
        Ui.col(t, "Émise le", f -> Ui.date(f.getDateEmission()), 90);
        Ui.col(t, "Patient", f -> ctx.rendezVous.detail(f.getIdRdv()).patient(), 140);
        Ui.col(t, "Examen", f -> ctx.rendezVous.detail(f.getIdRdv()).examen(), 170);
        Ui.col(t, "HT", f -> Ui.tnd(f.getMontantHT()), 100);
        Ui.col(t, "TTC", f -> Ui.tnd(f.getMontantTTC()), 100);
        Ui.col(t, "Assurance", f -> Ui.tnd(f.getPartAssurance()), 100);
        Ui.col(t, "Part patient", f -> Ui.tnd(f.getPartPatient()), 100);
        Ui.col(t, "Reste", f -> Ui.tnd(f.getResteAPayer()), 100);
        Ui.colPastille(t, "Statut", f -> f.getStatutPaiement().getLibelle(), f -> couleur(f.getStatutPaiement()), 90);

        ComboBox<String> choix = new ComboBox<>();
        choix.getItems().addAll("Toutes", "Impayées", "En retard");
        choix.setValue(filtre);
        choix.setOnAction(e -> { filtre = choix.getValue(); ctx.rafraichir(); });
        return new VBox(Ui.barre(new Label("Afficher :"), choix, Ui.espace(),
                Ui.primaire("Enregistrer un paiement", () -> Ui.executer(() -> payer(Ui.selection(t, "une facture")))),
                Ui.bouton("Détail", () -> Ui.executer(() -> detail(Ui.selection(t, "une facture")))),
                Ui.danger("Annuler la facture", () -> Ui.executer(() -> {
                    FactureMedicale f = Ui.selection(t, "une facture");
                    if (Ui.confirmer("Annuler la facture " + f.getNumFacture() + " ?")) { ctx.facturation.supprimerFacture(f.getNumFacture()); ctx.rafraichir(); }
                }))), t);
    }

    private void payer(FactureMedicale f) {
        if (f.getResteAPayer() <= 0) throw new IllegalArgumentException("Cette facture est déjà réglée.");
        Formulaire form = new Formulaire("Paiement de la facture " + f.getNumFacture());
        form.note("Reste à payer : " + Ui.tnd(f.getResteAPayer()));
        TextField montant = form.nombre("Montant reçu (TND)", f.getResteAPayer());
        ComboBox<ModePaiement> mode = form.choix("Mode de paiement", List.of(ModePaiement.values()), ModePaiement.ESPECES, ModePaiement::getLibelle);
        TextField preuve = form.texte("Référence de la preuve", null);
        form.note("La référence commence par le préfixe du mode : REC- (espèces), CHQ- (chèque), CB- (carte), VIR- (virement). Ex. CHQ-004512");
        DatePicker date = form.date("Date du paiement", LocalDate.now());
        if (form.afficher(() -> ctx.facturation.payer(f.getNumFacture(), Formulaire.date(date, "Date"),
                Formulaire.decimal(montant, "Montant"), Formulaire.valeur(mode, "Mode de paiement"), preuve.getText()))) {
            ctx.notifier("Paiement enregistré, solde du patient mis à jour.");
            ctx.rafraichir();
        }
    }

    private void detail(FactureMedicale f) {
        var l = ctx.rendezVous.detail(f.getIdRdv());
        VBox contenu = new VBox(12, Ui.fiche("Patient", l.patient() + " (" + f.getIdPatient() + ")", "Acte", l.examen() + " du " + Ui.date(l.date()),
                "Émission / échéance", Ui.date(f.getDateEmission()) + " / " + Ui.date(f.getDateEcheance()),
                "Montant HT", Ui.tnd(f.getMontantHT()), "TVA 19 %", Ui.tnd(f.getMontantTVA()), "Montant TTC", Ui.tnd(f.getMontantTTC()),
                "Part assurance", Ui.tnd(f.getPartAssurance()), "Part patient", Ui.tnd(f.getPartPatient()),
                "Déjà payé", Ui.tnd(f.getMontantPaye()), "Reste à payer", Ui.tnd(f.getResteAPayer()), "Statut", f.getStatutPaiement().getLibelle()));
        List<Paiement> p = ctx.facturation.paiementsDe(f.getNumFacture());
        if (!p.isEmpty()) {
            TableView<Paiement> t = tablePaiements(p);
            t.setPrefHeight(140);
            contenu.getChildren().addAll(Ui.section("Paiements"), t);
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Facture " + f.getNumFacture());
        a.setHeaderText("Facture " + f.getNumFacture());
        a.getDialogPane().setContent(contenu);
        a.getDialogPane().setPrefWidth(640);
        a.showAndWait();
    }

    private TableView<Paiement> tablePaiements(List<Paiement> l) {
        TableView<Paiement> t = Ui.table(l);
        Ui.col(t, "ID", Paiement::getId, 80);
        Ui.col(t, "Facture", Paiement::getNumFacture, 90);
        Ui.col(t, "Date", p -> Ui.date(p.getDatePaiement()), 90);
        Ui.col(t, "Montant", p -> Ui.tnd(p.getMontantRecu()), 110);
        Ui.col(t, "Mode", p -> p.getModePaiement().getLibelle(), 80);
        Ui.col(t, "Preuve", Paiement::getReferenceTextuellePreuve, 140);
        return t;
    }

    private Node paiements() {
        TableView<Paiement> t = tablePaiements(ctx.facturation.paiements());
        return new VBox(Ui.barre(Ui.danger("Annuler le paiement (erreur de saisie)", () -> Ui.executer(() -> {
            Paiement p = Ui.selection(t, "un paiement");
            if (Ui.confirmer("Annuler le paiement " + p.getId() + " de " + Ui.tnd(p.getMontantRecu()) + " ?")) {
                ctx.facturation.supprimerPaiement(p.getId());
                ctx.notifier("Paiement annulé, facture et solde recalculés.");
                ctx.rafraichir();
            }
        }))), t);
    }

    private Node bilan() {
        ComboBox<YearMonth> mois = new ComboBox<>();
        for (int i = 0; i < 12; i++) mois.getItems().add(YearMonth.now().minusMonths(i));
        mois.setValue(moisBilan);
        mois.setOnAction(e -> { moisBilan = mois.getValue(); ctx.rafraichir(); });
        BilanMensuel b = ctx.facturation.bilan(moisBilan);

        HBox ligne1 = new HBox(12, Ui.kpi("CA HT", Ui.tnd(b.chiffreAffairesHT()), "actes facturés", "kpi-accent"),
                Ui.kpi("TVA collectée (19 %)", Ui.tnd(b.tvaCollectee()), "à reverser", "kpi-accent"),
                Ui.kpi("CA TTC", Ui.tnd(b.chiffreAffairesTTC()), "", "kpi-accent"),
                Ui.kpi("Résultat", Ui.tnd(b.resultat()), "recettes − dépenses", b.resultat() >= 0 ? "kpi-accent" : "kpi-danger"));
        GridPane recettes = Ui.fiche("Encaissements patients", Ui.tnd(b.recettesPatients()), "Parts assurances (tiers payant)", Ui.tnd(b.recettesAssurances()),
                "Total recettes", Ui.tnd(b.totalRecettes()));
        GridPane depenses = Ui.fiche("Maintenance des équipements", Ui.tnd(b.depensesMaintenance()), "Salaires manipulateurs", Ui.tnd(b.salairesManipulateurs()),
                "Honoraires médecins", Ui.tnd(b.honorairesMedecins()), "Honoraires experts externes", Ui.tnd(b.honorairesExperts()),
                "Achats de consommables", Ui.tnd(b.achatsConsommables()), "Total dépenses", Ui.tnd(b.totalDepenses()));
        VBox c1 = Ui.carte(Ui.section("Recettes"), recettes);
        VBox c2 = Ui.carte(Ui.section("Dépenses"), depenses);
        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> g = new BarChart<>(x, y);
        g.setTitle("Évolution sur 6 mois (TND)");
        XYChart.Series<String, Number> r = new XYChart.Series<>();
        r.setName("Recettes");
        XYChart.Series<String, Number> d = new XYChart.Series<>();
        d.setName("Dépenses");
        for (BilanMensuel m : ctx.facturation.historique(moisBilan, 6)) {
            r.getData().add(new XYChart.Data<>(m.mois(), m.totalRecettes()));
            d.getData().add(new XYChart.Data<>(m.mois(), m.totalDepenses()));
        }
        g.getData().addAll(List.of(r, d));
        g.setPrefHeight(300);

        VBox page = new VBox(14, Ui.barre(new Label("Mois :"), mois), ligne1, new HBox(14, c1, c2), Ui.carte(g));
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #f2f5f8;");
        return sp;
    }
}
