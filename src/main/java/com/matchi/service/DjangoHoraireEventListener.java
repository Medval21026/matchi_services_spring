package com.matchi.service;

import com.matchi.dto.HoraireSyncEvent;
import com.matchi.dto.HoraireSyncEventRaw;
import com.matchi.model.IndisponibleHoraire;
import com.matchi.model.Proprietaire;
import com.matchi.model.ReservationPonctuelle;
import com.matchi.model.TerrainService;
import com.matchi.repository.IndisponibleHoraireRepository;
import com.matchi.repository.ProprietaireRepository;
import com.matchi.repository.ReservationPonctuelleRepository;
import com.matchi.repository.TerrainServiceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service pour écouter les événements de synchronisation des horaires
 * provenant de Django via Kafka
 * 
 * Ce service ne fonctionne que si Kafka est configuré
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(name = "kafkaListenerContainerFactory")
public class DjangoHoraireEventListener {

    private static final Logger log = LoggerFactory.getLogger(DjangoHoraireEventListener.class);

    private final IndisponibleHoraireRepository indisponibleHoraireRepository;
    private final TerrainServiceRepository terrainServiceRepository;
    private final ProprietaireRepository proprietaireRepository;
    private final ReservationPonctuelleRepository reservationPonctuelleRepository;
    
    @Autowired(required = false)
    private ApplicationContext applicationContext;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("✅ DjangoHoraireEventListener initialisé et prêt à écouter les messages Kafka");
        log.info("📡 Topic: horaire-sync-topic, Group ID: horaire-sync-group");
        log.info("📡 Bootstrap servers: localhost:9094");
        log.info("📡 Container Factory: kafkaListenerContainerFactory");
        
        // Vérifier que l'annotation @KafkaListener est bien présente
        try {
            java.lang.reflect.Method method = this.getClass().getMethod("handleHoraireSyncEvent", 
                    HoraireSyncEvent.class, Acknowledgment.class);
            if (method.isAnnotationPresent(org.springframework.kafka.annotation.KafkaListener.class)) {
                log.info("✅ @KafkaListener détecté sur la méthode handleHoraireSyncEvent");
            } else {
                log.warn("⚠️ @KafkaListener NON détecté sur la méthode handleHoraireSyncEvent");
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification de @KafkaListener: {}", e.getMessage());
        }
    }
    
    /**
     * Vérifier que le listener démarre après le démarrage de l'application
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("🚀 Application prête - vérification du démarrage du listener Kafka...");
        log.info("🚀 Le listener 'django-horaire-listener' devrait démarrer automatiquement");
        log.info("🚀 Vérifiez dans Kafka UI que le consumer group 'horaire-sync-group' apparaît");
        
        // Vérifier si le listener est enregistré et démarré
        try {
            Class<?> registryClass = Class.forName("org.springframework.kafka.config.KafkaListenerEndpointRegistry");
            if (applicationContext != null) {
                try {
                    Object registry = applicationContext.getBean(registryClass);
                    if (registry != null) {
                        log.info("✅ KafkaListenerEndpointRegistry trouvé");
                        
                        // Vérifier si le listener est enregistré
                        java.lang.reflect.Method getListenerContainersMethod = registryClass.getMethod("getListenerContainers");
                        java.util.Collection<?> containers = (java.util.Collection<?>) getListenerContainersMethod.invoke(registry);
                        log.info("📊 Nombre de listeners enregistrés: {}", containers.size());
                        
                        // Vérifier le statut de chaque listener
                        for (Object container : containers) {
                            java.lang.reflect.Method getIdMethod = container.getClass().getMethod("getListenerId");
                            String listenerId = (String) getIdMethod.invoke(container);
                            log.info("📋 Listener trouvé: {}", listenerId);
                            
                            // Vérifier si le container est démarré
                            java.lang.reflect.Method isRunningMethod = container.getClass().getMethod("isRunning");
                            boolean isRunning = (Boolean) isRunningMethod.invoke(container);
                            log.info("   Statut: {}", isRunning ? "✅ Démarré" : "❌ Arrêté");
                            
                            // Si le listener n'est pas démarré, le démarrer
                            if (!isRunning && "django-horaire-listener".equals(listenerId)) {
                                log.warn("⚠️ Le listener 'django-horaire-listener' n'est pas démarré, tentative de démarrage...");
                                try {
                                    java.lang.reflect.Method startMethod = container.getClass().getMethod("start");
                                    startMethod.invoke(container);
                                    log.info("✅ Listener 'django-horaire-listener' démarré manuellement");
                                } catch (Exception e) {
                                    log.error("❌ Erreur lors du démarrage manuel du listener: {}", e.getMessage());
                                }
                            }
                        }
                    } else {
                        log.warn("⚠️ KafkaListenerEndpointRegistry non trouvé dans le contexte");
                    }
                } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
                    log.error("❌ KafkaListenerEndpointRegistry n'est pas disponible - @EnableKafka n'est probablement pas activé");
                    log.error("❌ Cela signifie que les listeners Kafka ne seront pas enregistrés automatiquement");
                    log.error("❌ Vérifiez que @EnableKafka est présent sur une classe de configuration");
                }
            } else {
                log.warn("⚠️ ApplicationContext non disponible");
            }
        } catch (ClassNotFoundException e) {
            log.warn("⚠️ KafkaListenerEndpointRegistry non disponible dans le classpath");
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification du listener: {}", e.getMessage());
            log.debug("❌ Détails de l'erreur: ", e);
        }
    }

    /**
     * Écoute les événements de synchronisation des horaires provenant de Django
     * et applique les changements de manière idempotente
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.horaire-sync:horaire-sync-topic}",
            groupId = "${spring.kafka.consumer.group-id:horaire-sync-group}",
            containerFactory = "kafkaListenerContainerFactory",
            id = "django-horaire-listener",
            autoStartup = "true"
    )
    @Transactional
    public void handleHoraireSyncEvent(HoraireSyncEventRaw rawEvent, Acknowledgment acknowledgment) {
        log.info("📥 ===== RÉCEPTION D'UN ÉVÉNEMENT KAFKA =====");
        
        // ✅ Vérifier que rawEvent n'est pas null
        if (rawEvent == null) {
            log.error("❌ Événement brut est null !");
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            return;
        }
        
        log.info("📥 Action: {}, UUID: {}, TerrainId (initial): {}", 
                rawEvent.action(), rawEvent.uuid(), rawEvent.terrainId());
        log.info("📥 Date: {}, HeureDebut: {}, HeureFin: {}", 
                rawEvent.date(), rawEvent.heureDebut(), rawEvent.heureFin());
        log.info("📥 TypeReservation (raw): {}, SourceId: {}, Description: {}", 
                rawEvent.typeReservation(), rawEvent.sourceId(), rawEvent.description());
        log.info("📥 numTel: {}, proprietaireTelephone: {}", 
                rawEvent.numTel(), rawEvent.proprietaireTelephone());
        log.debug("📥 Événement brut complet: {}", rawEvent);
        
        // ✅ Trouver le terrainId via numTel (client = propriétaire par numéro de téléphone)
        Long terrainId = rawEvent.terrainId();
        Integer numTel = rawEvent.numTel() != null ? rawEvent.numTel() : rawEvent.proprietaireTelephone();
        
        if (numTel != null) {
            log.info("🔍 Recherche du terrain via numTel: {}", numTel);
            try {
                // Trouver le propriétaire par numéro de téléphone
                Optional<Proprietaire> proprietaireOpt = 
                        proprietaireRepository.findByTelephone(numTel);
                
                if (proprietaireOpt.isPresent()) {
                    Proprietaire proprietaire = proprietaireOpt.get();
                    log.info("✅ Propriétaire trouvé: ID={}, Nom={} {}", 
                            proprietaire.getId(), proprietaire.getNom(), proprietaire.getPrenom());
                    
                    // Trouver le terrain associé à ce propriétaire
                    List<TerrainService> terrains = 
                            terrainServiceRepository.findByProprietaireId(proprietaire.getId());
                    
                    if (!terrains.isEmpty()) {
                        // Prendre le premier terrain (un propriétaire a normalement un seul terrain)
                        TerrainService terrain = terrains.get(0);
                        terrainId = terrain.getId();
                        log.info("✅ Terrain trouvé via numTel: terrainId={}, Nom={}", 
                                terrainId, terrain.getNom());
                    } else {
                        log.warn("⚠️ Aucun terrain trouvé pour le propriétaire ID={}", proprietaire.getId());
                    }
                } else {
                    log.warn("⚠️ Aucun propriétaire trouvé avec numTel={}", numTel);
                }
            } catch (Exception e) {
                log.error("❌ Erreur lors de la recherche du terrain via numTel: {}", e.getMessage(), e);
            }
        } else {
            log.warn("⚠️ numTel et proprietaireTelephone sont null, utilisation du terrainId du message: {}", terrainId);
        }
        
        // ✅ Créer un nouveau rawEvent avec le terrainId trouvé
        // Utiliser le terrainId trouvé ou celui du message
        // @JsonAlias gère automatiquement les formats camelCase et snake_case
        Long finalTerrainId = terrainId != null ? terrainId : rawEvent.terrainId();
        
        HoraireSyncEventRaw rawEventWithTerrainId = new HoraireSyncEventRaw(
                rawEvent.uuid(),
                rawEvent.action(),
                finalTerrainId, // Utiliser le terrainId trouvé ou celui du message
                rawEvent.date(), // @JsonAlias gère automatiquement date et date_indisponibilite
                rawEvent.heureDebut(), // @JsonAlias gère automatiquement heureDebut et heure_debut
                rawEvent.heureFin(), // @JsonAlias gère automatiquement heureFin et heure_fin
                rawEvent.typeReservation(),
                rawEvent.sourceId(),
                rawEvent.description(),
                rawEvent.proprietaireTelephone(),
                rawEvent.numTel(),
                rawEvent.source(),
                rawEvent.id_jour(),
                rawEvent.joueur_numTel(),
                rawEvent.prix()
        );
        
        // ✅ Convertir le DTO brut en HoraireSyncEvent
        HoraireSyncEvent event;
        try {
            event = rawEventWithTerrainId.toHoraireSyncEvent();
            log.info("📥 Événement converti avec terrainId={}", event.terrainId());
            log.debug("📥 Événement converti complet: {}", event);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la conversion de l'événement brut: {}", e.getMessage(), e);
            if (acknowledgment != null) {
                acknowledgment.acknowledge(); // Accepter pour ne pas bloquer
            }
            return;
        }

        try {
            // ✅ Vérifier que l'événement est valide
            if (event.uuid() == null) {
                log.error("❌ Événement sans UUID, ignoré");
                if (acknowledgment != null) {
                    acknowledgment.acknowledge(); // Accepter pour ne pas bloquer
                }
                return;
            }
            
            // Pour "deleted", terrainId peut être null - on utilise uuid pour trouver
            if (event.terrainId() == null && !"deleted".equals(event.action())) {
                log.error("❌ Événement sans terrainId (même après recherche), ignoré");
                if (acknowledgment != null) {
                    acknowledgment.acknowledge(); // Accepter pour ne pas bloquer
                }
                return;
            }
            
            // Normaliser l'action pour gérer DELETE, CREATE, UPDATE (majuscules)
            String action = event.action();
            if (action != null) {
                String actionLower = action.toLowerCase();
                if ("delete".equals(actionLower) || "deleted".equals(actionLower)) {
                    action = "deleted";
                } else if ("create".equals(actionLower) || "created".equals(actionLower)) {
                    action = "created";
                } else if ("update".equals(actionLower) || "updated".equals(actionLower)) {
                    action = "updated";
                }
            }
            
            switch (action) {
                case "created":
                    handleCreatedEvent(rawEventWithTerrainId, event);
                    break;
                case "updated":
                    handleUpdatedEvent(event);
                    break;
                case "deleted":
                    handleDeletedEvent(event);
                    break;
                default:
                    log.warn("⚠️ Action inconnue dans l'événement de synchronisation: {}", event.action());
            }
            
            // Confirmer la réception du message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.debug("✅ Message Kafka confirmé (acknowledged)");
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de l'événement de synchronisation: {}", 
                    e.getMessage(), e);
            log.error("❌ Type d'erreur: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("❌ Cause: {}", e.getCause().getMessage());
            }
            // Ne pas confirmer le message en cas d'erreur pour qu'il soit retraité
            throw e; // Rejeter le message pour qu'il soit retraité
        }
    }

    /**
     * Convertit une chaîne en TypeReservation enum
     * Gère les cas où le type arrive comme chaîne depuis Django
     */
    private com.matchi.model.TypeReservation parseTypeReservation(Object typeValue) {
        if (typeValue == null) {
            return null;
        }
        
        // Si c'est déjà un enum, le retourner tel quel
        if (typeValue instanceof com.matchi.model.TypeReservation) {
            return (com.matchi.model.TypeReservation) typeValue;
        }
        
        // Si c'est une chaîne, la convertir en enum
        if (typeValue instanceof String) {
            String typeStr = ((String) typeValue).trim().toUpperCase();
            try {
                return com.matchi.model.TypeReservation.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Type de réservation inconnu: '{}', retour null", typeStr);
                return null;
            }
        }
        
        log.warn("⚠️ Type de réservation dans un format non supporté: {} (type: {}), retour null", 
                typeValue, typeValue.getClass().getName());
        return null;
    }
    
    /**
     * Gère la création d'un horaire depuis Django
     * Crée aussi une ReservationPonctuelle avec les informations du message
     * Vérifie l'idempotence pour éviter les doublons
     */
    private void handleCreatedEvent(HoraireSyncEventRaw rawEvent, HoraireSyncEvent event) {
        log.info("🔄 Traitement d'un événement 'created' depuis Django: uuid={}, terrainId={}", 
                event.uuid(), event.terrainId());
        
        // ✅ RÈGLE SPÉCIALE POUR DJANGO : Django ne crée jamais d'abonnements
        // Si typeReservation est null, c'est forcément une réservation ponctuelle depuis Django
        com.matchi.model.TypeReservation typeReservation = parseTypeReservation(event.typeReservation());
        
        // ✅ Si typeReservation est null, forcer RESERVATION_PONCTUELLE (Django ne crée que des réservations ponctuelles)
        if (typeReservation == null) {
            typeReservation = com.matchi.model.TypeReservation.RESERVATION_PONCTUELLE;
            log.info("✅ [CREATE] TypeReservation était null, forcé à RESERVATION_PONCTUELLE (message depuis Django)");
        }
        
        log.info("🔍 [CREATE] TypeReservation final: {}", typeReservation);
        
        // Vérifier si l'horaire existe déjà (idempotence) - vérification robuste
        Optional<IndisponibleHoraire> existing = indisponibleHoraireRepository.findByUuid(event.uuid());
        if (existing.isPresent()) {
            log.info("⚠️ Horaire avec UUID {} existe déjà (id: {}), ignoré (idempotence)", 
                    event.uuid(), existing.get().getId());
            return;
        }
        
        // Vérification supplémentaire : vérifier par sourceId et type pour éviter les doublons
        // Utiliser le type converti au lieu de event.typeReservation()
        if (event.sourceId() != null && typeReservation != null) {
            List<IndisponibleHoraire> existingBySource = indisponibleHoraireRepository
                    .findByTerrainIdAndTypeReservation(event.terrainId(), typeReservation)
                    .stream()
                    .filter(h -> h.getSourceId() != null && h.getSourceId().equals(event.sourceId()))
                    .filter(h -> h.getDate() != null && h.getDate().equals(event.date()))
                    .filter(h -> h.getHeureDebut() != null && h.getHeureDebut().equals(event.heureDebut()))
                    .filter(h -> h.getHeureFin() != null && h.getHeureFin().equals(event.heureFin()))
                    .collect(java.util.stream.Collectors.toList());
            
            if (!existingBySource.isEmpty()) {
                log.warn("⚠️ Horaire similaire existe déjà pour sourceId={}, type={}, terrainId={}, ignoré (idempotence)", 
                        event.sourceId(), typeReservation, event.terrainId());
                return;
            }
        }

        // Vérifier que le terrain existe
        TerrainService terrain = terrainServiceRepository.findById(event.terrainId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Terrain non trouvé pour l'événement de synchronisation: " + event.terrainId()));
        
        // ✅ RÈGLE SPÉCIALE POUR DJANGO : Toujours utiliser "Réservation ponctuelle" comme description
        // Django ne crée que des réservations ponctuelles
        String description = event.description();
        if (description == null || description.trim().isEmpty()) {
            // Pour les messages Django, toujours utiliser "Réservation ponctuelle"
            description = "Réservation ponctuelle";
            log.info("✅ [CREATE] Description complétée: '{}' (message depuis Django)", description);
        } else {
            log.debug("✅ [CREATE] Description fournie dans l'événement: '{}'", description);
        }

        // ✅ Créer la ReservationPonctuelle avec les informations du message Django
        Long reservationId = null;
        if (rawEvent.joueur_numTel() != null || rawEvent.prix() != null) {
            try {
                log.info("📝 Création d'une ReservationPonctuelle depuis Django: joueur_numTel={}, prix={}", 
                        rawEvent.joueur_numTel(), rawEvent.prix());
                
                ReservationPonctuelle reservation = new ReservationPonctuelle();
                reservation.setDate(event.date());
                reservation.setHeureDebut(event.heureDebut());
                reservation.setHeureFin(event.heureFin());
                reservation.setTerrain(terrain);
                
                // Convertir joueur_numTel (String) en Integer pour clientTelephone
                if (rawEvent.joueur_numTel() != null && !rawEvent.joueur_numTel().trim().isEmpty()) {
                    try {
                        Integer joueurTel = Integer.parseInt(rawEvent.joueur_numTel().trim());
                        reservation.setClientTelephone(joueurTel);
                        log.info("✅ Numéro de téléphone du joueur converti: {}", joueurTel);
                    } catch (NumberFormatException e) {
                        log.warn("⚠️ Impossible de convertir joueur_numTel '{}' en Integer: {}", 
                                rawEvent.joueur_numTel(), e.getMessage());
                    }
                }
                
                // Convertir prix (Double) en BigDecimal
                if (rawEvent.prix() != null) {
                    reservation.setPrix(java.math.BigDecimal.valueOf(rawEvent.prix()));
                    log.info("✅ Prix converti: {}", reservation.getPrix());
                }
                
                // Sauvegarder la réservation
                ReservationPonctuelle savedReservation = reservationPonctuelleRepository.save(reservation);
                reservationId = savedReservation.getId();
                log.info("✅ ReservationPonctuelle créée: id={}, date={}, heureDebut={}, heureFin={}, prix={}, clientTelephone={}", 
                        reservationId, savedReservation.getDate(), savedReservation.getHeureDebut(), 
                        savedReservation.getHeureFin(), savedReservation.getPrix(), savedReservation.getClientTelephone());
            } catch (Exception e) {
                log.error("❌ Erreur lors de la création de la ReservationPonctuelle depuis Django: {}", e.getMessage(), e);
                // Ne pas bloquer la création de l'IndisponibleHoraire si la réservation échoue
            }
        }
        
        // Utiliser l'ID de la réservation créée comme sourceId si sourceId n'était pas fourni
        Long finalSourceId = event.sourceId() != null ? event.sourceId() : reservationId;
        
        // Créer le nouvel horaire
        IndisponibleHoraire horaire = IndisponibleHoraire.builder()
                .uuid(event.uuid())
                .terrain(terrain)
                .date(event.date())
                .heureDebut(event.heureDebut())
                .heureFin(event.heureFin())
                .typeReservation(typeReservation) // Utiliser le type converti
                .sourceId(finalSourceId) // Utiliser l'ID de la réservation créée ou celui fourni
                .description(description) // Utiliser toujours la description complétée
                .build();

        try {
            indisponibleHoraireRepository.save(horaire);
            log.info("✅ Horaire créé depuis Django: uuid={}, terrainId={}, sourceId={}, type={}, description={}", 
                    event.uuid(), event.terrainId(), finalSourceId, typeReservation, description);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Si violation de contrainte unique (UUID), l'horaire existe déjà
            log.warn("⚠️ Violation de contrainte unique pour UUID {} - l'horaire existe déjà, ignoré (idempotence)", 
                    event.uuid());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'horaire depuis Django: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Gère la mise à jour d'un horaire depuis Django
     */
    private void handleUpdatedEvent(HoraireSyncEvent event) {
        // Trouver l'horaire par UUID
        IndisponibleHoraire horaire = indisponibleHoraireRepository.findByUuid(event.uuid())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Horaire non trouvé pour la mise à jour: " + event.uuid()));

        // Vérifier que le terrain existe
        TerrainService terrain = terrainServiceRepository.findById(event.terrainId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Terrain non trouvé pour l'événement de synchronisation: " + event.terrainId()));

        // ✅ RÈGLE SPÉCIALE POUR DJANGO : Django ne crée jamais d'abonnements
        // Si typeReservation est null, c'est forcément une réservation ponctuelle depuis Django
        com.matchi.model.TypeReservation typeReservation = parseTypeReservation(event.typeReservation());
        
        // ✅ Si typeReservation est null, forcer RESERVATION_PONCTUELLE (Django ne crée que des réservations ponctuelles)
        if (typeReservation == null) {
            typeReservation = com.matchi.model.TypeReservation.RESERVATION_PONCTUELLE;
            log.info("✅ [UPDATE] TypeReservation était null, forcé à RESERVATION_PONCTUELLE (message depuis Django)");
        }
        
        log.info("🔍 [UPDATE] TypeReservation final: {}", typeReservation);
        
        // ✅ RÈGLE SPÉCIALE POUR DJANGO : Toujours utiliser "Réservation ponctuelle" comme description
        // Django ne crée que des réservations ponctuelles
        String description = event.description();
        if (description == null || description.trim().isEmpty()) {
            // Pour les messages Django, toujours utiliser "Réservation ponctuelle"
            description = "Réservation ponctuelle";
            log.info("✅ [UPDATE] Description complétée: '{}' (message depuis Django)", description);
        } else {
            log.debug("✅ [UPDATE] Description fournie dans l'événement: '{}'", description);
        }

        // Mettre à jour les champs
        horaire.setTerrain(terrain);
        horaire.setDate(event.date());
        horaire.setHeureDebut(event.heureDebut());
        horaire.setHeureFin(event.heureFin());
        horaire.setTypeReservation(typeReservation); // Utiliser le type converti
        horaire.setSourceId(event.sourceId());
        horaire.setDescription(description); // Utiliser toujours la description complétée

        indisponibleHoraireRepository.save(horaire);
        log.info("✅ Horaire mis à jour depuis Django: uuid={}, terrainId={}, sourceId={}, type={}, description={}", 
                event.uuid(), event.terrainId(), event.sourceId(), typeReservation, description);
    }

    /**
     * Gère la suppression d'un horaire depuis Django
     * Utilise uniquement l'UUID pour trouver l'horaire (même si tous les autres champs sont null)
     */
    private void handleDeletedEvent(HoraireSyncEvent event) {
        log.info("🗑️ Traitement d'un événement 'deleted' depuis Django: uuid={}", event.uuid());
        
        // Trouver l'horaire par UUID (même si tous les autres champs sont null)
        Optional<IndisponibleHoraire> horaireOpt = indisponibleHoraireRepository.findByUuid(event.uuid());
        
        if (horaireOpt.isPresent()) {
            IndisponibleHoraire horaire = horaireOpt.get();
            
            // Si l'horaire a un sourceId et que c'est une réservation ponctuelle, supprimer aussi la réservation
            if (horaire.getSourceId() != null && 
                horaire.getTypeReservation() == com.matchi.model.TypeReservation.RESERVATION_PONCTUELLE) {
                try {
                    Optional<ReservationPonctuelle> reservationOpt = 
                            reservationPonctuelleRepository.findById(horaire.getSourceId());
                    if (reservationOpt.isPresent()) {
                        reservationPonctuelleRepository.delete(reservationOpt.get());
                        log.info("✅ ReservationPonctuelle supprimée: id={}", horaire.getSourceId());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Erreur lors de la suppression de la ReservationPonctuelle (id={}): {}", 
                            horaire.getSourceId(), e.getMessage());
                    // Continuer quand même avec la suppression de l'horaire
                }
            }
            
            // Supprimer l'horaire
            indisponibleHoraireRepository.delete(horaire);
            log.info("✅ Horaire supprimé depuis Django: uuid={}, id={}, sourceId={}", 
                    event.uuid(), horaire.getId(), horaire.getSourceId());
        } else {
            log.debug("⚠️ Horaire avec UUID {} n'existe pas, suppression ignorée (idempotence)", event.uuid());
        }
    }
}
