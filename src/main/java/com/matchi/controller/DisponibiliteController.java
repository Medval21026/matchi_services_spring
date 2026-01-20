package com.matchi.controller;

import com.matchi.dto.DisponibiliteResponseDTO;
import com.matchi.service.DisponibiliteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disponibilites")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DisponibiliteController {

    private final DisponibiliteService disponibiliteService;

    /**
     * Récupère TOUS les horaires occupés de TOUS les terrains
     * 
     * GET /api/disponibilites/horaires-occupes
     */
    @GetMapping("/horaires-occupes")
    public ResponseEntity<DisponibiliteResponseDTO> getTousLesHorairesOccupes() {
        DisponibiliteResponseDTO response = disponibiliteService.getTousLesHorairesOccupes();
        return ResponseEntity.ok(response);
    }
}
