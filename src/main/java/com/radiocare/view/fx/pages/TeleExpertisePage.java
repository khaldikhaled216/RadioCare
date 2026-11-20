package com.radiocare.view.fx.pages;

import com.radiocare.model.DemandeTeleExpertise;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.StatutExpertise;
import com.radiocare.service.RendezVousService.LignePlanning;
import com.radiocare.view.fx.Contexte;
import com.radiocare.view.fx.Formulaire;
import com.radiocare.view.fx.Page;
import com.radiocare.view.fx.Ui;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** F9 : télé-expertise et secondes lectures par des experts externes. */
public class TeleExpertisePage extends Page {
    public TeleExpertisePage(Contexte ctx) { super(ctx); }

    @Override public String titre() { return "Télé-expertise & secondes lectures"; }

    @Override
    public Node construire() {
        TableView<DemandeTeleExpertise> t = Ui.table(ctx.teleExpertise.demandes());
        Ui.col(t, "ID", DemandeTeleExpertise::getId, 80);
        Ui.col(t, "Patient", d -> ctx.rendezVous.detail(d.getIdRdv()).patient(), 140);
        Ui.col(t, "Examen", d -> ctx.rendezVous.detail(d.getIdRdv()).examen(), 180);
        Ui.col(t, "Demandeur", DemandeTeleExpertise::getIdMedecinDemandeur, 80);
        Ui.col(t, "Expert externe", DemandeTeleExpertise::getNomExpertExterne, 190);
        Ui.col(t, "Envoyée le", d -> Ui.dateHeure(d.getDateEnvoi()), 120);
        Ui.col(t, "Créneau", d -> Ui.dateHeure(d.getDateCreneau()), 120);
        Ui.col(t, "Honoraires", d -> Ui.tnd(d.getTarifHonorairesExpert()), 100);
        Ui.colPastille(t, "Statut", d -> d.getStatut().getLibelle(), d -> d.getStatut() == StatutExpertise.REPONDU ? "p-vert" : "p-orange", 90);

        VBox detail = Ui.carte(new Label("Sélectionnez une demande pour voir l'ordonnance, le compte-rendu initial et l'avis de l'expert."));
        detail.setMinHeight(170);
        t.getSelectionModel().selectedItemProperty().addListener((o, a, d) -> {
            if (d == null) return;
            detail.getChildren().setAll(Ui.section("Demande " + d.getId()), Ui.fiche("Ordonnance", d.getRefOrdonnanceTextuelle(),
                    "Compte-rendu initial", d.getCompteRenduInitialTextuel(), "Expert", d.getNomExpertExterne() + " [" + d.getIdMedecinExpertExterne() + "]",
                    "Avis de l'expert", d.getTranscriptionAvisTextuel() == null ? "(en attente)" : d.getTranscriptionAvisTextuel(),
                    "Répondu le", Ui.dateHeure(d.getDateReponse())));
        });

        return new VBox(Ui.barre(Ui.primaire("+ Nouvelle demande", () -> Ui.executer(() -> formulaire(null))),
                Ui.bouton("Replanifier", () -> Ui.executer(() -> formulaire(Ui.selection(t, "une demande")))),
                Ui.bouton("Enregistrer l'avis de l'expert", () -> Ui.executer(() -> repondre(Ui.selection(t, "une demande")))),
                Ui.danger("Supprimer", () -> Ui.executer(() -> {
                    DemandeTeleExpertise d = Ui.selection(t, "une demande");
                    if (Ui.confirmer("Supprimer la demande " + d.getId() + " ?")) { ctx.teleExpertise.supprimer(d.getId()); ctx.rafraichir(); }
                }))), t, detail);
    }

    private void formulaire(DemandeTeleExpertise ex) {
        if (ex != null && ex.getStatut() == StatutExpertise.REPONDU) throw new IllegalArgumentException("Une demande répondue ne peut plus être modifiée.");
        Formulaire f = new Formulaire(ex == null ? "Nouvelle demande de seconde lecture" : "Replanifier " + ex.getId());
        ComboBox<RendezVous> rdv = null;
        if (ex == null) {
            List<RendezVous> eligibles = ctx.teleExpertise.rdvEligibles();
            rdv = f.choix("Examen (réalisé, avec compte-rendu)", eligibles, null, r -> {
                LignePlanning l = ctx.rendezVous.detail(r.getId());
                return Ui.date(l.date()) + " — " + l.patient() + " — " + l.examen();
            });
            f.note("L'ordonnance et le compte-rendu initial sont joints automatiquement.");
        }
        TextField idExpert = f.texte("Identifiant de l'expert", ex == null ? null : ex.getIdMedecinExpertExterne());
        f.note("Référence normée, ex. EXP-CHU-SAHLOUL-04");
        TextField nom = f.texte("Nom de l'expert / établissement", ex == null ? null : ex.getNomExpertExterne());
        LocalDateTime envoi = ex == null ? LocalDateTime.now().withSecond(0).withNano(0) : ex.getDateEnvoi();
        DatePicker dEnvoi = f.date("Date d'envoi", envoi.toLocalDate());
        TextField hEnvoi = f.heure("Heure d'envoi", envoi.toLocalTime());
        LocalDateTime creneau = ex == null ? envoi.plusDays(1).withHour(10).withMinute(0) : ex.getDateCreneau();
        DatePicker dCreneau = f.date("Créneau de télé-expertise", creneau.toLocalDate());
        TextField hCreneau = f.heure("Heure du créneau (30 min)", creneau.toLocalTime());
        TextField hon = f.nombre("Honoraires de l'expert (TND)", ex == null ? 150.0 : ex.getTarifHonorairesExpert());
        ComboBox<RendezVous> choixRdv = rdv;
        if (f.afficher(() -> {
            DemandeTeleExpertise d = new DemandeTeleExpertise();
            RendezVous r = ex == null ? Formulaire.valeur(choixRdv, "Examen") : null;
            d.setIdExpertise(ex == null ? null : ex.getId());
            d.setIdRdv(ex == null ? r.getId() : ex.getIdRdv());
            d.setIdMedecinDemandeur(ex == null ? r.getIdMedecin() : ex.getIdMedecinDemandeur());
            d.setIdMedecinExpertExterne(idExpert.getText());
            d.setNomExpertExterne(nom.getText().trim());
            d.setDateEnvoi(LocalDateTime.of(Formulaire.date(dEnvoi, "Date d'envoi"), Formulaire.heure(hEnvoi, "Heure d'envoi")));
            d.setDateCreneau(LocalDateTime.of(Formulaire.date(dCreneau, "Créneau"), Formulaire.heure(hCreneau, "Heure du créneau")));
            d.setTarifHonorairesExpert(Formulaire.decimal(hon, "Honoraires"));
            if (ex == null) ctx.teleExpertise.envoyer(d); else ctx.teleExpertise.modifier(d);
        })) { ctx.notifier("Demande de télé-expertise enregistrée."); ctx.rafraichir(); }
    }

    private void repondre(DemandeTeleExpertise d) {
        if (d.getStatut() == StatutExpertise.REPONDU) throw new IllegalArgumentException("Cette demande a déjà reçu une réponse.");
        Formulaire f = new Formulaire("Avis de l'expert — " + d.getId());
        TextArea avis = f.zone("Transcription de l'avis", null);
        DatePicker date = f.date("Date de réception", java.time.LocalDate.now());
        TextField heure = f.heure("Heure de réception", LocalTime.now().withSecond(0).withNano(0));
        if (f.afficher(() -> ctx.teleExpertise.repondre(d.getId(), avis.getText(),
                LocalDateTime.of(Formulaire.date(date, "Date"), Formulaire.heure(heure, "Heure"))))) {
            ctx.notifier("Avis enregistré ; honoraires intégrés aux dépenses du mois.");
            ctx.rafraichir();
        }
    }
}
