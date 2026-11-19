package com.radiocare.model;

import com.radiocare.model.enums.ContreIndication;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité 1 : Patient. L'identifiant est le CIN / matricule.
 * <p>Convention du solde : {@code soldeCompte} = paiements reçus − parts patient facturées.
 * Un solde négatif signifie que le patient est débiteur.</p>
 */
public class Patient extends Personne {
    private LocalDate dateNaissance;
    private String numAssurance;
    /** Taux de prise en charge par l'assurance (0.0 à 1.0), 0 si non assuré. */
    private double tauxCouvertureAssurance;
    private double soldeCompte;
    private List<String> historiqueExamensIds = new ArrayList<>();
    private List<ContreIndication> contreIndications = new ArrayList<>();

    public Patient() { }

    public Patient(String cinMatricule, String nom, String prenom, LocalDate dateNaissance, String telephone,
                   String email, String numAssurance, double tauxCouvertureAssurance) {
        super(cinMatricule, nom, prenom, telephone, email);
        this.dateNaissance = dateNaissance;
        this.numAssurance = numAssurance;
        this.tauxCouvertureAssurance = tauxCouvertureAssurance;
    }

    @Override public String getId() { return cin; }

    public boolean isAssure() { return numAssurance != null && !numAssurance.isBlank() && tauxCouvertureAssurance > 0; }

    public int getAge() { return dateNaissance == null ? 0 : Period.between(dateNaissance, LocalDate.now()).getYears(); }

    public void ajouterExamenHistorique(String idRdv) {
        if (!historiqueExamensIds.contains(idRdv)) historiqueExamensIds.add(idRdv);
    }

    public void crediter(double montant) { soldeCompte += montant; }
    public void debiter(double montant) { soldeCompte -= montant; }

    public String getCinMatricule() { return cin; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getNumAssurance() { return numAssurance; }
    public void setNumAssurance(String numAssurance) { this.numAssurance = numAssurance; }
    public double getTauxCouvertureAssurance() { return tauxCouvertureAssurance; }
    public void setTauxCouvertureAssurance(double t) { this.tauxCouvertureAssurance = t; }
    public double getSoldeCompte() { return soldeCompte; }
    public void setSoldeCompte(double soldeCompte) { this.soldeCompte = soldeCompte; }
    public List<String> getHistoriqueExamensIds() { return historiqueExamensIds; }
    public void setHistoriqueExamensIds(List<String> ids) { this.historiqueExamensIds = ids == null ? new ArrayList<>() : ids; }
    public List<ContreIndication> getContreIndications() { return contreIndications; }
    public void setContreIndications(List<ContreIndication> ci) { this.contreIndications = ci == null ? new ArrayList<>() : ci; }
}
