package com.radiocare;

import com.radiocare.service.ServiceRegistry;
import com.radiocare.view.Console;
import com.radiocare.view.FinDeSaisieException;
import com.radiocare.view.MainView;
import com.radiocare.view.fx.RadioCareFx;

import java.nio.file.Path;

/**
 * Point d'entrée de RadioCare : interface graphique JavaFX par défaut, console avec --console.
 * <pre>
 * Options :
 *   --data &lt;dossier&gt;   dossier des fichiers JSON (défaut : ./data)
 *   --exports &lt;dossier&gt; dossier des rapports exportés (défaut : ./exports)
 *   --sans-demo        ne pas générer de données de démonstration au premier lancement
 *   --console          utiliser l'interface console au lieu de l'interface graphique JavaFX
 * </pre>
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        Path data = Path.of("data");
        Path exports = Path.of("exports");
        boolean demo = true;
        boolean console = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--data" -> data = Path.of(args[++i]);
                case "--exports" -> exports = Path.of(args[++i]);
                case "--sans-demo" -> demo = false;
                case "--console" -> console = true;
                case "--aide", "-h", "--help" -> {
                    System.out.println("Usage : java -jar radiocare-1.0.0-all.jar [--data <dossier>] [--exports <dossier>] [--sans-demo] [--console]");
                    return;
                }
                default -> System.err.println("Option ignorée : " + args[i]);
            }
        }
        if (!console) {
            RadioCareFx.demarrer(data, exports, demo);
            return;
        }
        Console terminal = Console.systeme();
        boolean premierLancement = !data.resolve("patients.json").toFile().exists();
        if (premierLancement && demo) terminal.info("Premier lancement : génération des données de démonstration...");
        ServiceRegistry reg = ServiceRegistry.ouvrir(data, demo);
        try {
            new MainView(terminal, reg, exports).demarrer();
        } catch (FinDeSaisieException e) {
            terminal.println("");
            terminal.info("Entrée fermée : fin de session. Les données sont sauvegardées.");
        }
    }
}
