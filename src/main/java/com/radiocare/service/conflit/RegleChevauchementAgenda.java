package com.radiocare.service.conflit;

import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.StatutDispo;
import com.radiocare.model.enums.TypeConflit;

import java.util.ArrayList;
import java.util.List;

/** Conflit 2 : un même médecin ou manipulateur ne peut pas être affecté à deux examens simultanés. */
public class RegleChevauchementAgenda implements RegleConflit {
    private static final TypeConflit T = TypeConflit.CHEVAUCHEMENT_AGENDA;

    @Override
    public List<Conflit> verifier(ContexteConflit ctx) {
        List<Conflit> res = new ArrayList<>();
        RendezVous rdv = ctx.rdv();
        var med = ctx.medecin();
        var man = ctx.manip();
        if (med.getDispoPlanning() != null && !med.getDispoPlanning().couvre(rdv.getDate(), rdv.getHeureDebut(), rdv.getHeureFin()))
            res.add(new Conflit(T, "Le Dr " + med.getNomComplet() + " n'est pas disponible sur ce créneau (planning : "
                    + med.getDispoPlanning().resume() + ")."));
        if (man.getStatutDispo() != StatutDispo.DISPONIBLE)
            res.add(new Conflit(T, "Le manipulateur " + man.getNomComplet() + " est " + man.getStatutDispo().getLibelle().toLowerCase() + "."));
        for (RendezVous autre : ctx.autresRdvActifs()) {
            if (!autre.chevauche(rdv)) continue;
            if (autre.getIdMedecin().equals(med.getId()))
                res.add(new Conflit(T, "Le Dr " + med.getNomComplet() + " est déjà affecté à " + autre.getIdRdv()
                        + " (" + autre.getHeureDebut() + "-" + autre.getHeureFin() + ")."));
            if (autre.getIdManip().equals(man.getId()))
                res.add(new Conflit(T, "Le manipulateur " + man.getNomComplet() + " est déjà affecté à " + autre.getIdRdv()
                        + " (" + autre.getHeureDebut() + "-" + autre.getHeureFin() + ")."));
            if (autre.getIdPatient().equals(rdv.getIdPatient()))
                res.add(new Conflit(T, "Le patient a déjà un examen sur ce créneau (" + autre.getIdRdv() + ")."));
        }
        return res;
    }
}
