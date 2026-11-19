package com.radiocare.service.conflit;

import com.radiocare.model.enums.Habilitation;
import com.radiocare.model.enums.TypeConflit;

import java.util.List;

/** Conflit 4 : un manipulateur ne possédant pas l'habilitation spécifique (IRM, Scanner...) ne peut être planifié. */
public class RegleHabilitation implements RegleConflit {

    @Override
    public List<Conflit> verifier(ContexteConflit ctx) {
        Habilitation requise = ctx.examen().getTypeEquipementRequis().getHabilitationRequise();
        if (!ctx.manip().possedeHabilitation(requise)) {
            return List.of(new Conflit(TypeConflit.HABILITATION, "Le manipulateur " + ctx.manip().getNomComplet()
                    + " ne possède pas l'habilitation « " + requise.getLibelle() + " » requise pour « "
                    + ctx.examen().getLibelle() + " » (habilitations : " + ctx.manip().getHabilitations() + ")."));
        }
        return List.of();
    }
}
