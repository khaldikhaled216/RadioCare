package com.radiocare.view.fx.pages;

import com.radiocare.controller.TableauDeBordController.Indicateurs;
import com.radiocare.service.AlerteService.Alerte;
import com.radiocare.service.ComptabiliteService.BilanMensuel;
import com.radiocare.service.RendezVousService.LignePlanning;
import com.radiocare.service.StatistiqueService.Occupation;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.*;

import java.time.YearMonth;
import java.util.List;

/** Tableau de bord : indicateurs clés, planning du jour, graphiques et alertes prioritaires. */
public class TableauDeBordPage extends Page {
    public TableauDeBordPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Tableau de bord"; }

    @Override
    public Node construire() {
        Indicateurs i = ctx.tableauDeBord.indicateurs();
        HBox kpis = new HBox(12,
                Ui.kpi("Patients", String.valueOf(i.patients()), "dossiers enregistrés", "kpi-accent"),
                Ui.kpi("RDV aujourd'hui", String.valueOf(i.rdvAujourdhui()), i.rdvSemaineAVenir() + " planifiés sur 7 jours", "kpi-accent"),
                Ui.kpi("CA TTC du mois", Ui.tnd(i.caMoisTTC()), "encaissé : " + Ui.tnd(i.encaissementsMois()), "kpi-accent"),
                Ui.kpi("Résultat du mois", Ui.tnd(i.resultatMois()), "recettes − dépenses", i.resultatMois() >= 0 ? "kpi-accent" : "kpi-danger"),
                Ui.kpi("Alertes actives", String.valueOf(i.alertes()), i.conflitsInterceptes() + " conflits interceptés", i.alertes() > 0 ? "kpi-alerte" : "kpi-accent"));

        // planning du jour
        List<LignePlanning> jour = ctx.tableauDeBord.rdvDuJour();
        TableView<LignePlanning> t = Ui.table(jour);
        Ui.col(t, "Heure", LignePlanning::heure, 100);
        Ui.col(t, "Patient", LignePlanning::patient, 140);
        Ui.col(t, "Examen", LignePlanning::examen, 190);
        Ui.col(t, "Salle", LignePlanning::salle, 100);
        Ui.col(t, "Médecin", LignePlanning::medecin, 130);
        Ui.colPastille(t, "Statut", LignePlanning::statut, l -> PlanningPage.couleurStatut(l.rdv().getStatut()), 90);
        t.setPrefHeight(300);
        VBox planning = Ui.carte(Ui.section("Planning du jour (" + jour.size() + ")"), t);
        HBox.setHgrow(planning, Priority.ALWAYS);

        // alertes prioritaires
        VBox alertes = Ui.carte(Ui.section("Alertes prioritaires"));
        List<Alerte> liste = ctx.tableauDeBord.alertes();
        liste.stream().limit(6).forEach(a -> alertes.getChildren().add(AlertesPage.ligne(a)));
        if (liste.isEmpty()) alertes.getChildren().add(Ui.pastille("Aucune alerte", "p-vert"));
        if (liste.size() > 6) alertes.getChildren().add(Ui.bouton("Voir les " + liste.size() + " alertes", () -> ctx.naviguer("alertes")));
        alertes.setPrefWidth(470);
        alertes.setMinWidth(380);

        // graphiques
        YearMonth mois = YearMonth.now();
        CategoryAxis x1 = new CategoryAxis();
        NumberAxis y1 = new NumberAxis(0, 100, 10);
        y1.setLabel("%");
        BarChart<String, Number> occupation = new BarChart<>(x1, y1);
        occupation.setTitle("Taux d'occupation des salles — " + mois);
        occupation.setLegendVisible(false);
        XYChart.Series<String, Number> s1 = new XYChart.Series<>();
        for (Occupation o : ctx.statistiques.rapport(mois).occupationSalles()) s1.getData().add(new XYChart.Data<>(o.libelle(), o.taux()));
        occupation.getData().add(s1);
        occupation.setPrefHeight(280);

        CategoryAxis x2 = new CategoryAxis();
        NumberAxis y2 = new NumberAxis();
        y2.setLabel("TND");
        BarChart<String, Number> finances = new BarChart<>(x2, y2);
        finances.setTitle("Recettes et dépenses (6 derniers mois)");
        XYChart.Series<String, Number> rec = new XYChart.Series<>();
        rec.setName("Recettes");
        XYChart.Series<String, Number> dep = new XYChart.Series<>();
        dep.setName("Dépenses");
        for (BilanMensuel b : ctx.facturation.historique(mois, 6)) {
            rec.getData().add(new XYChart.Data<>(b.mois(), b.totalRecettes()));
            dep.getData().add(new XYChart.Data<>(b.mois(), b.totalDepenses()));
        }
        finances.getData().addAll(List.of(rec, dep));
        finances.setPrefHeight(280);

        VBox c1 = Ui.carte(occupation);
        VBox c2 = Ui.carte(finances);
        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);
        c1.setPrefWidth(500);
        c2.setPrefWidth(500);

        VBox page = new VBox(16, kpis, new HBox(16, planning, alertes), new HBox(16, c1, c2));
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #f2f5f8;");
        return sp;
    }
}
