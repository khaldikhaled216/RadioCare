package com.radiocare.view.fx;

import com.radiocare.service.ServiceRegistry;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Outil de développement (hors tests JUnit) : ouvre l'interface sur un jeu de démonstration
 * et enregistre une capture PNG de chaque page. Usage : Captures <dossierDonnees> <dossierCaptures>
 */
public class Captures {
    public static void main(String[] args) throws Exception {
        Path donnees = Path.of(args[0]);
        Path sortie = Path.of(args[1]);
        Files.createDirectories(sortie);
        Locale.setDefault(Locale.FRANCE);
        Platform.startup(() -> {
            ServiceRegistry reg = ServiceRegistry.ouvrir(donnees, true);
            Contexte ctx = new Contexte(reg, sortie.resolve("exports"));
            Fenetre fenetre = new Fenetre(ctx, donnees.toAbsolutePath().toString());
            Scene scene = new Scene(fenetre, 1440, 900);
            scene.getStylesheets().add(Objects.requireNonNull(Captures.class.getResource("/css/radiocare.css")).toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            List<String> cles = new ArrayList<>(fenetre.clesPages());
            Timeline t = new Timeline();
            for (int i = 0; i <= cles.size(); i++) {
                final int n = i;
                t.getKeyFrames().add(new KeyFrame(Duration.millis(900.0 * (i + 1)), e -> {
                    if (n > 0) enregistrer(scene, sortie.resolve(String.format("%02d-%s.png", n, cles.get(n - 1))).toFile());
                    if (n < cles.size()) fenetre.afficher(cles.get(n));
                    else { captureFormulaire(sortie); stage.close(); Platform.exit(); }
                }));
            }
            t.play();
        });
    }

    /** Capture d'un formulaire type (nouveau RDV) avec un message de conflit affiché. */
    private static void captureFormulaire(Path sortie) {
        Formulaire f = new Formulaire("Nouveau rendez-vous");
        f.choix("Patient", java.util.List.of("Yosra Gabsi (11283756)"), "Yosra Gabsi (11283756)", x -> x);
        f.choix("Examen", java.util.List.of("IRM Cérébrale — 30 min"), "IRM Cérébrale — 30 min", x -> x);
        f.date("Date", java.time.LocalDate.now().plusDays(3));
        f.heure("Heure de début", java.time.LocalTime.of(18, 0));
        f.nombre("Durée (minutes)", 30.0);
        f.choix("Manipulateur", java.util.List.of("Nour Ben Ammar [Rayons X, Échographie]"), "Nour Ben Ammar [Rayons X, Échographie]", x -> x);
        f.note("Avant l'enregistrement, le moteur vérifie les 6 règles de conflit.");
        javafx.scene.control.DialogPane pane = f.panneau();
        pane.applyCss();
        pane.resize(pane.prefWidth(-1), pane.prefHeight(-1));
        pane.layout();
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(pane.snapshot(null, null), null), "png", sortie.resolve("12-formulaire-rdv.png").toFile());
            System.out.println("Capture : formulaire");
        } catch (Exception e) {
            System.out.println("Échec capture formulaire : " + e);
        }
    }

    private static void enregistrer(Scene scene, File f) {
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png", f);
            System.out.println("Capture : " + f);
        } catch (Exception e) {
            System.out.println("Échec capture " + f + " : " + e);
        }
    }
}
