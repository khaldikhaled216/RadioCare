package com.radiocare.model.enums;

/**
 * Contre-indications médicales majeures inscrites au dossier du patient (Conflit 5).
 * Chaque contre-indication sait si elle interdit un examen donné (polymorphisme par constante).
 */
public enum ContreIndication implements Libelle {
    PACEMAKER("Porteur de pacemaker") {
        @Override public boolean interdit(TypeEquipement eq, CategorieConsommable produit) { return eq == TypeEquipement.IRM; }
    },
    IMPLANT_METALLIQUE("Implant métallique non compatible IRM") {
        @Override public boolean interdit(TypeEquipement eq, CategorieConsommable produit) { return eq == TypeEquipement.IRM; }
    },
    ALLERGIE_IODE("Allergie au produit de contraste iodé") {
        @Override public boolean interdit(TypeEquipement eq, CategorieConsommable produit) { return produit == CategorieConsommable.CONTRASTE_IODE; }
    },
    ALLERGIE_GADOLINIUM("Allergie au Gadolinium") {
        @Override public boolean interdit(TypeEquipement eq, CategorieConsommable produit) { return produit == CategorieConsommable.CONTRASTE_GADOLINIUM; }
    },
    INSUFFISANCE_RENALE_SEVERE("Insuffisance rénale sévère") {
        @Override public boolean interdit(TypeEquipement eq, CategorieConsommable produit) { return produit != null && produit.isProduitContraste(); }
    },
    GROSSESSE("Grossesse en cours") {
        @Override public boolean interdit(TypeEquipement eq, CategorieConsommable produit) { return eq != null && eq.isIonisant(); }
    };

    private final String libelle;
    ContreIndication(String libelle) { this.libelle = libelle; }
    @Override public String getLibelle() { return libelle; }

    /**
     * @param eq      type d'équipement requis par l'examen
     * @param produit catégorie du consommable injecté (null si aucun)
     */
    public abstract boolean interdit(TypeEquipement eq, CategorieConsommable produit);

    @Override public String toString() { return libelle; }
}
