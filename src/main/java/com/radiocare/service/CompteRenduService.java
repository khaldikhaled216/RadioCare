package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.CompteRenduMedical;
import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.StatutRdv;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Comptes-rendus médicaux : intégration textuelle structurée des résultats d'imagerie (F6). */
public class CompteRenduService {
    private final DataStore store;

    public CompteRenduService(DataStore store) { this.store = store; }

    public List<CompteRenduMedical> lister() {
        return store.comptesRendus.findAll().stream().sorted(Comparator.comparing(CompteRenduMedical::getDate).reversed()).toList();
    }

    public Optional<CompteRenduMedical> deRdv(String idRdv) {
        return store.comptesRendus.findWhere(c -> c.getIdRdv().equals(idRdv)).stream().findFirst();
    }

    /** RDV réalisés n'ayant pas encore de compte-rendu. */
    public List<RendezVous> rdvSansCompteRendu() {
        return store.rendezVous.findWhere(r -> r.getStatut() == StatutRdv.REALISE && deRdv(r.getIdRdv()).isEmpty());
    }

    public CompteRenduMedical creer(CompteRenduMedical cr) {
        RendezVous r = store.rendezVous.findById(cr.getIdRdv()).orElseThrow(() -> new ValidationException("Rendez-vous inconnu."));
        if (r.getStatut() != StatutRdv.REALISE) throw new ValidationException("Un compte-rendu ne peut être rédigé que pour un examen réalisé.");
        if (deRdv(r.getIdRdv()).isPresent()) throw new ValidationException("Ce rendez-vous possède déjà un compte-rendu.");
        cr.setIdCompteRendu(store.comptesRendus.nextId());
        valider(cr, r);
        return store.comptesRendus.create(cr);
    }

    public CompteRenduMedical modifier(CompteRenduMedical cr) {
        CompteRenduMedical existant = store.comptesRendus.findById(cr.getId()).orElseThrow(() -> new ValidationException("Compte-rendu inconnu."));
        cr.setIdRdv(existant.getIdRdv());
        valider(cr, store.rendezVous.findById(cr.getIdRdv()).orElseThrow());
        return store.comptesRendus.update(cr);
    }

    public void supprimer(String id) {
        CompteRenduMedical cr = store.comptesRendus.findById(id).orElseThrow(() -> new ValidationException("Compte-rendu inconnu."));
        if (!store.teleExpertises.findWhere(t -> t.getIdRdv().equals(cr.getIdRdv())).isEmpty())
            throw new ValidationException("Ce compte-rendu est rattaché à une demande de télé-expertise.");
        store.comptesRendus.delete(id);
    }

    private void valider(CompteRenduMedical cr, RendezVous r) {
        Validateur.nonNul(cr.getDate(), "Date");
        if (cr.getDate().isBefore(r.getDate())) throw new ValidationException("Le compte-rendu ne peut précéder l'examen.");
        cr.setRefOrdonnanceTextuelle(Validateur.reference(cr.getRefOrdonnanceTextuelle(), "Référence ordonnance"));
        String t = Validateur.obligatoire(cr.getTranscriptionDiagnosticTextuel(), "Transcription du diagnostic");
        if (t.length() < 15) throw new ValidationException("La transcription du diagnostic doit comporter au moins 15 caractères.");
        cr.setTranscriptionDiagnosticTextuel(t);
        Validateur.nonNul(cr.getCodeResultat(), "Code résultat");
    }
}
