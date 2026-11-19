package com.radiocare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radiocare.dao.DataStore;
import com.radiocare.dao.JsonMapperFactory;
import com.radiocare.model.*;
import com.radiocare.model.enums.StatutRdv;
import com.radiocare.model.enums.TypeConflit;
import com.radiocare.model.enums.TypeEquipement;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** F10 : tableaux de bord statistiques, taux d'occupation, rentabilité et export des rapports. */
public class StatistiqueService {
    private final DataStore store;
    private final ComptabiliteService comptabiliteService;

    public StatistiqueService(DataStore store, ComptabiliteService comptabiliteService) {
        this.store = store;
        this.comptabiliteService = comptabiliteService;
    }

    public record Occupation(String id, String libelle, long minutesReservees, long minutesDisponibles, double taux) { }
    public record Rentabilite(String codeActe, String libelle, int nombreActes, double chiffreAffairesHT, double coutConsommables,
                              double marge, double tauxMarge) { }
    public record RapportActivite(String mois, LocalDateTime genereLe, int rdvTotal, Map<String, Long> rdvParStatut,
                                  List<Occupation> occupationSalles, List<Occupation> occupationParTypeEquipement,
                                  double attenteMoyenneMinutes, double delaiMoyenObtentionJours, Map<String, Long> conflitsParType,
                                  List<Rentabilite> rentabilite, ComptabiliteService.BilanMensuel bilan) { }

    /** Minutes d'ouverture du centre sur [debut, fin] (jours ouvrés uniquement). */
    public long minutesOuverture(LocalDate debut, LocalDate fin) {
        long jours = debut.datesUntil(fin.plusDays(1)).filter(d -> ParametresCentre.estJourOuvre(d.getDayOfWeek())).count();
        return jours * ParametresCentre.minutesOuvertureParJour();
    }

    private List<RendezVous> rdvOccupants(LocalDate debut, LocalDate fin) {
        return store.rendezVous.findWhere(r -> r.getStatut() != StatutRdv.ANNULE && !r.getDate().isBefore(debut) && !r.getDate().isAfter(fin));
    }

    /** Taux d'occupation par salle = minutes réservées (hors annulés) / minutes d'ouverture. */
    public List<Occupation> occupationSalles(LocalDate debut, LocalDate fin) {
        long dispo = minutesOuverture(debut, fin);
        List<RendezVous> rdvs = rdvOccupants(debut, fin);
        return store.salles.findAll().stream().sorted(Comparator.comparing(SalleRadiologie::getId)).map(s -> {
            long min = rdvs.stream().filter(r -> r.getIdSalle().equals(s.getId())).mapToLong(RendezVous::getDureeMinutes).sum();
            return new Occupation(s.getId(), s.getNom(), min, dispo, taux(min, dispo));
        }).toList();
    }

    /** Taux de charge par équipement lourd. */
    public List<Occupation> occupationEquipements(LocalDate debut, LocalDate fin) {
        long dispo = minutesOuverture(debut, fin);
        List<RendezVous> rdvs = rdvOccupants(debut, fin);
        return store.equipements.findAll().stream().sorted(Comparator.comparing(EquipementImagerie::getId)).map(e -> {
            Set<String> salles = new HashSet<>();
            store.salles.findWhere(s -> e.getId().equals(s.getEquipementAttribueId())).forEach(s -> salles.add(s.getId()));
            long min = rdvs.stream().filter(r -> salles.contains(r.getIdSalle())).mapToLong(RendezVous::getDureeMinutes).sum();
            return new Occupation(e.getId(), e.getLibelleCourt(), min, dispo, taux(min, dispo));
        }).toList();
    }

    /** Taux de charge agrégé par type d'équipement (ex. IRM vs Scanner). */
    public List<Occupation> occupationParTypeEquipement(LocalDate debut, LocalDate fin) {
        List<Occupation> parEq = occupationEquipements(debut, fin);
        List<Occupation> res = new ArrayList<>();
        for (TypeEquipement t : TypeEquipement.values()) {
            List<String> ids = store.equipements.findWhere(e -> e.getType() == t).stream().map(EquipementImagerie::getId).toList();
            if (ids.isEmpty()) continue;
            long min = parEq.stream().filter(o -> ids.contains(o.id())).mapToLong(Occupation::minutesReservees).sum();
            long dispo = parEq.stream().filter(o -> ids.contains(o.id())).mapToLong(Occupation::minutesDisponibles).sum();
            res.add(new Occupation(t.name(), t.getLibelle(), min, dispo, taux(min, dispo)));
        }
        return res;
    }

    private static double taux(long min, long dispo) { return dispo == 0 ? 0 : Validateur.arrondi(100.0 * min / dispo); }

    /** Temps moyen d'attente en salle (arrivée → prise en charge) des examens réalisés sur la période. */
    public double attenteMoyenneMinutes(LocalDate debut, LocalDate fin) {
        return store.rendezVous.findWhere(r -> !r.getDate().isBefore(debut) && !r.getDate().isAfter(fin) && r.getAttenteMinutes() >= 0)
                .stream().mapToLong(RendezVous::getAttenteMinutes).average().orElse(0);
    }

    /** Délai moyen (jours) entre la prise de RDV et la date de l'examen. */
    public double delaiMoyenObtentionJours(LocalDate debut, LocalDate fin) {
        return store.rendezVous.findWhere(r -> !r.getDate().isBefore(debut) && !r.getDate().isAfter(fin) && r.getDateCreation() != null)
                .stream().mapToLong(r -> Math.max(0, ChronoUnit.DAYS.between(r.getDateCreation().toLocalDate(), r.getDate()))).average().orElse(0);
    }

    /** Nombre de conflits d'horaires et de ressources interceptés par le moteur, par type. */
    public Map<String, Long> conflitsParType(LocalDate debut, LocalDate fin) {
        Map<String, Long> res = new LinkedHashMap<>();
        for (TypeConflit t : TypeConflit.values()) res.put(t.getLibelle(), 0L);
        store.journalConflits.findWhere(c -> !c.getDateHeure().toLocalDate().isBefore(debut) && !c.getDateHeure().toLocalDate().isAfter(fin))
                .forEach(c -> res.merge(c.getType().getLibelle(), 1L, Long::sum));
        return res;
    }

    public long totalConflits() { return store.journalConflits.count(); }

    public Map<String, Long> rdvParStatut(LocalDate debut, LocalDate fin) {
        Map<String, Long> res = new LinkedHashMap<>();
        for (StatutRdv s : StatutRdv.values()) res.put(s.getLibelle(), 0L);
        store.rendezVous.findWhere(r -> !r.getDate().isBefore(debut) && !r.getDate().isAfter(fin))
                .forEach(r -> res.merge(r.getStatut().getLibelle(), 1L, Long::sum));
        return res;
    }

    /** Analyse de rentabilité par type d'examen : CA HT − coût des consommables. */
    public List<Rentabilite> rentabiliteParExamen(LocalDate debut, LocalDate fin) {
        List<RendezVous> realises = store.rendezVous.findWhere(r -> r.getStatut() == StatutRdv.REALISE
                && !r.getDate().isBefore(debut) && !r.getDate().isAfter(fin));
        List<Rentabilite> res = new ArrayList<>();
        for (ExamenMedical ex : store.examens.findAll()) {
            long n = realises.stream().filter(r -> r.getIdExamen().equals(ex.getId())).count();
            double ca = n * ex.getTarifHT();
            double coutUnitaire = ex.necessiteConsommable()
                    ? store.consommables.findById(ex.getIdConsommableRequis()).map(c -> c.getPrixUnitaireHT() * ex.getQuantiteConsommable()).orElse(0.0) : 0;
            double cout = n * coutUnitaire;
            double marge = ca - cout;
            res.add(new Rentabilite(ex.getCodeActe(), ex.getLibelle(), (int) n, Validateur.arrondi(ca), Validateur.arrondi(cout),
                    Validateur.arrondi(marge), ca == 0 ? 0 : Validateur.arrondi(100 * marge / ca)));
        }
        res.sort(Comparator.comparingDouble(Rentabilite::marge).reversed());
        return res;
    }

    public RapportActivite rapport(YearMonth mois) {
        LocalDate d = mois.atDay(1), f = mois.atEndOfMonth();
        Map<String, Long> statuts = rdvParStatut(d, f);
        return new RapportActivite(mois.toString(), LocalDateTime.now().withNano(0), (int) statuts.values().stream().mapToLong(Long::longValue).sum(),
                statuts, occupationSalles(d, f), occupationParTypeEquipement(d, f), Validateur.arrondi(attenteMoyenneMinutes(d, f)),
                Validateur.arrondi(delaiMoyenObtentionJours(d, f)), conflitsParType(d, f), rentabiliteParExamen(d, f), comptabiliteService.bilan(mois));
    }

    /** Export structuré du rapport d'activité au format JSON et texte. Retourne les deux fichiers créés. */
    public List<Path> exporter(YearMonth mois, Path dossier) {
        RapportActivite r = rapport(mois);
        try {
            Files.createDirectories(dossier);
            Path json = dossier.resolve("rapport-activite-" + mois + ".json");
            Path txt = dossier.resolve("rapport-activite-" + mois + ".txt");
            ObjectMapper m = JsonMapperFactory.create();
            m.writeValue(json.toFile(), r);
            Files.writeString(txt, formaterTexte(r), StandardCharsets.UTF_8);
            return List.of(json, txt);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de l'export du rapport", e);
        }
    }

    public String formaterTexte(RapportActivite r) {
        StringBuilder sb = new StringBuilder();
        String sep = "=".repeat(78) + "\n";
        sb.append(sep).append("  RADIOCARE — RAPPORT D'ACTIVITÉ ").append(r.mois()).append("\n  Généré le ").append(r.genereLe()).append("\n").append(sep);
        sb.append("\n1. RENDEZ-VOUS (total : ").append(r.rdvTotal()).append(")\n");
        r.rdvParStatut().forEach((k, v) -> sb.append(String.format("   %-20s %6d%n", k, v)));
        sb.append("\n2. TAUX D'OCCUPATION DES SALLES\n");
        r.occupationSalles().forEach(o -> sb.append(String.format(Locale.FRANCE, "   %-28s %6d min / %6d min  %6.2f %%%n", o.libelle(), o.minutesReservees(), o.minutesDisponibles(), o.taux())));
        sb.append("\n3. TAUX DE CHARGE PAR TYPE D'ÉQUIPEMENT\n");
        r.occupationParTypeEquipement().forEach(o -> sb.append(String.format(Locale.FRANCE, "   %-28s %6.2f %%%n", o.libelle(), o.taux())));
        sb.append(String.format(Locale.FRANCE, "%n4. ATTENTE%n   Attente moyenne en salle      : %.1f min%n   Délai moyen d'obtention RDV   : %.1f jours%n",
                r.attenteMoyenneMinutes(), r.delaiMoyenObtentionJours()));
        sb.append("\n5. CONFLITS INTERCEPTÉS\n");
        r.conflitsParType().forEach((k, v) -> sb.append(String.format("   %-62s %4d%n", k, v)));
        sb.append("\n6. RENTABILITÉ PAR TYPE D'EXAMEN\n");
        sb.append(String.format("   %-34s %5s %12s %12s %12s %7s%n", "Examen", "Nb", "CA HT", "Coût cons.", "Marge", "%"));
        r.rentabilite().forEach(x -> sb.append(String.format(Locale.FRANCE, "   %-34s %5d %12.3f %12.3f %12.3f %6.1f%%%n",
                tronquer(x.libelle(), 34), x.nombreActes(), x.chiffreAffairesHT(), x.coutConsommables(), x.marge(), x.tauxMarge())));
        var b = r.bilan();
        sb.append("\n7. BILAN FINANCIER (TND)\n");
        sb.append(String.format(Locale.FRANCE, "   CA HT %.3f | TVA 19%% %.3f | CA TTC %.3f%n", b.chiffreAffairesHT(), b.tvaCollectee(), b.chiffreAffairesTTC()));
        sb.append(String.format(Locale.FRANCE, "   Recettes : patients %.3f + assurances %.3f = %.3f%n", b.recettesPatients(), b.recettesAssurances(), b.totalRecettes()));
        sb.append(String.format(Locale.FRANCE, "   Dépenses : maintenance %.3f, salaires %.3f, honoraires %.3f, experts %.3f, consommables %.3f = %.3f%n",
                b.depensesMaintenance(), b.salairesManipulateurs(), b.honorairesMedecins(), b.honorairesExperts(), b.achatsConsommables(), b.totalDepenses()));
        sb.append(String.format(Locale.FRANCE, "   RÉSULTAT : %.3f%n", b.resultat()));
        return sb.toString();
    }

    private static String tronquer(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "…"; }
}
