package com.radiocare.view;

import com.radiocare.controller.ExamenController;
import com.radiocare.controller.RendezVousController;
import com.radiocare.controller.TeleExpertiseController;
import com.radiocare.model.DemandeTeleExpertise;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.StatutExpertise;

import java.time.LocalDateTime;
import java.util.List;

import static com.radiocare.view.Console.*;

/** Vue console F9 : télé-expertise médicale et secondes lectures. */
public class TeleExpertiseView {
    private final Console console;
    private final TeleExpertiseController ctrl;
    private final RendezVousController rdv;
    private final ExamenController examens;

    public TeleExpertiseView(Console console, TeleExpertiseController ctrl, RendezVousController rdv, ExamenController examens) {
        this.console = console;
        this.ctrl = ctrl;
        this.rdv = rdv;
        this.examens = examens;
    }

    public void afficher() {
        new Menu(console, "TÉLÉ-EXPERTISE & SECONDES LECTURES (F9)")
                .option("Lister les demandes", this::lister)
                .option("Consulter une demande (ordonnance, CR initial, avis)", this::consulter)
                .option("Envoyer une demande de seconde lecture", this::envoyer)
                .option("Replanifier / modifier une demande en attente", this::modifier)
                .option("Enregistrer la réponse de l'expert", this::repondre)
                .option("Supprimer une demande en attente", this::supprimer)
                .executer();
    }

    private void lister() {
        Tableau.de(ctrl.demandes())
                .col("ID", DemandeTeleExpertise::getId).col("RDV", DemandeTeleExpertise::getIdRdv)
                .col("Patient", d -> rdv.detail(d.getIdRdv()).patient(), 20)
                .col("Demandeur", DemandeTeleExpertise::getIdMedecinDemandeur)
                .col("Expert externe", DemandeTeleExpertise::getNomExpertExterne, 28)
                .col("Envoyée le", d -> dateHeure(d.getDateEnvoi())).col("Créneau", d -> dateHeure(d.getDateCreneau()))
                .colD("Honoraires", d -> tnd(d.getTarifHonorairesExpert()))
                .col("Statut", d -> d.getStatut() == StatutExpertise.REPONDU ? console.vert("Répondu") : console.jaune("En Attente"))
                .afficher(console);
    }

    private DemandeTeleExpertise selectionner(boolean enAttente) {
        List<DemandeTeleExpertise> l = ctrl.demandes().stream().filter(d -> !enAttente || d.getStatut() == StatutExpertise.EN_ATTENTE).toList();
        return Selection.parRecherche(console, "Demande", l, DemandeTeleExpertise::getId,
                d -> d.getIdRdv() + " " + rdv.detail(d.getIdRdv()).patient() + " → " + d.getNomExpertExterne() + " (" + d.getStatut().getLibelle() + ")");
    }

    private void consulter() {
        DemandeTeleExpertise d = selectionner(false);
        var l = rdv.detail(d.getIdRdv());
        console.sousTitre("Demande " + d.getId());
        console.champ("Examen", l.examen() + " du " + date(l.date()) + " — " + l.patient());
        console.champ("Médecin demandeur", d.getIdMedecinDemandeur());
        console.champ("Expert externe", d.getNomExpertExterne() + " [" + d.getIdMedecinExpertExterne() + "]");
        console.champ("Envoyée le", dateHeure(d.getDateEnvoi()));
        console.champ("Créneau de télé-expertise", dateHeure(d.getDateCreneau()));
        console.champ("Honoraires expert", tnd(d.getTarifHonorairesExpert()));
        console.champ("Ordonnance", d.getRefOrdonnanceTextuelle());
        console.champ("Compte-rendu initial", d.getCompteRenduInitialTextuel());
        console.champ("Avis de l'expert", d.getTranscriptionAvisTextuel() == null ? "(en attente)" : d.getTranscriptionAvisTextuel());
        console.champ("Répondu le", dateHeure(d.getDateReponse()));
        console.pause();
    }

    private void saisir(DemandeTeleExpertise d) {
        d.setIdMedecinExpertExterne(console.lireTexte("Identifiant normé de l'expert (ex. EXP-CHU-SAHLOUL-04)", d.getIdMedecinExpertExterne()));
        d.setNomExpertExterne(console.lireTexte("Nom de l'expert / établissement", d.getNomExpertExterne()));
        d.setDateEnvoi(console.lireDateHeure("Envoi", d.getDateEnvoi()));
        d.setDateCreneau(console.lireDateHeure("Créneau de télé-expertise (30 min)", d.getDateCreneau() == null ? d.getDateEnvoi().plusDays(1) : d.getDateCreneau()));
        d.setTarifHonorairesExpert(console.lireDecimal("Honoraires dus à l'expert (TND)", d.getTarifHonorairesExpert() > 0 ? d.getTarifHonorairesExpert() : 150.0, 0.001, 100_000));
    }

    private void envoyer() {
        List<RendezVous> eligibles = ctrl.rdvEligibles();
        RendezVous r = Selection.parRecherche(console, "Examen réalisé avec compte-rendu", eligibles, RendezVous::getId,
                x -> date(x.getDate()) + " " + rdv.detail(x.getId()).patient() + " — " + rdv.detail(x.getId()).examen()
                        + " [" + examens.compteRenduDe(x.getId()).map(c -> c.getCodeResultat().getLibelle()).orElse("?") + "]");
        DemandeTeleExpertise d = new DemandeTeleExpertise();
        d.setIdRdv(r.getId());
        d.setIdMedecinDemandeur(r.getIdMedecin());
        console.info("Médecin demandeur : " + rdv.detail(r.getId()).medecin() + ". Ordonnance et compte-rendu initial seront joints automatiquement.");
        d.setDateEnvoi(LocalDateTime.now().withSecond(0).withNano(0));
        saisir(d);
        ctrl.envoyer(d);
        console.succes("Demande " + d.getId() + " envoyée à " + d.getNomExpertExterne() + ".");
    }

    private void modifier() {
        DemandeTeleExpertise ex = selectionner(true);
        DemandeTeleExpertise d = new DemandeTeleExpertise(ex.getId(), ex.getIdRdv(), ex.getIdMedecinDemandeur(), ex.getIdMedecinExpertExterne(),
                ex.getNomExpertExterne(), ex.getDateEnvoi(), ex.getDateCreneau(), ex.getTarifHonorairesExpert());
        console.info("Entrée = conserver la valeur actuelle.");
        saisir(d);
        ctrl.modifier(d);
        console.succes("Demande mise à jour.");
    }

    private void repondre() {
        DemandeTeleExpertise d = selectionner(true);
        String avis = console.lireTexte("Transcription de l'avis de l'expert");
        LocalDateTime date = console.lireDateHeure("Réponse reçue", LocalDateTime.now().withSecond(0).withNano(0));
        ctrl.repondre(d.getId(), avis, date);
        console.succes("Avis enregistré ; honoraires de " + tnd(d.getTarifHonorairesExpert()) + " intégrés aux dépenses du mois.");
    }

    private void supprimer() {
        DemandeTeleExpertise d = selectionner(true);
        if (console.confirmer("Supprimer la demande " + d.getId() + " ?", false)) { ctrl.supprimer(d.getId()); console.succes("Demande supprimée."); }
    }
}
