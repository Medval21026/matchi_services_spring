package com.matchi.dto;



import com.matchi.model.JourSemaine;
import java.math.BigDecimal;
import java.time.LocalTime;

public record TarifTerrainDTO(
        Long id,
        Long terrainId,
        JourSemaine jourSemaine,
        LocalTime heureDebut,
        LocalTime heureFin,
        BigDecimal prixParHeure
) {}
