package com.matchi.controller;

import com.matchi.dto.IndisponibleHoraireDTO;
import com.matchi.service.IndisponibleHoraireService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/indisponibles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IndisponibleHoraireController {

    private final IndisponibleHoraireService indisponibleHoraireService;

    /**
     * Récupérer tous les horaires indisponibles
     */
    @GetMapping
    public ResponseEntity<List<IndisponibleHoraireDTO>> getAllIndisponibles() {
        List<IndisponibleHoraireDTO> horaires = indisponibleHoraireService.getAllIndisponibles();
        return ResponseEntity.ok(horaires);
    }

    /**
     * Récupérer les horaires indisponibles d'un terrain
     */
    @GetMapping("/terrain/{terrainId}")
    public ResponseEntity<List<IndisponibleHoraireDTO>> getIndisponiblesByTerrain(@PathVariable Long terrainId) {
        List<IndisponibleHoraireDTO> horaires = indisponibleHoraireService.getIndisponiblesByTerrain(terrainId);
        return ResponseEntity.ok(horaires);
    }

    /**
     * Récupérer les horaires indisponibles d'un terrain pour une date
     */
    @GetMapping("/terrain/{terrainId}/date/{date}")
    public ResponseEntity<List<IndisponibleHoraireDTO>> getIndisponiblesByTerrainAndDate(
            @PathVariable Long terrainId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<IndisponibleHoraireDTO> horaires = indisponibleHoraireService.getIndisponiblesByTerrainAndDate(terrainId, date);
        return ResponseEntity.ok(horaires);
    }

    /**
     * Récupérer les horaires indisponibles d'un terrain sur une période
     */
    @GetMapping("/terrain/{terrainId}/periode")
    public ResponseEntity<List<IndisponibleHoraireDTO>> getIndisponiblesByTerrainAndPeriode(
            @PathVariable Long terrainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        List<IndisponibleHoraireDTO> horaires = indisponibleHoraireService.getIndisponiblesByTerrainAndPeriode(
                terrainId, dateDebut, dateFin);
        return ResponseEntity.ok(horaires);
    }

    /**
     * Synchroniser les horaires indisponibles d'un terrain
     */
    @PostMapping("/terrain/{terrainId}/synchroniser")
    public ResponseEntity<String> synchroniserHoraires(@PathVariable Long terrainId) {
        indisponibleHoraireService.synchroniserHorairesIndisponibles(terrainId);
        return ResponseEntity.ok("Synchronisation terminée pour le terrain " + terrainId);
    }

    /**
     * Synchroniser les horaires indisponibles de TOUS les terrains
     */
    @PostMapping("/synchroniser-tous")
    public ResponseEntity<String> synchroniserTous() {
        indisponibleHoraireService.synchroniserTousLesTerrains();
        return ResponseEntity.ok("Synchronisation terminée pour tous les terrains");
    }

    /**
     * Ajouter un horaire indisponible manuellement
     */
    @PostMapping
    public ResponseEntity<IndisponibleHoraireDTO> ajouterHoraire(@RequestBody IndisponibleHoraireDTO dto) {
        IndisponibleHoraireDTO created = indisponibleHoraireService.ajouterHoraireIndisponible(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Mettre à jour un horaire indisponible
     */
    @PutMapping("/{id}")
    public ResponseEntity<IndisponibleHoraireDTO> mettreAJourHoraire(
            @PathVariable Long id,
            @RequestBody IndisponibleHoraireDTO dto) {
        IndisponibleHoraireDTO updated = indisponibleHoraireService.mettreAJourHoraireIndisponible(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprimer un horaire indisponible
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerHoraire(@PathVariable Long id) {
        indisponibleHoraireService.supprimerHoraireIndisponible(id);
        return ResponseEntity.noContent().build();
    }
}
