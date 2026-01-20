package com.matchi.repository;



import com.matchi.model.TarifTerrain;
import com.matchi.model.JourSemaine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.Optional;

public interface TarifTerrainRepository extends JpaRepository<TarifTerrain, Long> {

    Optional<TarifTerrain> findByTerrainIdAndJourSemaineAndHeureDebutLessThanEqualAndHeureFinGreaterThanEqual(
            Long terrainId,
            JourSemaine jourSemaine,
            LocalTime heure,
            LocalTime heure2
    );
    Optional<TarifTerrain> findByTerrainId(Long terrainId);
}
