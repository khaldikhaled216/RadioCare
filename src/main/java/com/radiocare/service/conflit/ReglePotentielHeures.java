package com.radiocare.service.conflit;

import com.radiocare.model.enums.TypeConflit;

import java.util.List;
import java.util.Locale;

/**
 * Conflit 3 : un examen est rejeté si sa durée fait dépasser le quota d'heures autorisé à l'appareil avant maintenance.
 * Les heures déjà engagées par les autres RDV actifs sur le même équipement sont prises en compte.
 */
public class ReglePotentielHeures implements RegleConflit {

    @Override
    public List<Conflit> verifier(ContexteConflit ctx) {
        var eq = ctx.equipement();
        if (eq == null) return List.of();
        double heuresEngagees = ctx.autresRdvActifs().stream()
                .filter(ctx::memeEquipement)
                .mapToDouble(r -> r.getDureeMinutes() / 60.0).sum();
        double heuresDemandees = ctx.rdv().getDureeMinutes() / 60.0;
        double total = heuresEngagees + heuresDemandees;
        if (total > eq.getHeuresRestantesMaintenance() + 1e-9) {
            return List.of(new Conflit(TypeConflit.POTENTIEL_HEURES, String.format(Locale.FRANCE,
                    "L'équipement %s n'a plus que %.2f h avant maintenance ; %.2f h déjà planifiées + %.2f h demandées dépassent le quota.",
                    eq.getId(), eq.getHeuresRestantesMaintenance(), heuresEngagees, heuresDemandees)));
        }
        return List.of();
    }
}
