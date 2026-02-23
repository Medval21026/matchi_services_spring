package com.matchi.controller;

import com.matchi.dto.DisponibiliteResponseDTO;
import com.matchi.service.DisponibiliteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/disponibilites")
@RequiredArgsConstructor
@Tag(name = "Disponibilités")
public class DisponibiliteController {

    private final DisponibiliteService disponibiliteService;

    /**
     * Retourne tous les horaires indisponibles pour un terrain donné
     * (provenant de la table indisponibles_horaires) avec :
     * - date d'indisponibilité
     * - heure de début
     * - heure de fin
     * - numéro de téléphone du propriétaire du terrain
     */
    @GetMapping("/horaires-indisponibles/terrain/{terrainId}")
    public ResponseEntity<DisponibiliteResponseDTO> getHorairesIndisponiblesParTerrain(
            @PathVariable Long terrainId
    ) {
        DisponibiliteResponseDTO response = disponibiliteService.getHorairesIndisponiblesParTerrain(terrainId);
        return ResponseEntity.ok(response);
    }
}
