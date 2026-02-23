package com.matchi.service;

import com.matchi.dto.DisponibiliteResponseDTO;
import com.matchi.dto.HoraireOccupeDTO;
import com.matchi.model.IndisponibleHoraire;
import com.matchi.repository.IndisponibleHoraireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibiliteService {

    private final IndisponibleHoraireRepository indisponibleHoraireRepository;

    /**
     * Retourne tous les horaires indisponibles d'un terrain donné,
     * avec la date, l'heure de début, l'heure de fin et le numéro de téléphone
     * du propriétaire associé au terrain.
     */
    public DisponibiliteResponseDTO getHorairesIndisponiblesParTerrain(Long terrainId) {
        List<IndisponibleHoraire> indisponibles = indisponibleHoraireRepository.findByTerrainId(terrainId);

        LocalDate aujourdhui = LocalDate.now();
        LocalTime maintenant = LocalTime.now();

        List<HoraireOccupeDTO> horaires = indisponibles.stream()
                // Ne garder que les créneaux non dépassés
                .filter(h -> {
                    if (h.getDate() == null) {
                        return false;
                    }

                    if (h.getDate().isBefore(aujourdhui)) {
                        return false;
                    }

                    if (h.getDate().isEqual(aujourdhui)
                            && h.getHeureFin() != null
                            && h.getHeureFin().isBefore(maintenant)) {
                        return false;
                    }

                    return true;
                })
                .map(h -> new HoraireOccupeDTO(
                        h.getDate(),
                        h.getHeureDebut(),
                        h.getHeureFin(),
                        h.getTerrain() != null && h.getTerrain().getProprietaire() != null
                                ? h.getTerrain().getProprietaire().getTelephone()
                                : null,
                        h.getTerrain() != null ? h.getTerrain().getId() : null
                ))
                .sorted(Comparator
                        .comparing(HoraireOccupeDTO::date)
                        .thenComparing(HoraireOccupeDTO::heureDebut))
                .toList();

        return new DisponibiliteResponseDTO(horaires);
    }
}
