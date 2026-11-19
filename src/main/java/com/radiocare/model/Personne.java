package com.radiocare.model;

/** Classe abstraite factorisant l'identité civile commune aux patients et au personnel. */
public abstract class Personne implements Identifiable {
    protected String cin;
    protected String nom;
    protected String prenom;
    protected String telephone;
    protected String email;

    protected Personne() { }

    protected Personne(String cin, String nom, String prenom, String telephone, String email) {
        this.cin = cin;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
    }

    public String getNomComplet() { return prenom + " " + nom; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override public String toString() { return getNomComplet() + " (" + getId() + ")"; }
}
