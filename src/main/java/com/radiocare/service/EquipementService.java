package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.*;
import com.radiocare.model.enums.StatutEquipement;
import com.radiocare.model.enums.StatutSalle;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** F2 : gestion des équipements, des salles, de l'usure et des alertes de maintenance. */
public class EquipementService {
    private final DataStore store;

    public EquipementService(DataStore store) { this.store = store; }

    /** Ligne du rapport d'utilisation d'un équipement. */
    public record LigneUtilisation(EquipementImagerie equipement, String salle, double heuresCumulees, double heuresRestantes,
                                   double heuresPlanifiees, double pourcentagePotentiel, boolean alerte) { }

    // ----------------------------------------------------------------- Équipements (CRUD)
    public List<EquipementImagerie> listerEquipements() {
        return store.equipements.findAll().stream().sorted(Comparator.comparing(EquipementImagerie::getId)).toList();
    }

    public EquipementImagerie creerEquipement(EquipementImagerie e) {
        if (e.getIdEquipement() == null || e.getIdEquipement().isBlank()) e.setIdEquipement(store.equipements.nextId());
        validerEquipement(e);
        return store.equipements.create(e);
    }

    public EquipementImagerie modifierEquipement(EquipementImagerie e) {
        EquipementImagerie existant = store.equipements.findById(e.getId()).orElseThrow(() -> new ValidationException("Équipement inconnu."));
        validerEquipement(e);
        e.setInterventions(existant.getInterventions());
        store.salles.findWhere(s -> e.getId().equals(s.getEquipementAttribueId())).forEach(s -> {
            if (s.getTypeSalle().getTypeEquipementCompatible() != e.getType())
                throw new ValidationException("Le type ne correspond plus à la salle " + s.getNom() + " qui accueille cet équipement.");
        });
        return store.equipements.update(e);
    }

    public void supprimerEquipement(String id) {
        if (!store.salles.findWhere(s -> id.equals(s.getEquipementAttribueId())).isEmpty())
            throw new ValidationException("Cet équipement est attribué à une salle : retirez-le d'abord de la salle.");
        store.equipements.delete(id);
    }

    private void validerEquipement(EquipementImagerie e) {
        Validateur.nonNul(e.getType(), "Type");
        Validateur.obligatoire(e.getMarque(), "Marque");
        Validateur.positifOuNul(e.getHeuresCumulees(), "Heures cumulées");
        Validateur.positif(e.getIntervalleMaintenanceHeures(), "Intervalle de maintenance");
        Validateur.positifOuNul(e.getHeuresRestantesMaintenance(), "Heures restantes");
        if (e.getHeuresRestantesMaintenance() > e.getIntervalleMaintenanceHeures())
            throw new ValidationException("Les heures restantes ne peuvent dépasser l'intervalle de maintenance.");
        Validateur.nonNul(e.getStatut(), "Statut");
    }

    public void changerStatut(String idEquipement, StatutEquipement statut) {
        EquipementImagerie e = trouverEquipement(idEquipement);
        e.setStatut(statut);
        store.equipements.update(e);
    }

    public EquipementImagerie trouverEquipement(String id) {
        return store.equipements.findById(id).orElseThrow(() -> new ValidationException("Équipement inconnu : " + id));
    }

    /** Décompte d'heures après un examen réalisé. Passe l'appareil en maintenance si le potentiel est épuisé. */
    public void enregistrerUtilisation(String idEquipement, double heures) {
        EquipementImagerie e = trouverEquipement(idEquipement);
        e.enregistrerUtilisation(heures);
        if (e.getHeuresRestantesMaintenance() <= 0) e.setStatut(StatutEquipement.EN_MAINTENANCE);
        store.equipements.update(e);
    }

    /** Enregistre une intervention de maintenance avec sa pièce justificative textuelle (F2/F6). */
    public InterventionMaintenance enregistrerMaintenance(String idEquipement, LocalDate date, boolean preventive,
                                                          String description, String refPiece, double cout) {
        EquipementImagerie e = trouverEquipement(idEquipement);
        Validateur.nonNul(date, "Date");
        Validateur.obligatoire(description, "Description");
        Validateur.positifOuNul(cout, "Coût");
        String ref = Validateur.reference(refPiece, "Référence pièce justificative");
        String id = idEquipement + "-INT-" + String.format("%03d", e.getInterventions().size() + 1);
        InterventionMaintenance i = new InterventionMaintenance(id, date, preventive, description.trim(), ref, cout);
        e.effectuerMaintenance(i);
        store.equipements.update(e);
        return i;
    }

    public boolean estEnAlerteMaintenance(EquipementImagerie e) {
        double seuil = Math.max(ParametresCentre.SEUIL_ALERTE_MAINTENANCE_HEURES,
                e.getIntervalleMaintenanceHeures() * ParametresCentre.SEUIL_ALERTE_MAINTENANCE_POURCENT / 100.0);
        return e.getHeuresRestantesMaintenance() <= seuil;
    }

    public List<EquipementImagerie> alertesMaintenance() {
        return listerEquipements().stream().filter(this::estEnAlerteMaintenance).toList();
    }

    /** Rapport d'utilisation : heures restantes avant maintenance et heures déjà engagées par le planning. */
    public List<LigneUtilisation> rapportUtilisation() {
        List<LigneUtilisation> res = new ArrayList<>();
        for (EquipementImagerie e : listerEquipements()) {
            List<String> sallesIds = store.salles.findWhere(s -> e.getId().equals(s.getEquipementAttribueId())).stream().map(SalleRadiologie::getId).toList();
            String salle = sallesIds.isEmpty() ? "—" : String.join(", ", sallesIds);
            double planifiees = store.rendezVous.findWhere(r -> r.isActif() && sallesIds.contains(r.getIdSalle()))
                    .stream().mapToDouble(r -> r.getDureeMinutes() / 60.0).sum();
            res.add(new LigneUtilisation(e, salle, e.getHeuresCumulees(), e.getHeuresRestantesMaintenance(),
                    Validateur.arrondi(planifiees), Validateur.arrondi(e.getPourcentagePotentielRestant()), estEnAlerteMaintenance(e)));
        }
        return res;
    }

    public double depensesMaintenance(YearMonth mois) {
        return store.equipements.findAll().stream().flatMap(e -> e.getInterventions().stream())
                .filter(i -> YearMonth.from(i.getDate()).equals(mois)).mapToDouble(InterventionMaintenance::getCout).sum();
    }

    // ----------------------------------------------------------------- Salles (CRUD)
    public List<SalleRadiologie> listerSalles() {
        return store.salles.findAll().stream().sorted(Comparator.comparing(SalleRadiologie::getId)).toList();
    }

    public SalleRadiologie creerSalle(SalleRadiologie s) {
        if (s.getIdSalle() == null || s.getIdSalle().isBlank()) s.setIdSalle(store.salles.nextId());
        validerSalle(s);
        return store.salles.create(s);
    }

    public SalleRadiologie modifierSalle(SalleRadiologie s) {
        if (!store.salles.exists(s.getId())) throw new ValidationException("Salle inconnue.");
        validerSalle(s);
        return store.salles.update(s);
    }

    public void supprimerSalle(String id) {
        if (!store.rendezVous.findWhere(r -> r.getIdSalle().equals(id)).isEmpty())
            throw new ValidationException("Cette salle est référencée par des rendez-vous : passez-la plutôt au statut « Fermée ».");
        store.salles.delete(id);
    }

    private void validerSalle(SalleRadiologie s) {
        Validateur.obligatoire(s.getNom(), "Nom");
        Validateur.nonNul(s.getTypeSalle(), "Type de salle");
        Validateur.positifOuNul(s.getEpaisseurPlombMm(), "Épaisseur plomb");
        Validateur.entre(s.getNumeroEtage(), -3, 20, "Numéro d'étage");
        if (s.getStatutDispo() == null) s.setStatutDispo(StatutSalle.DISPONIBLE);
        if (s.getNormeBlindagePlomb() != null && !s.getNormeBlindagePlomb().isBlank())
            s.setNormeBlindagePlomb(Validateur.reference(s.getNormeBlindagePlomb(), "Norme de blindage"));
        if (s.getEquipementAttribueId() != null && !s.getEquipementAttribueId().isBlank()) {
            EquipementImagerie e = trouverEquipement(s.getEquipementAttribueId());
            if (e.getType() != s.getTypeSalle().getTypeEquipementCompatible())
                throw new ValidationException("Une " + s.getTypeSalle() + " ne peut accueillir qu'un équipement de type "
                        + s.getTypeSalle().getTypeEquipementCompatible() + " (reçu : " + e.getType() + ").");
            boolean dejaAttribue = store.salles.findAll().stream()
                    .anyMatch(o -> !o.getId().equals(s.getId()) && Objects.equals(o.getEquipementAttribueId(), e.getId()));
            if (dejaAttribue) throw new ValidationException("L'équipement " + e.getId() + " est déjà attribué à une autre salle.");
        } else {
            s.setEquipementAttribueId(null);
        }
    }

    /** Vérification de la disponibilité et de la conformité technique d'une salle (liste vide = conforme). */
    public List<String> verifierConformite(SalleRadiologie s) {
        List<String> anomalies = new ArrayList<>();
        if (!s.isDisponible()) anomalies.add("Salle " + s.getStatutDispo().getLibelle().toLowerCase());
        if (!s.isBlindageConforme())
            anomalies.add("Blindage insuffisant : " + s.getEpaisseurPlombMm() + " mm < "
                    + s.getTypeSalle().getTypeEquipementCompatible().getBlindagePlombMinMm() + " mm requis");
        if (s.getEquipementAttribueId() == null) anomalies.add("Aucun équipement attribué");
        else store.equipements.findById(s.getEquipementAttribueId()).ifPresentOrElse(e -> {
            if (!e.isOperationnel()) anomalies.add("Équipement " + e.getId() + " " + e.getStatut().getLibelle().toLowerCase());
        }, () -> anomalies.add("Équipement attribué introuvable"));
        return anomalies;
    }
}
