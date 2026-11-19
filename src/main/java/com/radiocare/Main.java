package com.radiocare;

import com.radiocare.service.ServiceRegistry;
import com.radiocare.view.Console;
import com.radiocare.view.FinDeSaisieException;
import com.radiocare.view.MainView;

import java.nio.file.Path;

/**
 * Point d'entrée de RadioCare (application console).
 * <pre>
 * Options :
 *   --data &lt;dossier&gt;   dossier des fichiers JSON (défaut : ./data)
 *   --exports &lt;dossier&gt; dossier des rapports exportés (défaut : ./exports)
 *   --sans-demo        ne pas générer de données de démonstration au premier lancement
 * </pre>
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        Path data = Path.of("data");
        Path exports = Path.of("exports");
        boolean demo = true;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--data" -> data = Path.of(args[++i]);
                case "--exports" -> exports = Path.of(args[++i]);
                case "--sans-demo" -> demo = false;
                case "--aide", "-h", "--help" -> {
                    System.out.println("Usage : java -jar radiocare-1.0.0-all.jar [--data <dossier>] [--exports <dossier>] [--sans-demo]");
                    return;
                }
                default -> System.err.println("Option ignorée : " + args[i]);
            }
        }
        Console console = Console.systeme();
        boolean premierLancement = !data.resolve("patients.json").toFile().exists();
        if (premierLancement && demo) console.info("Premier lancement : génération des données de démonstration...");
        ServiceRegistry reg = ServiceRegistry.ouvrir(data, demo);
        try {
            new MainView(console, reg, exports).demarrer();
        } catch (FinDeSaisieException e) {
            console.println("");
            console.info("Entrée fermée : fin de session. Les données sont sauvegardées.");
        }
    }
}
