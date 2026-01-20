package com.matchi.repository;

import com.matchi.model.IndisponibleHoraire;
import com.matchi.model.TypeReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IndisponibleHoraireRepository extends JpaRepository<IndisponibleHoraire, Long> {
    
    // Trouver tous les horaires indisponibles d'un terrain
    List<IndisponibleHoraire> findByTerrainId(Long terrainId);
    
    // Trouver par terrain et date
    List<IndisponibleHoraire> findByTerrainIdAndDate(Long terrainId, LocalDate date);
    
    // Trouver par terrain et période
    List<IndisponibleHoraire> findByTerrainIdAndDateBetween(Long terrainId, LocalDate dateDebut, LocalDate dateFin);
    
    // Trouver par type de réservation
    List<IndisponibleHoraire> findByTerrainIdAndTypeReservation(Long terrainId, TypeReservation typeReservation);
    
    // Supprimer par source (utile pour synchronisation)
    void deleteByTypeReservationAndSourceId(TypeReservation typeReservation, Long sourceId);
}
