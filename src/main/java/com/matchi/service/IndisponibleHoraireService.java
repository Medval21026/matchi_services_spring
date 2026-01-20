package com.matchi.service;

import com.matchi.dto.IndisponibleHoraireDTO;
import com.matchi.model.*;
import com.matchi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.matchi.event.AbonnementModifieEvent;
import com.matchi.event.ReservationModifieEvent;
import com.matchi.event.AbonnementHoraireChangeEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndisponibleHoraireService {

    private final IndisponibleHoraireRepository indisponibleHoraireRepository;
    private final AbonnementHoraireRepository abonnementHoraireRepository;
    private final ReservationPonctuelleRepository reservationPonctuelleRepository;
    private final TerrainServiceRepository terrainServiceRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    // ======== MAPPERS ========
    private IndisponibleHoraireDTO toDTO(IndisponibleHoraire horaire) {
        return new IndisponibleHoraireDTO(
                horaire.getId(),
                horaire.getTerrain() != null ? horaire.getTerrain().getId() : null,
                horaire.getDate(),
                horaire.getHeureDebut(),
                horaire.getHeureFin(),
                horaire.getTypeReservation(),
                horaire.getSourceId(),
                horaire.getDescription()
        );
    }

    // ======== CRUD ========
    public List<IndisponibleHoraireDTO> getAllIndisponibles() {
        LocalDate aujourdhui = LocalDate.now();
        java.time.LocalTime maintenant = java.time.LocalTime.now();
        
        return indisponibleHoraireRepository.findAll()
                .stream()
                // ✅ FILTRER : Exclure les horaires passés
                .filter(horaire -> {
                    if (horaire.getDate() == null) {
                        return false; // Exclure si pas de date
                    }
                    
                    // Si la date est dans le passé, exclure
                    if (horaire.getDate().isBefore(aujourdhui)) {
                        return false;
                    }
                    
                    // Si c'est aujourd'hui, vérifier que l'heure de fin n'est pas passée
                    if (horaire.getDate().equals(aujourdhui)) {
                        if (horaire.getHeureFin() != null && horaire.getHeureFin().isBefore(maintenant)) {
                            return false; // L'heure de fin est passée, exclure
                        }
                    }
                    
                    return true; // Inclure les horaires futurs ou en cours
                })
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<IndisponibleHoraireDTO> getIndisponiblesByTerrain(Long terrainId) {
        return indisponibleHoraireRepository.findByTerrainId(terrainId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<IndisponibleHoraireDTO> getIndisponiblesByTerrainAndDate(Long terrainId, LocalDate date) {
        return indisponibleHoraireRepository.findByTerrainIdAndDate(terrainId, date)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<IndisponibleHoraireDTO> getIndisponiblesByTerrainAndPeriode(
            Long terrainId, 
            LocalDate dateDebut, 
            LocalDate dateFin) {
        return indisponibleHoraireRepository.findByTerrainIdAndDateBetween(terrainId, dateDebut, dateFin)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ======== SYNCHRONISATION ========
    
    /**
     * Synchronise les horaires indisponibles d'un terrain
     * en les récupérant depuis AbonnementHoraire et ReservationPonctuelle
     * 
     * ✅ CORRECTION : Utilise REQUIRES_NEW pour s'assurer que les données sont bien commitées
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchroniserHorairesIndisponibles(Long terrainId) {
        // ✅ CLEAR : Vider le cache L1 au début pour forcer la relecture depuis la base
        entityManager.clear();
        
        // ✅ ÉVACUER LE CACHE L2 : Vider complètement le cache de niveau 2
        jakarta.persistence.Cache cache = entityManager.getEntityManagerFactory().getCache();
        if (cache != null) {
            cache.evictAll();
        }
        
        TerrainService terrain = terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));

        // Supprimer tous les anciens horaires indisponibles de ce terrain
        List<IndisponibleHoraire> anciens = indisponibleHoraireRepository.findByTerrainId(terrainId);
        indisponibleHoraireRepository.deleteAll(anciens);
        
        // ✅ FLUSH : S'assurer que la suppression est bien écrite
        entityManager.flush();
        entityManager.clear(); // Vider le cache après suppression
        
        // ✅ ÉVACUER LE CACHE L2 à nouveau après suppression
        if (cache != null) {
            cache.evictAll();
        }
        
        // ✅ CLEAR SUPPLÉMENTAIRE : Vider le cache juste avant de lire les données
        entityManager.clear();

        // ✅ Récupérer tous les horaires d'abonnement pour ce terrain depuis la base (pas du cache)
        // Utiliser une requête JPQL avec hints agressifs pour forcer la relecture depuis la base
        jakarta.persistence.Query query = entityManager.createQuery(
            "SELECT ah FROM AbonnementHoraire ah " +
            "JOIN FETCH ah.abonnement a " +
            "JOIN FETCH a.terrain t " +
            "WHERE t.id = :terrainId",
            AbonnementHoraire.class
        );
        query.setParameter("terrainId", terrainId);
        query.setHint("jakarta.persistence.cache.retrieveMode", 
                     jakarta.persistence.CacheRetrieveMode.BYPASS);
        query.setHint("jakarta.persistence.cache.storeMode", 
                     jakarta.persistence.CacheStoreMode.BYPASS);
        query.setHint("org.hibernate.cacheable", false);
        @SuppressWarnings("unchecked")
        List<AbonnementHoraire> abonnementHoraires = query.getResultList();
        
        // ✅ DETACH : Détacher toutes les entités pour forcer la relecture
        for (AbonnementHoraire ah : abonnementHoraires) {
            entityManager.detach(ah);
            if (ah.getAbonnement() != null) {
                entityManager.detach(ah.getAbonnement());
            }
        }

        // Créer les horaires indisponibles pour les abonnements
        List<IndisponibleHoraire> horairesIndisponibles = new java.util.ArrayList<>();
        
        for (AbonnementHoraire ah : abonnementHoraires) {
            IndisponibleHoraire indispo = IndisponibleHoraire.builder()
                    .terrain(terrain)
                    .date(ah.getDate())
                    .heureDebut(ah.getHeureDebut())
                    .heureFin(ah.getHeureFin())
                    .typeReservation(TypeReservation.ABONNEMENT)
                    .sourceId(ah.getId())
                    .description("Abonnement - " + ah.getJourSemaine())
                    .build();
            horairesIndisponibles.add(indispo);
        }

        // ✅ Récupérer toutes les réservations ponctuelles pour ce terrain depuis la base
        // Utiliser une requête JPQL avec hints agressifs pour forcer la relecture depuis la base
        entityManager.clear();
        jakarta.persistence.Query queryReservations = entityManager.createQuery(
            "SELECT rp FROM ReservationPonctuelle rp " +
            "WHERE rp.terrain.id = :terrainId",
            ReservationPonctuelle.class
        );
        queryReservations.setParameter("terrainId", terrainId);
        queryReservations.setHint("jakarta.persistence.cache.retrieveMode", 
                                  jakarta.persistence.CacheRetrieveMode.BYPASS);
        queryReservations.setHint("jakarta.persistence.cache.storeMode", 
                                  jakarta.persistence.CacheStoreMode.BYPASS);
        queryReservations.setHint("org.hibernate.cacheable", false);
        @SuppressWarnings("unchecked")
        List<ReservationPonctuelle> reservations = queryReservations.getResultList();
        
        // ✅ DETACH : Détacher toutes les réservations pour forcer la relecture
        for (ReservationPonctuelle rp : reservations) {
            entityManager.detach(rp);
        }

        // Créer les horaires indisponibles pour les réservations ponctuelles
        for (ReservationPonctuelle rp : reservations) {
            IndisponibleHoraire indispo = IndisponibleHoraire.builder()
                    .terrain(terrain)
                    .date(rp.getDate())
                    .heureDebut(rp.getHeureDebut())
                    .heureFin(rp.getHeureFin())
                    .typeReservation(TypeReservation.RESERVATION_PONCTUELLE)
                    .sourceId(rp.getId())
                    .description("Réservation ponctuelle")
                    .build();
            horairesIndisponibles.add(indispo);
        }
        
        // ✅ Sauvegarder tous les horaires indisponibles en une seule fois
        if (!horairesIndisponibles.isEmpty()) {
            indisponibleHoraireRepository.saveAll(horairesIndisponibles);
            // ✅ FLUSH : Forcer l'écriture en base pour garantir la persistance
            entityManager.flush();
        }
    }

    /**
     * Synchronise les horaires indisponibles de TOUS les terrains
     */
    @Transactional
    public void synchroniserTousLesTerrains() {
        List<TerrainService> terrains = terrainServiceRepository.findAll();
        for (TerrainService terrain : terrains) {
            synchroniserHorairesIndisponibles(terrain.getId());
        }
    }

    /**
     * Ajoute un horaire indisponible manuellement
     */
    @Transactional
    public IndisponibleHoraireDTO ajouterHoraireIndisponible(IndisponibleHoraireDTO dto) {
        TerrainService terrain = terrainServiceRepository.findById(dto.terrainId())
                .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));

        IndisponibleHoraire horaire = IndisponibleHoraire.builder()
                .terrain(terrain)
                .date(dto.date())
                .heureDebut(dto.heureDebut())
                .heureFin(dto.heureFin())
                .typeReservation(dto.typeReservation())
                .sourceId(dto.sourceId())
                .description(dto.description())
                .build();

        IndisponibleHoraire saved = indisponibleHoraireRepository.save(horaire);
        return toDTO(saved);
    }

    /**
     * Supprime un horaire indisponible
     */
    @Transactional
    public void supprimerHoraireIndisponible(Long id) {
        if (!indisponibleHoraireRepository.existsById(id)) {
            throw new IllegalArgumentException("Horaire indisponible introuvable");
        }
        indisponibleHoraireRepository.deleteById(id);
    }
    
    // ======== EVENT LISTENERS ========
    
    /**
     * ✅ Écoute l'événement de modification/suppression d'abonnement APRÈS le commit
     * pour déclencher la synchronisation des horaires indisponibles.
     * 
     * Cet événement est déclenché lors de :
     * - Création d'un abonnement
     * - Modification d'un abonnement (dates, horaires)
     * - Suppression d'un abonnement
     * - Modification/suppression d'un horaire d'abonnement
     * 
     * ✅ Utilise REQUIRES_NEW pour s'exécuter dans une nouvelle transaction après le commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAbonnementModifie(AbonnementModifieEvent event) {
        synchroniserHorairesIndisponibles(event.terrainId());
    }
    
    /**
     * ✅ Écoute l'événement de modification/suppression de réservation APRÈS le commit
     * pour déclencher la synchronisation des horaires indisponibles.
     * 
     * Cet événement est déclenché lors de :
     * - Création d'une réservation ponctuelle
     * - Modification d'une réservation ponctuelle
     * - Suppression d'une réservation ponctuelle
     * 
     * ✅ Utilise REQUIRES_NEW pour s'exécuter dans une nouvelle transaction après le commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReservationModifie(ReservationModifieEvent event) {
        synchroniserHorairesIndisponibles(event.terrainId());
    }
    
    /**
     * ✅ Écoute l'événement de modification/ajout d'un horaire d'abonnement APRÈS le commit
     * pour déclencher la synchronisation des horaires indisponibles.
     * 
     * Cet événement est déclenché lors de :
     * - Ajout d'un horaire d'abonnement
     * - Modification d'un horaire d'abonnement
     * 
     * ✅ Utilise REQUIRES_NEW pour s'exécuter dans une nouvelle transaction après le commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAbonnementHoraireChange(AbonnementHoraireChangeEvent event) {
        synchroniserHorairesIndisponibles(event.terrainId());
    }
}
