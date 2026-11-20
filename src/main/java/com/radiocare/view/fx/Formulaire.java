package com.radiocare.view.fx;

import com.radiocare.service.ValidationException;
import com.radiocare.service.conflit.ConflitException;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

/**
 * Boîte de dialogue de saisie construite champ par champ.
 * La validation est exécutée au clic sur « Enregistrer » : en cas d'erreur, le message s'affiche
 * dans le formulaire, qui reste ouvert.
 */
public class Formulaire {
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final GridPane grille = new GridPane();
    private final Label erreur = new Label();
    private int ligne = 0;

    public Formulaire(String titre) {
        dialog.setTitle(titre);
        dialog.setHeaderText(titre);
        grille.getStyleClass().add("formulaire");
        erreur.getStyleClass().add("form-erreur");
        erreur.setVisible(false);
        erreur.setManaged(false);
        erreur.setMaxWidth(560);
        VBox contenu = new VBox(grille, erreur);
        dialog.getDialogPane().setContent(contenu);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(Formulaire.class.getResource("/css/radiocare.css")).toExternalForm());
        dialog.setResizable(true);
    }

    private <N extends javafx.scene.Node> N ajouter(String libelle, N champ) {
        Label l = new Label(libelle);
        l.getStyleClass().add("champ-libelle");
        grille.addRow(ligne++, l, champ);
        return champ;
    }

    public void note(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("texte-gris");
        l.setWrapText(true);
        l.setMaxWidth(520);
        grille.add(l, 0, ligne++, 2, 1);
    }

    public TextField texte(String libelle, String valeur) {
        TextField t = new TextField(valeur == null ? "" : valeur);
        t.setPrefWidth(360);
        return ajouter(libelle, t);
    }

    public TextArea zone(String libelle, String valeur) {
        TextArea t = new TextArea(valeur == null ? "" : valeur);
        t.setPrefRowCount(4);
        t.setWrapText(true);
        t.setPrefWidth(360);
        return ajouter(libelle, t);
    }

    public TextField nombre(String libelle, Double valeur) {
        return texte(libelle, valeur == null ? "" : (valeur == Math.rint(valeur) ? String.valueOf(valeur.longValue()) : String.valueOf(valeur)));
    }

    public DatePicker date(String libelle, LocalDate valeur) {
        DatePicker d = new DatePicker(valeur);
        d.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate x) { return x == null ? "" : x.format(Ui.DATE); }
            @Override public LocalDate fromString(String s) {
                try { return s == null || s.isBlank() ? null : LocalDate.parse(s.trim(), Ui.DATE); } catch (DateTimeParseException e) { return null; }
            }
        });
        d.setPromptText("jj/mm/aaaa");
        return ajouter(libelle, d);
    }

    public TextField heure(String libelle, LocalTime valeur) {
        TextField t = texte(libelle, valeur == null ? "" : valeur.toString());
        t.setPromptText("HH:mm");
        return t;
    }

    public <T> ComboBox<T> choix(String libelle, List<T> elements, T valeur, Function<T, String> texte) {
        ComboBox<T> c = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(elements));
        c.setConverter(new StringConverter<>() {
            @Override public String toString(T x) { return x == null ? "— aucun —" : texte.apply(x); }
            @Override public T fromString(String s) { return null; }
        });
        c.setValue(valeur);
        c.setPrefWidth(360);
        c.setVisibleRowCount(12);
        return ajouter(libelle, c);
    }

    /** Ensemble de cases à cocher ; retourne une fonction donnant les éléments cochés. */
    public <T> java.util.function.Supplier<List<T>> cases(String libelle, List<T> elements, Collection<T> coches, Function<T, String> texte) {
        FlowPane f = new FlowPane(12, 6);
        f.setPrefWrapLength(360);
        Map<T, CheckBox> boites = new LinkedHashMap<>();
        for (T e : elements) {
            CheckBox cb = new CheckBox(texte.apply(e));
            cb.setSelected(coches != null && coches.contains(e));
            boites.put(e, cb);
            f.getChildren().add(cb);
        }
        ajouter(libelle, f);
        return () -> boites.entrySet().stream().filter(x -> x.getValue().isSelected()).map(Map.Entry::getKey).toList();
    }

    public CheckBox caseACocher(String libelle, boolean valeur) {
        CheckBox c = new CheckBox();
        c.setSelected(valeur);
        return ajouter(libelle, c);
    }

    /**
     * Affiche le formulaire. {@code enregistrer} lit les champs et appelle le contrôleur ;
     * s'il lève une exception métier, le message est affiché et le formulaire reste ouvert.
     * @return true si l'enregistrement a réussi
     */
    public boolean afficher(Runnable enregistrer) {
        Button ok = (Button) dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        ok.getStyleClass().add("btn-primaire");
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                enregistrer.run();
            } catch (ConflitException e) {
                montrerErreur("Conflit détecté :\n" + Ui.formaterConflits(e));
                ev.consume();
            } catch (ValidationException | IllegalArgumentException | IllegalStateException e) {
                montrerErreur(e.getMessage());
                ev.consume();
            } catch (RuntimeException e) {
                montrerErreur("Erreur inattendue : " + e);
                ev.consume();
            }
        });
        Optional<ButtonType> r = dialog.showAndWait();
        return r.isPresent() && r.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    /** Panneau du dialogue (utilisé par l'outil de captures d'écran). */
    public DialogPane panneau() { return dialog.getDialogPane(); }

    private void montrerErreur(String message) {
        montrerErreurPourCapture(message);
        dialog.getDialogPane().getScene().getWindow().sizeToScene();
    }

    /** Affiche le message d'erreur dans le formulaire (sans redimensionner la fenêtre). */
    public void montrerErreurPourCapture(String message) {
        erreur.setText(message);
        erreur.setVisible(true);
        erreur.setManaged(true);
    }

    // ------------------------------------------------------------------ lecture typée des champs
    public static String texteOuNull(TextField t) {
        String s = t.getText().trim();
        return s.isEmpty() ? null : s;
    }

    public static double decimal(TextField t, String champ) {
        try {
            return Double.parseDouble(t.getText().trim().replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException e) {
            throw new ValidationException("« " + champ + " » : nombre attendu.");
        }
    }

    public static int entier(TextField t, String champ) {
        try {
            return Integer.parseInt(t.getText().trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("« " + champ + " » : nombre entier attendu.");
        }
    }

    public static LocalTime heure(TextField t, String champ) {
        try {
            String s = t.getText().trim();
            return LocalTime.parse(s.length() == 4 && s.charAt(1) == ':' ? "0" + s : s);
        } catch (DateTimeParseException e) {
            throw new ValidationException("« " + champ + " » : heure attendue au format HH:mm.");
        }
    }

    public static LocalDate date(DatePicker d, String champ) {
        // valide la saisie clavier non encore confirmée
        String s = d.getEditor().getText();
        if (s != null && !s.isBlank()) {
            try { d.setValue(LocalDate.parse(s.trim(), Ui.DATE)); } catch (DateTimeParseException e) {
                throw new ValidationException("« " + champ + " » : date attendue au format jj/mm/aaaa.");
            }
        }
        if (d.getValue() == null) throw new ValidationException("« " + champ + " » est obligatoire.");
        return d.getValue();
    }

    public static LocalDate dateOuNull(DatePicker d) {
        String s = d.getEditor().getText();
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim(), Ui.DATE); } catch (DateTimeParseException e) {
            throw new ValidationException("Date invalide : format jj/mm/aaaa attendu.");
        }
    }

    public static <T> T valeur(ComboBox<T> c, String champ) {
        if (c.getValue() == null) throw new ValidationException("Choisissez « " + champ + " ».");
        return c.getValue();
    }
}
