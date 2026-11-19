package com.radiocare.model.enums;

/** Mode de paiement ; chaque mode impose un préfixe normé pour la référence textuelle de preuve (F6). */
public enum ModePaiement implements Libelle {
    ESPECES("Espèces", "REC"), CHEQUE("Chèque", "CHQ"), CARTE("Carte", "CB"), VIREMENT("Virement", "VIR");

    private final String libelle;
    private final String prefixePreuve;
    ModePaiement(String libelle, String prefixePreuve) { this.libelle = libelle; this.prefixePreuve = prefixePreuve; }
    @Override public String getLibelle() { return libelle; }
    public String getPrefixePreuve() { return prefixePreuve; }
    @Override public String toString() { return libelle; }
}
