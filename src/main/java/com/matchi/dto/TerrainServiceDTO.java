package com.matchi.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record TerrainServiceDTO(
        Long id,
        String nom,
        String adresse,
        Long proprietaireId,
        LocalTime heureOuverture,
        LocalTime heureFermeture,
        LocalDateTime createdAt
) {}
