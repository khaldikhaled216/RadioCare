package com.radiocare.service;

import com.radiocare.dao.DataStore;
import com.radiocare.model.FactureMedicale;
import com.radiocare.model.Paiement;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** F5 : rapprochement financier mensuel du centre (recettes patients/assurances vs dépenses). */
public class ComptabiliteService {
    private final DataStore store;
    private final PersonnelService personnelService;
    private final EquipementService equipementService;
    private final StockService stockService;
    private final TeleExpertiseService teleExpertiseService;

    public ComptabiliteService(DataStore store, PersonnelService personnelService, EquipementService equipementService,
                               StockService stockService, TeleExpertiseService teleExpertiseService) {
        this.store = store;
        this.personnelService = personnelService;
        this.equipementService = equipementService;
        this.stockService = stockService;
        this.teleExpertiseService = teleExpertiseService;
    }

    /** Bilan mensuel complet. Les recettes sont exprimées en TTC encaissé, la TVA collectée est isolée. */
    public record BilanMensuel(String mois, double chiffreAffairesHT, double tvaCollectee, double chiffreAffairesTTC,
                               double recettesPatients, double recettesAssurances, double totalRecettes,
                               double depensesMaintenance, double salairesManipulateurs, double honorairesMedecins,
                               double honorairesExperts, double achatsConsommables, double totalDepenses, double resultat) { }

    public BilanMensuel bilan(YearMonth mois) {
        List<FactureMedicale> factures = store.factures.findWhere(f -> YearMonth.from(f.getDateEmission()).equals(mois));
        double ht = factures.stream().mapToDouble(FactureMedicale::getMontantHT).sum();
        double ttc = factures.stream().mapToDouble(FactureMedicale::getMontantTTC).sum();
        double assurances = factures.stream().mapToDouble(FactureMedicale::getPartAssurance).sum();
        double patients = store.paiements.findWhere(p -> YearMonth.from(p.getDatePaiement()).equals(mois))
                .stream().mapToDouble(Paiement::getMontantRecu).sum();
        double maintenance = equipementService.depensesMaintenance(mois);
        double salaires = personnelService.totalSalairesManipulateurs(mois);
        double honoraires = personnelService.totalHonorairesMedecins(mois);
        double experts = teleExpertiseService.honorairesExperts(mois);
        double achats = stockService.depensesReapprovisionnement(mois);
        double recettes = patients + assurances;
        double depenses = maintenance + salaires + honoraires + experts + achats;
        return new BilanMensuel(mois.toString(), r(ht), r(ttc - ht), r(ttc), r(patients), r(assurances), r(recettes),
                r(maintenance), r(salaires), r(honoraires), r(experts), r(achats), r(depenses), r(recettes - depenses));
    }

    /** Bilans des n derniers mois (le plus ancien en premier), pour les graphiques d'évolution. */
    public List<BilanMensuel> historique(YearMonth fin, int nombreMois) {
        List<BilanMensuel> res = new ArrayList<>();
        for (int i = nombreMois - 1; i >= 0; i--) res.add(bilan(fin.minusMonths(i)));
        return res;
    }

    private static double r(double v) { return Validateur.arrondi(v); }
}
