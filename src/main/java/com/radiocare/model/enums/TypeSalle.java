package com.radiocare.model.enums;

/** Type de salle : détermine le type d'équipement qu'elle peut accueillir. */
public enum TypeSalle implements Libelle {
    SALLE_IRM("Salle IRM", TypeEquipement.IRM),
    SALLE_SCANNER("Salle Scanner", TypeEquipement.SCANNER_CT),
    SALLE_RADIOGRAPHIE("Salle Radiographie", TypeEquipement.TABLE_RX),
    SALLE_ECHOGRAPHIE("Salle Échographie", TypeEquipement.ECHOGRAPHE);

    private final String libelle;
    private final TypeEquipement typeEquipementCompatible;
    TypeSalle(String libelle, TypeEquipement t) { this.libelle = libelle; this.typeEquipementCompatible = t; }
    @Override public String getLibelle() { return libelle; }
    public TypeEquipement getTypeEquipementCompatible() { return typeEquipementCompatible; }
    @Override public String toString() { return libelle; }
}
