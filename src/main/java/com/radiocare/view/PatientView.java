package com.radiocare.view;

import com.radiocare.controller.PatientController;
import com.radiocare.model.Patient;
import com.radiocare.model.enums.ContreIndication;
import com.radiocare.service.PatientService;

import java.util.List;

import static com.radiocare.view.Console.*;

/** Vue console F1 : gestion intégrée des patients et dossiers médicaux. */
public class PatientView {
    private final Console console;
    private final PatientController ctrl;

    public PatientView(Console console, PatientController ctrl) {
        this.console = console;
        this.ctrl = ctrl;
    }

    public void afficher() {
        new Menu(console, "PATIENTS & DOSSIERS MÉDICAUX (F1)")
                .option("Lister les patients", () -> lister(ctrl.lister()))
                .option("Rechercher un patient", this::rechercher)
                .option("Consulter le dossier complet d'un patient", this::dossier)
                .option("Ajouter un patient", this::ajouter)
                .option("Modifier un patient", this::modifier)
                .option("Supprimer un patient", this::supprimer)
                .executer();
    }

    private void lister(List<Patient> patients) {
        Tableau.de(patients)
                .col("CIN", Patient::getCin)
                .col("Nom", Patient::getNom)
                .col("Prénom", Patient::getPrenom)
                .colD("Âge", Patient::getAge)
                .col("Téléphone", Patient::getTelephone)
                .col("Assurance", p -> p.isAssure() ? p.getNumAssurance() + " (" + Math.round(p.getTauxCouvertureAssurance() * 100) + "%)" : "Non assuré", 30)
                .colD("Solde", p -> tnd(p.getSoldeCompte()))
                .col("Contre-ind.", p -> p.getContreIndications().isEmpty() ? "" : String.valueOf(p.getContreIndications().size()))
                .afficher(console);
    }

    private void rechercher() {
        String t = console.lireTexte("Texte recherché (CIN, nom, prénom, téléphone, n° assurance)");
        lister(ctrl.rechercher(t));
    }

    Patient selectionner() {
        return Selection.parRecherche(console, "Patient", ctrl.lister(), Patient::getCin, Patient::getNomComplet);
    }

    private void dossier() {
        Patient p = selectionner();
        console.sousTitre("Fiche patient — " + p.getNomComplet());
        console.champ("CIN / Matricule", p.getCin());
        console.champ("Date de naissance", date(p.getDateNaissance()) + " (" + p.getAge() + " ans)");
        console.champ("Téléphone", p.getTelephone());
        console.champ("E-mail", p.getEmail());
        console.champ("N° assurance", p.isAssure() ? p.getNumAssurance() + " — prise en charge " + Math.round(p.getTauxCouvertureAssurance() * 100) + " %" : "Non assuré");
        console.champ("Contre-indications", p.getContreIndications().isEmpty() ? "Aucune"
                : console.rouge(String.join(", ", p.getContreIndications().stream().map(ContreIndication::getLibelle).toList())));

        PatientService.BilanPatient b = ctrl.bilan(p.getCin());
        console.sousTitre("Bilan financier");
        console.champ("Examens réalisés", b.nombreExamens());
        console.champ("Montant global des actes (TTC)", tnd(b.totalActesTTC()));
        console.champ("Part prise en charge assurance", tnd(b.partAssurance()));
        console.champ("Part patient", tnd(b.partPatient()));
        console.champ("Montants déjà réglés", tnd(b.dejaRegle()));
        console.champ("Reste à payer", b.resteAPayer() > 0 ? console.jaune(tnd(b.resteAPayer())) : console.vert(tnd(0)));
        console.champ("Solde du compte", b.soldeCompte() < 0 ? console.rouge(tnd(b.soldeCompte()) + " (débiteur)") : tnd(b.soldeCompte()));

        console.sousTitre("Historique des examens d'imagerie");
        Tableau.de(ctrl.historique(p.getCin()))
                .col("RDV", PatientService.LigneHistorique::idRdv)
                .col("Date", l -> date(l.date()))
                .col("Examen", PatientService.LigneHistorique::examen, 38)
                .col("Statut", PatientService.LigneHistorique::statut)
                .col("Résultat", PatientService.LigneHistorique::resultat)
                .colD("Montant TTC", l -> l.montantTTC() > 0 ? tnd(l.montantTTC()) : "—")
                .afficher(console);
        console.pause();
    }

    private void ajouter() {
        console.sousTitre("Nouveau patient");
        Patient p = new Patient();
        p.setCin(console.lireTexte("CIN / matricule (8 chiffres)"));
        saisir(p);
        ctrl.ajouter(p);
        console.succes("Patient " + p.getNomComplet() + " enregistré.");
    }

    private void modifier() {
        Patient existant = selectionner();
        Patient p = new Patient();
        p.setCin(existant.getCin());
        p.setNom(existant.getNom());
        p.setPrenom(existant.getPrenom());
        p.setDateNaissance(existant.getDateNaissance());
        p.setTelephone(existant.getTelephone());
        p.setEmail(existant.getEmail());
        p.setNumAssurance(existant.getNumAssurance());
        p.setTauxCouvertureAssurance(existant.getTauxCouvertureAssurance());
        p.setContreIndications(new java.util.ArrayList<>(existant.getContreIndications()));
        console.info("Entrée = conserver la valeur actuelle.");
        saisir(p);
        ctrl.modifier(p);
        console.succes("Patient mis à jour.");
    }

    private void saisir(Patient p) {
        p.setNom(console.lireTexte("Nom", p.getNom()));
        p.setPrenom(console.lireTexte("Prénom", p.getPrenom()));
        p.setDateNaissance(console.lireDate("Date de naissance", p.getDateNaissance()));
        p.setTelephone(console.lireTexteOptionnel("Téléphone", p.getTelephone()));
        p.setEmail(console.lireTexteOptionnel("E-mail", p.getEmail()));
        p.setNumAssurance(console.lireTexteOptionnel("N° d'assurance", p.getNumAssurance()));
        if (p.getNumAssurance() != null) {
            double taux = console.lireDecimal("Taux de prise en charge (%)", p.getTauxCouvertureAssurance() * 100, 0, 100);
            p.setTauxCouvertureAssurance(taux / 100.0);
        } else {
            p.setTauxCouvertureAssurance(0);
        }
        p.setContreIndications(console.choisirPlusieurs("Contre-indications médicales majeures", List.of(ContreIndication.values()),
                ContreIndication::getLibelle, p.getContreIndications(), true));
    }

    private void supprimer() {
        Patient p = selectionner();
        if (console.confirmer("Supprimer définitivement " + p.getNomComplet() + " ?", false)) {
            ctrl.supprimer(p.getCin());
            console.succes("Patient supprimé.");
        }
    }
}
