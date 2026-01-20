package com.matchi.dto;

import java.util.List;

/**
 * DTO de réponse complète contenant tous les horaires occupés
 */
public record DisponibiliteResponseDTO(
        List<HoraireOccupeDTO> horairesOccupes
) {}
