package com.radiocare.view.fx.pages;

import com.radiocare.model.*;
import com.radiocare.model.enums.StatutRdv;
import com.radiocare.service.RendezVousService;
import com.radiocare.service.RendezVousService.LignePlanning;
import com.radiocare.service.conflit.Conflit;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Formulaire;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** F4 : planning hebdomadaire et cycle de vie des rendez-vous, sous contrôle du moteur de conflits. */
public class PlanningPage extends Page {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMM", Locale.FRENCH);
    private LocalDate lundi = RendezVousService.lundiDe(LocalDate.now());
    private String selection;
    private int onglet = 0;

    public PlanningPage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Rendez-vous & planning"; }

    public static String couleurStatut(StatutRdv s) {
        return switch (s) {
            case PLANIFIE -> "p-bleu";
            case EN_COURS -> "p-orange";
            case REALISE -> "p-vert";
            case ANNULE -> "p-gris";
        };
    }

    @Override
    public Node construire() {
        List<LignePlanning> lignes = ctx.rendezVous.planningSemaine(lundi);
        Label semaine = Ui.section("Semaine du " + Ui.date(lundi) + " au " + Ui.date(lundi.plusDays(5)) + " — " + lignes.size() + " RDV");
        HBox nav = Ui.barre(Ui.bouton("◀", () -> { lundi = lundi.minusWeeks(1); ctx.rafraichir(); }),
                Ui.bouton("Cette semaine", () -> { lundi = RendezVousService.lundiDe(LocalDate.now()); ctx.rafraichir(); }),
                Ui.bouton("▶", () -> { lundi = lundi.plusWeeks(1); ctx.rafraichir(); }), semaine, Ui.espace(),
                Ui.primaire("+ Nouveau RDV", () -> formulaire(null, false)),
                Ui.bouton("Vérifier un créneau", () -> formulaire(null, true)));
        HBox actions = Ui.barre(new Label("RDV sélectionné :"),
                Ui.bouton("Détails", () -> Ui.executer(() -> details(rdvSelectionne()))),
                Ui.bouton("Modifier", () -> Ui.executer(() -> formulaire(rdvSelectionne(), false))),
                Ui.bouton("Accueillir / démarrer", () -> Ui.executer(() -> demarrer(rdvSelectionne()))),
                Ui.primaire("Réaliser l'examen", () -> Ui.executer(() -> realiser(rdvSelectionne()))),
                Ui.bouton("Annuler le RDV", () -> Ui.executer(() -> {
                    RendezVous r = rdvSelectionne();
                    if (Ui.confirmer("Annuler le rendez-vous " + r.getId() + " ?")) { ctx.rendezVous.annuler(r.getId()); ctx.notifier("RDV annulé."); ctx.rafraichir(); }
                })),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    RendezVous r = rdvSelectionne();
                    if (Ui.confirmer("Supprimer définitivement le rendez-vous " + r.getId() + " ?")) { ctx.rendezVous.supprimer(r.getId()); selection = null; ctx.rafraichir(); }
                })));

        TabPane tabs = new TabPane(new Tab("Vue semaine", vueSemaine(lignes)), new Tab("Liste détaillée", liste(lignes)));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(onglet);
        tabs.getSelectionModel().selectedIndexProperty().addListener((o, a, n) -> onglet = n.intValue());
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return new VBox(nav, actions, tabs);
    }

    private RendezVous rdvSelectionne() {
        if (selection == null) throw new IllegalArgumentException("Sélectionnez d'abord un rendez-vous (cliquez sur une carte ou une ligne).");
        return ctx.rendezVous.trouver(selection);
    }

    /** Six colonnes (lundi → samedi), une carte par rendez-vous. */
    private Node vueSemaine(List<LignePlanning> lignes) {
        var parJour = lignes.stream().collect(Collectors.groupingBy(LignePlanning::date));
        HBox colonnes = new HBox(10);
        for (int i = 0; i < 6; i++) {
            LocalDate jour = lundi.plusDays(i);
            List<LignePlanning> rdvs = parJour.getOrDefault(jour, List.of());
            Label titre = new Label(jour.format(JOUR) + "  (" + rdvs.size() + ")");
            titre.getStyleClass().add("jour-titre");
            VBox col = new VBox(titre);
            col.getStyleClass().add("jour-colonne");
            if (jour.equals(LocalDate.now())) col.getStyleClass().add("jour-aujourdhui");
            for (LignePlanning l : rdvs) col.getChildren().add(carte(l));
            col.setPrefWidth(200);
            col.setMinWidth(170);
            HBox.setHgrow(col, Priority.ALWAYS);
            colonnes.getChildren().add(col);
        }
        ScrollPane sp = new ScrollPane(colonnes);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #f2f5f8;");
        return sp;
    }

    private Node carte(LignePlanning l) {
        Label heure = new Label(l.heure() + "  ·  " + l.salle());
        heure.getStyleClass().add("rdv-heure");
        Label patient = new Label(l.patient());
        Label examen = new Label(l.examen());
        Label personnel = new Label(l.medecin() + " / " + l.manipulateur());
        personnel.getStyleClass().add("texte-gris");
        for (Label x : List.of(heure, patient, examen, personnel)) { x.setWrapText(true); x.setMaxWidth(190); }
        VBox c = new VBox(heure, patient, examen, personnel);
        c.getStyleClass().addAll("rdv-carte", switch (l.rdv().getStatut()) {
            case PLANIFIE -> "rdv-planifie";
            case EN_COURS -> "rdv-encours";
            case REALISE -> "rdv-realise";
            case ANNULE -> "rdv-annule";
        });
        if (l.rdv().getId().equals(selection)) c.getStyleClass().add("rdv-selection");
        Tooltip.install(c, new Tooltip(l.rdv().getId() + " — " + l.statut() + "\n" + l.equipement()));
        c.setOnMouseClicked(e -> {
            selection = l.rdv().getId();
            ctx.notifier("Sélection : " + selection + " (" + l.patient() + ", " + l.examen() + ")");
            if (e.getClickCount() == 2) Ui.executer(() -> details(l.rdv()));
            else ctx.rafraichir();
        });
        return c;
    }

    /** Tableau au format [Heure - Patient - Examen - Salle - Équipement - Médecin - Manipulateur]. */
    private Node liste(List<LignePlanning> lignes) {
        TableView<LignePlanning> t = Ui.table(lignes);
        Ui.col(t, "Date", l -> l.date().format(JOUR), 110);
        Ui.col(t, "Heure", LignePlanning::heure, 100);
        Ui.col(t, "Patient", LignePlanning::patient, 140);
        Ui.col(t, "Examen", LignePlanning::examen, 190);
        Ui.col(t, "Salle", LignePlanning::salle, 100);
        Ui.col(t, "Équipement", LignePlanning::equipement, 170);
        Ui.col(t, "Médecin", LignePlanning::medecin, 130);
        Ui.col(t, "Manipulateur", LignePlanning::manipulateur, 120);
        Ui.colPastille(t, "Statut", LignePlanning::statut, l -> couleurStatut(l.rdv().getStatut()), 90);
        Ui.col(t, "ID", l -> l.rdv().getId(), 80);
        t.getSelectionModel().selectedItemProperty().addListener((o, a, l) -> { if (l != null) selection = l.rdv().getId(); });
        t.getItems().stream().filter(l -> l.rdv().getId().equals(selection)).findFirst().ifPresent(l -> t.getSelectionModel().select(l));
        return t;
    }

    // ------------------------------------------------------------------ actions
    private void formulaire(RendezVous ex, boolean simulation) {
        Formulaire f = new Formulaire(simulation ? "Vérifier la disponibilité d'un créneau" : ex == null ? "Nouveau rendez-vous" : "Modifier le rendez-vous " + ex.getId());
        List<Patient> patients = ctx.patients.lister();
        List<ExamenMedical> examens = ctx.examens.catalogue();
        List<SalleRadiologie> salles = ctx.equipements.salles();
        List<MedecinRadiologue> medecins = ctx.personnel.medecins();
        List<ManipulateurRadio> manips = ctx.personnel.manipulateurs();
        ComboBox<Patient> patient = f.choix("Patient", patients, ex == null ? null : trouver(patients, ex.getIdPatient()), p -> p.getNomComplet() + " (" + p.getCin() + ")");
        ComboBox<ExamenMedical> examen = f.choix("Examen", examens, ex == null ? null : trouver(examens, ex.getIdExamen()), e -> e.getLibelle() + " — " + e.getDureeEstimeeMinutes() + " min");
        DatePicker date = f.date("Date", ex == null ? LocalDate.now().plusDays(1) : ex.getDate());
        TextField heure = f.heure("Heure de début", ex == null ? LocalTime.of(9, 0) : ex.getHeureDebut());
        TextField duree = f.nombre("Durée (minutes)", ex == null ? null : (double) ex.getDureeMinutes());
        examen.setOnAction(e -> { if (examen.getValue() != null) duree.setText(String.valueOf(examen.getValue().getDureeEstimeeMinutes())); });
        ComboBox<SalleRadiologie> salle = f.choix("Salle", salles, ex == null ? null : trouver(salles, ex.getIdSalle()), s -> s.getNom() + " (" + s.getTypeSalle().getLibelle() + ")");
        ComboBox<MedecinRadiologue> medecin = f.choix("Médecin radiologue", medecins, ex == null ? null : trouver(medecins, ex.getIdMedecin()),
                m -> "Dr " + m.getNomComplet() + " — " + m.getDispoPlanning().resume());
        ComboBox<ManipulateurRadio> manip = f.choix("Manipulateur", manips, ex == null ? null : trouver(manips, ex.getIdManip()),
                m -> m.getNomComplet() + " " + m.getHabilitations() + (m.getStatutDispo() == com.radiocare.model.enums.StatutDispo.DISPONIBLE ? "" : " — " + m.getStatutDispo().getLibelle()));
        f.note("Avant l'enregistrement, le moteur vérifie : double réservation de salle/équipement, agenda du personnel, "
                + "potentiel d'heures de l'équipement, habilitations, contre-indications et solde du patient, stock de produit de contraste.");
        boolean ok = f.afficher(() -> {
            RendezVous r = new RendezVous();
            if (ex != null) r.setIdRdv(ex.getId());
            r.setIdPatient(Formulaire.valeur(patient, "Patient").getCin());
            r.setIdExamen(Formulaire.valeur(examen, "Examen").getId());
            r.setDate(Formulaire.date(date, "Date"));
            r.setHeureDebut(Formulaire.heure(heure, "Heure"));
            r.setDureeMinutes(duree.getText().isBlank() ? 0 : Formulaire.entier(duree, "Durée"));
            r.setIdSalle(Formulaire.valeur(salle, "Salle").getId());
            r.setIdMedecin(Formulaire.valeur(medecin, "Médecin").getId());
            r.setIdManip(Formulaire.valeur(manip, "Manipulateur").getId());
            if (simulation) {
                List<Conflit> conflits = ctx.rendezVous.verifier(r);
                if (!conflits.isEmpty()) throw new com.radiocare.service.conflit.ConflitException(conflits);
                Ui.info("Créneau disponible", "Aucun conflit : les 6 règles du moteur sont respectées.");
            } else if (ex == null) {
                selection = ctx.rendezVous.planifier(r).getId();
            } else {
                ctx.rendezVous.modifier(r);
            }
        });
        if (ok && !simulation) {
            lundi = RendezVousService.lundiDe(Formulaire.date(date, "Date"));
            ctx.notifier("Rendez-vous " + selection + " enregistré : aucun conflit détecté.");
            ctx.rafraichir();
        }
    }

    private static <T extends Identifiable> T trouver(List<T> l, String id) {
        return l.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
    }

    private void details(RendezVous r) {
        LignePlanning l = ctx.rendezVous.detail(r.getId());
        String attente = r.getHeureArrivee() == null ? "—" : r.getHeureArrivee() + " → " + r.getHeurePriseEnCharge() + " (" + r.getAttenteMinutes() + " min d'attente)";
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Rendez-vous " + r.getId());
        a.setHeaderText(l.patient() + " — " + l.examen());
        GridPane g = Ui.fiche("Date / heure", Ui.date(l.date()) + "  " + l.heure() + " (" + r.getDureeMinutes() + " min)",
                "Salle", l.salle(), "Équipement", l.equipement(), "Médecin", l.medecin(), "Manipulateur", l.manipulateur(),
                "Statut", l.statut(), "Pris le", Ui.dateHeure(r.getDateCreation()), "Arrivée / prise en charge", attente);
        a.getDialogPane().setContent(g);
        a.showAndWait();
    }

    private void demarrer(RendezVous r) {
        Formulaire f = new Formulaire("Accueil du patient — " + r.getId());
        LocalTime maintenant = LocalTime.now().withSecond(0).withNano(0);
        TextField arrivee = f.heure("Heure d'arrivée", maintenant);
        TextField pec = f.heure("Heure de prise en charge", maintenant);
        if (f.afficher(() -> ctx.rendezVous.demarrer(r.getId(), Formulaire.heure(arrivee, "Arrivée"), Formulaire.heure(pec, "Prise en charge")))) {
            ctx.notifier("Examen " + r.getId() + " en cours.");
            ctx.rafraichir();
        }
    }

    private void realiser(RendezVous r) {
        if (!Ui.confirmer("Confirmer la réalisation de l'examen " + r.getId() + " ?\n"
                + "Les heures machine seront décomptées, le consommable déduit du stock et la facture générée.")) return;
        FactureMedicale f = ctx.rendezVous.realiser(r.getId());
        Ui.info("Examen réalisé", String.format(Locale.FRANCE, "Facture %s émise%n%nMontant HT : %s%nTVA 19 %% : %s%nMontant TTC : %s%n%nPart assurance : %s%nPart patient : %s",
                f.getNumFacture(), Ui.tnd(f.getMontantHT()), Ui.tnd(f.getMontantTVA()), Ui.tnd(f.getMontantTTC()),
                Ui.tnd(f.getPartAssurance()), Ui.tnd(f.getPartPatient())));
        ctx.rafraichir();
    }
}
