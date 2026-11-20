package com.radiocare.view.fx;

import com.radiocare.service.ServiceRegistry;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Application JavaFX : point d'entrée de l'interface graphique. */
public class RadioCareFx extends Application {
    private static Path dossierDonnees = Path.of("data");
    private static Path dossierExports = Path.of("exports");
    private static boolean demo = true;

    /** Appelé par Main (classe non-JavaFX, nécessaire pour un jar exécutable). */
    public static void demarrer(Path donnees, Path exports, boolean donneesDemo) {
        dossierDonnees = donnees;
        dossierExports = exports;
        demo = donneesDemo;
        launch(RadioCareFx.class);
    }

    @Override
    public void start(Stage stage) {
        Locale.setDefault(Locale.FRANCE);
        ServiceRegistry reg = ServiceRegistry.ouvrir(dossierDonnees, demo);
        Contexte ctx = new Contexte(reg, dossierExports);
        Fenetre fenetre = new Fenetre(ctx, dossierDonnees.toAbsolutePath().toString());

        Scene scene = new Scene(fenetre, 1360, 840);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/radiocare.css")).toExternalForm());
        stage.setTitle("RadioCare — Gestion du centre de radiologie");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();

        if (!reg.store.getAvertissements().isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, String.join("\n", reg.store.getAvertissements()));
            a.setHeaderText("Certains fichiers de données n'ont pas pu être lus");
            a.showAndWait();
        }
    }

    /** Permet aussi « mvn javafx:run ». */
    public static void main(String[] args) {
        launch(args);
    }
}
