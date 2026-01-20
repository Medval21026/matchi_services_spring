package com.matchi.service;

import com.matchi.model.TarifTerrain;
import com.matchi.model.TerrainService;
import com.matchi.repository.TarifTerrainRepository;
import com.matchi.repository.TerrainServiceRepository;
import com.matchi.dto.TarifTerrainDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TarifTerrainService {

    private final TarifTerrainRepository tarifTerrainRepository;
    private final TerrainServiceRepository terrainServiceRepository;

    // ======== MAPPERS ========
    private TarifTerrainDTO toDTO(TarifTerrain tarif) {
        return new TarifTerrainDTO(
                tarif.getId(),
                tarif.getTerrain() != null ? tarif.getTerrain().getId() : null,
                tarif.getJourSemaine(),
                tarif.getHeureDebut(),
                tarif.getHeureFin(),
                tarif.getPrixParHeure()
        );
    }

    private TarifTerrain toEntity(TarifTerrainDTO dto) {
        TarifTerrain tarif = new TarifTerrain();
        tarif.setId(dto.id());
        tarif.setJourSemaine(dto.jourSemaine());
        tarif.setHeureDebut(dto.heureDebut());
        tarif.setHeureFin(dto.heureFin());
        tarif.setPrixParHeure(dto.prixParHeure());

        if (dto.terrainId() != null) {
            TerrainService terrain = terrainServiceRepository.findById(dto.terrainId())
                    .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));
            tarif.setTerrain(terrain);
        }

        return tarif;
    }

    // ======== CRUD ========

    public List<TarifTerrainDTO> getAllTarifs() {
        return tarifTerrainRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TarifTerrainDTO getTarifById(Long id) {
        return tarifTerrainRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Tarif introuvable"));
    }

    public List<TarifTerrainDTO> getTarifsByTerrainId(Long terrainId) {
        return tarifTerrainRepository.findByTerrainId(terrainId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TarifTerrainDTO createTarif(TarifTerrainDTO dto) {
        TarifTerrain tarif = toEntity(dto);
        TarifTerrain saved = tarifTerrainRepository.save(tarif);
        return toDTO(saved);
    }

    public TarifTerrainDTO updateTarif(Long id, TarifTerrainDTO dto) {
        TarifTerrain existing = tarifTerrainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarif introuvable"));

        // Mise à jour partielle - uniquement les champs non-null
        if (dto.jourSemaine() != null) {
            existing.setJourSemaine(dto.jourSemaine());
        }
        if (dto.heureDebut() != null) {
            existing.setHeureDebut(dto.heureDebut());
        }
        if (dto.heureFin() != null) {
            existing.setHeureFin(dto.heureFin());
        }
        if (dto.prixParHeure() != null) {
            existing.setPrixParHeure(dto.prixParHeure());
        }

        if (dto.terrainId() != null) {
            TerrainService terrain = terrainServiceRepository.findById(dto.terrainId())
                    .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));
            existing.setTerrain(terrain);
        }

        TarifTerrain saved = tarifTerrainRepository.save(existing);
        return toDTO(saved);
    }

    public void deleteTarif(Long id) {
        if (!tarifTerrainRepository.existsById(id)) {
            throw new IllegalArgumentException("Tarif introuvable");
        }
        tarifTerrainRepository.deleteById(id);
    }
}
