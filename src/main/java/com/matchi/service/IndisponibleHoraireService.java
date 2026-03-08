package com.matchi.service;

import com.matchi.dto.IndisponibleHoraireDTO;
import com.matchi.model.*;
import com.matchi.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.matchi.event.AbonnementModifieEvent;
import com.matchi.event.ReservationModifieEvent;
import com.matchi.event.AbonnementHoraireChangeEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndisponibleHoraireService {
    
    // ✅ Verrous par terrainId pour éviter les doublons lors d'appels parallèles
    private static final ConcurrentHashMap<Long, Lock> terrainLocks = new ConcurrentHashMap<>();
    
    /**
     * Obtient un verrou pour un terrainId donné
     */
    private Lock getTerrainLock(Long terrainId) {
        return terrainLocks.computeIfAbsent(terrainId, k -> new ReentrantLock());
    }

    private static final Logger log = LoggerFactory.getLogger(IndisponibleHoraireService.class);

    private final IndisponibleHoraireRepository indisponibleHoraireRepository;
    private final AbonnementHoraireRepository abonnementHoraireRepository;
    private final ReservationPonctuelleRepository reservationPonctuelleRepository;
    private final TerrainServiceRepository terrainServiceRepository;
    private final DjangoSyncService djangoSyncService;
    private final KafkaAvailabilityService kafkaAvailabilityService;
    
    @Autowired(required = false)
    private HoraireEventPublisherService horaireEventPublisherService;
    
    @PostConstruct
    public void init() {
        if (horaireEventPublisherService != null) {
            log.info("✅ IndisponibleHoraireService: HoraireEventPublisherService est injecté et disponible");
        } else {
            log.warn("⚠️ IndisponibleHoraireService: HoraireEventPublisherService est NULL - les événements Kafka ne seront pas publiés");
            log.warn("⚠️ Vérifiez que le bean 'kafkaTemplate' est bien créé dans KafkaConfig");
        }
    }
    
    @PersistenceContext
    private EntityManager entityManager;

    // ======== MAPPERS ========
    private IndisponibleHoraireDTO toDTO(IndisponibleHoraire horaire) {
        return new IndisponibleHoraireDTO(
                horaire.getId(),
                horaire.getUuid(),
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
     * ✅ VERROU : Utilise un verrou par terrainId pour éviter les doublons lors d'appels parallèles
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchroniserHorairesIndisponibles(Long terrainId) {
        // ✅ VERROU : Verrouiller par terrainId pour éviter les appels parallèles
        Lock lock = getTerrainLock(terrainId);
        lock.lock();
        try {
            synchroniserHorairesIndisponiblesInternal(terrainId);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Logique interne de synchronisation (appelée avec le verrou)
     */
    private void synchroniserHorairesIndisponiblesInternal(Long terrainId) {
        // ✅ VALIDATION : Vérifier que Kafka est disponible avant de créer des horaires potentiels
        if (!kafkaAvailabilityService.isKafkaAvailable()) {
            log.warn("⚠️ Impossible de synchroniser les horaires indisponibles pour le terrain {} : Kafka n'est pas démarré ou n'est pas disponible", terrainId);
            throw new IllegalStateException("Impossible de créer des horaires potentiels : Kafka n'est pas démarré ou n'est pas disponible. Veuillez démarrer Kafka avant de créer des abonnements ou réservations.");
        }
        
        // ✅ CLEAR : Vider le cache L1 au début pour forcer la relecture depuis la base
        entityManager.clear();
        
        // ✅ ÉVACUER LE CACHE L2 : Vider complètement le cache de niveau 2
        jakarta.persistence.Cache cache = entityManager.getEntityManagerFactory().getCache();
        if (cache != null) {
            cache.evictAll();
        }
        
        TerrainService terrain = terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));

        // ✅ NOUVELLE APPROCHE : Ne pas supprimer tous les horaires, mais seulement ceux qui n'ont plus de source
        // Récupérer les horaires existants
        List<IndisponibleHoraire> horairesExistants = indisponibleHoraireRepository.findByTerrainId(terrainId);
        
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
        List<IndisponibleHoraire> horairesMisAJour = new java.util.ArrayList<>();
        Set<Long> abonnementSourceIdsUtilises = new HashSet<>();
        
        for (AbonnementHoraire ah : abonnementHoraires) {
            abonnementSourceIdsUtilises.add(ah.getId());
            
            // ✅ VÉRIFICATION PAR SOURCE ID SEULEMENT : Chercher un horaire existant par sourceId uniquement
            // (sans comparer date/heures car elles peuvent changer lors de la mise à jour)
            // Cela permet de trouver l'horaire existant même si ses valeurs ont changé
            List<IndisponibleHoraire> horairesExistantsPourSource = indisponibleHoraireRepository
                    .findByTerrainIdAndTypeReservation(terrainId, TypeReservation.ABONNEMENT)
                    .stream()
                    .filter(h -> h.getSourceId() != null && h.getSourceId().equals(ah.getId()))
                    .collect(Collectors.toList());
            
            if (!horairesExistantsPourSource.isEmpty()) {
                // Prendre le premier horaire trouvé (il ne devrait y en avoir qu'un)
                IndisponibleHoraire existant = horairesExistantsPourSource.get(0);
                
                // Si plusieurs horaires existent pour la même source, supprimer les doublons
                if (horairesExistantsPourSource.size() > 1) {
                    log.warn("⚠️ {} horaires trouvés pour l'abonnement horaire sourceId={}, suppression des doublons", 
                            horairesExistantsPourSource.size(), ah.getId());
                    // Supprimer tous sauf le premier
                    for (int i = 1; i < horairesExistantsPourSource.size(); i++) {
                        IndisponibleHoraire doublon = horairesExistantsPourSource.get(i);
                        if (horaireEventPublisherService != null && doublon.getUuid() != null) {
                            TerrainService terrainService = terrainServiceRepository.findById(terrainId).orElse(null);
                            Integer proprietaireTelephone = null;
                            if (terrainService != null && terrainService.getProprietaire() != null) {
                                proprietaireTelephone = terrainService.getProprietaire().getTelephone();
                            }
                            horaireEventPublisherService.publishDeletedEvent(
                                    doublon.getUuid(),
                                    terrainId,
                                    doublon.getDate(),
                                    doublon.getHeureDebut(),
                                    doublon.getHeureFin(),
                                    proprietaireTelephone
                            );
                        }
                        indisponibleHoraireRepository.delete(doublon);
                    }
                    entityManager.flush();
                }
                
                // ✅ Mettre à jour l'horaire existant (garder le même UUID)
                // Toujours mettre à jour même si les valeurs semblent identiques (pour garantir la cohérence)
                boolean aChange = false;
                
                // Vérifier si la date a changé
                if (ah.getDate() != null && (existant.getDate() == null || !ah.getDate().equals(existant.getDate()))) {
                    existant.setDate(ah.getDate());
                    aChange = true;
                    log.debug("📝 Date mise à jour pour l'abonnement horaire sourceId={}: {} -> {}", 
                            ah.getId(), existant.getDate(), ah.getDate());
                }
                
                // Vérifier si l'heure de début a changé
                if (ah.getHeureDebut() != null && (existant.getHeureDebut() == null || !ah.getHeureDebut().equals(existant.getHeureDebut()))) {
                    existant.setHeureDebut(ah.getHeureDebut());
                    aChange = true;
                    log.debug("📝 Heure de début mise à jour pour l'abonnement horaire sourceId={}: {} -> {}", 
                            ah.getId(), existant.getHeureDebut(), ah.getHeureDebut());
                }
                
                // Vérifier si l'heure de fin a changé
                if (ah.getHeureFin() != null && (existant.getHeureFin() == null || !ah.getHeureFin().equals(existant.getHeureFin()))) {
                    existant.setHeureFin(ah.getHeureFin());
                    aChange = true;
                    log.debug("📝 Heure de fin mise à jour pour l'abonnement horaire sourceId={}: {} -> {}", 
                            ah.getId(), existant.getHeureFin(), ah.getHeureFin());
                }
                
                // Mettre à jour la description
                String nouvelleDescription = "Abonnement - " + ah.getJourSemaine();
                if (existant.getDescription() == null || !nouvelleDescription.equals(existant.getDescription())) {
                    existant.setDescription(nouvelleDescription);
                    aChange = true;
                    log.debug("📝 Description mise à jour pour l'abonnement horaire sourceId={}: {} -> {}", 
                            ah.getId(), existant.getDescription(), nouvelleDescription);
                }
                
                // ✅ TOUJOURS AJOUTER À LA LISTE DE MISE À JOUR pour garantir la publication Kafka
                // Même si aChange est false, on sauvegarde pour s'assurer que tout est synchronisé
                horairesMisAJour.add(existant);
                if (aChange) {
                    log.info("✅ Horaire existant modifié pour l'abonnement horaire sourceId={}, UUID={} - événement Kafka 'updated' sera publié", 
                            ah.getId(), existant.getUuid());
                } else {
                    log.debug("ℹ️ Horaire existant vérifié pour l'abonnement horaire sourceId={}, UUID={} (aucun changement détecté mais sera sauvegardé)", 
                            ah.getId(), existant.getUuid());
                }
                continue; // Ne pas l'ajouter à la liste de création
            }
            
            IndisponibleHoraire indispo = IndisponibleHoraire.builder()
                    .uuid(UUID.randomUUID()) // Générer un UUID unique
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
        
        // Supprimer les horaires d'abonnements qui n'ont plus de source
        List<IndisponibleHoraire> horairesAbonnementASupprimer = horairesExistants.stream()
                .filter(h -> h.getTypeReservation() == TypeReservation.ABONNEMENT
                        && (h.getSourceId() == null || !abonnementSourceIdsUtilises.contains(h.getSourceId())))
                .collect(Collectors.toList());
        
        if (!horairesAbonnementASupprimer.isEmpty()) {
            log.info("Suppression de {} horaires d'abonnements obsolètes", horairesAbonnementASupprimer.size());
            // Publier les événements de suppression
            if (horaireEventPublisherService != null) {
                TerrainService terrainService = terrainServiceRepository.findById(terrainId).orElse(null);
                Integer proprietaireTelephone = null;
                if (terrainService != null && terrainService.getProprietaire() != null) {
                    proprietaireTelephone = terrainService.getProprietaire().getTelephone();
                }
                for (IndisponibleHoraire aSupprimer : horairesAbonnementASupprimer) {
                    if (aSupprimer.getUuid() != null) {
                        horaireEventPublisherService.publishDeletedEvent(
                                aSupprimer.getUuid(),
                                terrainId,
                                aSupprimer.getDate(),
                                aSupprimer.getHeureDebut(),
                                aSupprimer.getHeureFin(),
                                proprietaireTelephone
                        );
                    }
                }
            }
            indisponibleHoraireRepository.deleteAll(horairesAbonnementASupprimer);
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
        // Utiliser une approche "upsert" : mettre à jour si existe, créer si n'existe pas
        Set<Long> sourceIdsUtilises = new HashSet<>();
        for (ReservationPonctuelle rp : reservations) {
            sourceIdsUtilises.add(rp.getId());
            
            // ✅ VÉRIFICATION DIRECTE EN BASE : Chercher un horaire existant directement dans la base
            // pour éviter les doublons même si la méthode est appelée plusieurs fois en parallèle
            List<IndisponibleHoraire> horairesExistantsPourSource = indisponibleHoraireRepository
                    .findByTerrainIdAndTypeReservation(terrainId, TypeReservation.RESERVATION_PONCTUELLE)
                    .stream()
                    .filter(h -> h.getSourceId() != null && h.getSourceId().equals(rp.getId()))
                    .collect(Collectors.toList());
            
            if (!horairesExistantsPourSource.isEmpty()) {
                // Prendre le premier horaire trouvé (il ne devrait y en avoir qu'un)
                IndisponibleHoraire existant = horairesExistantsPourSource.get(0);
                
                // Si plusieurs horaires existent pour la même source, supprimer les doublons
                if (horairesExistantsPourSource.size() > 1) {
                    log.warn("⚠️ {} horaires trouvés pour la réservation sourceId={}, suppression des doublons", 
                            horairesExistantsPourSource.size(), rp.getId());
                    // Supprimer tous sauf le premier
                    for (int i = 1; i < horairesExistantsPourSource.size(); i++) {
                        IndisponibleHoraire doublon = horairesExistantsPourSource.get(i);
                        if (horaireEventPublisherService != null && doublon.getUuid() != null) {
                            TerrainService terrainService = terrainServiceRepository.findById(terrainId).orElse(null);
                            Integer proprietaireTelephone = null;
                            if (terrainService != null && terrainService.getProprietaire() != null) {
                                proprietaireTelephone = terrainService.getProprietaire().getTelephone();
                            }
                            horaireEventPublisherService.publishDeletedEvent(
                                    doublon.getUuid(),
                                    terrainId,
                                    doublon.getDate(),
                                    doublon.getHeureDebut(),
                                    doublon.getHeureFin(),
                                    proprietaireTelephone
                            );
                        }
                        indisponibleHoraireRepository.delete(doublon);
                    }
                    entityManager.flush();
                }
                
                // Mettre à jour l'horaire existant (garder le même UUID)
                // Vérifier si quelque chose a vraiment changé
                boolean aChange = false;
                if (!rp.getDate().equals(existant.getDate())) {
                    existant.setDate(rp.getDate());
                    aChange = true;
                }
                if (!rp.getHeureDebut().equals(existant.getHeureDebut())) {
                    existant.setHeureDebut(rp.getHeureDebut());
                    aChange = true;
                }
                if (!rp.getHeureFin().equals(existant.getHeureFin())) {
                    existant.setHeureFin(rp.getHeureFin());
                    aChange = true;
                }
                existant.setDescription("Réservation ponctuelle");
                
                if (aChange) {
                    horairesMisAJour.add(existant);
                    log.debug("Horaire existant mis à jour pour la réservation ponctuelle ID: {}", rp.getId());
                }
                continue; // Ne pas l'ajouter à la liste de création
            }
            
            // Créer un nouvel horaire seulement si aucun n'existe
            IndisponibleHoraire indispo = IndisponibleHoraire.builder()
                    .uuid(UUID.randomUUID()) // Générer un UUID unique
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
        
        // Supprimer les horaires de réservations ponctuelles qui n'ont plus de source
        List<IndisponibleHoraire> horairesASupprimer = horairesExistants.stream()
                .filter(h -> h.getTypeReservation() == TypeReservation.RESERVATION_PONCTUELLE
                        && (h.getSourceId() == null || !sourceIdsUtilises.contains(h.getSourceId())))
                .collect(Collectors.toList());
        
        if (!horairesASupprimer.isEmpty()) {
            log.info("Suppression de {} horaires de réservations ponctuelles obsolètes", horairesASupprimer.size());
            // Publier les événements de suppression
            if (horaireEventPublisherService != null) {
                TerrainService terrainService = terrainServiceRepository.findById(terrainId).orElse(null);
                Integer proprietaireTelephone = null;
                if (terrainService != null && terrainService.getProprietaire() != null) {
                    proprietaireTelephone = terrainService.getProprietaire().getTelephone();
                }
                for (IndisponibleHoraire aSupprimer : horairesASupprimer) {
                    if (aSupprimer.getUuid() != null) {
                        horaireEventPublisherService.publishDeletedEvent(
                                aSupprimer.getUuid(),
                                terrainId,
                                aSupprimer.getDate(),
                                aSupprimer.getHeureDebut(),
                                aSupprimer.getHeureFin(),
                                proprietaireTelephone
                        );
                    }
                }
            }
            indisponibleHoraireRepository.deleteAll(horairesASupprimer);
        }
        
        // ✅ Sauvegarder les horaires mis à jour et publier les événements "updated"
        if (!horairesMisAJour.isEmpty()) {
            List<IndisponibleHoraire> savedUpdated = indisponibleHoraireRepository.saveAll(horairesMisAJour);
            entityManager.flush();
            
            log.info("✅ {} horaires indisponibles mis à jour pour le terrain {}", savedUpdated.size(), terrainId);
            
            // Publier les événements de mise à jour pour chaque horaire modifié
            if (horaireEventPublisherService != null) {
                log.info("📤 Publication de {} événements de mise à jour pour les horaires modifiés du terrain {}", 
                        savedUpdated.size(), terrainId);
                for (IndisponibleHoraire horaire : savedUpdated) {
                    log.debug("📤 Publication de l'événement 'updated' pour l'horaire UUID: {}", horaire.getUuid());
                    horaireEventPublisherService.publishUpdatedEvent(horaire);
                }
                log.info("✅ Tous les événements de mise à jour ont été envoyés pour le terrain {}", terrainId);
            } else {
                log.error("❌ HoraireEventPublisherService est NULL - les événements Kafka ne seront PAS publiés pour le terrain {}", terrainId);
            }
        }
        
        // ✅ Sauvegarder tous les horaires indisponibles en une seule fois
        log.info("🔍 Synchronisation: {} horaires indisponibles à créer pour le terrain {}", 
                horairesIndisponibles.size(), terrainId);
        log.info("🔍 HoraireEventPublisherService injecté: {}", horaireEventPublisherService != null);
        
        if (!horairesIndisponibles.isEmpty()) {
            // ✅ VÉRIFICATION FINALE : Vérifier une dernière fois en base avant de sauvegarder
            // pour éviter les doublons même en cas d'appels parallèles
            List<IndisponibleHoraire> horairesACreer = new java.util.ArrayList<>();
            for (IndisponibleHoraire horaire : horairesIndisponibles) {
                // Vérifier si un horaire avec le même sourceId existe déjà en base
                boolean existeDeja = indisponibleHoraireRepository
                        .findByTerrainIdAndTypeReservation(terrainId, horaire.getTypeReservation())
                        .stream()
                        .anyMatch(h -> h.getSourceId() != null && h.getSourceId().equals(horaire.getSourceId()));
                
                if (existeDeja) {
                    log.warn("⚠️ Horaire avec sourceId={} existe déjà en base, ignoré pour éviter le doublon", 
                            horaire.getSourceId());
                    continue;
                }
                
                horairesACreer.add(horaire);
            }
            
            if (!horairesACreer.isEmpty()) {
                // ✅ SAUVEGARDE AVEC GESTION D'EXCEPTION : Gérer les violations de contrainte unique
                List<IndisponibleHoraire> saved = new java.util.ArrayList<>();
                for (IndisponibleHoraire horaire : horairesACreer) {
                    try {
                        IndisponibleHoraire savedHoraire = indisponibleHoraireRepository.save(horaire);
                        saved.add(savedHoraire);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // Si violation de contrainte unique (UUID ou autre), l'horaire existe déjà
                        log.warn("⚠️ Violation de contrainte unique pour l'horaire sourceId={}, UUID={} - probablement créé par un autre thread, ignoré", 
                                horaire.getSourceId(), horaire.getUuid());
                        // Vérifier si l'horaire existe vraiment
                        Optional<IndisponibleHoraire> existing = indisponibleHoraireRepository
                                .findByTerrainIdAndTypeReservation(terrainId, horaire.getTypeReservation())
                                .stream()
                                .filter(h -> h.getSourceId() != null && h.getSourceId().equals(horaire.getSourceId()))
                                .findFirst();
                        if (existing.isPresent()) {
                            log.info("✅ Horaire existant trouvé pour sourceId={}, UUID={}", 
                                    horaire.getSourceId(), existing.get().getUuid());
                        }
                    } catch (Exception e) {
                        log.error("❌ Erreur lors de la sauvegarde de l'horaire sourceId={}: {}", 
                                horaire.getSourceId(), e.getMessage(), e);
                    }
                }
                
                // ✅ FLUSH : Forcer l'écriture en base pour garantir la persistance
                if (!saved.isEmpty()) {
                    entityManager.flush();
                    
                    log.info("✅ {} horaires indisponibles sauvegardés pour le terrain {}", saved.size(), terrainId);
                    
                    // Publier les événements de création pour chaque horaire
                    if (horaireEventPublisherService != null) {
                        log.info("📤 Publication de {} événements de création pour les nouveaux horaires du terrain {}", 
                                saved.size(), terrainId);
                        for (IndisponibleHoraire horaire : saved) {
                            log.debug("📤 Publication de l'événement pour l'horaire UUID: {}", horaire.getUuid());
                            horaireEventPublisherService.publishCreatedEvent(horaire);
                        }
                        log.info("✅ Tous les événements de création ont été envoyés pour le terrain {}", terrainId);
                    } else {
                        log.error("❌ HoraireEventPublisherService est NULL - les événements Kafka ne seront PAS publiés pour le terrain {}", terrainId);
                    }
                } else {
                    log.info("ℹ️ Aucun nouvel horaire sauvegardé (tous existaient déjà ou ont échoué) pour le terrain {}", terrainId);
                }
            } else {
                log.info("ℹ️ Tous les horaires existaient déjà en base, aucun nouveau à créer pour le terrain {}", terrainId);
            }
        } else {
            log.info("ℹ️ Aucun nouvel horaire indisponible à créer pour le terrain {}", terrainId);
        }

        // ✅ Une fois la synchro Spring terminée, appeler Django pour qu'il se mette à jour
        try {
            djangoSyncService.notifierDjangoSynchronisation(terrainId);
        } catch (Exception e) {
            // On log mais on ne fait pas échouer la transaction de synchro interne
            // (la synchro Django est "best effort")
            System.err.println("Erreur lors de l'appel à Django après synchronisation des horaires : " + e.getMessage());
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
        // ✅ VALIDATION : Vérifier que Kafka est disponible avant de créer un horaire potentiel
        if (!kafkaAvailabilityService.isKafkaAvailable()) {
            throw new IllegalStateException("Impossible de créer un horaire potentiel : Kafka n'est pas démarré ou n'est pas disponible. Veuillez démarrer Kafka avant de créer un horaire.");
        }
        
        TerrainService terrain = terrainServiceRepository.findById(dto.terrainId())
                .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));

        // Générer un UUID si non fourni (pour compatibilité avec l'existant)
        UUID uuid = dto.uuid() != null ? dto.uuid() : UUID.randomUUID();

        IndisponibleHoraire horaire = IndisponibleHoraire.builder()
                .uuid(uuid)
                .terrain(terrain)
                .date(dto.date())
                .heureDebut(dto.heureDebut())
                .heureFin(dto.heureFin())
                .typeReservation(dto.typeReservation())
                .sourceId(dto.sourceId())
                .description(dto.description())
                .build();

        IndisponibleHoraire saved = indisponibleHoraireRepository.save(horaire);
        
        // Publier l'événement de création
        if (horaireEventPublisherService != null) {
            log.info("Publication de l'événement de création pour l'horaire UUID: {}", saved.getUuid());
            horaireEventPublisherService.publishCreatedEvent(saved);
        } else {
            log.warn("HoraireEventPublisherService est null - l'événement ne sera pas publié sur Kafka");
        }
        
        return toDTO(saved);
    }

    /**
     * Met à jour un horaire indisponible
     */
    @Transactional
    public IndisponibleHoraireDTO mettreAJourHoraireIndisponible(Long id, IndisponibleHoraireDTO dto) {
        IndisponibleHoraire horaire = indisponibleHoraireRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Horaire indisponible introuvable"));

        // Mise à jour partielle - uniquement les champs non-null
        if (dto.terrainId() != null) {
            TerrainService terrain = terrainServiceRepository.findById(dto.terrainId())
                    .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));
            horaire.setTerrain(terrain);
        }
        if (dto.date() != null) {
            horaire.setDate(dto.date());
        }
        if (dto.heureDebut() != null) {
            horaire.setHeureDebut(dto.heureDebut());
        }
        if (dto.heureFin() != null) {
            horaire.setHeureFin(dto.heureFin());
        }
        if (dto.typeReservation() != null) {
            horaire.setTypeReservation(dto.typeReservation());
        }
        if (dto.sourceId() != null) {
            horaire.setSourceId(dto.sourceId());
        }
        if (dto.description() != null) {
            horaire.setDescription(dto.description());
        }

        IndisponibleHoraire saved = indisponibleHoraireRepository.save(horaire);
        
        // Publier l'événement de mise à jour
        if (horaireEventPublisherService != null) {
            horaireEventPublisherService.publishUpdatedEvent(saved);
        }
        
        return toDTO(saved);
    }

    /**
     * Supprime un horaire indisponible
     */
    @Transactional
    public void supprimerHoraireIndisponible(Long id) {
        IndisponibleHoraire horaire = indisponibleHoraireRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Horaire indisponible introuvable"));
        
        // Sauvegarder les informations avant suppression pour l'événement
        UUID uuid = horaire.getUuid();
        Long terrainId = horaire.getTerrain() != null ? horaire.getTerrain().getId() : null;
        java.time.LocalDate date = horaire.getDate();
        java.time.LocalTime heureDebut = horaire.getHeureDebut();
        java.time.LocalTime heureFin = horaire.getHeureFin();
        
        // Récupérer le téléphone du propriétaire avant la suppression
        Integer proprietaireTelephone = null;
        if (horaire.getTerrain() != null && horaire.getTerrain().getProprietaire() != null) {
            proprietaireTelephone = horaire.getTerrain().getProprietaire().getTelephone();
        }
        
        indisponibleHoraireRepository.deleteById(id);
        
        // Publier l'événement de suppression
        if (uuid != null && horaireEventPublisherService != null) {
            horaireEventPublisherService.publishDeletedEvent(
                    uuid, 
                    terrainId, 
                    date, 
                    heureDebut, 
                    heureFin,
                    proprietaireTelephone
            );
        }
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
     * ✅ PUBLIE LES ÉVÉNEMENTS KAFKA : Les horaires mis à jour publieront des événements "updated" sur Kafka
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAbonnementModifie(AbonnementModifieEvent event) {
        log.info("📨 Événement AbonnementModifieEvent reçu pour le terrain {} - Déclenchement de la synchronisation avec publication Kafka",
                event.terrainId());
        synchroniserHorairesIndisponibles(event.terrainId());
        log.info("✅ Synchronisation terminée pour le terrain {} - Les événements Kafka (created/updated/deleted) ont été publiés",
                event.terrainId());
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
        log.info("📨 Événement ReservationModifieEvent reçu pour le terrain {} - Déclenchement de la synchronisation", 
                event.terrainId());
        synchroniserHorairesIndisponibles(event.terrainId());
        log.info("✅ Synchronisation terminée pour le terrain {} - Les événements Kafka devraient être publiés", 
                event.terrainId());
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
