package com.radiocare.model;

import com.radiocare.model.enums.CategorieConsommable;

import java.time.LocalDate;

/** Entité 11 (extension) : Consommable médical en stock. */
public class ConsommableMedical implements Identifiable {
    private String idProduit;
    private String designation;
    private CategorieConsommable categorie;
    private int quantiteEnStock;
    private int seuilAlerte;
    private double prixUnitaireHT;
    private LocalDate datePeremption;

    public ConsommableMedical() { }

    public ConsommableMedical(String idProduit, String designation, CategorieConsommable categorie, int quantiteEnStock,
                              int seuilAlerte, double prixUnitaireHT, LocalDate datePeremption) {
        this.idProduit = idProduit;
        this.designation = designation;
        this.categorie = categorie;
        this.quantiteEnStock = quantiteEnStock;
        this.seuilAlerte = seuilAlerte;
        this.prixUnitaireHT = prixUnitaireHT;
        this.datePeremption = datePeremption;
    }

    @Override public String getId() { return idProduit; }

    public boolean isStockCritique() { return quantiteEnStock < seuilAlerte; }

    public boolean isPerimeA(LocalDate date) { return datePeremption != null && datePeremption.isBefore(date); }

    public boolean expireAvant(LocalDate limite) { return datePeremption != null && !datePeremption.isAfter(limite); }

    public double getValeurStock() { return quantiteEnStock * prixUnitaireHT; }

    public String getIdProduit() { return idProduit; }
    public void setIdProduit(String idProduit) { this.idProduit = idProduit; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public CategorieConsommable getCategorie() { return categorie; }
    public void setCategorie(CategorieConsommable categorie) { this.categorie = categorie; }
    public int getQuantiteEnStock() { return quantiteEnStock; }
    public void setQuantiteEnStock(int q) { this.quantiteEnStock = q; }
    public int getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(int seuilAlerte) { this.seuilAlerte = seuilAlerte; }
    public double getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(double p) { this.prixUnitaireHT = p; }
    public LocalDate getDatePeremption() { return datePeremption; }
    public void setDatePeremption(LocalDate d) { this.datePeremption = d; }

    @Override public String toString() { return idProduit + " - " + designation; }
}
