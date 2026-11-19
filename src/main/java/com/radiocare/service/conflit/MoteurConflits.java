package com.radiocare.service.conflit;

import com.radiocare.dao.DataStore;
import com.radiocare.model.*;
import com.radiocare.service.ParametresCentre;
import com.radiocare.service.StockService;
import com.radiocare.service.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Moteur central de détection et d'interdiction des conflits (F4 / section 5).
 * Il exécute séquentiellement les six règles et agrège tous les conflits trouvés.
 */
public class MoteurConflits {
    private final DataStore store;
    private final List<RegleConflit> regles = new ArrayList<>();

    public MoteurConflits(DataStore store, StockService stockService) {
        this.store = store;
        regles.add(new RegleDoubleReservation());
        regles.add(new RegleChevauchementAgenda());
        regles.add(new ReglePotentielHeures());
        regles.add(new RegleHabilitation());
        regles.add(new RegleBlocagePatient(ParametresCentre.SEUIL_SOLDE_DEBITEUR));
        regles.add(new RegleRuptureStock(stockService));
    }

    public List<RegleConflit> getRegles() { return List.copyOf(regles); }

    /** Résout les références du RDV ; lève ValidationException si une référence est inconnue. */
    public ContexteConflit construireContexte(RendezVous rdv) {
        Patient p = store.patients.findById(rdv.getIdPatient()).orElseThrow(() -> new ValidationException("Patient inconnu : " + rdv.getIdPatient()));
        MedecinRadiologue m = store.medecins.findById(rdv.getIdMedecin()).orElseThrow(() -> new ValidationException("Médecin inconnu : " + rdv.getIdMedecin()));
        ManipulateurRadio man = store.manipulateurs.findById(rdv.getIdManip()).orElseThrow(() -> new ValidationException("Manipulateur inconnu : " + rdv.getIdManip()));
        SalleRadiologie s = store.salles.findById(rdv.getIdSalle()).orElseThrow(() -> new ValidationException("Salle inconnue : " + rdv.getIdSalle()));
        ExamenMedical ex = store.examens.findById(rdv.getIdExamen()).orElseThrow(() -> new ValidationException("Examen inconnu : " + rdv.getIdExamen()));
        EquipementImagerie eq = s.getEquipementAttribueId() == null ? null : store.equipements.findById(s.getEquipementAttribueId()).orElse(null);
        List<RendezVous> autres = store.rendezVous.findWhere(r -> r.isActif() && !Objects.equals(r.getIdRdv(), rdv.getIdRdv()));
        return new ContexteConflit(rdv, p, m, man, s, eq, ex, autres, store);
    }

    /** Exécute toutes les règles sans rien enregistrer. */
    public List<Conflit> verifier(RendezVous rdv) {
        ContexteConflit ctx = construireContexte(rdv);
        List<Conflit> conflits = new ArrayList<>();
        for (RegleConflit r : regles) conflits.addAll(r.verifier(ctx));
        return conflits;
    }

    /** Vérifie puis, en cas de conflit, journalise et lève une ConflitException (interdiction). */
    public void verifierOuRejeter(RendezVous rdv, String contexte) {
        List<Conflit> conflits = verifier(rdv);
        if (!conflits.isEmpty()) {
            journaliser(conflits, contexte);
            throw new ConflitException(conflits);
        }
    }

    /** Enregistre les conflits interceptés dans journal_conflits.json (statistiques F10). */
    public void journaliser(List<Conflit> conflits, String contexte) {
        for (Conflit c : conflits) {
            store.journalConflits.create(new ConflitJournal(store.journalConflits.nextId(), LocalDateTime.now(), c.type(), c.message(), contexte));
        }
    }
}
