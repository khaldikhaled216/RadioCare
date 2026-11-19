package com.radiocare.dao;

import com.radiocare.model.Identifiable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Contrat générique CRUD de la couche d'accès aux données. */
public interface Dao<T extends Identifiable> {
    List<T> findAll();
    Optional<T> findById(String id);
    List<T> findWhere(Predicate<T> filtre);
    T create(T entite);
    T update(T entite);
    boolean delete(String id);
    boolean exists(String id);
    int count();
    /** Génère le prochain identifiant libre (ex. RDV-0042). */
    String nextId();
}
