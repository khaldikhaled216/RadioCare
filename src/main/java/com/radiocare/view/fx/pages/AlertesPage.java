package com.radiocare.view.fx.pages;

import com.radiocare.service.AlerteService.Alerte;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** Centre de notifications du secrétariat : maintenance, stock, péremption, factures, télé-expertise. */
public class AlertesPage extends Page {
    public AlertesPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Centre de notifications"; }

    @Override
    public Node construire() {
        List<Alerte> alertes = ctx.tableauDeBord.alertes();
        VBox liste = new VBox(8);
        long crit = alertes.stream().filter(a -> a.niveau() == com.radiocare.service.AlerteService.Niveau.CRITIQUE).count();
        liste.getChildren().add(Ui.barre(Ui.section(alertes.size() + " alerte(s) active(s), dont " + crit + " critique(s)"), Ui.espace(),
                Ui.bouton("Actualiser", ctx::rafraichir)));
        if (alertes.isEmpty()) liste.getChildren().add(Ui.pastille("Aucune alerte : tout est en ordre.", "p-vert"));
        alertes.forEach(a -> liste.getChildren().add(ligne(a)));
        ScrollPane sp = new ScrollPane(liste);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #f2f5f8;");
        return sp;
    }

    /** Ligne d'alerte colorée selon le niveau (réutilisée par le tableau de bord). */
    public static HBox ligne(Alerte a) {
        String couleur = switch (a.niveau()) {
            case CRITIQUE -> "p-rouge";
            case AVERTISSEMENT -> "p-orange";
            case INFO -> "p-bleu";
        };
        String bord = switch (a.niveau()) {
            case CRITIQUE -> "alerte-critique";
            case AVERTISSEMENT -> "alerte-avertissement";
            case INFO -> "alerte-info";
        };
        Label niveau = Ui.pastille(a.niveau().name(), couleur);
        Label cat = new Label(a.categorie());
        cat.setStyle("-fx-font-weight: bold;");
        Label msg = new Label(a.message());
        msg.setWrapText(true);
        msg.setMaxWidth(Double.MAX_VALUE);
        HBox entete = new HBox(10, niveau, cat);
        entete.setAlignment(Pos.CENTER_LEFT);
        VBox texte = new VBox(4, entete, msg);
        HBox.setHgrow(texte, javafx.scene.layout.Priority.ALWAYS);
        HBox h = new HBox(texte);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().addAll("alerte-ligne", bord);
        return h;
    }
}
