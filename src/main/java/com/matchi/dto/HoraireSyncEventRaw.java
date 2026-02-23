package com.matchi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.UUID;

/**
 * DTO brut pour recevoir les événements Kafka depuis Django
 * Gère les formats de dates/heures comme tableaux [année, mois, jour] ou ISO strings
 * Gère les formats camelCase et snake_case
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HoraireSyncEventRaw(
        UUID uuid,
        String action,
        @JsonAlias({"terrainId", "terrain_id"})
        Long terrainId,
        @JsonDeserialize(using = LocalDateArrayDeserializer.class)
        @JsonAlias({"date", "date_indisponibilite"})
        java.time.LocalDate date,
        @JsonDeserialize(using = LocalTimeArrayDeserializer.class)
        @JsonAlias({"heureDebut", "heure_debut"})
        java.time.LocalTime heureDebut,
        @JsonDeserialize(using = LocalTimeArrayDeserializer.class)
        @JsonAlias({"heureFin", "heure_fin"})
        java.time.LocalTime heureFin,
        String typeReservation, // String au lieu d'enum pour gérer les null
        Long sourceId,
        String description,
        Integer proprietaireTelephone, // Ancien champ (pour compatibilité)
        Integer numTel, // Nouveau champ depuis Django (numéro du client = numéro du propriétaire)
        String source, // Champ "source" de Django (ignoré mais accepté)
        Integer id_jour, // ID du jour depuis Django (pour référence)
        String joueur_numTel, // Numéro de téléphone du joueur (String depuis Django)
        Double prix // Prix de la réservation depuis Django
) {
    /**
     * Convertit ce DTO brut en HoraireSyncEvent
     * Normalise l'action (DELETE -> deleted) et gère les formats camelCase/snake_case
     */
    public HoraireSyncEvent toHoraireSyncEvent() {
        // Normaliser l'action (DELETE -> deleted, CREATE -> created, UPDATE -> updated)
        String normalizedAction = action;
        if (action != null) {
            String actionLower = action.toLowerCase();
            if ("delete".equals(actionLower) || "deleted".equals(actionLower)) {
                normalizedAction = "deleted";
            } else if ("create".equals(actionLower) || "created".equals(actionLower)) {
                normalizedAction = "created";
            } else if ("update".equals(actionLower) || "updated".equals(actionLower)) {
                normalizedAction = "updated";
            }
        }
        
        com.matchi.model.TypeReservation type = null;
        if (typeReservation != null && !typeReservation.trim().isEmpty()) {
            try {
                type = com.matchi.model.TypeReservation.valueOf(typeReservation.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Type inconnu, laisser null
            }
        }
        
        // Utiliser numTel si disponible, sinon proprietaireTelephone (pour compatibilité)
        Integer tel = numTel != null ? numTel : proprietaireTelephone;
        
        return new HoraireSyncEvent(
                uuid,
                normalizedAction,
                terrainId,
                date,
                heureDebut,
                heureFin,
                type,
                sourceId,
                description,
                tel
        );
    }
}
