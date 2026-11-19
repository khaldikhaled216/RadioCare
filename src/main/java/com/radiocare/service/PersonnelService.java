package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.*;
import com.radiocare.model.enums.StatutRdv;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** F3 : gestion du personnel médical et calcul automatique des rémunérations mensuelles. */
public class PersonnelService {
    private final DataStore store;

    public PersonnelService(DataStore store) { this.store = store; }

    /** Ligne de la fiche de rémunération mensuelle. */
    public record LigneRemuneration(String id, String nomComplet, String fonction, int nombreActes, double montantActesHT,
                                    double heuresActes, double heuresGarde, double remuneration) { }

    // ----------------------------------------------------------------- Médecins
    public List<MedecinRadiologue> listerMedecins() {
        return store.medecins.findAll().stream().sorted(Comparator.comparing(MedecinRadiologue::getNom)).toList();
    }

    public MedecinRadiologue creerMedecin(MedecinRadiologue m) {
        if (m.getIdMedecin() == null || m.getIdMedecin().isBlank()) m.setIdMedecin(store.medecins.nextId());
        validerPersonne(m);
        Validateur.nonNul(m.getSpecialite(), "Spécialité");
        Validateur.entre(m.getTarifPourcentageActe(), 0.01, 100, "Pourcentage sur actes");
        if (m.getDispoPlanning() == null || m.getDispoPlanning().getJours().isEmpty())
            throw new ValidationException("Le planning de disponibilité doit comporter au moins un jour.");
        if (!m.getDispoPlanning().getHeureDebut().isBefore(m.getDispoPlanning().getHeureFin()))
            throw new ValidationException("L'heure de début de disponibilité doit précéder l'heure de fin.");
        if (store.medecins.exists(m.getId())) return store.medecins.update(m);
        return store.medecins.create(m);
    }

    public MedecinRadiologue modifierMedecin(MedecinRadiologue m) {
        if (!store.medecins.exists(m.getId())) throw new ValidationException("Médecin inconnu.");
        return creerMedecin(m);
    }

    public void supprimerMedecin(String id) {
        if (!store.rendezVous.findWhere(r -> r.getIdMedecin().equals(id)).isEmpty())
            throw new ValidationException("Ce médecin est référencé par des rendez-vous : suppression impossible.");
        if (!store.teleExpertises.findWhere(t -> id.equals(t.getIdMedecinDemandeur())).isEmpty())
            throw new ValidationException("Ce médecin est référencé par des demandes de télé-expertise.");
        store.medecins.delete(id);
    }

    // ----------------------------------------------------------------- Manipulateurs
    public List<ManipulateurRadio> listerManipulateurs() {
        return store.manipulateurs.findAll().stream().sorted(Comparator.comparing(ManipulateurRadio::getNom)).toList();
    }

    public ManipulateurRadio creerManipulateur(ManipulateurRadio m) {
        if (m.getIdManip() == null || m.getIdManip().isBlank()) m.setIdManip(store.manipulateurs.nextId());
        validerPersonne(m);
        if (m.getHabilitations().isEmpty()) throw new ValidationException("Au moins une habilitation est requise.");
        Validateur.positif(m.getTarifHoraire(), "Tarif horaire");
        Validateur.nonNul(m.getStatutDispo(), "Statut de disponibilité");
        if (store.manipulateurs.exists(m.getId())) {
            m.setHeuresGardeParMois(store.manipulateurs.findById(m.getId()).get().getHeuresGardeParMois());
            return store.manipulateurs.update(m);
        }
        return store.manipulateurs.create(m);
    }

    public ManipulateurRadio modifierManipulateur(ManipulateurRadio m) {
        if (!store.manipulateurs.exists(m.getId())) throw new ValidationException("Manipulateur inconnu.");
        return creerManipulateur(m);
    }

    public void supprimerManipulateur(String id) {
        if (!store.rendezVous.findWhere(r -> r.getIdManip().equals(id)).isEmpty())
            throw new ValidationException("Ce manipulateur est référencé par des rendez-vous : suppression impossible.");
        store.manipulateurs.delete(id);
    }

    /** Saisie des heures de garde d'un manipulateur pour un mois. */
    public void saisirHeuresGarde(String idManip, YearMonth mois, double heures) {
        Validateur.entre(heures, 0, 300, "Heures de garde");
        ManipulateurRadio m = store.manipulateurs.findById(idManip).orElseThrow(() -> new ValidationException("Manipulateur inconnu."));
        m.getHeuresGardeParMois().put(mois.toString(), heures);
        store.manipulateurs.update(m);
    }

    private void validerPersonne(PersonnelMedical p) {
        Validateur.cin(p.getCin());
        Validateur.obligatoire(p.getNom(), "Nom");
        Validateur.obligatoire(p.getPrenom(), "Prénom");
        Validateur.telephoneOptionnel(p.getTelephone());
        Validateur.emailOptionnel(p.getEmail());
        boolean cinPris = Stream.concat(store.medecins.findAll().stream(), store.manipulateurs.findAll().stream())
                .anyMatch(o -> o.getCin().equals(p.getCin()) && !o.getId().equals(p.getId()));
        if (cinPris) throw new ValidationException("Le CIN " + p.getCin() + " est déjà attribué à un autre membre du personnel.");
    }

    // ----------------------------------------------------------------- Rémunérations
    /** Agrège l'activité réalisée (RDV au statut Réalisé) d'un membre du personnel sur un mois. */
    public ActiviteMensuelle activite(PersonnelMedical p, YearMonth mois) {
        List<RendezVous> actes = store.rendezVous.findWhere(r -> r.getStatut() == StatutRdv.REALISE
                && YearMonth.from(r.getDate()).equals(mois)
                && (p.getId().equals(r.getIdMedecin()) || p.getId().equals(r.getIdManip())));
        double montant = actes.stream().mapToDouble(r -> store.examens.findById(r.getIdExamen()).map(ExamenMedical::getTarifHT).orElse(0.0)).sum();
        double heures = actes.stream().mapToDouble(r -> r.getDureeMinutes() / 60.0).sum();
        double garde = p instanceof ManipulateurRadio m ? m.getHeuresGarde(mois.toString()) : 0;
        return new ActiviteMensuelle(mois.toString(), actes.size(), montant, heures, garde);
    }

    /** Calcul polymorphe : honoraires (médecin) ou salaire (manipulateur). */
    public double remuneration(PersonnelMedical p, YearMonth mois) {
        return Validateur.arrondi(p.calculerRemuneration(activite(p, mois)));
    }

    /** Fiche de rémunération de fin de mois pour tout le personnel. */
    public List<LigneRemuneration> etatRemunerations(YearMonth mois) {
        List<PersonnelMedical> tous = new ArrayList<>(listerMedecins());
        tous.addAll(listerManipulateurs());
        return tous.stream().map(p -> {
            ActiviteMensuelle a = activite(p, mois);
            return new LigneRemuneration(p.getId(), p.getNomComplet(), p.getFonction(), a.nombreActes(),
                    Validateur.arrondi(a.montantActesHT()), Validateur.arrondi(a.heuresActes()), a.heuresGarde(),
                    Validateur.arrondi(p.calculerRemuneration(a)));
        }).toList();
    }

    public double totalHonorairesMedecins(YearMonth mois) {
        return listerMedecins().stream().mapToDouble(m -> remuneration(m, mois)).sum();
    }

    public double totalSalairesManipulateurs(YearMonth mois) {
        return listerManipulateurs().stream().mapToDouble(m -> remuneration(m, mois)).sum();
    }
}
