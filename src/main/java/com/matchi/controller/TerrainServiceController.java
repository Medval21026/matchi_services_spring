package com.matchi.controller;

import com.matchi.dto.TerrainServiceDTO;
import com.matchi.service.TerrainServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terrains")
@RequiredArgsConstructor
public class TerrainServiceController {

    private final TerrainServiceService terrainServiceService;

    // 🔹 Récupérer tous les terrains
    @GetMapping
    public List<TerrainServiceDTO> getAllTerrains() {
        return terrainServiceService.getAllTerrains();
    }

    // 🔹 Récupérer un terrain par ID
    @GetMapping("/{id}")
    public TerrainServiceDTO trouverTerrainParId(@PathVariable Long id) {
        return terrainServiceService.trouverTerrainParId(id);
    }

    // 🔹 Ajouter un nouveau terrain
    @PostMapping
    public TerrainServiceDTO ajouterTerrain(@RequestBody TerrainServiceDTO terrainDTO) {
        return terrainServiceService.ajouterTerrain(terrainDTO);
    }

    // 🔹 Mettre à jour un terrain
    @PutMapping("/{id}")
    public TerrainServiceDTO mettreAJourTerrain(
            @PathVariable Long id,
            @RequestBody TerrainServiceDTO terrainDTO) {
        return terrainServiceService.mettreAJourTerrain(id, terrainDTO);
    }

    // 🔹 Supprimer un terrain
    @DeleteMapping("/{id}")
    public boolean supprimerTerrain(@PathVariable Long id) {
        return terrainServiceService.supprimerTerrain(id);
    }

    // 🔹 Récupérer tous les horaires d'un terrain (de l'heure d'ouverture à l'heure de fermeture)
    @GetMapping("/{id}/horaires")
    public List<java.time.LocalTime> getHorairesTerrain(@PathVariable Long id) {
        return terrainServiceService.getHorairesTerrain(id);
    }
}
