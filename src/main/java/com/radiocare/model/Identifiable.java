package com.radiocare.model;

/** Toute entité persistée possède un identifiant unique (clé primaire dans son fichier JSON). */
public interface Identifiable {
    String getId();
}
