package com.matchi.repository;

import com.matchi.model.ReservationPonctuelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationPonctuelleRepository extends JpaRepository<ReservationPonctuelle, Long> {

    // Récupérer toutes les réservations pour un terrain donné
    List<ReservationPonctuelle> findByTerrain_Id(Long terrainId);
    
    // ✅ Vérifier les conflits : même terrain, même date, même heure de début
    Optional<ReservationPonctuelle> findByTerrain_IdAndDateAndHeureDebut(Long terrainId, LocalDate date, LocalTime heureDebut);
    
    // ✅ Vérifier les conflits avec chevauchement de créneaux horaires
    List<ReservationPonctuelle> findByTerrain_IdAndDate(Long terrainId, LocalDate date);
}
