package com.radiocare.view.fx;

import com.radiocare.service.ValidationException;
import com.radiocare.service.conflit.Conflit;
import com.radiocare.service.conflit.ConflitException;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Fabrique de composants graphiques réutilisables et affichage uniforme des erreurs. */
public final class Ui {
    private Ui() { }

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String tnd(double v) { return String.format(Locale.FRANCE, "%,.3f TND", v); }
    public static String pct(double v) { return String.format(Locale.FRANCE, "%.1f %%", v); }
    public static String nombre(double v) { return String.format(Locale.FRANCE, "%.1f", v); }
    public static String date(LocalDate d) { return d == null ? "—" : d.format(DATE); }
    public static String dateHeure(LocalDateTime d) { return d == null ? "—" : d.format(DATE_HEURE); }

    // ------------------------------------------------------------------ boutons et mise en page
    public static Button bouton(String texte, Runnable action) { return bouton(texte, "btn-secondaire", action); }
    public static Button primaire(String texte, Runnable action) { return bouton(texte, "btn-primaire", action); }
    public static Button danger(String texte, Runnable action) { return bouton(texte, "btn-danger", action); }

    private static Button bouton(String texte, String style, Runnable action) {
        Button b = new Button(texte);
        b.getStyleClass().add(style);
        b.setOnAction(e -> action.run());
        return b;
    }

    public static HBox barre(Node... noeuds) {
        HBox h = new HBox(8, noeuds);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(0, 0, 10, 0));
        return h;
    }

    public static Region espace() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Label section(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("section-titre");
        return l;
    }

    public static VBox carte(Node... contenu) {
        VBox v = new VBox(8, contenu);
        v.getStyleClass().add("carte");
        return v;
    }

    /** Carte indicateur (KPI) : titre, valeur et sous-texte ; accent = kpi-accent / kpi-alerte / kpi-danger. */
    public static VBox kpi(String titre, String valeur, String sous, String accent) {
        Label t = new Label(titre); t.getStyleClass().add("kpi-titre");
        Label v = new Label(valeur); v.getStyleClass().add("kpi-valeur");
        Label s = new Label(sous); s.getStyleClass().add("kpi-sous");
        VBox box = new VBox(4, t, v, s);
        box.getStyleClass().addAll("carte", accent);
        box.setMinWidth(170);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /** Grille « libellé : valeur » pour les fiches détaillées. */
    public static GridPane fiche(String... paires) {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(6);
        for (int i = 0; i + 1 < paires.length; i += 2) {
            Label l = new Label(paires[i]); l.getStyleClass().add("champ-libelle");
            Label v = new Label(paires[i + 1] == null ? "—" : paires[i + 1]); v.getStyleClass().add("champ-valeur");
            v.setMaxWidth(520);
            g.addRow(i / 2, l, v);
        }
        return g;
    }

    public static Label pastille(String texte, String couleur) {
        Label l = new Label(texte);
        l.getStyleClass().addAll("pastille", couleur);
        return l;
    }

    // ------------------------------------------------------------------ tables
    public static <T> TableView<T> table(List<T> elements) {
        TableView<T> t = new TableView<>(FXCollections.observableArrayList(elements));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPlaceholder(new Label("Aucune donnée"));
        VBox.setVgrow(t, Priority.ALWAYS);
        return t;
    }

    public static <T> TableColumn<T, Object> col(TableView<T> t, String titre, Function<T, Object> valeur, double largeur) {
        TableColumn<T, Object> c = new TableColumn<>(titre);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valeur.apply(d.getValue())));
        c.setPrefWidth(largeur);
        t.getColumns().add(c);
        return c;
    }

    /** Colonne affichant une pastille colorée ; couleur(T) retourne p-vert, p-rouge... */
    public static <T> void colPastille(TableView<T> t, String titre, Function<T, String> texte, Function<T, String> couleur, double largeur) {
        TableColumn<T, T> c = new TableColumn<>(titre);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        c.setCellFactory(x -> new TableCell<>() {
            @Override protected void updateItem(T item, boolean vide) {
                super.updateItem(item, vide);
                setGraphic(vide || item == null ? null : pastille(texte.apply(item), couleur.apply(item)));
                setText(null);
            }
        });
        c.setPrefWidth(largeur);
        t.getColumns().add(c);
    }

    /** Colonne avec barre de progression (0-100 %). */
    public static <T> void colProgression(TableView<T> t, String titre, Function<T, Double> pourcentage, double largeur) {
        TableColumn<T, T> c = new TableColumn<>(titre);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        c.setCellFactory(x -> new TableCell<>() {
            @Override protected void updateItem(T item, boolean vide) {
                super.updateItem(item, vide);
                if (vide || item == null) { setGraphic(null); setText(null); return; }
                double p = pourcentage.apply(item);
                ProgressBar bar = new ProgressBar(Math.max(0, Math.min(1, p / 100)));
                bar.setPrefWidth(largeur - 70);
                if (p < 10) bar.getStyleClass().add("basse"); else if (p < 30) bar.getStyleClass().add("moyenne");
                Label l = new Label(pct(p));
                HBox h = new HBox(6, bar, l);
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h);
                setText(null);
            }
        });
        c.setPrefWidth(largeur);
        t.getColumns().add(c);
    }

    /** Élément sélectionné ou message d'erreur métier. */
    public static <T> T selection(TableView<T> t, String quoi) {
        T s = t.getSelectionModel().getSelectedItem();
        if (s == null) throw new ValidationException("Sélectionnez d'abord " + quoi + " dans la liste.");
        return s;
    }

    // ------------------------------------------------------------------ dialogues
    /** Exécute une action ; les erreurs métier et les conflits sont affichés dans une boîte de dialogue. */
    public static boolean executer(Runnable action) {
        try {
            action.run();
            return true;
        } catch (ConflitException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Conflit détecté");
            a.setHeaderText("Opération refusée par le moteur de conflits");
            a.setContentText(formaterConflits(e));
            a.getDialogPane().setMinWidth(560);
            a.showAndWait();
        } catch (ValidationException | IllegalArgumentException | IllegalStateException e) {
            erreur(e.getMessage());
        } catch (RuntimeException e) {
            erreur("Erreur inattendue : " + e);
        }
        return false;
    }

    public static String formaterConflits(ConflitException e) {
        StringBuilder sb = new StringBuilder();
        for (Conflit c : e.getConflits()) sb.append("• ").append(c.type().getLibelle()).append("\n   ").append(c.message()).append("\n");
        return sb.append("\nCes conflits ont été enregistrés dans le journal.").toString();
    }

    public static void erreur(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void info(String titre, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle(titre);
        a.setHeaderText(titre);
        a.getDialogPane().setMinWidth(480);
        a.showAndWait();
    }

    public static boolean confirmer(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle("Confirmation");
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }
}
