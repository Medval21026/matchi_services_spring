package com.matchi.repository;



import com.matchi.model.ClientAbonne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientAbonneRepository extends JpaRepository<ClientAbonne, Long> {

    Optional<ClientAbonne> findByTelephone(Integer telephone);
    boolean existsByTelephone(Integer telephone);
}
