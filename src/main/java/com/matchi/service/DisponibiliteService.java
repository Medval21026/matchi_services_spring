package com.matchi.service;

import com.matchi.dto.DisponibiliteResponseDTO;
import com.matchi.dto.HoraireOccupeDTO;
import com.matchi.model.*;
import com.matchi.repository.AbonnementHoraireRepository;
import com.matchi.repository.ReservationPonctuelleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibiliteService {

    private final AbonnementHoraireRepository abonnementHoraireRepository;
    private final ReservationPonctuelleRepository reservationPonctuelleRepository;

    /**
     * Récupère TOUS les horaires occupés de TOUS les terrains
     * 
     * @return DisponibiliteResponseDTO avec tous les horaires occupés
     */
    public DisponibiliteResponseDTO getTousLesHorairesOccupes() {
        List<HoraireOccupeDTO> horairesOccupes = new ArrayList<>();

        // 1. Récupérer TOUS les horaires d'abonnement
        List<AbonnementHoraire> abonnementHoraires = abonnementHoraireRepository.findAll();

        for (AbonnementHoraire ah : abonnementHoraires) {
            // Récupérer le téléphone du propriétaire via terrain
            Integer telephone = null;
            Long terrainId = null;
            
            if (ah.getAbonnement() != null 
                    && ah.getAbonnement().getTerrain() != null) {
                terrainId = ah.getAbonnement().getTerrain().getId();
                
                if (ah.getAbonnement().getTerrain().getProprietaire() != null) {
                    telephone = ah.getAbonnement().getTerrain().getProprietaire().getTelephone();
                }
            }

            horairesOccupes.add(new HoraireOccupeDTO(
                    ah.getDate(),
                    ah.getHeureDebut(),
                    ah.getHeureFin(),
                    telephone,
                    terrainId
            ));
        }

        // 2. Récupérer TOUTES les réservations ponctuelles
        List<ReservationPonctuelle> reservations = reservationPonctuelleRepository.findAll();

        for (ReservationPonctuelle rp : reservations) {
            // Récupérer le téléphone du propriétaire via terrain
            Integer telephone = null;
            Long terrainId = null;
            
            if (rp.getTerrain() != null) {
                terrainId = rp.getTerrain().getId();
                
                if (rp.getTerrain().getProprietaire() != null) {
                    telephone = rp.getTerrain().getProprietaire().getTelephone();
                }
            }

            horairesOccupes.add(new HoraireOccupeDTO(
                    rp.getDate(),
                    rp.getHeureDebut(),
                    rp.getHeureFin(),
                    telephone,
                    terrainId
            ));
        }

        // 3. Trier les horaires par date puis par heure de début
        horairesOccupes.sort(Comparator
                .comparing(HoraireOccupeDTO::date)
                .thenComparing(HoraireOccupeDTO::heureDebut));

        return new DisponibiliteResponseDTO(horairesOccupes);
    }
}
