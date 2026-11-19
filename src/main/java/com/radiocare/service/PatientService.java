package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.*;
import com.radiocare.model.enums.StatutRdv;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** F1 : gestion intégrée des patients, dossiers médicaux et bilan financier individuel. */
public class PatientService {
    private final DataStore store;

    public PatientService(DataStore store) { this.store = store; }

    /** Ligne de l'historique d'imagerie d'un patient. */
    public record LigneHistorique(String idRdv, LocalDate date, String examen, String statut, String resultat, double montantTTC) { }

    /** Bilan financier individuel (F1 / entité Patient). */
    public record BilanPatient(int nombreExamens, double totalActesTTC, double partAssurance, double partPatient,
                               double dejaRegle, double resteAPayer, double soldeCompte) { }

    public List<Patient> lister() {
        return store.patients.findAll().stream().sorted(Comparator.comparing(Patient::getNom).thenComparing(Patient::getPrenom)).toList();
    }

    /** Recherche multi-critères (CIN, nom, prénom, téléphone, n° d'assurance). */
    public List<Patient> rechercher(String texte) {
        if (texte == null || texte.isBlank()) return lister();
        String t = texte.toLowerCase(Locale.ROOT).trim();
        return lister().stream().filter(p -> contient(p.getCin(), t) || contient(p.getNom(), t) || contient(p.getPrenom(), t)
                || contient(p.getTelephone(), t) || contient(p.getNumAssurance(), t) || contient(p.getNomComplet(), t)).toList();
    }

    private static boolean contient(String champ, String t) { return champ != null && champ.toLowerCase(Locale.ROOT).contains(t); }

    public Patient trouver(String cin) {
        return store.patients.findById(cin).orElseThrow(() -> new ValidationException("Patient introuvable : " + cin));
    }

    public Patient creer(Patient p) {
        valider(p);
        if (store.patients.exists(p.getCin())) throw new ValidationException("Un patient avec le CIN " + p.getCin() + " existe déjà.");
        return store.patients.create(p);
    }

    /** Modification des données administratives ; le solde et l'historique (calculés) sont préservés. */
    public Patient modifier(Patient p) {
        valider(p);
        Patient existant = trouver(p.getCin());
        p.setSoldeCompte(existant.getSoldeCompte());
        p.setHistoriqueExamensIds(existant.getHistoriqueExamensIds());
        return store.patients.update(p);
    }

    public void supprimer(String cin) {
        trouver(cin);
        if (!store.rendezVous.findWhere(r -> r.getIdPatient().equals(cin)).isEmpty())
            throw new ValidationException("Ce patient possède des rendez-vous : supprimez ou archivez-les d'abord.");
        if (!store.factures.findWhere(f -> f.getIdPatient().equals(cin)).isEmpty())
            throw new ValidationException("Ce patient possède des factures : suppression impossible.");
        store.patients.delete(cin);
    }

    private void valider(Patient p) {
        Validateur.cin(p.getCin());
        Validateur.obligatoire(p.getNom(), "Nom");
        Validateur.obligatoire(p.getPrenom(), "Prénom");
        Validateur.nonNul(p.getDateNaissance(), "Date de naissance");
        if (p.getDateNaissance().isAfter(LocalDate.now())) throw new ValidationException("La date de naissance est dans le futur.");
        Validateur.telephoneOptionnel(p.getTelephone());
        Validateur.emailOptionnel(p.getEmail());
        Validateur.entre(p.getTauxCouvertureAssurance(), 0, 1, "Taux de couverture");
        if (p.getTauxCouvertureAssurance() > 0 && (p.getNumAssurance() == null || p.getNumAssurance().isBlank()))
            throw new ValidationException("Un numéro d'assurance est requis lorsque le taux de couverture est non nul.");
    }

    /** Historique complet des examens d'imagerie du patient (tous statuts, du plus récent au plus ancien). */
    public List<LigneHistorique> historique(String cin) {
        return store.rendezVous.findWhere(r -> r.getIdPatient().equals(cin)).stream()
                .sorted(Comparator.comparing(RendezVous::getDebut).reversed())
                .map(r -> {
                    String examen = store.examens.findById(r.getIdExamen()).map(ExamenMedical::getLibelle).orElse(r.getIdExamen());
                    String resultat = store.comptesRendus.findWhere(c -> c.getIdRdv().equals(r.getIdRdv())).stream()
                            .findFirst().map(c -> c.getCodeResultat().getLibelle()).orElse(r.getStatut() == StatutRdv.REALISE ? "CR en attente" : "—");
                    double ttc = store.factures.findWhere(f -> r.getIdRdv().equals(f.getIdRdv())).stream()
                            .mapToDouble(FactureMedicale::getMontantTTC).sum();
                    return new LigneHistorique(r.getIdRdv(), r.getDate(), examen, r.getStatut().getLibelle(), resultat, ttc);
                }).toList();
    }

    /** Montant global des actes, part assurance, montants déjà réglés et solde restant à payer. */
    public BilanPatient bilan(String cin) {
        Patient p = trouver(cin);
        List<FactureMedicale> factures = store.factures.findWhere(f -> f.getIdPatient().equals(cin));
        double ttc = factures.stream().mapToDouble(FactureMedicale::getMontantTTC).sum();
        double assurance = factures.stream().mapToDouble(FactureMedicale::getPartAssurance).sum();
        double partPatient = factures.stream().mapToDouble(FactureMedicale::getPartPatient).sum();
        double regle = factures.stream().mapToDouble(FactureMedicale::getMontantPaye).sum();
        int nb = (int) store.rendezVous.findWhere(r -> r.getIdPatient().equals(cin) && r.getStatut() == StatutRdv.REALISE).size();
        return new BilanPatient(nb, Validateur.arrondi(ttc), Validateur.arrondi(assurance), Validateur.arrondi(partPatient),
                Validateur.arrondi(regle), Validateur.arrondi(Math.max(0, partPatient - regle)), Validateur.arrondi(p.getSoldeCompte()));
    }
}
