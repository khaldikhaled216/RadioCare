package com.radiocare.view;

import com.radiocare.model.enums.Libelle;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

/**
 * Entrées / sorties console (couche Vue) : lecture typée et validée avec java.util.Scanner,
 * messages colorés (ANSI) et mise en forme. Aucune logique métier ici.
 * <p>Convention : une saisie vide conserve la valeur par défaut affichée entre crochets ;
 * « ! » abandonne l'opération en cours.</p>
 */
public class Console {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String RESET = "\u001B[0m", GRAS = "\u001B[1m", ROUGE = "\u001B[31m", VERT = "\u001B[32m",
            JAUNE = "\u001B[33m", BLEU = "\u001B[34m", CYAN = "\u001B[36m", GRIS = "\u001B[90m";

    private final Scanner scanner;
    private final PrintStream out;
    private final boolean couleurs;

    public Console(InputStream in, PrintStream out, boolean couleurs) {
        this.scanner = new Scanner(in, java.nio.charset.StandardCharsets.UTF_8);
        this.out = out;
        this.couleurs = couleurs;
    }

    /** Console standard ; couleurs désactivées si NO_COLOR est défini ou si la sortie n'est pas un terminal. */
    public static Console systeme() {
        boolean couleurs = System.getenv("NO_COLOR") == null && System.console() != null;
        return new Console(System.in, System.out, couleurs);
    }

    // ================================================================== Affichage
    public String c(String code, String texte) { return couleurs ? code + texte + RESET : texte; }
    public String gras(String t) { return c(GRAS, t); }
    public String rouge(String t) { return c(ROUGE, t); }
    public String vert(String t) { return c(VERT, t); }
    public String jaune(String t) { return c(JAUNE, t); }
    public String cyan(String t) { return c(CYAN, t); }
    public String gris(String t) { return c(GRIS, t); }

    public void println(String s) { out.println(s); }
    public void println() { out.println(); }
    public void printf(String f, Object... a) { out.printf(Locale.FRANCE, f, a); }

    public void titre(String t) {
        String ligne = "═".repeat(Math.max(40, t.length() + 4));
        out.println();
        out.println(c(BLEU, ligne));
        out.println(c(BLEU + GRAS, "  " + t));
        out.println(c(BLEU, ligne));
    }

    public void sousTitre(String t) { out.println(); out.println(c(CYAN + GRAS, "── " + t + " " + "─".repeat(Math.max(3, 60 - t.length())))); }
    public void succes(String m) { out.println(c(VERT, "✔ " + m)); }
    public void erreur(String m) { out.println(c(ROUGE, "✘ " + m)); }
    public void avertissement(String m) { out.println(c(JAUNE, "⚠ " + m)); }
    public void info(String m) { out.println(c(GRIS, "ℹ " + m)); }

    /** Affiche une ligne « libellé : valeur » alignée. */
    public void champ(String libelle, Object valeur) {
        out.printf("  %-40s %s%n", libelle + " :", valeur == null ? "—" : valeur);
    }

    public void pause() {
        out.print(gris("\nAppuyez sur Entrée pour continuer..."));
        ligne();
    }

    // ================================================================== Saisie de base
    private String ligne() {
        if (!scanner.hasNextLine()) throw new FinDeSaisieException();
        return scanner.nextLine().trim();
    }

    private String demander(String invite, String defaut) {
        String suffixe = defaut == null || defaut.isEmpty() ? "" : gris(" [" + defaut + "]");
        out.print(c(GRAS, invite) + suffixe + " : ");
        String s = ligne();
        if (s.equals("!")) throw new SaisieAnnuleeException();
        return s.isEmpty() && defaut != null ? defaut : s;
    }

    public String lireTexte(String invite, String defaut) {
        while (true) {
            String s = demander(invite, defaut);
            if (!s.isEmpty()) return s;
            erreur("Valeur obligatoire (tapez ! pour annuler).");
        }
    }

    public String lireTexte(String invite) { return lireTexte(invite, null); }

    /** Texte facultatif : « - » efface la valeur par défaut. */
    public String lireTexteOptionnel(String invite, String defaut) {
        String s = demander(invite + " (facultatif, - pour vider)", defaut);
        return s.equals("-") || s.isEmpty() ? null : s;
    }

    public int lireEntier(String invite, Integer defaut, int min, int max) {
        while (true) {
            String s = demander(invite, defaut == null ? null : String.valueOf(defaut));
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
                erreur("Valeur attendue entre " + min + " et " + max + ".");
            } catch (NumberFormatException e) {
                erreur("Nombre entier attendu.");
            }
        }
    }

    public double lireDecimal(String invite, Double defaut, double min, double max) {
        while (true) {
            String s = demander(invite, defaut == null ? null : formatDecimal(defaut));
            try {
                double v = Double.parseDouble(s.replace(',', '.').replace(" ", ""));
                if (v >= min && v <= max) return v;
                erreur("Valeur attendue entre " + formatDecimal(min) + " et " + formatDecimal(max) + ".");
            } catch (NumberFormatException e) {
                erreur("Nombre décimal attendu (ex. 12.5).");
            }
        }
    }

    private static String formatDecimal(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    public boolean confirmer(String question, boolean defaut) {
        while (true) {
            String s = demander(question + " (o/n)", defaut ? "o" : "n").toLowerCase(Locale.ROOT);
            if (s.equals("o") || s.equals("oui")) return true;
            if (s.equals("n") || s.equals("non")) return false;
            erreur("Répondez par o ou n.");
        }
    }

    public LocalDate lireDate(String invite, LocalDate defaut) {
        while (true) {
            String s = demander(invite + " (jj/mm/aaaa)", defaut == null ? null : defaut.format(DATE));
            if (s.equalsIgnoreCase("auj")) return LocalDate.now();
            try { return LocalDate.parse(s, DATE); } catch (DateTimeParseException e) { erreur("Date invalide, format jj/mm/aaaa (ou « auj »)."); }
        }
    }

    public LocalDate lireDateOptionnelle(String invite, LocalDate defaut) {
        while (true) {
            String s = demander(invite + " (jj/mm/aaaa, facultatif, - pour vider)", defaut == null ? "" : defaut.format(DATE));
            if (s.isEmpty() || s.equals("-")) return null;
            try { return LocalDate.parse(s, DATE); } catch (DateTimeParseException e) { erreur("Date invalide, format jj/mm/aaaa."); }
        }
    }

    public LocalTime lireHeure(String invite, LocalTime defaut) {
        while (true) {
            String s = demander(invite + " (HH:mm)", defaut == null ? null : defaut.format(HEURE));
            try { return LocalTime.parse(s.length() == 4 && s.charAt(1) == ':' ? "0" + s : s, HEURE); }
            catch (DateTimeParseException e) { erreur("Heure invalide, format HH:mm (ex. 09:30)."); }
        }
    }

    public LocalDateTime lireDateHeure(String invite, LocalDateTime defaut) {
        LocalDate d = lireDate(invite + " — date", defaut == null ? LocalDate.now() : defaut.toLocalDate());
        LocalTime t = lireHeure(invite + " — heure", defaut == null ? LocalTime.now().withSecond(0).withNano(0) : defaut.toLocalTime());
        return LocalDateTime.of(d, t);
    }

    public YearMonth lireMois(String invite, YearMonth defaut) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MM/yyyy");
        while (true) {
            String s = demander(invite + " (mm/aaaa)", defaut == null ? null : defaut.format(f));
            try { return YearMonth.parse(s.length() == 6 ? "0" + s : s, f); } catch (DateTimeParseException e) { erreur("Mois invalide, format mm/aaaa."); }
        }
    }

    // ================================================================== Choix dans une liste
    /** Choix numéroté dans une liste ; retourne l'élément choisi. */
    public <T> T choisir(String invite, List<T> elements, Function<T, String> libelle, T defaut) {
        if (elements.isEmpty()) throw new IllegalStateException("Aucun élément disponible pour : " + invite);
        out.println(c(GRAS, invite) + " :");
        Integer numDefaut = null;
        for (int i = 0; i < elements.size(); i++) {
            T e = elements.get(i);
            if (Objects.equals(e, defaut)) numDefaut = i + 1;
            out.printf("   %s %s%n", cyan(String.format("%3d.", i + 1)), libelle.apply(e));
        }
        int n = lireEntier("  Votre choix", numDefaut, 1, elements.size());
        return elements.get(n - 1);
    }

    public <E extends Enum<E> & Libelle> E choisirEnum(String invite, Class<E> type, E defaut) {
        return choisir(invite, List.of(type.getEnumConstants()), Libelle::getLibelle, defaut);
    }

    /** Choix multiple : numéros séparés par des virgules (ou « 0 » pour aucun si autorisé). */
    public <T> List<T> choisirPlusieurs(String invite, List<T> elements, Function<T, String> libelle, Collection<T> defaut, boolean videAutorise) {
        out.println(c(GRAS, invite) + " :");
        List<String> numsDefaut = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            out.printf("   %s %s%n", cyan(String.format("%3d.", i + 1)), libelle.apply(elements.get(i)));
            if (defaut != null && defaut.contains(elements.get(i))) numsDefaut.add(String.valueOf(i + 1));
        }
        String def = numsDefaut.isEmpty() ? (videAutorise ? "0" : null) : String.join(",", numsDefaut);
        while (true) {
            String s = demander("  Numéros séparés par des virgules" + (videAutorise ? " (0 = aucun)" : ""), def);
            if (videAutorise && s.equals("0")) return new ArrayList<>();
            try {
                List<T> res = new ArrayList<>();
                for (String p : s.split("[,;\\s]+")) {
                    int n = Integer.parseInt(p);
                    if (n < 1 || n > elements.size()) throw new NumberFormatException();
                    if (!res.contains(elements.get(n - 1))) res.add(elements.get(n - 1));
                }
                if (!res.isEmpty()) return res;
            } catch (NumberFormatException e) {
                // message ci-dessous
            }
            erreur("Saisie invalide : indiquez des numéros entre 1 et " + elements.size() + ".");
        }
    }

    // ================================================================== Formatage
    public static String tnd(double montant) { return String.format(Locale.FRANCE, "%,.3f TND", montant); }
    public static String pct(double v) { return String.format(Locale.FRANCE, "%.1f %%", v); }
    public static String date(LocalDate d) { return d == null ? "—" : d.format(DATE); }
    public static String dateHeure(LocalDateTime d) { return d == null ? "—" : d.format(DATE_HEURE); }

    /** Barre de progression textuelle (ex. potentiel restant, taux d'occupation). */
    public static String barre(double pourcentage, int largeur) {
        int plein = (int) Math.round(Math.max(0, Math.min(100, pourcentage)) / 100.0 * largeur);
        return "█".repeat(plein) + "░".repeat(largeur - plein);
    }
}
