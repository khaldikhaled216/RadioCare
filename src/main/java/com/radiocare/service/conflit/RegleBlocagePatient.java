package com.radiocare.service.conflit;

import com.radiocare.model.ConsommableMedical;
import com.radiocare.model.enums.CategorieConsommable;
import com.radiocare.model.enums.ContreIndication;
import com.radiocare.model.enums.TypeConflit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Conflit 5 : refus du RDV si contre-indication médicale majeure au dossier ou solde débiteur trop élevé. */
public class RegleBlocagePatient implements RegleConflit {
    private final double seuilSoldeDebiteur;

    public RegleBlocagePatient(double seuilSoldeDebiteur) { this.seuilSoldeDebiteur = seuilSoldeDebiteur; }

    @Override
    public List<Conflit> verifier(ContexteConflit ctx) {
        List<Conflit> res = new ArrayList<>();
        var p = ctx.patient();
        var ex = ctx.examen();
        CategorieConsommable produit = ex.necessiteConsommable()
                ? ctx.store().consommables.findById(ex.getIdConsommableRequis()).map(ConsommableMedical::getCategorie).orElse(null)
                : null;
        for (ContreIndication ci : p.getContreIndications()) {
            if (ci.interdit(ex.getTypeEquipementRequis(), produit))
                res.add(new Conflit(TypeConflit.BLOCAGE_PATIENT, "Contre-indication majeure : « " + ci.getLibelle()
                        + " » incompatible avec l'examen « " + ex.getLibelle() + " »."));
        }
        if (p.getSoldeCompte() < -seuilSoldeDebiteur) {
            res.add(new Conflit(TypeConflit.BLOCAGE_PATIENT, String.format(Locale.FRANCE,
                    "Solde débiteur trop élevé : %.3f TND (seuil autorisé : -%.3f TND).", p.getSoldeCompte(), seuilSoldeDebiteur)));
        }
        return res;
    }
}
