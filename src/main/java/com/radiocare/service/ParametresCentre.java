package com.radiocare.service;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Paramètres de fonctionnement du centre (règles métier configurables). */
public final class ParametresCentre {
    private ParametresCentre() { }

    public static final LocalTime HEURE_OUVERTURE = LocalTime.of(8, 0);
    public static final LocalTime HEURE_FERMETURE = LocalTime.of(19, 0);
    /** Jours ouvrés : du lundi au samedi. */
    public static final DayOfWeek DERNIER_JOUR_OUVRE = DayOfWeek.SATURDAY;
    /** Conflit 5 : seuil au-delà duquel un solde débiteur bloque la prise de RDV (TND). */
    public static final double SEUIL_SOLDE_DEBITEUR = 500.0;
    public static final int DELAI_PAIEMENT_JOURS = 30;
    /** Alerte maintenance : heures restantes ≤ max(50 h, 10 % de l'intervalle). */
    public static final double SEUIL_ALERTE_MAINTENANCE_HEURES = 50.0;
    public static final double SEUIL_ALERTE_MAINTENANCE_POURCENT = 10.0;
    /** Alerte péremption : produits périmant dans les 30 jours. */
    public static final int DELAI_ALERTE_PEREMPTION_JOURS = 30;
    /** F9 : délai légal de transmission d'un compte-rendu urgent à l'expert. */
    public static final int DELAI_LEGAL_URGENCE_HEURES = 24;

    public static long minutesOuvertureParJour() {
        return java.time.Duration.between(HEURE_OUVERTURE, HEURE_FERMETURE).toMinutes();
    }

    public static boolean estJourOuvre(DayOfWeek j) { return j.getValue() <= DERNIER_JOUR_OUVRE.getValue(); }
}
