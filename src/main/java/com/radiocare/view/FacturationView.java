package com.radiocare.view;

import com.radiocare.controller.FacturationController;
import com.radiocare.controller.RendezVousController;
import com.radiocare.model.FactureMedicale;
import com.radiocare.model.Paiement;
import com.radiocare.model.enums.ModePaiement;
import com.radiocare.model.enums.StatutPaiement;
import com.radiocare.service.ComptabiliteService.BilanMensuel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import static com.radiocare.view.Console.*;

/** Vue console F5 : facturation, tiers payant, paiements et bilan financier mensuel. */
public class FacturationView {
    private final Console console;
    private final FacturationController ctrl;
    private final RendezVousController rdv;
    private final PatientView patients;

    public FacturationView(Console console, FacturationController ctrl, RendezVousController rdv, PatientView patients) {
        this.console = console;
        this.ctrl = ctrl;
        this.rdv = rdv;
        this.patients = patients;
    }

    public void afficher() {
        new Menu(console, "FACTURATION, PAIEMENTS & BILAN FINANCIER (F5)")
                .option("Lister les factures", () -> listerFactures(ctrl.factures()))
                .option("Factures impayées / en retard", () -> listerFactures(ctrl.factures().stream().filter(f -> f.getStatutPaiement() != StatutPaiement.REGLEE).toList()))
                .option("Factures d'un patient", this::facturesPatient)
                .option("Détail d'une facture", this::detail)
                .option("Enregistrer un paiement", this::payer)
                .option("Lister les paiements", this::listerPaiements)
                .option("Annuler un paiement (erreur de saisie)", this::supprimerPaiement)
                .option("Annuler une facture sans paiement", this::supprimerFacture)
                .option("Bilan financier mensuel du centre", this::bilan)
                .option("Évolution sur 6 mois (recettes / dépenses)", this::evolution)
                .executer();
    }

    private void listerFactures(List<FactureMedicale> factures) {
        Tableau.de(factures)
                .col("N° facture", FactureMedicale::getNumFacture).col("Émise le", f -> date(f.getDateEmission()))
                .col("Patient", f -> rdv.detail(f.getIdRdv()).patient(), 22).col("Examen", f -> rdv.detail(f.getIdRdv()).examen(), 26)
                .colD("HT", f -> tnd(f.getMontantHT())).colD("TTC", f -> tnd(f.getMontantTTC()))
                .colD("Assurance", f -> tnd(f.getPartAssurance())).colD("Patient", f -> tnd(f.getPartPatient()))
                .colD("Reste", f -> tnd(f.getResteAPayer()))
                .col("Statut", f -> switch (f.getStatutPaiement()) {
                    case REGLEE -> console.vert("Réglée");
                    case EN_ATTENTE -> console.jaune("En Attente");
                    case RETARD -> console.rouge("Retard");
                })
                .afficher(console);
    }

    private void facturesPatient() {
        var p = patients.selectionner();
        listerFactures(ctrl.factures().stream().filter(f -> f.getIdPatient().equals(p.getCin())).toList());
    }

    private FactureMedicale selectionner(boolean impayeesSeulement) {
        List<FactureMedicale> l = ctrl.factures().stream().filter(f -> !impayeesSeulement || f.getResteAPayer() > 0).toList();
        return Selection.parRecherche(console, "Facture (n° ou nom du patient)", l, FactureMedicale::getNumFacture,
                f -> rdv.detail(f.getIdRdv()).patient() + " — " + date(f.getDateEmission()) + " — reste " + tnd(f.getResteAPayer()));
    }

    private void detail() {
        FactureMedicale f = selectionner(false);
        var l = rdv.detail(f.getIdRdv());
        console.sousTitre("Facture " + f.getNumFacture());
        console.champ("Patient", l.patient() + " (" + f.getIdPatient() + ")");
        console.champ("Acte", l.examen() + " du " + date(l.date()) + " — RDV " + f.getIdRdv());
        console.champ("Date d'émission / échéance", date(f.getDateEmission()) + " / " + date(f.getDateEcheance()));
        console.champ("Montant HT", tnd(f.getMontantHT()));
        console.champ("TVA " + Math.round(f.getTauxTVA() * 100) + " %", tnd(f.getMontantTVA()));
        console.champ("Montant TTC", console.gras(tnd(f.getMontantTTC())));
        console.champ("Part assurance (tiers payant)", tnd(f.getPartAssurance()));
        console.champ("Part patient", tnd(f.getPartPatient()));
        console.champ("Déjà payé", tnd(f.getMontantPaye()));
        console.champ("Reste à payer", tnd(f.getResteAPayer()));
        console.champ("Statut", f.getStatutPaiement().getLibelle());
        List<Paiement> p = ctrl.paiementsDe(f.getNumFacture());
        if (!p.isEmpty()) { console.sousTitre("Paiements"); tableauPaiements(p); }
        console.pause();
    }

    private void payer() {
        FactureMedicale f = selectionner(true);
        console.info("Reste à payer : " + tnd(f.getResteAPayer()));
        double montant = console.lireDecimal("Montant reçu (TND)", f.getResteAPayer(), 0.001, f.getResteAPayer());
        ModePaiement mode = console.choisirEnum("Mode de paiement", ModePaiement.class, ModePaiement.ESPECES);
        String preuve = console.lireTexte("Référence de la preuve (doit commencer par " + mode.getPrefixePreuve() + "-, ex. "
                + mode.getPrefixePreuve() + "-004512)");
        LocalDate d = console.lireDate("Date du paiement", LocalDate.now());
        Paiement p = ctrl.payer(f.getNumFacture(), d, montant, mode, preuve);
        FactureMedicale maj = ctrl.facture(f.getNumFacture());
        console.succes("Paiement " + p.getId() + " enregistré ; facture " + maj.getStatutPaiement().getLibelle().toLowerCase()
                + ", reste " + tnd(maj.getResteAPayer()) + ". Solde patient mis à jour.");
    }

    private void tableauPaiements(List<Paiement> l) {
        Tableau.de(l)
                .col("ID", Paiement::getId).col("Facture", Paiement::getNumFacture).col("Date", p -> date(p.getDatePaiement()))
                .colD("Montant", p -> tnd(p.getMontantRecu())).col("Mode", p -> p.getModePaiement().getLibelle())
                .col("Preuve", Paiement::getReferenceTextuellePreuve)
                .afficher(console);
    }

    private void listerPaiements() { tableauPaiements(ctrl.paiements()); }

    private void supprimerPaiement() {
        Paiement p = Selection.parRecherche(console, "Paiement (ID ou n° facture)", ctrl.paiements(), Paiement::getId,
                x -> x.getNumFacture() + " " + date(x.getDatePaiement()) + " " + tnd(x.getMontantRecu()));
        if (console.confirmer("Annuler le paiement " + p.getId() + " de " + tnd(p.getMontantRecu()) + " ?", false)) {
            ctrl.supprimerPaiement(p.getId());
            console.succes("Paiement annulé, facture et solde patient recalculés.");
        }
    }

    private void supprimerFacture() {
        FactureMedicale f = selectionner(false);
        if (console.confirmer("Annuler la facture " + f.getNumFacture() + " ?", false)) { ctrl.supprimerFacture(f.getNumFacture()); console.succes("Facture annulée."); }
    }

    private void bilan() {
        YearMonth mois = console.lireMois("Mois", YearMonth.now());
        BilanMensuel b = ctrl.bilan(mois);
        console.sousTitre("Rapprochement financier — " + mois);
        console.println(console.gras("  Chiffre d'affaires"));
        console.champ("CA HT (actes facturés)", tnd(b.chiffreAffairesHT()));
        console.champ("TVA collectée 19 %", tnd(b.tvaCollectee()));
        console.champ("CA TTC", tnd(b.chiffreAffairesTTC()));
        console.println(console.gras("  Recettes"));
        console.champ("Encaissements patients", tnd(b.recettesPatients()));
        console.champ("Parts assurances (tiers payant)", tnd(b.recettesAssurances()));
        console.champ("Total recettes", console.vert(tnd(b.totalRecettes())));
        console.println(console.gras("  Dépenses"));
        console.champ("Maintenance des équipements", tnd(b.depensesMaintenance()));
        console.champ("Salaires manipulateurs", tnd(b.salairesManipulateurs()));
        console.champ("Honoraires médecins", tnd(b.honorairesMedecins()));
        console.champ("Honoraires experts (télé-exp.)", tnd(b.honorairesExperts()));
        console.champ("Réapprovisionnement consommables", tnd(b.achatsConsommables()));
        console.champ("Total dépenses", console.rouge(tnd(b.totalDepenses())));
        console.champ("RÉSULTAT", b.resultat() >= 0 ? console.vert(tnd(b.resultat())) : console.rouge(tnd(b.resultat())));
        console.pause();
    }

    private void evolution() {
        List<BilanMensuel> h = ctrl.historique(YearMonth.now(), 6);
        double max = h.stream().mapToDouble(b -> Math.max(b.totalRecettes(), b.totalDepenses())).max().orElse(1);
        console.sousTitre("Recettes (R) et dépenses (D) sur 6 mois");
        for (BilanMensuel b : h) {
            console.println(String.format(Locale.FRANCE, "  %s R %s %s", b.mois(), console.vert(barre(100 * b.totalRecettes() / max, 40)), tnd(b.totalRecettes())));
            console.println(String.format(Locale.FRANCE, "          D %s %s   → résultat %s", console.rouge(barre(100 * b.totalDepenses() / max, 40)),
                    tnd(b.totalDepenses()), tnd(b.resultat())));
        }
        console.pause();
    }
}
