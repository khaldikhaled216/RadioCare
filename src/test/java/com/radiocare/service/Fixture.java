package com.radiocare.service;

import com.radiocare.model.*;
import com.radiocare.model.enums.*;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;

/** Jeu de données minimal et maîtrisé pour les tests unitaires. */
public final class Fixture {
    public static final String PATIENT = "11111111";
    public static final String PATIENT2 = "22222222";
    public static final String MED = "MED-0001";
    public static final String MAN_IRM = "MAN-0001";
    public static final String MAN_RX = "MAN-0002";
    public static final String SALLE_IRM = "SAL-0001";
    public static final String SALLE_RX = "SAL-0002";
    public static final String EQ_IRM = "EQP-0001";
    public static final String EQ_RX = "EQP-0002";
    public static final String EXA_IRM = "EXA-0001";
    public static final String EXA_IRM_INJ = "EXA-0002";
    public static final String EXA_RX = "EXA-0003";
    public static final String GADO = "CSM-0001";

    private Fixture() { }

    public static ServiceRegistry creer(Path dir) {
        ServiceRegistry reg = new ServiceRegistry(new com.radiocare.dao.DataStore(dir));
        var s = reg.store;
        s.consommables.create(new ConsommableMedical(GADO, "Gadolinium", CategorieConsommable.CONTRASTE_GADOLINIUM, 5, 2, 45, LocalDate.now().plusYears(1)));
        s.examens.create(new ExamenMedical(EXA_IRM, "IRM-CER", "IRM Cérébrale", TypeEquipement.IRM, 30, 280, null, 0));
        s.examens.create(new ExamenMedical(EXA_IRM_INJ, "IRM-CER-INJ", "IRM injectée", TypeEquipement.IRM, 45, 380, GADO, 1));
        s.examens.create(new ExamenMedical(EXA_RX, "RX-PMN", "RX Poumon", TypeEquipement.TABLE_RX, 10, 35, null, 0));
        s.equipements.create(new EquipementImagerie(EQ_IRM, TypeEquipement.IRM, "Siemens", "Vida", 1000, 100, 500));
        s.equipements.create(new EquipementImagerie(EQ_RX, TypeEquipement.TABLE_RX, "Philips", "C90", 1000, 400, 800));
        s.salles.create(new SalleRadiologie(SALLE_IRM, "Salle IRM", 0, TypeSalle.SALLE_IRM, EQ_IRM, "CAGE-FARADAY", 0));
        s.salles.create(new SalleRadiologie(SALLE_RX, "Salle Radio", 1, TypeSalle.SALLE_RADIOGRAPHIE, EQ_RX, "NF-C15-160", 2.0));
        s.medecins.create(new MedecinRadiologue(MED, "01234567", "Trabelsi", "Sana", Specialite.NEURORADIOLOGIE, 30,
                new DisponibiliteHebdo(EnumSet.allOf(DayOfWeek.class), LocalTime.of(8, 0), LocalTime.of(19, 0))));
        s.manipulateurs.create(new ManipulateurRadio(MAN_IRM, "02345678", "Ferchichi", "Amine", EnumSet.of(Habilitation.IRM, Habilitation.SCANNER), 20, StatutDispo.DISPONIBLE));
        s.manipulateurs.create(new ManipulateurRadio(MAN_RX, "03456789", "Ben Ammar", "Nour", EnumSet.of(Habilitation.RAYONS_X), 15, StatutDispo.DISPONIBLE));
        s.patients.create(new Patient(PATIENT, "Khemiri", "Amel", LocalDate.of(1985, 7, 3), "22111222", null, "CNAM-1", 0.7));
        s.patients.create(new Patient(PATIENT2, "Hammami", "Salma", LocalDate.of(1995, 5, 30), null, null, null, 0));
        return reg;
    }

    /** Prochain lundi (toujours un jour ouvré dans le futur). */
    public static LocalDate lundiProchain() { return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)); }

    /** Dernier jour ouvré passé (hier ou avant), pour les examens à réaliser. */
    public static LocalDate jourOuvrePasse() {
        LocalDate d = LocalDate.now().minusDays(1);
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.minusDays(1) : d;
    }

    public static RendezVous rdv(LocalDate date, String heure, String patient, String manip, String salle, String examen, int duree) {
        return new RendezVous(null, date, LocalTime.parse(heure), duree, patient, MED, manip, salle, examen);
    }

    /** Insère un RDV passé directement (contourne l'interdiction de planifier dans le passé) puis le retourne. */
    public static RendezVous rdvPasse(ServiceRegistry reg, String heure, String patient, String manip, String salle, String examen, int duree) {
        RendezVous r = rdv(jourOuvrePasse(), heure, patient, manip, salle, examen, duree);
        r.setIdRdv(reg.store.rendezVous.nextId());
        return reg.store.rendezVous.create(r);
    }
}
