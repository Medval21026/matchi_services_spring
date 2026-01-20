package com.matchi.dto;

import com.matchi.model.TypeReservation;
import java.time.LocalDate;
import java.time.LocalTime;

public record IndisponibleHoraireDTO(
        Long id,
        Long terrainId,
        LocalDate date,
        LocalTime heureDebut,
        LocalTime heureFin,
        TypeReservation typeReservation,
        Long sourceId,
        String description
) {}
