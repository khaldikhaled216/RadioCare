package com.radiocare.model;

import com.radiocare.model.enums.Habilitation;
import com.radiocare.model.enums.StatutDispo;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Entité 3 : Manipulateur radio, salarié à l'heure (heures d'actes + heures de garde majorées). */
public class ManipulateurRadio extends PersonnelMedical {
    /** Majoration appliquée aux heures de garde. */
    public static final double MAJORATION_GARDE = 1.5;

    private String idManip;
    private Set<Habilitation> habilitations = new TreeSet<>();
    private double tarifHoraire;
    private StatutDispo statutDispo = StatutDispo.DISPONIBLE;
    /** Heures de garde saisies par mois (clé au format AAAA-MM). */
    private Map<String, Double> heuresGardeParMois = new HashMap<>();

    public ManipulateurRadio() { }

    public ManipulateurRadio(String idManip, String cin, String nom, String prenom, Set<Habilitation> habilitations,
                             double tarifHoraire, StatutDispo statutDispo) {
        super(cin, nom, prenom, null, null);
        this.idManip = idManip;
        this.habilitations = habilitations.isEmpty() ? new TreeSet<>() : new TreeSet<>(EnumSet.copyOf(habilitations));
        this.tarifHoraire = tarifHoraire;
        this.statutDispo = statutDispo;
    }

    @Override public String getId() { return idManip; }
    @Override public String getFonction() { return "Manipulateur radio"; }

    /** Salaire = tarif horaire × heures d'actes + tarif horaire × majoration × heures de garde. */
    @Override
    public double calculerRemuneration(ActiviteMensuelle activite) {
        return tarifHoraire * activite.heuresActes() + tarifHoraire * MAJORATION_GARDE * activite.heuresGarde();
    }

    public boolean possedeHabilitation(Habilitation h) { return habilitations.contains(h); }

    public double getHeuresGarde(String mois) { return heuresGardeParMois.getOrDefault(mois, 0.0); }

    public String getIdManip() { return idManip; }
    public void setIdManip(String idManip) { this.idManip = idManip; }
    public Set<Habilitation> getHabilitations() { return habilitations; }
    public void setHabilitations(Set<Habilitation> h) { this.habilitations = h == null ? new TreeSet<>() : new TreeSet<>(h); }
    public double getTarifHoraire() { return tarifHoraire; }
    public void setTarifHoraire(double tarifHoraire) { this.tarifHoraire = tarifHoraire; }
    public StatutDispo getStatutDispo() { return statutDispo; }
    public void setStatutDispo(StatutDispo statutDispo) { this.statutDispo = statutDispo; }
    public Map<String, Double> getHeuresGardeParMois() { return heuresGardeParMois; }
    public void setHeuresGardeParMois(Map<String, Double> m) { this.heuresGardeParMois = m == null ? new HashMap<>() : m; }
}
