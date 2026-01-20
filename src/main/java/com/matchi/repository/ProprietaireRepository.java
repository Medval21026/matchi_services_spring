package com.matchi.repository;



import com.matchi.model.Proprietaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProprietaireRepository extends JpaRepository<Proprietaire, Long> {

    Optional<Proprietaire> findByTelephone(Integer telephone);

    boolean existsByTelephone(Integer telephone);
}
