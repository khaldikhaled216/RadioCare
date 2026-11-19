package com.radiocare.model;

/** Abstraction du personnel médical : chaque catégorie définit sa propre formule de rémunération. */
public abstract class PersonnelMedical extends Personne {

    protected PersonnelMedical() { }

    protected PersonnelMedical(String cin, String nom, String prenom, String telephone, String email) {
        super(cin, nom, prenom, telephone, email);
    }

    /** Libellé de la fonction (Médecin radiologue, Manipulateur radio...). */
    public abstract String getFonction();

    /** Calcul de la rémunération mensuelle (honoraires ou salaire) à partir de l'activité du mois. */
    public abstract double calculerRemuneration(ActiviteMensuelle activite);
}
