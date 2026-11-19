package com.radiocare.service;

import com.radiocare.model.*;
import com.radiocare.model.enums.*;
import com.radiocare.service.conflit.ConflitException;

import java.time.*;
import java.util.*;

/**
 * Génère un jeu de données de démonstration fictif et cohérent (premier lancement).
 * Les rendez-vous passés sont créés via le moteur de conflits puis réalisés par le service,
 * ce qui produit factures, paiements, consommation de stock et décompte d'heures machine réalistes.
 */
public class DemoDataService {
    private final ServiceRegistry reg;
    private final Random rnd = new Random(42);

    public DemoDataService(ServiceRegistry reg) { this.reg = reg; }

    public void generer(LocalDate aujourdHui) {
        var s = reg.store;
        // ------------------------------------------------------------ Consommables (stocks larges pendant la génération)
        List<ConsommableMedical> conso = List.of(
                new ConsommableMedical("CSM-0001", "Gadolinium (Dotarem 10 ml)", CategorieConsommable.CONTRASTE_GADOLINIUM, 500, 10, 45.0, aujourdHui.plusMonths(10)),
                new ConsommableMedical("CSM-0002", "Iode (Omnipaque 350 - 100 ml)", CategorieConsommable.CONTRASTE_IODE, 500, 10, 38.0, aujourdHui.plusDays(20)),
                new ConsommableMedical("CSM-0003", "Gel d'échographie 250 ml", CategorieConsommable.GEL_ECHO, 500, 15, 4.5, aujourdHui.plusMonths(18)),
                new ConsommableMedical("CSM-0004", "Seringue haute pression 200 ml", CategorieConsommable.SERINGUE, 30, 20, 12.0, aujourdHui.plusYears(2)),
                new ConsommableMedical("CSM-0005", "Film radiographique 35x43", CategorieConsommable.FILM, 500, 50, 2.8, aujourdHui.plusMonths(14)));
        s.consommables.createAll(new ArrayList<>(conso));

        // ------------------------------------------------------------ Examens
        s.examens.createAll(new ArrayList<>(List.of(
                new ExamenMedical("EXA-0001", "IRM-CER", "IRM Cérébrale", TypeEquipement.IRM, 30, 280, null, 0),
                new ExamenMedical("EXA-0002", "IRM-CER-INJ", "IRM Cérébrale injectée (Gadolinium)", TypeEquipement.IRM, 45, 380, "CSM-0001", 1),
                new ExamenMedical("EXA-0003", "IRM-GEN", "IRM du Genou", TypeEquipement.IRM, 30, 260, null, 0),
                new ExamenMedical("EXA-0004", "SCN-THX", "Scanner Thoracique", TypeEquipement.SCANNER_CT, 20, 180, null, 0),
                new ExamenMedical("EXA-0005", "SCN-ABD-INJ", "Scanner Abdominal injecté (Iode)", TypeEquipement.SCANNER_CT, 30, 250, "CSM-0002", 1),
                new ExamenMedical("EXA-0006", "ECHO-ABD", "Échographie Abdominale", TypeEquipement.ECHOGRAPHE, 20, 70, "CSM-0003", 1),
                new ExamenMedical("EXA-0007", "ECHO-THY", "Échographie Thyroïdienne", TypeEquipement.ECHOGRAPHE, 15, 60, "CSM-0003", 1),
                new ExamenMedical("EXA-0008", "RX-PMN", "Radiographie Pulmonaire (RX Poumon)", TypeEquipement.TABLE_RX, 10, 35, "CSM-0005", 1),
                new ExamenMedical("EXA-0009", "RX-RACH", "Radiographie du Rachis lombaire", TypeEquipement.TABLE_RX, 15, 45, "CSM-0005", 1))));

        // ------------------------------------------------------------ Équipements & salles
        EquipementImagerie irm = new EquipementImagerie("EQP-0001", TypeEquipement.IRM, "Siemens", "Magnetom Vida 1.5T", 4120, 500, 500);
        EquipementImagerie scan = new EquipementImagerie("EQP-0002", TypeEquipement.SCANNER_CT, "GE Healthcare", "Revolution CT", 2810, 400, 400);
        EquipementImagerie rx = new EquipementImagerie("EQP-0003", TypeEquipement.TABLE_RX, "Philips", "DigitalDiagnost C90", 6020, 800, 800);
        EquipementImagerie echo1 = new EquipementImagerie("EQP-0004", TypeEquipement.ECHOGRAPHE, "Samsung", "RS85 Prestige", 1530, 1000, 1000);
        EquipementImagerie echo2 = new EquipementImagerie("EQP-0005", TypeEquipement.ECHOGRAPHE, "Mindray", "Resona 7", 640, 1000, 1000);
        s.equipements.createAll(new ArrayList<>(List.of(irm, scan, rx, echo1, echo2)));
        s.salles.createAll(new ArrayList<>(List.of(
                new SalleRadiologie("SAL-0001", "Salle IRM", 0, TypeSalle.SALLE_IRM, "EQP-0001", "CAGE-FARADAY/NF-EN-60601", 0),
                new SalleRadiologie("SAL-0002", "Salle Scanner", 0, TypeSalle.SALLE_SCANNER, "EQP-0002", "NF-C15-160/PB-2.5MM", 2.5),
                new SalleRadiologie("SAL-0003", "Salle Radio 1", 1, TypeSalle.SALLE_RADIOGRAPHIE, "EQP-0003", "NF-C15-160/PB-2MM", 2.0),
                new SalleRadiologie("SAL-0004", "Salle Écho A", 1, TypeSalle.SALLE_ECHOGRAPHIE, "EQP-0004", null, 0),
                new SalleRadiologie("SAL-0005", "Salle Écho B", 1, TypeSalle.SALLE_ECHOGRAPHIE, "EQP-0005", null, 0))));

        // ------------------------------------------------------------ Personnel
        var lunSam = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY);
        var lunVen = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        s.medecins.createAll(new ArrayList<>(List.of(
                med("MED-0001", "07123456", "Trabelsi", "Sana", Specialite.NEURORADIOLOGIE, 30, lunSam, 8, 19, "98 123 456"),
                med("MED-0002", "08234567", "Jaziri", "Karim", Specialite.OSTEO_ARTICULAIRE, 28, lunVen, 8, 17, "97 234 567"),
                med("MED-0003", "06345678", "Mansouri", "Leila", Specialite.THORACIQUE, 30, lunSam, 8, 19, "22 345 678"),
                med("MED-0004", "09456789", "Gharbi", "Mehdi", Specialite.GENERALE, 25,
                        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), 8, 14, "55 456 789"))));
        s.manipulateurs.createAll(new ArrayList<>(List.of(
                man("MAN-0001", "11223344", "Ferchichi", "Amine", EnumSet.of(Habilitation.IRM, Habilitation.SCANNER), 18, StatutDispo.DISPONIBLE),
                man("MAN-0002", "12334455", "Ben Ammar", "Nour", EnumSet.of(Habilitation.RAYONS_X, Habilitation.ECHOGRAPHIE), 15, StatutDispo.DISPONIBLE),
                man("MAN-0003", "13445566", "Khelifi", "Yassine", EnumSet.of(Habilitation.SCANNER, Habilitation.RAYONS_X), 16.5, StatutDispo.DISPONIBLE),
                man("MAN-0004", "14556677", "Bouazizi", "Rim", EnumSet.of(Habilitation.IRM, Habilitation.ECHOGRAPHIE), 17, StatutDispo.DISPONIBLE),
                man("MAN-0005", "15667788", "Haddad", "Skander", EnumSet.of(Habilitation.RAYONS_X), 14, StatutDispo.DISPONIBLE))));

        // ------------------------------------------------------------ Patients
        List<Patient> patients = new ArrayList<>(List.of(
                pat("10293847", "Ben Salah", "Hedi", 1958, 4, 12, "98 765 432", null, null, 0),
                pat("10384756", "Khemiri", "Amel", 1985, 7, 3, "22 111 222", "amel.khemiri@mail.tn", "CNAM-1985-07731", 0.7),
                pat("10475665", "Saidi", "Mohamed", 1972, 1, 25, "97 333 444", null, "CNAM-1972-04412", 0.8),
                pat("10566574", "Jlassi", "Ines", 1990, 11, 9, "55 222 333", "ines.jlassi@mail.tn", "ASSUR-STAR-88213", 0.6),
                pat("10657483", "Ayari", "Walid", 1966, 3, 17, "99 888 777", null, "CNAM-1966-11872", 0.8),
                pat("10748392", "Hammami", "Salma", 1995, 5, 30, "28 444 555", "salma.h@mail.tn", null, 0),
                pat("10839201", "Dridi", "Fethi", 1950, 9, 2, "71 222 444", null, "CNRPS-1950-3321", 0.85),
                pat("10920110", "Mejri", "Olfa", 1988, 12, 14, "24 666 777", "olfa.mejri@mail.tn", "CNAM-1988-55120", 0.7),
                pat("11011029", "Bouzid", "Anis", 1979, 6, 6, "52 999 000", null, "ASSUR-GAT-55410", 0.5),
                pat("11101938", "Chaabane", "Rania", 2001, 2, 20, "20 123 987", "rania.ch@mail.tn", "CNAM-2001-90017", 0.7),
                pat("11192847", "Nasri", "Tarek", 1961, 8, 11, "98 321 654", null, "CNAM-1961-22014", 0.8),
                pat("11283756", "Gabsi", "Yosra", 1993, 10, 27, "21 456 789", "yosra.gabsi@mail.tn", null, 0)));
        String[] noms = {"Ouni", "Riahi", "Zaafrane", "Kefi", "Sassi", "Toumi", "Baccouche", "Chebbi", "Hamdi", "Labidi",
                "Mzoughi", "Feki", "Karray", "Masmoudi", "Ellouze", "Ghanmi", "Jebali", "Zribi", "Abidi", "Mabrouk"};
        String[] prenoms = {"Ahmed", "Mariem", "Sami", "Hela", "Bilel", "Asma", "Nizar", "Wafa", "Khaled", "Imen",
                "Oussama", "Sonia", "Fares", "Dorra", "Hatem", "Emna", "Slim", "Nadia", "Achraf", "Lamia"};
        for (int i = 0; i < 40; i++) {
            boolean assure = rnd.nextDouble() < 0.75;
            String cin = String.valueOf(12000000 + 104729 * (i + 1));
            patients.add(pat(cin, noms[(i * 7) % noms.length], prenoms[(i * 3 + 1) % prenoms.length], 1945 + rnd.nextInt(60),
                    1 + rnd.nextInt(12), 1 + rnd.nextInt(28), (20 + rnd.nextInt(80)) + " " + (100 + rnd.nextInt(900)) + " " + (100 + rnd.nextInt(900)),
                    null, assure ? "CNAM-" + cin : null, assure ? 0.7 : 0));
        }
        patients.get(2).getContreIndications().add(ContreIndication.PACEMAKER);
        patients.get(3).getContreIndications().add(ContreIndication.ALLERGIE_IODE);
        patients.get(6).getContreIndications().add(ContreIndication.INSUFFISANCE_RENALE_SEVERE);
        patients.get(9).getContreIndications().add(ContreIndication.GROSSESSE);
        s.patients.createAll(patients);
        Patient hedi = patients.get(0);

        // ------------------------------------------------------------ Historique patient débiteur (2 IRM injectées non réglées)
        LocalDate j1 = ouvre(aujourdHui.minusDays(40));
        LocalDate j2 = ouvre(aujourdHui.minusDays(33));
        insererEtRealiser(new RendezVous(null, j1, LocalTime.of(8, 0), 45, hedi.getId(), "MED-0001", "MAN-0001", "SAL-0001", "EXA-0002"), aujourdHui);
        insererEtRealiser(new RendezVous(null, j2, LocalTime.of(8, 0), 45, hedi.getId(), "MED-0001", "MAN-0004", "SAL-0001", "EXA-0002"), aujourdHui);

        // ------------------------------------------------------------ Activité des 8 dernières semaines + 2 semaines à venir
        Map<TypeSalle, List<String>> examensParSalle = Map.of(
                TypeSalle.SALLE_IRM, List.of("EXA-0001", "EXA-0002", "EXA-0003"),
                TypeSalle.SALLE_SCANNER, List.of("EXA-0004", "EXA-0005", "EXA-0004"),
                TypeSalle.SALLE_RADIOGRAPHIE, List.of("EXA-0008", "EXA-0009", "EXA-0008"),
                TypeSalle.SALLE_ECHOGRAPHIE, List.of("EXA-0006", "EXA-0007"));
        List<Patient> tirables = patients.subList(1, patients.size());
        LocalTime maintenant = LocalTime.now();
        for (LocalDate d = aujourdHui.minusWeeks(8); !d.isAfter(aujourdHui.plusWeeks(2)); d = d.plusDays(1)) {
            if (!ParametresCentre.estJourOuvre(d.getDayOfWeek())) continue;
            boolean futur = d.isAfter(aujourdHui);
            for (SalleRadiologie salle : s.salles.findAll()) {
                if (salle.getId().equals("SAL-0005")) continue;
                int nb = futur ? 2 + rnd.nextInt(3) : 2 + rnd.nextInt(4);
                LocalTime t = LocalTime.of(8, 30).plusMinutes(15L * rnd.nextInt(3));
                for (int i = 0; i < nb; i++) {
                    List<String> exs = examensParSalle.get(salle.getTypeSalle());
                    ExamenMedical ex = s.examens.findById(exs.get(rnd.nextInt(exs.size()))).orElseThrow();
                    if (t.plusMinutes(ex.getDureeEstimeeMinutes()).isAfter(ParametresCentre.HEURE_FERMETURE)) break;
                    RendezVous r = null;
                    for (int essai = 0; essai < 8 && r == null; essai++) {
                        Patient p = tirables.get(rnd.nextInt(tirables.size()));
                        RendezVous c = new RendezVous(null, d, t, ex.getDureeEstimeeMinutes(), p.getId(),
                                choisir(s.medecins.findAll()).getId(), manipPour(ex.getTypeEquipementRequis()), salle.getId(), ex.getId());
                        c.setDateCreation(d.minusDays(1 + rnd.nextInt(21)).atTime(9 + rnd.nextInt(8), rnd.nextInt(60)));
                        c.setIdRdv(s.rendezVous.nextId());
                        if (reg.moteurConflits.verifier(c).isEmpty()) r = c;
                    }
                    if (r == null) { t = t.plusMinutes(15); continue; }
                    boolean passe = d.isBefore(aujourdHui) || (d.equals(aujourdHui) && r.getHeureFin().isBefore(maintenant));
                    if (passe) insererEtRealiser(r, aujourdHui);
                    else inserer(r);
                    t = r.getHeureFin().plusMinutes(5L * rnd.nextInt(7));
                }
            }
        }

        reg.facturation.actualiserStatuts(aujourdHui);

        // ------------------------------------------------------------ Comptes rendus
        String[] normaux = {
                "Examen réalisé dans de bonnes conditions techniques. Absence d'anomalie décelable. Structures d'aspect normal.",
                "Pas de lésion focale identifiée. Aspect radiologique dans les limites de la normale pour l'âge.",
                "Absence d'épanchement, de masse ou de processus expansif. Examen strictement normal."};
        String[] anomalies = {
                "Nodule de 8 mm du lobe supérieur droit, à contrôler par un scanner dans 3 mois.",
                "Lésion méniscale interne de grade 2 sans rupture franche. Avis orthopédique conseillé.",
                "Hypersignal punctiforme de la substance blanche, aspecifique. Corrélation clinique recommandée."};
        List<RendezVous> realises = new ArrayList<>(s.rendezVous.findWhere(r -> r.getStatut() == StatutRdv.REALISE));
        realises.sort(Comparator.comparing(RendezVous::getDebut));
        int seq = 3000;
        for (RendezVous r : realises) {
            if (!r.getDate().isBefore(aujourdHui.minusDays(1)) || rnd.nextDouble() < 0.1) continue;
            boolean anomalie = rnd.nextDouble() < 0.2;
            CompteRenduMedical cr = new CompteRenduMedical(null, r.getIdRdv(), r.getDate(), "ORD-" + r.getDate().getYear() + "-" + (seq++),
                    anomalie ? anomalies[rnd.nextInt(anomalies.length)] : normaux[rnd.nextInt(normaux.length)],
                    anomalie ? CodeResultat.ANOMALIE : CodeResultat.NORMAL);
            reg.comptesRendus.creer(cr);
        }
        // un compte-rendu urgent sur un examen d'hier (ou du dernier jour ouvré)
        RendezVous urgent = realises.stream().filter(r -> r.getDate().isBefore(aujourdHui) && reg.comptesRendus.deRdv(r.getIdRdv()).isEmpty())
                .reduce((a, b) -> b).orElse(null);
        if (urgent != null) {
            reg.comptesRendus.creer(new CompteRenduMedical(null, urgent.getIdRdv(), urgent.getDate(), "ORD-" + urgent.getDate().getYear() + "-" + (seq++),
                    "Suspicion d'embolie pulmonaire segmentaire droite. Prise en charge urgente et avis spécialisé immédiat.", CodeResultat.URGENT));
        }

        // ------------------------------------------------------------ Télé-expertise
        List<CompteRenduMedical> crAnomalie = s.comptesRendus.findWhere(c -> c.getCodeResultat() == CodeResultat.ANOMALIE);
        if (crAnomalie.size() >= 2) {
            RendezVous r1 = s.rendezVous.findById(crAnomalie.get(0).getIdRdv()).orElseThrow();
            DemandeTeleExpertise d1 = new DemandeTeleExpertise(null, r1.getIdRdv(), r1.getIdMedecin(), "EXP-CHU-MONGI-SLIM-07",
                    "Pr. Zouari (CHU Mongi Slim)", r1.getFin().plusHours(3), r1.getFin().plusDays(1).withHour(14).withMinute(0), 150);
            reg.teleExpertise.creer(d1);
            reg.teleExpertise.repondre(d1.getId(), "Je confirme l'interprétation initiale. Contrôle évolutif à 3 mois recommandé, pas d'indication de biopsie.",
                    r1.getFin().plusDays(1).withHour(15).withMinute(0));
            RendezVous r2 = s.rendezVous.findById(crAnomalie.get(crAnomalie.size() - 1).getIdRdv()).orElseThrow();
            reg.teleExpertise.creer(new DemandeTeleExpertise(null, r2.getIdRdv(), r2.getIdMedecin(), "EXP-INST-KASSAB-12",
                    "Dr Belhadj (Institut Kassab)", r2.getFin().plusHours(2), aujourdHui.plusDays(2).atTime(11, 0), 120));
        }
        if (urgent != null) {
            reg.teleExpertise.creer(new DemandeTeleExpertise(null, urgent.getIdRdv(), urgent.getIdMedecin(), "EXP-ABDERRAHMEN-MAMI-03",
                    "Pr. Kammoun (Hôpital A. Mami)", urgent.getFin().plusHours(1), urgent.getFin().plusHours(4), 200));
        }

        // ------------------------------------------------------------ Maintenance & état final des équipements
        reg.equipements.enregistrerMaintenance("EQP-0001", aujourdHui.minusMonths(2).withDayOfMonth(5), true,
                "Maintenance préventive semestrielle, recalibrage des bobines", "BI-SIEMENS-2026/0412", 3200);
        reg.equipements.enregistrerMaintenance("EQP-0002", aujourdHui.minusMonths(1).withDayOfMonth(12), false,
                "Remplacement du tube RX (pièce de rechange)", "FAC-GEHC-2026-7781", 18500);
        reg.equipements.enregistrerMaintenance("EQP-0003", aujourdHui.withDayOfMonth(3), true,
                "Contrôle qualité annuel et calibration détecteur", "BI-PHILIPS-2026/1190", 1450);
        etatFinal("EQP-0001", 180, StatutEquipement.EN_SERVICE);
        etatFinal("EQP-0002", 38, StatutEquipement.EN_SERVICE);
        etatFinal("EQP-0003", 610, StatutEquipement.EN_SERVICE);
        etatFinal("EQP-0004", 870, StatutEquipement.EN_SERVICE);
        etatFinal("EQP-0005", 655, StatutEquipement.EN_PANNE);

        // ------------------------------------------------------------ État final des stocks + commandes fournisseurs
        stockFinal("CSM-0001", 24);
        stockFinal("CSM-0002", 7);
        stockFinal("CSM-0003", 46);
        stockFinal("CSM-0004", 14);
        stockFinal("CSM-0005", 180);
        CommandeFournisseur c1 = new CommandeFournisseur(null, "CSM-0001", "MedImaging Tunisie", 40, aujourdHui.minusMonths(1).withDayOfMonth(8),
                aujourdHui.minusMonths(1).withDayOfMonth(12), 0, "FF-MEDIMAG-2026-0311");
        reg.stock.passerCommande(c1);
        CommandeFournisseur c1b = s.commandes.findById(c1.getId()).orElseThrow();
        c1b.setStatutLivraison(StatutLivraison.LIVREE);
        s.commandes.update(c1b);
        reg.stock.passerCommande(new CommandeFournisseur(null, "CSM-0002", "Pharmadis SA", 30, aujourdHui.minusDays(2),
                aujourdHui.plusDays(3), 0, "FF-PHARMADIS-2026-1187"));
        reg.stock.passerCommande(new CommandeFournisseur(null, "CSM-0003", "Echo Supplies", 60, aujourdHui.withDayOfMonth(1),
                aujourdHui.withDayOfMonth(1).plusDays(4), 0, "FF-ECHOSUP-2026-0094"));

        // ------------------------------------------------------------ Heures de garde
        YearMonth mois = YearMonth.from(aujourdHui);
        for (ManipulateurRadio m : s.manipulateurs.findAll()) {
            reg.personnel.saisirHeuresGarde(m.getId(), mois.minusMonths(1), 12 + rnd.nextInt(5) * 4);
            reg.personnel.saisirHeuresGarde(m.getId(), mois, 4 + rnd.nextInt(4) * 4);
        }
        ManipulateurRadio skander = s.manipulateurs.findById("MAN-0005").orElseThrow();
        skander.setStatutDispo(StatutDispo.EN_CONGE);
        s.manipulateurs.update(skander);

        // ------------------------------------------------------------ Tentatives conflictuelles (journal réel du moteur)
        LocalDate lundi = aujourdHui.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
        tenter(new RendezVous(null, lundi, LocalTime.of(18, 0), 30, "11283756", "MED-0001", "MAN-0002", "SAL-0001", "EXA-0001"));   // habilitation
        tenter(new RendezVous(null, lundi, LocalTime.of(18, 0), 45, hedi.getId(), "MED-0003", "MAN-0001", "SAL-0001", "EXA-0002"));  // solde débiteur
        tenter(new RendezVous(null, lundi, LocalTime.of(17, 0), 30, "10475665", "MED-0001", "MAN-0004", "SAL-0001", "EXA-0001"));   // pacemaker
        tenter(new RendezVous(null, lundi, LocalTime.of(9, 0), 20, "10566574", "MED-0003", "MAN-0003", "SAL-0002", "EXA-0005"));    // allergie iode + chevauchement
        tenter(new RendezVous(null, lundi, LocalTime.of(10, 0), 30, "11011029", "MED-0001", "MAN-0001", "SAL-0005", "EXA-0006"));   // équipement en panne
    }

    // ================================================================== utilitaires
    private static MedecinRadiologue med(String id, String cin, String nom, String prenom, Specialite sp, double pct,
                                         Set<DayOfWeek> jours, int hd, int hf, String tel) {
        MedecinRadiologue m = new MedecinRadiologue(id, cin, nom, prenom, sp, pct, new DisponibiliteHebdo(jours, LocalTime.of(hd, 0), LocalTime.of(hf, 0)));
        m.setTelephone(tel);
        m.setEmail(prenom.toLowerCase() + "." + nom.toLowerCase().replace(" ", "") + "@radiocare.tn");
        return m;
    }

    private static ManipulateurRadio man(String id, String cin, String nom, String prenom, Set<Habilitation> h, double tarif, StatutDispo st) {
        ManipulateurRadio m = new ManipulateurRadio(id, cin, nom, prenom, h, tarif, st);
        m.setTelephone("5" + cin.substring(1, 3) + " " + cin.substring(3, 6) + " " + cin.substring(5, 8));
        return m;
    }

    private static Patient pat(String cin, String nom, String prenom, int a, int mo, int j, String tel, String mail, String assur, double taux) {
        return new Patient(cin, nom, prenom, LocalDate.of(a, mo, j), tel, mail, assur, taux);
    }

    private <T> T choisir(List<T> l) { return l.get(rnd.nextInt(l.size())); }

    private String manipPour(TypeEquipement t) {
        List<ManipulateurRadio> ok = reg.store.manipulateurs.findWhere(m -> m.possedeHabilitation(t.getHabilitationRequise()));
        return choisir(ok).getId();
    }

    private static LocalDate ouvre(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.minusDays(1) : d;
    }

    /** Insère un RDV seulement si le moteur ne détecte aucun conflit (cohérence garantie). */
    private boolean inserer(RendezVous r) {
        r.setIdRdv(reg.store.rendezVous.nextId());
        if (r.getDateCreation() == null) r.setDateCreation(r.getDate().minusDays(7).atTime(10, 0));
        if (!reg.moteurConflits.verifier(r).isEmpty()) return false;
        reg.store.rendezVous.create(r);
        return true;
    }

    private void insererEtRealiser(RendezVous r, LocalDate aujourdHui) {
        if (!inserer(r)) return;
        if (rnd.nextDouble() < 0.07) { reg.rendezVous.annuler(r.getIdRdv()); return; }
        LocalTime arrivee = r.getHeureDebut().minusMinutes(rnd.nextInt(20));
        reg.rendezVous.demarrer(r.getIdRdv(), arrivee, r.getHeureDebut().plusMinutes(rnd.nextInt(25)));
        FactureMedicale f = reg.rendezVous.realiser(r.getIdRdv());
        payer(f, aujourdHui);
    }

    /** Règlement réaliste : 85 % payées, 11 % partiellement, 4 % impayées (patient débiteur exclu). */
    private void payer(FactureMedicale f, LocalDate aujourdHui) {
        if (f.getIdPatient().equals("10293847") || f.getPartPatient() <= 0) return;
        double tirage = rnd.nextDouble();
        LocalDate dp = f.getDateEmission().plusDays(rnd.nextInt(12));
        if (dp.isAfter(aujourdHui)) dp = aujourdHui;
        ModePaiement mode = ModePaiement.values()[rnd.nextInt(ModePaiement.values().length)];
        String preuve = mode.getPrefixePreuve() + "-" + (100000 + rnd.nextInt(900000));
        if (tirage < 0.85) reg.facturation.enregistrerPaiement(f.getNumFacture(), dp, f.getResteAPayer(), mode, preuve);
        else if (tirage < 0.96) reg.facturation.enregistrerPaiement(f.getNumFacture(), dp, Validateur.arrondi(f.getResteAPayer() / 2), mode, preuve);
    }

    private void tenter(RendezVous r) {
        try { reg.rendezVous.planifier(r); } catch (ConflitException | ValidationException ignored) { /* journalisé par le moteur */ }
    }

    private void etatFinal(String id, double restantes, StatutEquipement statut) {
        EquipementImagerie e = reg.store.equipements.findById(id).orElseThrow();
        e.setHeuresRestantesMaintenance(restantes);
        e.setStatut(statut);
        reg.store.equipements.update(e);
    }

    private void stockFinal(String id, int qte) {
        ConsommableMedical c = reg.store.consommables.findById(id).orElseThrow();
        c.setQuantiteEnStock(qte);
        reg.store.consommables.update(c);
    }
}
