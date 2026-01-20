package com.matchi.repository;

import com.matchi.model.AbonnementHoraire;
import com.matchi.model.JourSemaine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbonnementHoraireRepository extends JpaRepository<AbonnementHoraire, Long> {

    List<AbonnementHoraire> findByAbonnementId(Long abonnementId);

    List<AbonnementHoraire> findByJourSemaine(JourSemaine jourSemaine);
}
