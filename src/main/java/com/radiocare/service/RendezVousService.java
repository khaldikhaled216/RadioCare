package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.*;
import com.radiocare.model.enums.StatutRdv;
import com.radiocare.model.enums.TypeConflit;
import com.radiocare.service.conflit.Conflit;
import com.radiocare.service.conflit.ConflitException;
import com.radiocare.service.conflit.MoteurConflits;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** F4 : planification des rendez-vous, cycle de vie et planning hebdomadaire, sous contrôle du moteur de conflits. */
public class RendezVousService {
    private final DataStore store;
    private final MoteurConflits moteur;
    private final EquipementService equipementService;
    private final StockService stockService;
    private final FacturationService facturationService;

    public RendezVousService(DataStore store, MoteurConflits moteur, EquipementService equipementService,
                             StockService stockService, FacturationService facturationService) {
        this.store = store;
        this.moteur = moteur;
        this.equipementService = equipementService;
        this.stockService = stockService;
        this.facturationService = facturationService;
    }

    /** Ligne du planning au format [Heure - Patient - Examen - Salle - Équipement - Médecin - Manipulateur]. */
    public record LignePlanning(RendezVous rdv, LocalDate date, String heure, String patient, String examen, String salle,
                                String equipement, String medecin, String manipulateur, String statut) { }

    public List<RendezVous> lister() {
        return store.rendezVous.findAll().stream().sorted(Comparator.comparing(RendezVous::getDebut)).toList();
    }

    public RendezVous trouver(String id) {
        return store.rendezVous.findById(id).orElseThrow(() -> new ValidationException("Rendez-vous inconnu : " + id));
    }

    /** Durée par défaut = durée paramétrée de l'examen. */
    public int dureeParDefaut(String idExamen) {
        return store.examens.findById(idExamen).map(ExamenMedical::getDureeEstimeeMinutes).orElse(30);
    }

    private void validerChamps(RendezVous r) {
        Validateur.nonNul(r.getDate(), "Date");
        Validateur.nonNul(r.getHeureDebut(), "Heure de début");
        Validateur.obligatoire(r.getIdPatient(), "Patient");
        Validateur.obligatoire(r.getIdExamen(), "Examen");
        Validateur.obligatoire(r.getIdSalle(), "Salle");
        Validateur.obligatoire(r.getIdMedecin(), "Médecin");
        Validateur.obligatoire(r.getIdManip(), "Manipulateur");
        if (r.getDureeMinutes() <= 0) r.setDureeMinutes(dureeParDefaut(r.getIdExamen()));
        Validateur.entre(r.getDureeMinutes(), 5, 240, "Durée (minutes)");
        if (!ParametresCentre.estJourOuvre(r.getDate().getDayOfWeek()))
            throw new ValidationException("Le centre est fermé le dimanche.");
        if (r.getHeureDebut().isBefore(ParametresCentre.HEURE_OUVERTURE) || r.getHeureFin().isAfter(ParametresCentre.HEURE_FERMETURE)
                || r.getHeureFin().isBefore(r.getHeureDebut()))
            throw new ValidationException("Le créneau doit être compris dans les heures d'ouverture ("
                    + ParametresCentre.HEURE_OUVERTURE + " - " + ParametresCentre.HEURE_FERMETURE + ").");
    }

    /** Simulation : liste des conflits sans enregistrement ni journalisation. */
    public List<Conflit> simuler(RendezVous r) {
        validerChamps(r);
        return moteur.verifier(r);
    }

    /** Planifie un nouveau RDV après passage obligatoire par le moteur de conflits. */
    public RendezVous planifier(RendezVous r) {
        validerChamps(r);
        if (r.getDate().isBefore(LocalDate.now()))
            throw new ValidationException("Impossible de planifier un rendez-vous dans le passé.");
        r.setIdRdv(store.rendezVous.nextId());
        r.setStatut(StatutRdv.PLANIFIE);
        if (r.getDateCreation() == null) r.setDateCreation(LocalDateTime.now());
        moteur.verifierOuRejeter(r, "Planification " + r.getIdRdv() + " (" + r.getDate() + " " + r.getHeureDebut() + ")");
        return store.rendezVous.create(r);
    }

    /** Modifie un RDV planifié (re-vérification complète, le RDV lui-même étant exclu). */
    public RendezVous modifier(RendezVous r) {
        RendezVous existant = trouver(r.getIdRdv());
        if (existant.getStatut() != StatutRdv.PLANIFIE)
            throw new ValidationException("Seul un rendez-vous planifié peut être modifié (statut actuel : " + existant.getStatut() + ").");
        validerChamps(r);
        r.setStatut(StatutRdv.PLANIFIE);
        r.setDateCreation(existant.getDateCreation());
        moteur.verifierOuRejeter(r, "Modification " + r.getIdRdv());
        return store.rendezVous.update(r);
    }

    public RendezVous annuler(String id) {
        RendezVous r = trouver(id);
        if (!r.isActif()) throw new ValidationException("Ce rendez-vous est déjà " + r.getStatut().getLibelle().toLowerCase() + ".");
        r.setStatut(StatutRdv.ANNULE);
        return store.rendezVous.update(r);
    }

    public void supprimer(String id) {
        RendezVous r = trouver(id);
        if (r.getStatut() == StatutRdv.REALISE)
            throw new ValidationException("Un examen réalisé (facturé) ne peut pas être supprimé.");
        store.rendezVous.delete(id);
    }

    /** Accueil du patient et début de l'examen (statut En Cours). */
    public RendezVous demarrer(String id, LocalTime heureArrivee, LocalTime heurePriseEnCharge) {
        RendezVous r = trouver(id);
        if (r.getStatut() != StatutRdv.PLANIFIE) throw new ValidationException("Seul un RDV planifié peut être démarré.");
        if (r.getDate().isAfter(LocalDate.now())) throw new ValidationException("Ce rendez-vous est prévu le " + r.getDate() + " : il ne peut pas encore être démarré.");
        Validateur.nonNul(heureArrivee, "Heure d'arrivée");
        Validateur.nonNul(heurePriseEnCharge, "Heure de prise en charge");
        if (heurePriseEnCharge.isBefore(heureArrivee))
            throw new ValidationException("La prise en charge ne peut précéder l'arrivée du patient.");
        r.setHeureArrivee(heureArrivee);
        r.setHeurePriseEnCharge(heurePriseEnCharge);
        r.setStatut(StatutRdv.EN_COURS);
        return store.rendezVous.update(r);
    }

    /**
     * Réalisation de l'acte : contrôle préalable (équipement opérationnel, potentiel, stock réel),
     * puis décompte des heures machine, décrément du stock, historique patient et facture automatique.
     */
    public FactureMedicale realiser(String id) {
        RendezVous r = trouver(id);
        if (!r.isActif()) throw new ValidationException("Ce rendez-vous est " + r.getStatut().getLibelle().toLowerCase() + ".");
        if (r.getDate().isAfter(LocalDate.now())) throw new ValidationException("Ce rendez-vous est prévu le " + r.getDate() + " : il ne peut pas encore être réalisé.");
        ExamenMedical ex = store.examens.findById(r.getIdExamen()).orElseThrow();
        SalleRadiologie salle = store.salles.findById(r.getIdSalle()).orElseThrow();
        EquipementImagerie eq = salle.getEquipementAttribueId() == null ? null : store.equipements.findById(salle.getEquipementAttribueId()).orElse(null);
        double heures = r.getDureeMinutes() / 60.0;
        List<Conflit> conflits = new ArrayList<>();
        if (eq == null || !eq.isOperationnel())
            conflits.add(new Conflit(TypeConflit.DOUBLE_RESERVATION, "Équipement indisponible pour réaliser l'acte."));
        else if (heures > eq.getHeuresRestantesMaintenance() + 1e-9)
            conflits.add(new Conflit(TypeConflit.POTENTIEL_HEURES, "Potentiel d'heures insuffisant sur " + eq.getId() + "."));
        if (ex.necessiteConsommable()) {
            ConsommableMedical c = store.consommables.findById(ex.getIdConsommableRequis()).orElse(null);
            if (c == null || c.getQuantiteEnStock() < ex.getQuantiteConsommable() || c.isPerimeA(r.getDate()))
                conflits.add(new Conflit(TypeConflit.RUPTURE_STOCK, "Stock réel insuffisant ou périmé pour " + ex.getIdConsommableRequis() + "."));
        }
        if (!conflits.isEmpty()) {
            moteur.journaliser(conflits, "Réalisation " + id);
            throw new ConflitException(conflits);
        }
        if (r.getHeureArrivee() == null) r.setHeureArrivee(r.getHeureDebut());
        if (r.getHeurePriseEnCharge() == null) r.setHeurePriseEnCharge(r.getHeureDebut());
        if (ex.necessiteConsommable()) stockService.consommer(ex.getIdConsommableRequis(), ex.getQuantiteConsommable(), r.getDate());
        equipementService.enregistrerUtilisation(eq.getId(), heures);
        r.setStatut(StatutRdv.REALISE);
        store.rendezVous.update(r);
        Patient p = store.patients.findById(r.getIdPatient()).orElseThrow();
        p.ajouterExamenHistorique(r.getIdRdv());
        store.patients.update(p);
        return facturationService.genererFacture(r, r.getDate());
    }

    // ----------------------------------------------------------------- Planning
    public static LocalDate lundiDe(LocalDate d) { return d.with(DayOfWeek.MONDAY); }

    public List<LignePlanning> planning(LocalDate debut, LocalDate fin) {
        return store.rendezVous.findWhere(r -> !r.getDate().isBefore(debut) && !r.getDate().isAfter(fin)).stream()
                .sorted(Comparator.comparing(RendezVous::getDebut)).map(this::ligne).toList();
    }

    public List<LignePlanning> planningSemaine(LocalDate jourQuelconque) {
        LocalDate lundi = lundiDe(jourQuelconque);
        return planning(lundi, lundi.plusDays(6));
    }

    public LignePlanning ligne(RendezVous r) {
        String patient = store.patients.findById(r.getIdPatient()).map(Patient::getNomComplet).orElse(r.getIdPatient());
        String examen = store.examens.findById(r.getIdExamen()).map(ExamenMedical::getLibelle).orElse(r.getIdExamen());
        SalleRadiologie s = store.salles.findById(r.getIdSalle()).orElse(null);
        String salle = s == null ? r.getIdSalle() : s.getNom();
        String eq = s == null || s.getEquipementAttribueId() == null ? "—"
                : store.equipements.findById(s.getEquipementAttribueId()).map(EquipementImagerie::getLibelleCourt).orElse("—");
        String med = store.medecins.findById(r.getIdMedecin()).map(m -> "Dr " + m.getNomComplet()).orElse(r.getIdMedecin());
        String man = store.manipulateurs.findById(r.getIdManip()).map(Personne::getNomComplet).orElse(r.getIdManip());
        return new LignePlanning(r, r.getDate(), r.getHeureDebut() + " - " + r.getHeureFin(), patient, examen, salle, eq, med, man, r.getStatut().getLibelle());
    }
}
