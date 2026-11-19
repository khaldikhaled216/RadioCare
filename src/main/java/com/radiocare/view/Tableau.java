package com.radiocare.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Affichage tabulaire formaté en console (largeurs automatiques, troncature, alignement à droite des nombres).
 * Usage : {@code Tableau.de(liste).col("Nom", Patient::getNom).colD("Solde", p -> tnd(p.getSoldeCompte())).afficher(console);}
 */
public class Tableau<T> {
    private record Colonne<T>(String titre, Function<T, Object> valeur, boolean droite, int max) { }

    private final List<T> lignes;
    private final List<Colonne<T>> colonnes = new ArrayList<>();
    private boolean numeroter;

    private Tableau(List<T> lignes) { this.lignes = lignes; }

    public static <T> Tableau<T> de(List<T> lignes) { return new Tableau<>(lignes); }

    public Tableau<T> col(String titre, Function<T, Object> valeur) { colonnes.add(new Colonne<>(titre, valeur, false, 40)); return this; }
    public Tableau<T> col(String titre, Function<T, Object> valeur, int max) { colonnes.add(new Colonne<>(titre, valeur, false, max)); return this; }
    /** Colonne alignée à droite (montants, quantités). */
    public Tableau<T> colD(String titre, Function<T, Object> valeur) { colonnes.add(new Colonne<>(titre, valeur, true, 40)); return this; }
    public Tableau<T> numeroter() { this.numeroter = true; return this; }

    public void afficher(Console console) {
        if (lignes.isEmpty()) { console.info("Aucun élément à afficher."); return; }
        int n = colonnes.size();
        String[][] cellules = new String[lignes.size()][n];
        int[] largeurs = new int[n];
        for (int j = 0; j < n; j++) largeurs[j] = colonnes.get(j).titre().length();
        for (int i = 0; i < lignes.size(); i++) {
            for (int j = 0; j < n; j++) {
                Object v = colonnes.get(j).valeur().apply(lignes.get(i));
                String s = v == null ? "—" : v.toString().replace('\n', ' ');
                int max = colonnes.get(j).max();
                if (s.length() > max) s = s.substring(0, max - 1) + "…";
                cellules[i][j] = s;
                largeurs[j] = Math.max(largeurs[j], s.length());
            }
        }
        int largeurNum = String.valueOf(lignes.size()).length() + 1;
        StringBuilder sep = new StringBuilder("  ");
        if (numeroter) sep.append("─".repeat(largeurNum + 2)).append("┼");
        for (int j = 0; j < n; j++) sep.append("─".repeat(largeurs[j] + 2)).append(j < n - 1 ? "┼" : "");
        StringBuilder entete = new StringBuilder("  ");
        if (numeroter) entete.append(" ").append(" ".repeat(largeurNum)).append(" │");
        for (int j = 0; j < n; j++) entete.append(' ').append(pad(colonnes.get(j).titre(), largeurs[j], colonnes.get(j).droite())).append(j < n - 1 ? " │" : "");
        console.println(console.gras(entete.toString()));
        console.println(sep.toString());
        for (int i = 0; i < lignes.size(); i++) {
            StringBuilder l = new StringBuilder("  ");
            if (numeroter) l.append(' ').append(pad((i + 1) + ".", largeurNum, true)).append(" │");
            for (int j = 0; j < n; j++) l.append(' ').append(pad(cellules[i][j], largeurs[j], colonnes.get(j).droite())).append(j < n - 1 ? " │" : "");
            console.println(l.toString());
        }
        console.println(console.gris("  (" + lignes.size() + " ligne" + (lignes.size() > 1 ? "s" : "") + ")"));
    }

    private static String pad(String s, int w, boolean droite) {
        String espaces = " ".repeat(Math.max(0, w - s.length()));
        return droite ? espaces + s : s + espaces;
    }
}
