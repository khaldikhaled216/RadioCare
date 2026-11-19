package com.radiocare.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** DispoPlanning d'un médecin : jours de présence et plage horaire (composition dans MedecinRadiologue). */
public class DisponibiliteHebdo {
    private Set<DayOfWeek> jours = new TreeSet<>();
    private LocalTime heureDebut = LocalTime.of(8, 0);
    private LocalTime heureFin = LocalTime.of(17, 0);

    public DisponibiliteHebdo() { }

    public DisponibiliteHebdo(Set<DayOfWeek> jours, LocalTime heureDebut, LocalTime heureFin) {
        this.jours = new TreeSet<>(jours);
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
    }

    public static DisponibiliteHebdo semaineComplete() {
        return new DisponibiliteHebdo(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), LocalTime.of(8, 0), LocalTime.of(18, 0));
    }

    /** Le créneau [debut, fin] du jour donné est-il couvert par la disponibilité ? */
    public boolean couvre(LocalDate date, LocalTime debut, LocalTime fin) {
        return jours.contains(date.getDayOfWeek()) && !debut.isBefore(heureDebut) && !fin.isAfter(heureFin);
    }

    public String resume() {
        String j = jours.stream().sorted().map(d -> d.getDisplayName(TextStyle.SHORT, Locale.FRENCH)).collect(Collectors.joining(", "));
        return j + " " + heureDebut + "-" + heureFin;
    }

    public Set<DayOfWeek> getJours() { return jours; }
    public void setJours(Set<DayOfWeek> jours) { this.jours = jours == null ? new TreeSet<>() : new TreeSet<>(jours); }
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }
    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    @Override public String toString() { return resume(); }
}
