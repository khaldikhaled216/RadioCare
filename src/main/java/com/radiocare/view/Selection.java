package com.radiocare.view;

import com.radiocare.service.ValidationException;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Sélection d'un élément par identifiant ou par recherche textuelle, puis choix numéroté si plusieurs résultats. */
public final class Selection {
    private Selection() { }

    public static <T> T parRecherche(Console console, String quoi, List<T> elements, Function<T, String> id, Function<T, String> libelle) {
        if (elements.isEmpty()) throw new ValidationException("Aucun(e) " + quoi + " enregistré(e).");
        String saisie = console.lireTexte(quoi + " — identifiant ou texte à rechercher (vide = tout lister)", "*");
        if (!saisie.equals("*")) {
            for (T e : elements) if (id.apply(e).equalsIgnoreCase(saisie)) return e;
        }
        String t = saisie.toLowerCase(Locale.ROOT);
        List<T> trouves = saisie.equals("*") ? elements
                : elements.stream().filter(e -> libelle.apply(e).toLowerCase(Locale.ROOT).contains(t) || id.apply(e).toLowerCase(Locale.ROOT).contains(t)).toList();
        if (trouves.isEmpty()) throw new ValidationException("Aucun résultat pour « " + saisie + " ».");
        if (trouves.size() == 1) {
            console.info("Sélection : " + libelle.apply(trouves.get(0)));
            return trouves.get(0);
        }
        if (trouves.size() > 40) {
            console.avertissement(trouves.size() + " résultats : seuls les 40 premiers sont affichés, affinez la recherche si besoin.");
            trouves = trouves.subList(0, 40);
        }
        return console.choisir("Choisissez", trouves, e -> id.apply(e) + " — " + libelle.apply(e), null);
    }
}
