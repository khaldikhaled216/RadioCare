package com.radiocare.model.enums;

/** Les six règles du moteur de conflits (section 5 du cahier des charges) + conflits de télé-expertise (F9). */
public enum TypeConflit implements Libelle {
    DOUBLE_RESERVATION("Conflit 1 : Double réservation de salle / équipement"),
    CHEVAUCHEMENT_AGENDA("Conflit 2 : Chevauchement d'agenda médecin / manipulateur"),
    POTENTIEL_HEURES("Conflit 3 : Dépassement du potentiel d'heures matériel"),
    HABILITATION("Conflit 4 : Incompatibilité d'habilitation personnel"),
    BLOCAGE_PATIENT("Conflit 5 : Blocage médical / financier patient"),
    RUPTURE_STOCK("Conflit 6 : Rupture de stock consommable"),
    EXPERTISE_DOUBLE("Conflit d'expertise : double demande simultanée"),
    EXPERTISE_DELAI("Conflit d'expertise : délai légal de transmission dépassé");

    private final String libelle;
    TypeConflit(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }
    @Override public String toString() { return libelle; }
}
