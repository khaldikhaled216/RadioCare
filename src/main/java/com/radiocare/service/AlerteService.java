package com.radiocare.service;

import com.radiocare.model.ConsommableMedical;
import com.radiocare.model.EquipementImagerie;
import com.radiocare.model.FactureMedicale;
import com.radiocare.model.SalleRadiologie;
import com.radiocare.model.enums.StatutEquipement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Centre de notifications du secrétariat : agrège toutes les alertes métier. */
public class AlerteService {
    public enum Niveau { CRITIQUE, AVERTISSEMENT, INFO }

    public record Alerte(Niveau niveau, String categorie, String message) { }

    private static final java.time.format.DateTimeFormatter FR = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EquipementService equipementService;
    private final StockService stockService;
    private final FacturationService facturationService;
    private final TeleExpertiseService teleExpertiseService;

    public AlerteService(EquipementService e, StockService s, FacturationService f, TeleExpertiseService t) {
        this.equipementService = e;
        this.stockService = s;
        this.facturationService = f;
        this.teleExpertiseService = t;
    }

    public List<Alerte> alertes(LocalDate aujourdHui) {
        List<Alerte> res = new ArrayList<>();
        for (EquipementImagerie e : equipementService.listerEquipements()) {
            if (e.getStatut() == StatutEquipement.EN_PANNE)
                res.add(new Alerte(Niveau.CRITIQUE, "Équipement", e.getLibelleCourt() + " (" + e.getId() + ") est en panne."));
            else if (e.getStatut() == StatutEquipement.EN_MAINTENANCE)
                res.add(new Alerte(Niveau.AVERTISSEMENT, "Équipement", e.getLibelleCourt() + " (" + e.getId() + ") est en maintenance."));
            if (equipementService.estEnAlerteMaintenance(e))
                res.add(new Alerte(e.getHeuresRestantesMaintenance() <= 0 ? Niveau.CRITIQUE : Niveau.AVERTISSEMENT, "Maintenance",
                        String.format(Locale.FRANCE, "Maintenance préventive proche : %s — %.1f h restantes sur %.0f h.",
                                e.getId(), e.getHeuresRestantesMaintenance(), e.getIntervalleMaintenanceHeures())));
        }
        for (SalleRadiologie s : equipementService.listerSalles()) {
            List<String> anomalies = equipementService.verifierConformite(s);
            if (!anomalies.isEmpty() && s.isDisponible())
                res.add(new Alerte(Niveau.AVERTISSEMENT, "Salle", s.getNom() + " : " + String.join(", ", anomalies)));
        }
        for (ConsommableMedical c : stockService.alertesStockCritique())
            res.add(new Alerte(c.getQuantiteEnStock() == 0 ? Niveau.CRITIQUE : Niveau.AVERTISSEMENT, "Stock",
                    "Stock critique : " + c.getDesignation() + " — " + c.getQuantiteEnStock() + " / seuil " + c.getSeuilAlerte()
                            + " (réappro. suggéré : " + stockService.quantiteSuggeree(c) + ")."));
        for (ConsommableMedical c : stockService.alertesPeremption(aujourdHui))
            res.add(new Alerte(c.isPerimeA(aujourdHui) ? Niveau.CRITIQUE : Niveau.AVERTISSEMENT, "Péremption",
                    c.getDesignation() + (c.isPerimeA(aujourdHui) ? " est périmé depuis le " : " périme le ") + c.getDatePeremption().format(FR) + "."));
        List<FactureMedicale> retards = facturationService.facturesEnRetard();
        if (!retards.isEmpty()) {
            double total = retards.stream().mapToDouble(FactureMedicale::getResteAPayer).sum();
            String plusAnciennes = retards.stream().sorted(java.util.Comparator.comparing(FactureMedicale::getDateEcheance)).limit(3)
                    .map(FactureMedicale::getNumFacture).collect(java.util.stream.Collectors.joining(", "));
            res.add(new Alerte(Niveau.INFO, "Facturation", String.format(Locale.FRANCE,
                    "%d facture(s) en retard de paiement, reste dû total %,.3f TND (plus anciennes : %s).", retards.size(), total, plusAnciennes)));
        }
        teleExpertiseService.urgencesEnRetard(LocalDateTime.now()).forEach(d ->
                res.add(new Alerte(Niveau.CRITIQUE, "Télé-expertise", "Avis urgent non reçu sous " + ParametresCentre.DELAI_LEGAL_URGENCE_HEURES
                        + " h : " + d.getId() + " (" + d.getNomExpertExterne() + ").")));
        res.sort((a, b) -> a.niveau().compareTo(b.niveau()));
        return res;
    }
}
