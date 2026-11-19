package com.radiocare.view;

import com.radiocare.service.ValidationException;
import com.radiocare.service.conflit.Conflit;
import com.radiocare.service.conflit.ConflitException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Menu interactif réutilisable : options numérotées, « 0 » pour revenir.
 * Chaque action est exécutée sous protection : les erreurs métier sont affichées sans quitter le menu.
 */
public class Menu {
    private record Option(String libelle, Runnable action) { }

    private final Console console;
    private final String titre;
    private final List<Option> options = new ArrayList<>();
    private Supplier<String> entete;
    private String libelleRetour = "Retour";

    public Menu(Console console, String titre) {
        this.console = console;
        this.titre = titre;
    }

    public Menu option(String libelle, Runnable action) { options.add(new Option(libelle, action)); return this; }
    /** Texte dynamique affiché sous le titre (ex. compteur d'alertes). */
    public Menu entete(Supplier<String> entete) { this.entete = entete; return this; }
    public Menu retour(String libelle) { this.libelleRetour = libelle; return this; }

    public void executer() {
        while (true) {
            console.titre(titre);
            if (entete != null) {
                String e = entete.get();
                if (e != null && !e.isBlank()) console.println(e);
            }
            for (int i = 0; i < options.size(); i++)
                console.println("  " + console.cyan(String.format("%2d.", i + 1)) + " " + options.get(i).libelle());
            console.println("  " + console.cyan(" 0.") + " " + libelleRetour);
            int choix;
            try {
                choix = console.lireEntier("Votre choix", null, 0, options.size());
            } catch (SaisieAnnuleeException e) {
                continue;
            }
            if (choix == 0) return;
            proteger(console, options.get(choix - 1).action());
        }
    }

    /** Exécute une action en affichant proprement les erreurs de validation et les conflits. */
    public static void proteger(Console console, Runnable action) {
        try {
            action.run();
        } catch (SaisieAnnuleeException e) {
            console.avertissement("Opération annulée.");
        } catch (ConflitException e) {
            console.erreur("Opération refusée par le moteur de conflits (" + e.getConflits().size() + " conflit(s)) :");
            for (Conflit c : e.getConflits()) {
                console.println("    " + console.rouge("• " + c.type().getLibelle()));
                console.println("      " + c.message());
            }
            console.info("Ces conflits ont été enregistrés dans le journal du moteur.");
        } catch (ValidationException e) {
            console.erreur(e.getMessage());
        } catch (FinDeSaisieException e) {
            throw e;
        } catch (IllegalStateException | IllegalArgumentException e) {
            console.erreur(e.getMessage());
        } catch (RuntimeException e) {
            console.erreur("Erreur inattendue : " + e);
        }
    }
}
