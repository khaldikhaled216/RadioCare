package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.CompteRenduMedical;
import com.radiocare.model.DemandeTeleExpertise;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.CodeResultat;
import com.radiocare.model.enums.StatutExpertise;
import com.radiocare.model.enums.StatutRdv;
import com.radiocare.model.enums.TypeConflit;
import com.radiocare.service.conflit.Conflit;
import com.radiocare.service.conflit.ConflitException;
import com.radiocare.service.conflit.MoteurConflits;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** F9 : télé-expertise / secondes lectures auprès de médecins experts externes. */
public class TeleExpertiseService {
    private final DataStore store;
    private final MoteurConflits moteur;

    public TeleExpertiseService(DataStore store, MoteurConflits moteur) {
        this.store = store;
        this.moteur = moteur;
    }

    public List<DemandeTeleExpertise> lister() {
        return store.teleExpertises.findAll().stream().sorted(Comparator.comparing(DemandeTeleExpertise::getDateEnvoi).reversed()).toList();
    }

    /** RDV éligibles : examens réalisés disposant d'un compte-rendu. */
    public List<RendezVous> rdvEligibles() {
        return store.rendezVous.findWhere(r -> r.getStatut() == StatutRdv.REALISE
                && !store.comptesRendus.findWhere(c -> c.getIdRdv().equals(r.getIdRdv())).isEmpty());
    }

    /** Détection des conflits d'expertise : doubles demandes simultanées et délai légal d'urgence dépassé. */
    public List<Conflit> verifierConflits(DemandeTeleExpertise d) {
        List<Conflit> res = new ArrayList<>();
        for (DemandeTeleExpertise autre : store.teleExpertises.findAll()) {
            if (Objects.equals(autre.getId(), d.getId())) continue;
            if (autre.getIdRdv().equals(d.getIdRdv()) && autre.getStatut() == StatutExpertise.EN_ATTENTE)
                res.add(new Conflit(TypeConflit.EXPERTISE_DOUBLE, "Une demande d'expertise est déjà en attente pour " + d.getIdRdv() + " (" + autre.getId() + ")."));
            else if (autre.getIdMedecinExpertExterne().equalsIgnoreCase(d.getIdMedecinExpertExterne())
                    && autre.getStatut() == StatutExpertise.EN_ATTENTE && autre.creneauChevauche(d))
                res.add(new Conflit(TypeConflit.EXPERTISE_DOUBLE, "L'expert " + d.getIdMedecinExpertExterne()
                        + " a déjà une séance planifiée sur ce créneau (" + autre.getId() + ")."));
        }
        RendezVous rdv = store.rendezVous.findById(d.getIdRdv()).orElse(null);
        CompteRenduMedical cr = store.comptesRendus.findWhere(c -> c.getIdRdv().equals(d.getIdRdv())).stream().findFirst().orElse(null);
        if (rdv != null && cr != null && cr.getCodeResultat() == CodeResultat.URGENT) {
            LocalDateTime limite = rdv.getFin().plusHours(ParametresCentre.DELAI_LEGAL_URGENCE_HEURES);
            if (d.getDateEnvoi().isAfter(limite))
                res.add(new Conflit(TypeConflit.EXPERTISE_DELAI, "Compte-rendu urgent : transmission au-delà du délai légal de "
                        + ParametresCentre.DELAI_LEGAL_URGENCE_HEURES + " h (limite : " + limite.toLocalDate() + " " + limite.toLocalTime() + ")."));
        }
        return res;
    }

    /** Crée une demande en intégrant automatiquement l'ordonnance et le compte-rendu initial. */
    public DemandeTeleExpertise creer(DemandeTeleExpertise d) {
        valider(d);
        d.setIdExpertise(store.teleExpertises.nextId());
        d.setStatut(StatutExpertise.EN_ATTENTE);
        CompteRenduMedical cr = store.comptesRendus.findWhere(c -> c.getIdRdv().equals(d.getIdRdv())).stream().findFirst().orElseThrow();
        d.setRefOrdonnanceTextuelle(cr.getRefOrdonnanceTextuelle());
        d.setCompteRenduInitialTextuel("[" + cr.getCodeResultat().getLibelle() + "] " + cr.getTranscriptionDiagnosticTextuel());
        List<Conflit> conflits = verifierConflits(d);
        if (!conflits.isEmpty()) {
            moteur.journaliser(conflits, "Télé-expertise " + d.getIdRdv());
            throw new ConflitException(conflits);
        }
        return store.teleExpertises.create(d);
    }

    public DemandeTeleExpertise modifier(DemandeTeleExpertise d) {
        DemandeTeleExpertise existante = store.teleExpertises.findById(d.getId()).orElseThrow(() -> new ValidationException("Demande inconnue."));
        if (existante.getStatut() != StatutExpertise.EN_ATTENTE) throw new ValidationException("Une demande répondue ne peut plus être modifiée.");
        valider(d);
        d.setRefOrdonnanceTextuelle(existante.getRefOrdonnanceTextuelle());
        d.setCompteRenduInitialTextuel(existante.getCompteRenduInitialTextuel());
        d.setStatut(StatutExpertise.EN_ATTENTE);
        List<Conflit> conflits = verifierConflits(d);
        if (!conflits.isEmpty()) {
            moteur.journaliser(conflits, "Télé-expertise " + d.getId());
            throw new ConflitException(conflits);
        }
        return store.teleExpertises.update(d);
    }

    private void valider(DemandeTeleExpertise d) {
        RendezVous r = store.rendezVous.findById(d.getIdRdv()).orElseThrow(() -> new ValidationException("Rendez-vous inconnu."));
        if (r.getStatut() != StatutRdv.REALISE) throw new ValidationException("L'examen doit être réalisé.");
        if (store.comptesRendus.findWhere(c -> c.getIdRdv().equals(r.getIdRdv())).isEmpty())
            throw new ValidationException("Un compte-rendu initial est requis avant toute demande d'expertise.");
        if (!store.medecins.exists(d.getIdMedecinDemandeur())) throw new ValidationException("Médecin demandeur inconnu.");
        d.setIdMedecinExpertExterne(Validateur.reference(d.getIdMedecinExpertExterne(), "Identifiant expert externe"));
        Validateur.obligatoire(d.getNomExpertExterne(), "Nom de l'expert");
        Validateur.nonNul(d.getDateEnvoi(), "Date d'envoi");
        Validateur.nonNul(d.getDateCreneau(), "Créneau de télé-expertise");
        if (d.getDateCreneau().isBefore(d.getDateEnvoi())) throw new ValidationException("Le créneau doit être postérieur à l'envoi.");
        Validateur.positif(d.getTarifHonorairesExpert(), "Honoraires de l'expert");
    }

    /** Enregistre l'avis textuel de l'expert. */
    public DemandeTeleExpertise repondre(String id, String avis, LocalDateTime dateReponse) {
        DemandeTeleExpertise d = store.teleExpertises.findById(id).orElseThrow(() -> new ValidationException("Demande inconnue."));
        if (d.getStatut() == StatutExpertise.REPONDU) throw new ValidationException("Cette demande a déjà reçu une réponse.");
        String texte = Validateur.obligatoire(avis, "Avis de l'expert");
        if (texte.length() < 15) throw new ValidationException("L'avis de l'expert doit comporter au moins 15 caractères.");
        Validateur.nonNul(dateReponse, "Date de réponse");
        if (dateReponse.isBefore(d.getDateEnvoi())) throw new ValidationException("La réponse ne peut précéder l'envoi.");
        d.setTranscriptionAvisTextuel(texte);
        d.setDateReponse(dateReponse);
        d.setStatut(StatutExpertise.REPONDU);
        return store.teleExpertises.update(d);
    }

    public void supprimer(String id) {
        DemandeTeleExpertise d = store.teleExpertises.findById(id).orElseThrow(() -> new ValidationException("Demande inconnue."));
        if (d.getStatut() == StatutExpertise.REPONDU)
            throw new ValidationException("Une expertise répondue est intégrée à la comptabilité et ne peut être supprimée.");
        store.teleExpertises.delete(id);
    }

    /** Honoraires dus aux experts externes pour les avis rendus dans le mois (dépense F5). */
    public double honorairesExperts(YearMonth mois) {
        return store.teleExpertises.findWhere(d -> d.getStatut() == StatutExpertise.REPONDU && d.getDateReponse() != null
                && YearMonth.from(d.getDateReponse()).equals(mois)).stream().mapToDouble(DemandeTeleExpertise::getTarifHonorairesExpert).sum();
    }

    /** Demandes en attente issues d'un compte-rendu urgent dont la réponse dépasse le délai légal. */
    public List<DemandeTeleExpertise> urgencesEnRetard(LocalDateTime maintenant) {
        return store.teleExpertises.findWhere(d -> d.getStatut() == StatutExpertise.EN_ATTENTE
                && d.getCompteRenduInitialTextuel() != null && d.getCompteRenduInitialTextuel().startsWith("[" + CodeResultat.URGENT.getLibelle() + "]")
                && maintenant.isAfter(d.getDateEnvoi().plusHours(ParametresCentre.DELAI_LEGAL_URGENCE_HEURES)));
    }
}
