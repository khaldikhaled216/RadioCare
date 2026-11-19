package com.radiocare.model;

import com.radiocare.model.enums.StatutLivraison;

import java.time.LocalDate;

/** Entité 13 (extension) : Commande fournisseur de réapprovisionnement. */
public class CommandeFournisseur implements Identifiable {
    private String idCommande;
    private String idProduit;
    private String nomFournisseur;
    private int quantiteCommandee;
    private LocalDate dateCommande;
    private LocalDate dateLivraisonPrevue;
    private double montantTotal;
    private String refFactureTextuelle;
    private StatutLivraison statutLivraison = StatutLivraison.EN_COURS;
    /** Nouvelle date de péremption du lot livré (optionnelle). */
    private LocalDate datePeremptionLot;

    public CommandeFournisseur() { }

    public CommandeFournisseur(String idCommande, String idProduit, String nomFournisseur, int quantiteCommandee,
                               LocalDate dateCommande, LocalDate dateLivraisonPrevue, double montantTotal,
                               String refFactureTextuelle) {
        this.idCommande = idCommande;
        this.idProduit = idProduit;
        this.nomFournisseur = nomFournisseur;
        this.quantiteCommandee = quantiteCommandee;
        this.dateCommande = dateCommande;
        this.dateLivraisonPrevue = dateLivraisonPrevue;
        this.montantTotal = montantTotal;
        this.refFactureTextuelle = refFactureTextuelle;
    }

    @Override public String getId() { return idCommande; }

    public String getIdCommande() { return idCommande; }
    public void setIdCommande(String idCommande) { this.idCommande = idCommande; }
    public String getIdProduit() { return idProduit; }
    public void setIdProduit(String idProduit) { this.idProduit = idProduit; }
    public String getNomFournisseur() { return nomFournisseur; }
    public void setNomFournisseur(String n) { this.nomFournisseur = n; }
    public int getQuantiteCommandee() { return quantiteCommandee; }
    public void setQuantiteCommandee(int q) { this.quantiteCommandee = q; }
    public LocalDate getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDate d) { this.dateCommande = d; }
    public LocalDate getDateLivraisonPrevue() { return dateLivraisonPrevue; }
    public void setDateLivraisonPrevue(LocalDate d) { this.dateLivraisonPrevue = d; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }
    public String getRefFactureTextuelle() { return refFactureTextuelle; }
    public void setRefFactureTextuelle(String r) { this.refFactureTextuelle = r; }
    public StatutLivraison getStatutLivraison() { return statutLivraison; }
    public void setStatutLivraison(StatutLivraison s) { this.statutLivraison = s; }
    public LocalDate getDatePeremptionLot() { return datePeremptionLot; }
    public void setDatePeremptionLot(LocalDate d) { this.datePeremptionLot = d; }
}
