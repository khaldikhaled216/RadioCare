package com.radiocare.dao;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radiocare.model.Identifiable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implémentation générique d'un DAO persistant dans un fichier JSON.
 * <ul>
 *   <li>Chargement automatique à la construction (fichier absent → collection vide).</li>
 *   <li>Fichier corrompu → sauvegardé sous {@code *.corrompu-<horodatage>} et collection vide + avertissement.</li>
 *   <li>Sauvegarde automatique et atomique (fichier temporaire + déplacement) après chaque modification.</li>
 * </ul>
 */
public class JsonDao<T extends Identifiable> implements Dao<T> {
    private final Path fichier;
    private final Class<T> type;
    private final String prefixeId;
    private final ObjectMapper mapper;
    private final Consumer<String> avertisseur;
    private final Map<String, T> cache = new LinkedHashMap<>();

    public JsonDao(Path fichier, Class<T> type, String prefixeId, ObjectMapper mapper, Consumer<String> avertisseur) {
        this.fichier = fichier;
        this.type = type;
        this.prefixeId = prefixeId;
        this.mapper = mapper;
        this.avertisseur = avertisseur == null ? s -> { } : avertisseur;
        charger();
    }

    private void charger() {
        cache.clear();
        if (!Files.exists(fichier)) return;
        try {
            if (Files.size(fichier) == 0) return;
            JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
            List<T> elements = mapper.readValue(fichier.toFile(), listType);
            for (T e : elements) {
                if (e == null || e.getId() == null || e.getId().isBlank()) {
                    avertisseur.accept(fichier.getFileName() + " : élément sans identifiant ignoré.");
                    continue;
                }
                cache.put(e.getId(), e);
            }
        } catch (IOException | RuntimeException ex) {
            String horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path sauvegarde = fichier.resolveSibling(fichier.getFileName() + ".corrompu-" + horodatage);
            try {
                Files.move(fichier, sauvegarde, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // si le déplacement échoue, le fichier sera écrasé à la prochaine sauvegarde
            }
            cache.clear();
            avertisseur.accept("Fichier " + fichier.getFileName() + " illisible (" + ex.getClass().getSimpleName()
                    + "). Copie conservée sous " + sauvegarde.getFileName() + ".");
        }
    }

    /** Écrit la collection complète sur disque de manière atomique. */
    protected synchronized void sauvegarder() {
        try {
            Files.createDirectories(fichier.toAbsolutePath().getParent());
            Path tmp = fichier.resolveSibling(fichier.getFileName() + ".tmp");
            mapper.writeValue(tmp.toFile(), new ArrayList<>(cache.values()));
            try {
                Files.move(tmp, fichier, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, fichier, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'écrire " + fichier, e);
        }
    }

    @Override public List<T> findAll() { return new ArrayList<>(cache.values()); }

    @Override public Optional<T> findById(String id) { return id == null ? Optional.empty() : Optional.ofNullable(cache.get(id)); }

    @Override public List<T> findWhere(Predicate<T> filtre) { return cache.values().stream().filter(filtre).toList(); }

    @Override
    public T create(T entite) {
        Objects.requireNonNull(entite, "entité nulle");
        if (entite.getId() == null || entite.getId().isBlank()) throw new IllegalArgumentException("Identifiant obligatoire");
        if (cache.containsKey(entite.getId())) throw new IllegalArgumentException("L'identifiant " + entite.getId() + " existe déjà");
        cache.put(entite.getId(), entite);
        sauvegarder();
        return entite;
    }

    @Override
    public T update(T entite) {
        Objects.requireNonNull(entite, "entité nulle");
        if (!cache.containsKey(entite.getId())) throw new IllegalArgumentException("Identifiant inconnu : " + entite.getId());
        cache.put(entite.getId(), entite);
        sauvegarder();
        return entite;
    }

    @Override
    public boolean delete(String id) {
        boolean supprime = cache.remove(id) != null;
        if (supprime) sauvegarder();
        return supprime;
    }

    @Override public boolean exists(String id) { return id != null && cache.containsKey(id); }

    @Override public int count() { return cache.size(); }

    @Override
    public String nextId() {
        Pattern p = Pattern.compile("^" + Pattern.quote(prefixeId) + "-(\\d+)$");
        int max = 0;
        for (String id : cache.keySet()) {
            Matcher m = p.matcher(id);
            if (m.matches()) max = Math.max(max, Integer.parseInt(m.group(1)));
        }
        return String.format("%s-%04d", prefixeId, max + 1);
    }

    /** Enregistre une collection complète d'un coup (utilisé pour l'import des données de démonstration). */
    public void createAll(List<T> entites) {
        for (T e : entites) {
            if (cache.containsKey(e.getId())) throw new IllegalArgumentException("Identifiant en double : " + e.getId());
            cache.put(e.getId(), e);
        }
        sauvegarder();
    }

    public Path getFichier() { return fichier; }
}
