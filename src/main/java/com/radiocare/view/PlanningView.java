package com.radiocare.view;

import com.radiocare.controller.*;
import com.radiocare.model.*;
import com.radiocare.model.enums.StatutRdv;
import com.radiocare.service.RendezVousService;
import com.radiocare.service.RendezVousService.LignePlanning;
import com.radiocare.service.conflit.Conflit;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.radiocare.view.Console.*;

/** Vue console F4 : planification des rendez-vous, planning hebdomadaire et moteur de conflits. */
public class PlanningView {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.FRENCH);

    private final Console console;
    private final RendezVousController ctrl;
    private final PatientView patients;
    private final PersonnelController personnel;
    private final EquipementController equipements;
    private final ExamenView examens;

    public PlanningView(Console console, RendezVousController ctrl, PatientView patients, PersonnelController personnel,
                        EquipementController equipements, ExamenView examens) {
        this.console = console;
        this.ctrl = ctrl;
        this.patients = patients;
        this.personnel = personnel;
        this.equipements = equipements;
        this.examens = examens;
    }

    public void afficher() {
        new Menu(console, "PLANIFICATION DES RDV & MOTEUR DE CONFLITS (F4)")
                .option("Planning hebdomadaire", this::planningHebdo)
                .option("Planning d'une journée", () -> afficherJour(console.lireDate("Jour", LocalDate.now())))
                .option("Nouveau rendez-vous", this::nouveau)
                .option("Vérifier un créneau (simulation sans enregistrement)", this::simuler)
                .option("Détail d'un rendez-vous", this::detail)
                .option("Modifier un rendez-vous planifié", this::modifier)
                .option("Accueillir le patient / démarrer l'examen", this::demarrer)
                .option("Réaliser l'examen (décompte heures, stock, facture)", this::realiser)
                .option("Annuler un rendez-vous", this::annuler)
                .option("Supprimer un rendez-vous", this::supprimer)
                .executer();
    }

    // ------------------------------------------------------------------ planning
    private void planningHebdo() {
        LocalDate lundi = RendezVousService.lundiDe(LocalDate.now());
        while (true) {
            console.titre("Semaine du " + date(lundi) + " au " + date(lundi.plusDays(5)));
            List<LignePlanning> lignes = ctrl.planningSemaine(lundi);
            if (lignes.isEmpty()) console.info("Aucun rendez-vous cette semaine.");
            lignes.stream().collect(Collectors.groupingBy(LignePlanning::date, java.util.TreeMap::new, Collectors.toList()))
                    .forEach((jour, l) -> { console.sousTitre(jour.format(JOUR) + " — " + l.size() + " RDV"); tableau(l); });
            String s = console.lireTexte("[p] semaine précédente, [s] suivante, [a] aujourd'hui, [q] quitter", "q").toLowerCase(Locale.ROOT);
            switch (s) {
                case "p" -> lundi = lundi.minusWeeks(1);
                case "s" -> lundi = lundi.plusWeeks(1);
                case "a" -> lundi = RendezVousService.lundiDe(LocalDate.now());
                default -> { return; }
            }
        }
    }

    private void afficherJour(LocalDate jour) {
        console.sousTitre(jour.format(JOUR));
        tableau(ctrl.planning(jour, jour));
    }

    /** Format imposé : [Heure - Patient - Examen - Salle - Équipement - Médecin - Manipulateur]. */
    private void tableau(List<LignePlanning> lignes) {
        Tableau.de(lignes)
                .col("Heure", LignePlanning::heure)
                .col("Patient", LignePlanning::patient, 22)
                .col("Examen", LignePlanning::examen, 28)
                .col("Salle", LignePlanning::salle)
                .col("Équipement", LignePlanning::equipement, 26)
                .col("Médecin", LignePlanning::medecin, 20)
                .col("Manipulateur", LignePlanning::manipulateur, 18)
                .col("Statut", l -> statut(l.rdv().getStatut()))
                .col("ID", l -> l.rdv().getId())
                .afficher(console);
    }

    private String statut(StatutRdv s) {
        return switch (s) {
            case PLANIFIE -> console.cyan(s.getLibelle());
            case EN_COURS -> console.jaune(s.getLibelle());
            case REALISE -> console.vert(s.getLibelle());
            case ANNULE -> console.gris(s.getLibelle());
        };
    }

    // ------------------------------------------------------------------ saisie
    private RendezVous saisir(RendezVous base) {
        RendezVous r = new RendezVous();
        if (base != null) {
            r.setIdRdv(base.getId()); r.setDate(base.getDate()); r.setHeureDebut(base.getHeureDebut()); r.setDureeMinutes(base.getDureeMinutes());
            r.setIdPatient(base.getIdPatient()); r.setIdMedecin(base.getIdMedecin()); r.setIdManip(base.getIdManip());
            r.setIdSalle(base.getIdSalle()); r.setIdExamen(base.getIdExamen()); r.setDateCreation(base.getDateCreation());
        }
        Patient p = base != null && !console.confirmer("Changer de patient ?", false) ? null : patients.selectionner();
        if (p != null) r.setIdPatient(p.getCin());
        ExamenMedical ex = base != null && !console.confirmer("Changer d'examen ?", false) ? null : examens.selectionner();
        if (ex != null) r.setIdExamen(ex.getId());
        r.setDate(console.lireDate("Date de l'examen", r.getDate() == null ? LocalDate.now().plusDays(1) : r.getDate()));
        r.setHeureDebut(console.lireHeure("Heure de début", r.getHeureDebut() == null ? LocalTime.of(9, 0) : r.getHeureDebut()));
        r.setDureeMinutes(console.lireEntier("Durée (minutes)", ex != null ? ctrl.dureeParDefaut(ex.getId()) : r.getDureeMinutes(), 5, 240));
        List<SalleRadiologie> salles = equipements.salles();
        r.setIdSalle(console.choisir("Salle", salles, s -> s.getNom() + " (" + s.getTypeSalle().getLibelle() + ", " + s.getStatutDispo().getLibelle() + ")",
                salles.stream().filter(s -> s.getId().equals(r.getIdSalle())).findFirst().orElse(null)).getId());
        List<MedecinRadiologue> meds = personnel.medecins();
        r.setIdMedecin(console.choisir("Médecin radiologue", meds, m -> "Dr " + m.getNomComplet() + " — " + m.getSpecialite().getLibelle()
                + " (" + m.getDispoPlanning().resume() + ")", meds.stream().filter(m -> m.getId().equals(r.getIdMedecin())).findFirst().orElse(null)).getId());
        List<ManipulateurRadio> mans = personnel.manipulateurs();
        r.setIdManip(console.choisir("Manipulateur radio", mans, m -> m.getNomComplet() + " " + m.getHabilitations() + " — " + m.getStatutDispo().getLibelle(),
                mans.stream().filter(m -> m.getId().equals(r.getIdManip())).findFirst().orElse(null)).getId());
        return r;
    }

    private void nouveau() {
        console.sousTitre("Nouveau rendez-vous (contrôle par le moteur de conflits avant enregistrement)");
        RendezVous r = saisir(null);
        RendezVous cree = ctrl.planifier(r);
        console.succes("Aucun conflit : rendez-vous " + cree.getId() + " planifié le " + date(cree.getDate()) + " à " + cree.getHeureDebut() + ".");
    }

    private void simuler() {
        console.sousTitre("Simulation d'un créneau");
        RendezVous r = saisir(null);
        List<Conflit> conflits = ctrl.verifier(r);
        if (conflits.isEmpty()) console.succes("Créneau disponible : les 6 règles du moteur sont satisfaites.");
        else {
            console.erreur(conflits.size() + " conflit(s) détecté(s) :");
            conflits.forEach(c -> console.println("    " + console.rouge("• " + c.type().getLibelle()) + "\n      " + c.message()));
        }
    }

    private RendezVous selectionner(boolean actifsSeulement) {
        List<LignePlanning> lignes = ctrl.planning(LocalDate.now().minusYears(5), LocalDate.now().plusYears(5)).stream()
                .filter(l -> !actifsSeulement || l.rdv().isActif()).toList();
        if (actifsSeulement) lignes = lignes.stream().sorted(java.util.Comparator.comparing(
                (LignePlanning l) -> Math.abs(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), l.date())))).toList();
        LignePlanning l = Selection.parRecherche(console, "Rendez-vous (ID, patient, examen ou date jj/mm/aaaa)", lignes, x -> x.rdv().getId(),
                x -> date(x.date()) + " " + x.heure() + " | " + x.patient() + " | " + x.examen() + " | " + x.statut());
        return l.rdv();
    }

    private void detail() {
        RendezVous r = selectionner(false);
        LignePlanning l = ctrl.detail(r.getId());
        console.sousTitre("Rendez-vous " + r.getId());
        console.champ("Date / heure", date(l.date()) + " " + l.heure() + " (" + r.getDureeMinutes() + " min)");
        console.champ("Patient", l.patient());
        console.champ("Examen", l.examen());
        console.champ("Salle / équipement", l.salle() + " / " + l.equipement());
        console.champ("Médecin", l.medecin());
        console.champ("Manipulateur", l.manipulateur());
        console.champ("Statut", statut(r.getStatut()));
        console.champ("Pris le", dateHeure(r.getDateCreation()));
        if (r.getHeureArrivee() != null)
            console.champ("Arrivée / prise en charge", r.getHeureArrivee() + " / " + r.getHeurePriseEnCharge() + " (attente " + r.getAttenteMinutes() + " min)");
    }

    private void modifier() {
        RendezVous r = selectionner(true);
        console.info("Entrée = conserver la valeur actuelle.");
        RendezVous m = saisir(r);
        ctrl.modifier(m);
        console.succes("Rendez-vous " + m.getId() + " modifié (aucun conflit).");
    }

    private void demarrer() {
        RendezVous r = selectionner(true);
        LocalTime arrivee = console.lireHeure("Heure d'arrivée du patient", LocalTime.now().withSecond(0).withNano(0));
        LocalTime pec = console.lireHeure("Heure de prise en charge", LocalTime.now().withSecond(0).withNano(0));
        ctrl.demarrer(r.getId(), arrivee, pec);
        console.succes("Examen " + r.getId() + " en cours.");
    }

    private void realiser() {
        RendezVous r = selectionner(true);
        if (!console.confirmer("Confirmer la réalisation de " + r.getId() + " ?", true)) return;
        FactureMedicale f = ctrl.realiser(r.getId());
        console.succes("Examen réalisé : heures machine décomptées, consommables déduits du stock.");
        console.succes(String.format(Locale.FRANCE, "Facture %s émise : HT %s + TVA 19%% = TTC %s (assurance %s, patient %s).",
                f.getNumFacture(), tnd(f.getMontantHT()), tnd(f.getMontantTTC()), tnd(f.getPartAssurance()), tnd(f.getPartPatient())));
    }

    private void annuler() {
        RendezVous r = selectionner(true);
        if (console.confirmer("Annuler le rendez-vous " + r.getId() + " ?", false)) { ctrl.annuler(r.getId()); console.succes("Rendez-vous annulé."); }
    }

    private void supprimer() {
        RendezVous r = selectionner(false);
        if (console.confirmer("Supprimer définitivement le rendez-vous " + r.getId() + " ?", false)) { ctrl.supprimer(r.getId()); console.succes("Rendez-vous supprimé."); }
    }
}
