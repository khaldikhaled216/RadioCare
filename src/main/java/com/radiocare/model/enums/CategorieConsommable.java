package com.radiocare.model.enums;

public enum CategorieConsommable implements Libelle {
    CONTRASTE_IODE("Produit de contraste iodé", true),
    CONTRASTE_GADOLINIUM("Produit de contraste Gadolinium", true),
    GEL_ECHO("Gel d'échographie", false),
    SERINGUE("Seringue haute pression", false),
    FILM("Film radiographique", false);

    private final String libelle;
    private final boolean produitContraste;
    CategorieConsommable(String libelle, boolean produitContraste) { this.libelle = libelle; this.produitContraste = produitContraste; }
    @Override public String getLibelle() { return libelle; }
    public boolean isProduitContraste() { return produitContraste; }
    @Override public String toString() { return libelle; }
}
