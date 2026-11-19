package com.radiocare.service.conflit;

import com.radiocare.model.RendezVous;
import com.radiocare.model.enums.TypeConflit;

import java.util.ArrayList;
import java.util.List;

/**
 * Conflit 1 : une salle ou un équipement ne peut pas être planifié sur deux examens aux créneaux chevauchants.
 * Vérifie également la disponibilité et la conformité technique de la salle et de son équipement.
 */
public class RegleDoubleReservation implements RegleConflit {
    private static final TypeConflit T = TypeConflit.DOUBLE_RESERVATION;

    @Override
    public List<Conflit> verifier(ContexteConflit ctx) {
        List<Conflit> res = new ArrayList<>();
        var salle = ctx.salle();
        var eq = ctx.equipement();
        if (!salle.isDisponible())
            res.add(new Conflit(T, "La salle " + salle.getNom() + " n'est pas disponible (" + salle.getStatutDispo() + ")."));
        if (!salle.isBlindageConforme())
            res.add(new Conflit(T, "La salle " + salle.getNom() + " n'est pas conforme : blindage plomb insuffisant ("
                    + salle.getEpaisseurPlombMm() + " mm)."));
        if (eq == null) {
            res.add(new Conflit(T, "Aucun équipement n'est attribué à la salle " + salle.getNom() + "."));
        } else {
            if (!eq.isOperationnel())
                res.add(new Conflit(T, "L'équipement " + eq.getLibelleCourt() + " est indisponible (" + eq.getStatut() + ")."));
            if (eq.getType() != ctx.examen().getTypeEquipementRequis())
                res.add(new Conflit(T, "L'examen « " + ctx.examen().getLibelle() + " » requiert un équipement "
                        + ctx.examen().getTypeEquipementRequis() + " mais la salle est équipée d'un " + eq.getType() + "."));
        }
        for (RendezVous autre : ctx.autresRdvActifs()) {
            if (!autre.chevauche(ctx.rdv())) continue;
            if (autre.getIdSalle().equals(salle.getId())) {
                res.add(new Conflit(T, "La salle " + salle.getNom() + " est déjà réservée par " + autre.getIdRdv()
                        + " (" + autre.getHeureDebut() + "-" + autre.getHeureFin() + ")."));
            } else if (ctx.memeEquipement(autre)) {
                res.add(new Conflit(T, "L'équipement " + eq.getId() + " est déjà réservé par " + autre.getIdRdv()
                        + " (" + autre.getHeureDebut() + "-" + autre.getHeureFin() + ")."));
            }
        }
        return res;
    }
}
