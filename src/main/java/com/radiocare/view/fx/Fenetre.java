package com.radiocare.view.fx;

import com.radiocare.view.fx.pages.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Fenêtre principale : barre latérale de navigation, en-tête, page courante et barre d'état. */
public class Fenetre extends BorderPane {
    private final Contexte ctx;
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, Button> boutons = new LinkedHashMap<>();
    private final Label titre = new Label();
    private final Label statut = new Label("Prêt.");
    private final Label badge = new Label();
    private final StackPane zone = new StackPane();
    private String pageCourante;

    public Fenetre(Contexte ctx, String dossierDonnees) {
        this.ctx = ctx;
        ctx.setFenetre(this);
        getStyleClass().add("fenetre");

        VBox menu = new VBox();
        menu.getStyleClass().add("sidebar");
        Label logo = new Label("✚ RadioCare");
        logo.getStyleClass().add("logo");
        Label sous = new Label("Centre d'imagerie médicale");
        sous.getStyleClass().add("logo-sous");
        menu.getChildren().addAll(logo, sous);

        section(menu, "PILOTAGE");
        lien(menu, "dashboard", "Tableau de bord", new TableauDeBordPage(ctx));
        lien(menu, "alertes", "Notifications", new AlertesPage(ctx));
        section(menu, "ACTIVITÉ MÉDICALE");
        lien(menu, "patients", "Patients", new PatientsPage(ctx));
        lien(menu, "planning", "Rendez-vous & planning", new PlanningPage(ctx));
        lien(menu, "examens", "Examens & comptes-rendus", new ExamensPage(ctx));
        lien(menu, "tele", "Télé-expertise", new TeleExpertisePage(ctx));
        section(menu, "RESSOURCES");
        lien(menu, "personnel", "Personnel médical", new PersonnelPage(ctx));
        lien(menu, "equipements", "Équipements & salles", new EquipementsPage(ctx));
        lien(menu, "stock", "Stocks & commandes", new StockPage(ctx));
        section(menu, "GESTION");
        lien(menu, "facturation", "Facturation & paiements", new FacturationPage(ctx));
        lien(menu, "stats", "Statistiques & rapports", new StatistiquesPage(ctx));

        Region vide = new Region();
        VBox.setVgrow(vide, Priority.ALWAYS);
        Label pied = new Label("Données JSON :\n" + dossierDonnees);
        pied.getStyleClass().add("sidebar-pied");
        menu.getChildren().addAll(vide, pied);
        ScrollPane menuDefilant = new ScrollPane(menu);
        menuDefilant.setFitToWidth(true);
        menuDefilant.setFitToHeight(true);
        menuDefilant.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        menuDefilant.setStyle("-fx-background: #13293d; -fx-background-color: #13293d;");
        setLeft(menuDefilant);

        titre.getStyleClass().add("entete-titre");
        Label date = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
        date.getStyleClass().add("entete-date");
        HBox entete = new HBox(12, titre, Ui.espace(), date);
        entete.setAlignment(Pos.CENTER_LEFT);
        entete.getStyleClass().add("entete");

        zone.getStyleClass().add("contenu");
        HBox barreStatut = new HBox(statut);
        barreStatut.getStyleClass().add("barre-statut");

        setCenter(new VBox(entete, zone, barreStatut));
        VBox.setVgrow(zone, Priority.ALWAYS);
        badge.getStyleClass().add("badge");
        boutons.get("alertes").setGraphicTextGap(8);
        boutons.get("alertes").setContentDisplay(ContentDisplay.RIGHT);
        afficher("dashboard");
    }

    private void section(VBox menu, String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("nav-section");
        menu.getChildren().add(l);
    }

    private void lien(VBox menu, String cle, String texte, Page page) {
        Button b = new Button(texte);
        b.getStyleClass().add("nav-bouton");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> afficher(cle));
        pages.put(cle, page);
        boutons.put(cle, b);
        menu.getChildren().add(b);
    }

    public void afficher(String cle) {
        pageCourante = cle;
        boutons.forEach((k, b) -> b.getStyleClass().remove("actif"));
        boutons.get(cle).getStyleClass().add("actif");
        rafraichir();
    }

    public void rafraichir() {
        Page p = pages.get(pageCourante);
        titre.setText(p.titre());
        Node contenu;
        try {
            contenu = p.construire();
        } catch (RuntimeException e) {
            contenu = new Label("Impossible d'afficher la page : " + e.getMessage());
        }
        zone.getChildren().setAll(contenu);
        int n = ctx.tableauDeBord.alertes().size();
        badge.setText(String.valueOf(n));
        boutons.get("alertes").setGraphic(n > 0 ? badge : null);
    }

    public void statut(String message, boolean erreur) {
        statut.setText(message);
        statut.getStyleClass().removeAll("statut-ok", "statut-erreur");
        statut.getStyleClass().add(erreur ? "statut-erreur" : "statut-ok");
    }

    /** Utilisé par l'outil de captures d'écran. */
    public java.util.Set<String> clesPages() { return pages.keySet(); }
}
