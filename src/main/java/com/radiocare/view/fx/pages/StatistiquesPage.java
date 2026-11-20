package com.radiocare.view.fx.pages;

import com.radiocare.model.ConflitJournal;
import com.radiocare.service.StatistiqueService.Occupation;
import com.radiocare.service.StatistiqueService.RapportActivite;
import com.radiocare.service.StatistiqueService.Rentabilite;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** F10 : tableaux de bord analytiques, taux d'occupation, rentabilité et export des rapports. */
public class StatistiquesPage extends Page {
    private YearMonth mois = YearMonth.now();

    public StatistiquesPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Statistiques & rapports"; }

    @Override
    public Node construire() {
        RapportActivite r = ctx.statistiques.rapport(mois);
        ComboBox<YearMonth> choix = new ComboBox<>();
        for (int i = 0; i < 12; i++) choix.getItems().add(YearMonth.now().minusMonths(i));
        choix.setValue(mois);
        choix.setOnAction(e -> { mois = choix.getValue(); ctx.rafraichir(); });
        HBox outils = Ui.barre(new Label("Mois analysé :"), choix, Ui.espace(),
                Ui.primaire("Exporter le rapport (JSON + texte)", () -> Ui.executer(() -> {
                    List<Path> f = ctx.statistiques.exporter(mois);
                    Ui.info("Rapport exporté", "Fichiers créés :\n" + f.get(0).toAbsolutePath() + "\n" + f.get(1).toAbsolutePath());
                })));

        long realises = r.rdvParStatut().getOrDefault("Réalisé", 0L);
        long conflits = r.conflitsParType().values().stream().mapToLong(Long::longValue).sum();
        HBox kpis = new HBox(12, Ui.kpi("Rendez-vous", String.valueOf(r.rdvTotal()), realises + " réalisés", "kpi-accent"),
                Ui.kpi("Attente moyenne en salle", Ui.nombre(r.attenteMoyenneMinutes()) + " min", "arrivée → prise en charge", "kpi-accent"),
                Ui.kpi("Délai moyen d'obtention", Ui.nombre(r.delaiMoyenObtentionJours()) + " jours", "prise de RDV → examen", "kpi-accent"),
                Ui.kpi("Conflits interceptés", String.valueOf(conflits), "par le moteur ce mois-ci", conflits > 0 ? "kpi-alerte" : "kpi-accent"));

        BarChart<String, Number> salles = barres("Taux d'occupation par salle (%)", r.occupationSalles());
        BarChart<String, Number> types = barres("Taux de charge par type d'équipement (%)", r.occupationParTypeEquipement());
        VBox c1 = Ui.carte(salles);
        VBox c2 = Ui.carte(types);
        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);

        CategoryAxis xc = new CategoryAxis();
        NumberAxis yc = new NumberAxis();
        BarChart<Number, String> conflitsChart = new BarChart<>(yc, xc);
        conflitsChart.setTitle("Conflits interceptés par type");
        conflitsChart.setLegendVisible(false);
        XYChart.Series<Number, String> s = new XYChart.Series<>();
        for (Map.Entry<String, Long> e : r.conflitsParType().entrySet())
            s.getData().add(new XYChart.Data<>(e.getValue(), e.getKey().replace("Conflit d'expertise : ", "Expertise : ")));
        conflitsChart.getData().add(s);
        conflitsChart.setPrefHeight(300);

        TableView<Rentabilite> rent = Ui.table(r.rentabilite());
        Ui.col(rent, "Code", Rentabilite::codeActe, 100);
        Ui.col(rent, "Examen", Rentabilite::libelle, 220);
        Ui.col(rent, "Actes", Rentabilite::nombreActes, 60);
        Ui.col(rent, "CA HT", x -> Ui.tnd(x.chiffreAffairesHT()), 120);
        Ui.col(rent, "Coût consommables", x -> Ui.tnd(x.coutConsommables()), 130);
        Ui.col(rent, "Marge", x -> Ui.tnd(x.marge()), 120);
        Ui.col(rent, "Taux de marge", x -> Ui.pct(x.tauxMarge()), 90);
        rent.setPrefHeight(300);

        TableView<ConflitJournal> journal = Ui.table(ctx.statistiques.journalConflits(100));
        Ui.col(journal, "Date", c -> Ui.dateHeure(c.getDateHeure()), 120);
        Ui.col(journal, "Type", c -> c.getType().getLibelle(), 280);
        Ui.col(journal, "Détail", ConflitJournal::getMessage, 420);
        Ui.col(journal, "Contexte", ConflitJournal::getContexte, 200);
        journal.setPrefHeight(260);

        VBox page = new VBox(14, outils, kpis, new HBox(14, c1, c2), Ui.carte(conflitsChart),
                Ui.carte(Ui.section("Rentabilité par type d'examen"), rent),
                Ui.carte(Ui.section("Journal des conflits interceptés (100 derniers)"), journal));
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #f2f5f8;");
        return sp;
    }

    private BarChart<String, Number> barres(String titre, List<Occupation> donnees) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 10);
        BarChart<String, Number> g = new BarChart<>(x, y);
        g.setTitle(titre);
        g.setLegendVisible(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (Occupation o : donnees) s.getData().add(new XYChart.Data<>(o.libelle(), o.taux()));
        g.getData().add(s);
        g.setPrefHeight(300);
        return g;
    }
}
