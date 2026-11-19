package com.radiocare.model.enums;

/**
 * Types d'équipements lourds. Chaque type connaît l'habilitation requise pour le manipuler,
 * s'il émet des rayonnements ionisants et l'épaisseur minimale de blindage plomb exigée.
 */
public enum TypeEquipement implements Libelle {
    IRM("IRM", Habilitation.IRM, false, 0.0),
    SCANNER_CT("Scanner-CT", Habilitation.SCANNER, true, 2.0),
    TABLE_RX("Table-RX", Habilitation.RAYONS_X, true, 1.5),
    ECHOGRAPHE("Échographe", Habilitation.ECHOGRAPHIE, false, 0.0);

    private final String libelle;
    private final Habilitation habilitationRequise;
    private final boolean ionisant;
    private final double blindagePlombMinMm;

    TypeEquipement(String libelle, Habilitation habilitationRequise, boolean ionisant, double blindagePlombMinMm) {
        this.libelle = libelle;
        this.habilitationRequise = habilitationRequise;
        this.ionisant = ionisant;
        this.blindagePlombMinMm = blindagePlombMinMm;
    }

    @Override public String getLibelle() { return libelle; }
    public Habilitation getHabilitationRequise() { return habilitationRequise; }
    public boolean isIonisant() { return ionisant; }
    public double getBlindagePlombMinMm() { return blindagePlombMinMm; }
    @Override public String toString() { return libelle; }
}
