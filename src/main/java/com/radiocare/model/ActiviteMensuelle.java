package com.radiocare.model;

/**
 * Objet-valeur décrivant l'activité d'un membre du personnel sur un mois,
 * servant de base au calcul polymorphe de la rémunération.
 */
public record ActiviteMensuelle(String mois, int nombreActes, double montantActesHT, double heuresActes, double heuresGarde) {
}
