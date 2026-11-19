package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.*;
import com.radiocare.model.enums.ModePaiement;
import com.radiocare.model.enums.StatutPaiement;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** F5 (partie patient) : facturation avec TVA 19 %, tiers payant et paiements avec mise à jour du solde. */
public class FacturationService {
    private final DataStore store;

    public FacturationService(DataStore store) { this.store = store; }

    /** Résultat du calcul de facturation. */
    public record Montants(double ht, double tva, double ttc, double partAssurance, double partPatient) { }

    /** TTC = HT × (1 + 19 %) ; part assurance = TTC × taux de couverture ; part patient = TTC − part assurance. */
    public static Montants calculerMontants(double montantHT, double tauxCouverture) {
        double ht = Validateur.arrondi(montantHT);
        double tva = Validateur.arrondi(ht * FactureMedicale.TAUX_TVA);
        double ttc = Validateur.arrondi(ht + tva);
        double assurance = Validateur.arrondi(ttc * Math.max(0, Math.min(1, tauxCouverture)));
        double patient = Validateur.arrondi(ttc - assurance);
        return new Montants(ht, tva, ttc, assurance, patient);
    }

    public List<FactureMedicale> listerFactures() {
        return store.factures.findAll().stream().sorted(Comparator.comparing(FactureMedicale::getDateEmission).reversed()
                .thenComparing(FactureMedicale::getNumFacture, Comparator.reverseOrder())).toList();
    }

    public FactureMedicale trouverFacture(String num) {
        return store.factures.findById(num).orElseThrow(() -> new ValidationException("Facture inconnue : " + num));
    }

    /** Génère la facture d'un RDV réalisé et débite la part patient sur son solde. */
    public FactureMedicale genererFacture(RendezVous rdv, LocalDate dateEmission) {
        if (!store.factures.findWhere(f -> rdv.getIdRdv().equals(f.getIdRdv())).isEmpty())
            throw new ValidationException("Le rendez-vous " + rdv.getIdRdv() + " est déjà facturé.");
        Patient p = store.patients.findById(rdv.getIdPatient()).orElseThrow(() -> new ValidationException("Patient inconnu."));
        ExamenMedical ex = store.examens.findById(rdv.getIdExamen()).orElseThrow(() -> new ValidationException("Examen inconnu."));
        Montants m = calculerMontants(ex.getTarifHT(), p.isAssure() ? p.getTauxCouvertureAssurance() : 0);
        FactureMedicale f = new FactureMedicale(store.factures.nextId(), p.getId(), rdv.getIdRdv(), dateEmission, ParametresCentre.DELAI_PAIEMENT_JOURS);
        f.setMontantHT(m.ht());
        f.setMontantTTC(m.ttc());
        f.setPartAssurance(m.partAssurance());
        f.setPartPatient(m.partPatient());
        f.setStatutPaiement(m.partPatient() <= 0 ? StatutPaiement.REGLEE : StatutPaiement.EN_ATTENTE);
        store.factures.create(f);
        p.debiter(m.partPatient());
        store.patients.update(p);
        return f;
    }

    /** Suppression d'une facture sans paiement (annulation) : le débit sur le solde patient est annulé. */
    public void supprimerFacture(String num) {
        FactureMedicale f = trouverFacture(num);
        if (!store.paiements.findWhere(p -> p.getNumFacture().equals(num)).isEmpty())
            throw new ValidationException("Cette facture a des paiements enregistrés : supprimez-les d'abord.");
        store.patients.findById(f.getIdPatient()).ifPresent(p -> { p.crediter(f.getPartPatient()); store.patients.update(p); });
        store.factures.delete(num);
    }

    /** Recalcule le statut (Réglée / En Attente / Retard) d'une facture à une date. */
    public StatutPaiement statutA(FactureMedicale f, LocalDate aujourdHui) {
        if (f.getResteAPayer() <= 0.0005) return StatutPaiement.REGLEE;
        if (f.getDateEcheance() != null && aujourdHui.isAfter(f.getDateEcheance())) return StatutPaiement.RETARD;
        return StatutPaiement.EN_ATTENTE;
    }

    /** Met à jour les statuts de toutes les factures (passage automatique en « Retard »). */
    public int actualiserStatuts(LocalDate aujourdHui) {
        int n = 0;
        for (FactureMedicale f : store.factures.findAll()) {
            StatutPaiement s = statutA(f, aujourdHui);
            if (s != f.getStatutPaiement()) { f.setStatutPaiement(s); store.factures.update(f); n++; }
        }
        return n;
    }

    public List<FactureMedicale> facturesEnRetard() {
        return store.factures.findWhere(f -> f.getStatutPaiement() == StatutPaiement.RETARD);
    }

    // ----------------------------------------------------------------- Paiements
    public List<Paiement> listerPaiements() {
        return store.paiements.findAll().stream().sorted(Comparator.comparing(Paiement::getDatePaiement).reversed()).toList();
    }

    public List<Paiement> paiementsDeFacture(String num) {
        return store.paiements.findWhere(p -> p.getNumFacture().equals(num));
    }

    /** Enregistre un paiement : preuve normée, pas de trop-perçu, mise à jour automatique du solde et du statut. */
    public Paiement enregistrerPaiement(String numFacture, LocalDate date, double montant, ModePaiement mode, String preuve) {
        FactureMedicale f = trouverFacture(numFacture);
        Validateur.nonNul(date, "Date de paiement");
        Validateur.nonNul(mode, "Mode de paiement");
        Validateur.positif(montant, "Montant reçu");
        if (date.isBefore(f.getDateEmission())) throw new ValidationException("La date de paiement précède l'émission de la facture.");
        double reste = f.getResteAPayer();
        if (montant > reste + 0.0005)
            throw new ValidationException(String.format(java.util.Locale.FRANCE, "Le montant (%.3f TND) dépasse le reste à payer (%.3f TND).", montant, reste));
        String ref = Validateur.preuvePaiement(preuve, mode);
        Paiement p = new Paiement(store.paiements.nextId(), numFacture, date, Validateur.arrondi(montant), mode, ref);
        store.paiements.create(p);
        f.setMontantPaye(Validateur.arrondi(f.getMontantPaye() + p.getMontantRecu()));
        f.setStatutPaiement(statutA(f, LocalDate.now()));
        store.factures.update(f);
        Patient patient = store.patients.findById(f.getIdPatient()).orElseThrow();
        patient.crediter(p.getMontantRecu());
        patient.setSoldeCompte(Validateur.arrondi(patient.getSoldeCompte()));
        store.patients.update(patient);
        return p;
    }

    /** Annulation d'un paiement (erreur de saisie) : opérations inverses. */
    public void supprimerPaiement(String idPaiement) {
        Paiement p = store.paiements.findById(idPaiement).orElseThrow(() -> new ValidationException("Paiement inconnu."));
        FactureMedicale f = trouverFacture(p.getNumFacture());
        f.setMontantPaye(Validateur.arrondi(f.getMontantPaye() - p.getMontantRecu()));
        f.setStatutPaiement(statutA(f, LocalDate.now()));
        store.factures.update(f);
        store.patients.findById(f.getIdPatient()).ifPresent(pat -> {
            pat.debiter(p.getMontantRecu());
            pat.setSoldeCompte(Validateur.arrondi(pat.getSoldeCompte()));
            store.patients.update(pat);
        });
        store.paiements.delete(idPaiement);
    }
}
