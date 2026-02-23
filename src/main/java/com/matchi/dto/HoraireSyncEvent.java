package com.matchi.dto;

import com.matchi.model.TypeReservation;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Événement de synchronisation pour les horaires indisponibles
 * Publié sur le broker Kafka lors des opérations CRUD
 */
public record HoraireSyncEvent(
        UUID uuid,
        String action, // "created", "updated", "deleted"
        Long terrainId,
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        TypeReservation typeReservation,
        Long sourceId,
        String description,
        Integer proprietaireTelephone // Numéro de téléphone du propriétaire du terrain
) {}
