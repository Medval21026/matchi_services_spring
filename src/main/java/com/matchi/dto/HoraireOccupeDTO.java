package com.matchi.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO représentant un horaire occupé (provenant d'AbonnementHoraire ou ReservationPonctuelle)
 */
public record HoraireOccupeDTO(
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        Integer telephoneProprietaire,
        Long terrainId
) {}
