package com.radiocare.service.conflit;

import com.radiocare.model.enums.TypeConflit;
import com.radiocare.service.StockService;

import java.util.List;

/**
 * Conflit 6 : interdiction de valider un examen nécessitant un consommable (ex. IRM avec contraste)
 * si le stock prévisionnel du produit à la date de l'examen est insuffisant.
 */
public class RegleRuptureStock implements RegleConflit {
    private final StockService stockService;

    public RegleRuptureStock(StockService stockService) { this.stockService = stockService; }

    @Override
    public List<Conflit> verifier(ContexteConflit ctx) {
        var ex = ctx.examen();
        if (!ex.necessiteConsommable()) return List.of();
        var produit = ctx.store().consommables.findById(ex.getIdConsommableRequis()).orElse(null);
        if (produit == null)
            return List.of(new Conflit(TypeConflit.RUPTURE_STOCK, "Consommable " + ex.getIdConsommableRequis() + " introuvable."));
        int previsionnel = stockService.stockPrevisionnel(produit.getId(), ctx.rdv().getDate(), ctx.rdv().getIdRdv());
        if (previsionnel < ex.getQuantiteConsommable()) {
            return List.of(new Conflit(TypeConflit.RUPTURE_STOCK, "Stock prévisionnel de « " + produit.getDesignation()
                    + " » au " + ctx.rdv().getDate() + " : " + previsionnel + " (requis : " + ex.getQuantiteConsommable() + ")."));
        }
        return List.of();
    }
}
