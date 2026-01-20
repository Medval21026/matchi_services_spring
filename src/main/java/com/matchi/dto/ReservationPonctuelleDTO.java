package com.matchi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationPonctuelleDTO(
        Long id,
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        BigDecimal prix,
        Integer clientTelephone,
        Long terrainId
) {}
