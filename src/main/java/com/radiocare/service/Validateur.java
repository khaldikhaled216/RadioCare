package com.radiocare.service;

import com.radiocare.model.enums.ModePaiement;

import java.util.Locale;
import java.util.regex.Pattern;

/** Règles de validation communes et normalisation des références textuelles (F6). */
public final class Validateur {
    private Validateur() { }

    private static final Pattern CIN = Pattern.compile("^\\d{8}$");
    private static final Pattern TELEPHONE = Pattern.compile("^(\\+216\\s?)?\\d{2}\\s?\\d{3}\\s?\\d{3}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    /** Référence normée : segments alphanumériques majuscules séparés par '-' ou '/', 4 à 40 caractères. */
    private static final Pattern REFERENCE = Pattern.compile("^[A-Z0-9]+([-/][A-Z0-9]+)+$");

    public static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) throw new ValidationException("Le champ « " + champ + " » est obligatoire.");
        return valeur.trim();
    }

    public static <T> T nonNul(T valeur, String champ) {
        if (valeur == null) throw new ValidationException("Le champ « " + champ + " » est obligatoire.");
        return valeur;
    }

    public static void cin(String cin) {
        if (cin == null || !CIN.matcher(cin.trim()).matches())
            throw new ValidationException("Le CIN / matricule doit comporter exactement 8 chiffres.");
    }

    public static void telephoneOptionnel(String tel) {
        if (tel != null && !tel.isBlank() && !TELEPHONE.matcher(tel.trim()).matches())
            throw new ValidationException("Numéro de téléphone invalide (format attendu : 8 chiffres, ex. 98 123 456).");
    }

    public static void emailOptionnel(String email) {
        if (email != null && !email.isBlank() && !EMAIL.matcher(email.trim()).matches())
            throw new ValidationException("Adresse e-mail invalide.");
    }

    public static void positif(double v, String champ) {
        if (v <= 0 || Double.isNaN(v)) throw new ValidationException("« " + champ + " » doit être strictement positif.");
    }

    public static void positifOuNul(double v, String champ) {
        if (v < 0 || Double.isNaN(v)) throw new ValidationException("« " + champ + " » ne peut pas être négatif.");
    }

    public static void entre(double v, double min, double max, String champ) {
        if (v < min || v > max) throw new ValidationException("« " + champ + " » doit être compris entre " + min + " et " + max + ".");
    }

    /** Normalise (majuscules, espaces → tirets) puis valide une référence textuelle normée. */
    public static String reference(String brute, String champ) {
        String r = obligatoire(brute, champ).toUpperCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (r.length() < 4 || r.length() > 40 || !REFERENCE.matcher(r).matches())
            throw new ValidationException("« " + champ + " » doit être une référence normée alphanumérique "
                    + "(ex. ORD-2026-0153), segments séparés par « - » ou « / », 4 à 40 caractères. Reçu : " + brute);
        return r;
    }

    /** Référence de preuve de paiement : doit commencer par le préfixe du mode (REC-, CHQ-, CB-, VIR-). */
    public static String preuvePaiement(String brute, ModePaiement mode) {
        String r = reference(brute, "Référence de la preuve");
        if (!r.startsWith(mode.getPrefixePreuve() + "-"))
            throw new ValidationException("Pour un paiement par " + mode.getLibelle() + ", la référence doit commencer par « "
                    + mode.getPrefixePreuve() + "- » (ex. " + mode.getPrefixePreuve() + "-004512).");
        return r;
    }

    public static double arrondi(double v) { return Math.round(v * 1000.0) / 1000.0; }
}
