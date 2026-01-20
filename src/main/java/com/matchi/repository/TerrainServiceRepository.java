package com.matchi.repository;



import com.matchi.model.TerrainService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerrainServiceRepository extends JpaRepository<TerrainService, Long> {

    

    List<TerrainService> findByProprietaireId(Long proprietaireId);
}
