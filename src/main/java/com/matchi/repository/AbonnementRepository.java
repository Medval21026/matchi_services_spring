package com.matchi.repository;



import com.matchi.model.Abonnement;
import com.matchi.model.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    List<Abonnement> findByTerrainId(Long terrainId);

    List<Abonnement> findByClientId(Long clientId);

    List<Abonnement> findByStatus(StatutAbonnement status);
}
