package com.radiocare.controller;

import com.radiocare.model.enums.StatutRdv;
import com.radiocare.service.AlerteService;
import com.radiocare.service.RendezVousService;
import com.radiocare.service.ServiceRegistry;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Synthèse du tableau de bord et centre de notifications. */
public class TableauDeBordController {
    private final ServiceRegistry reg;

    public TableauDeBordController(ServiceRegistry reg) { this.reg = reg; }

    public record Indicateurs(int patients, int rdvAujourdhui, int rdvSemaineAVenir, double caMoisTTC, double encaissementsMois,
                              double resultatMois, double occupationSemaine, long conflitsInterceptes, int alertes) { }

    public Indicateurs indicateurs() {
        LocalDate auj = LocalDate.now();
        YearMonth mois = YearMonth.from(auj);
        var bilan = reg.comptabilite.bilan(mois);
        LocalDate lundi = RendezVousService.lundiDe(auj);
        double occ = reg.statistiques.occupationSalles(lundi, lundi.plusDays(5)).stream()
                .mapToDouble(o -> o.taux()).average().orElse(0);
        int aVenir = (int) reg.rendezVous.planning(auj, auj.plusDays(7)).stream().filter(l -> l.rdv().getStatut() == StatutRdv.PLANIFIE).count();
        return new Indicateurs(reg.store.patients.count(), reg.rendezVous.planning(auj, auj).size(), aVenir,
                bilan.chiffreAffairesTTC(), bilan.recettesPatients(), bilan.resultat(), occ, reg.statistiques.totalConflits(),
                alertes().size());
    }

    public List<RendezVousService.LignePlanning> rdvDuJour() { return reg.rendezVous.planning(LocalDate.now(), LocalDate.now()); }

    public List<AlerteService.Alerte> alertes() { return reg.alertes.alertes(LocalDate.now()); }

    public List<String> avertissementsChargement() { return reg.store.getAvertissements(); }

    public String dossierDonnees() { return reg.store.getDossier().toAbsolutePath().toString(); }
}
