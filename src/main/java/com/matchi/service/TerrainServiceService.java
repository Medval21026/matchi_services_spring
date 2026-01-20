package com.matchi.service;

import com.matchi.dto.TerrainServiceDTO;
import com.matchi.model.Proprietaire;
import com.matchi.model.TerrainService;
import com.matchi.repository.ProprietaireRepository;
import com.matchi.repository.TerrainServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TerrainServiceService {

    private final TerrainServiceRepository terrainRepository;
    private final ProprietaireRepository proprietaireRepository;

    // ======== MAPPERS ========
    private TerrainServiceDTO toDTO(TerrainService terrain) {
        return new TerrainServiceDTO(
                terrain.getId(),
                terrain.getNom(),
                terrain.getAdresse(),
                terrain.getProprietaire() != null ? terrain.getProprietaire().getId() : null,
                terrain.getHeureOuverture(),
                terrain.getHeureFermeture(),
                terrain.getCreatedAt()
        );
    }

    private TerrainService toEntity(TerrainServiceDTO dto) {
        TerrainService terrain = new TerrainService();
        terrain.setId(dto.id());
        terrain.setNom(dto.nom());
        terrain.setAdresse(dto.adresse());
        terrain.setHeureOuverture(dto.heureOuverture());
        terrain.setHeureFermeture(dto.heureFermeture());

        if (dto.proprietaireId() != null) {
            // ✅ VALIDATION : Vérifier que le propriétaire existe
            Proprietaire proprietaire = proprietaireRepository.findById(dto.proprietaireId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Propriétaire introuvable avec l'ID %d", dto.proprietaireId())
                    ));
            terrain.setProprietaire(proprietaire);
        }

        return terrain;
    }

    /**
     * ✅ Valide qu'un propriétaire n'a qu'un seul terrain
     * Un propriétaire ne peut pas avoir plusieurs terrains
     */
    private void validerUnSeulTerrainParProprietaire(Long proprietaireId, Long terrainIdExclu) {
        if (proprietaireId == null) {
            return; // Pas de validation si pas de propriétaire
        }
        
        List<TerrainService> terrainsExistants = terrainRepository.findByProprietaireId(proprietaireId);
        
        // Exclure le terrain en cours de modification (si modification)
        List<TerrainService> terrainsAutres = terrainsExistants.stream()
                .filter(t -> terrainIdExclu == null || !t.getId().equals(terrainIdExclu))
                .toList();
        
        // Si le propriétaire a déjà un autre terrain, lever une exception
        if (!terrainsAutres.isEmpty()) {
            TerrainService terrainExistant = terrainsAutres.get(0);
            throw new IllegalArgumentException(
                String.format("Le propriétaire avec l'ID %d a déjà un terrain (ID: %d, Nom: %s). " +
                             "Un propriétaire ne peut avoir qu'un seul terrain.",
                    proprietaireId, terrainExistant.getId(), terrainExistant.getNom())
            );
        }
    }

    // ======== SERVICES ========
    public List<TerrainServiceDTO> getAllTerrains() {
        return terrainRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public TerrainServiceDTO ajouterTerrain(TerrainServiceDTO dto) {
        // ✅ VALIDATION : Vérifier qu'un propriétaire n'a qu'un seul terrain
        if (dto.proprietaireId() != null) {
            validerUnSeulTerrainParProprietaire(dto.proprietaireId(), null);
        }
        
        TerrainService terrain = toEntity(dto);
        TerrainService saved = terrainRepository.save(terrain);
        return toDTO(saved);
    }

    public TerrainServiceDTO trouverTerrainParId(Long id) {
        return terrainRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    public boolean supprimerTerrain(Long id) {
        if (!terrainRepository.existsById(id)) {
            return false;
        }
        terrainRepository.deleteById(id);
        return true;
    }

    public TerrainServiceDTO mettreAJourTerrain(Long id, TerrainServiceDTO dto) {
        return terrainRepository.findById(id)
                .map(existant -> {
                    // Mise à jour partielle - uniquement les champs non-null
                    if (dto.nom() != null) {
                        existant.setNom(dto.nom());
                    }
                    if (dto.adresse() != null) {
                        existant.setAdresse(dto.adresse());
                    }
                    if (dto.heureOuverture() != null) {
                        existant.setHeureOuverture(dto.heureOuverture());
                    }
                    if (dto.heureFermeture() != null) {
                        existant.setHeureFermeture(dto.heureFermeture());
                    }

                    // ✅ VALIDATION : Vérifier que le propriétaire existe avant modification
                    if (dto.proprietaireId() != null) {
                        // Vérifier que le propriétaire existe
                        Proprietaire proprietaire = proprietaireRepository.findById(dto.proprietaireId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                    String.format("Propriétaire introuvable avec l'ID %d", dto.proprietaireId())
                                ));
                        
                        // ✅ VALIDATION : Vérifier que le propriétaire n'a pas déjà un autre terrain
                        // Exclure le terrain en cours de modification
                        Long ancienProprietaireId = existant.getProprietaire() != null ? 
                                                    existant.getProprietaire().getId() : null;
                        
                        // Si on change de propriétaire OU si le propriétaire change, vérifier
                        if (!dto.proprietaireId().equals(ancienProprietaireId)) {
                            validerUnSeulTerrainParProprietaire(dto.proprietaireId(), id);
                        }
                        
                        existant.setProprietaire(proprietaire);
                    }

                    return toDTO(terrainRepository.save(existant));
                })
                .orElse(null);
    }

    /**
     * Retourne tous les horaires d'un terrain de l'heure d'ouverture jusqu'à l'heure de fermeture
     * Gère les cas où l'heure de fermeture est après minuit (ex: 18h -> 2h du lendemain)
     * Pour les créneaux traversant minuit, s'arrête à minuit (00h) au lieu d'aller jusqu'à l'heure de fermeture
     * 
     * @param terrainId L'ID du terrain
     * @return Liste des horaires (LocalTime) de l'ouverture à la fermeture (ou minuit si traverse minuit)
     */
    public List<LocalTime> getHorairesTerrain(Long terrainId) {

        TerrainService terrain = terrainRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Terrain introuvable avec l'ID: " + terrainId));

        LocalTime heureOuverture = terrain.getHeureOuverture();
        LocalTime heureFermeture = terrain.getHeureFermeture();

        if (heureOuverture == null || heureFermeture == null) {
            throw new IllegalArgumentException(
                    "Les heures d'ouverture et de fermeture doivent être définies");
        }

        List<LocalTime> horaires = new ArrayList<>();
        LocalTime minuit = LocalTime.of(0, 0);
        
        // Vérifier si le terrain ferme après minuit (ex: 18h -> 2h)
        boolean terrainFermeApresMinuit = heureFermeture.isBefore(heureOuverture);

        LocalTime heureCourante = heureOuverture;

        // Boucle infinie contrôlée (max 24 itérations)
        while (true) {
            horaires.add(heureCourante);

            // Si le terrain ferme après minuit, s'arrêter à minuit (00h)
            if (terrainFermeApresMinuit && heureCourante.equals(minuit)) {
                break;
            }
            
            // Sinon, STOP quand on atteint l'heure de fermeture
            if (!terrainFermeApresMinuit && heureCourante.equals(heureFermeture)) {
                break;
            }

            // Avancer d'une heure (gère automatiquement minuit)
            heureCourante = heureCourante.plusHours(1);
        }

        return horaires;
    }
    
}
