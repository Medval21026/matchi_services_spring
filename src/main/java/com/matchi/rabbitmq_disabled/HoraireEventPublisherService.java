package com.matchi.rabbitmq_disabled;

import com.matchi.dto.HoraireSyncEvent;
import com.matchi.model.IndisponibleHoraire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Service pour publier les événements de synchronisation des horaires
 * sur le broker RabbitMQ
 * 
 * Ce service ne fonctionne que si RabbitMQ est configuré
 */
@Service
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
public class HoraireEventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(HoraireEventPublisherService.class);

    @Autowired(required = false)
    private Object rabbitTemplate; // Utilisé comme RabbitTemplate si disponible

    @Value("${spring.rabbitmq.exchange:horaire-sync-exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.routing-key:horaire-sync}")
    private String routingKey;

    /**
     * Publie un événement de création d'horaire
     */
    public void publishCreatedEvent(IndisponibleHoraire horaire) {
        if (horaire.getUuid() == null) {
            log.warn("Tentative de publication d'un événement pour un horaire sans UUID: {}", horaire.getId());
            return;
        }

        HoraireSyncEvent event = new HoraireSyncEvent(
                horaire.getUuid(),
                "created",
                horaire.getTerrain() != null ? horaire.getTerrain().getId() : null,
                horaire.getDate(),
                horaire.getHeureDebut(),
                horaire.getHeureFin(),
                horaire.getTypeReservation(),
                horaire.getSourceId(),
                horaire.getDescription(),
                null // proprietaireTelephone - non utilisé dans ce fichier désactivé
        );

        publishEvent(event);
    }

    /**
     * Publie un événement de mise à jour d'horaire
     */
    public void publishUpdatedEvent(IndisponibleHoraire horaire) {
        if (horaire.getUuid() == null) {
            log.warn("Tentative de publication d'un événement pour un horaire sans UUID: {}", horaire.getId());
            return;
        }

        HoraireSyncEvent event = new HoraireSyncEvent(
                horaire.getUuid(),
                "updated",
                horaire.getTerrain() != null ? horaire.getTerrain().getId() : null,
                horaire.getDate(),
                horaire.getHeureDebut(),
                horaire.getHeureFin(),
                horaire.getTypeReservation(),
                horaire.getSourceId(),
                horaire.getDescription(),
                null // proprietaireTelephone - non utilisé dans ce fichier désactivé
        );

        publishEvent(event);
    }

    /**
     * Publie un événement de suppression d'horaire
     */
    public void publishDeletedEvent(UUID uuid, Long terrainId, LocalDate date, 
                                     LocalTime heureDebut, LocalTime heureFin) {
        HoraireSyncEvent event = new HoraireSyncEvent(
                uuid,
                "deleted",
                terrainId,
                date,
                heureDebut,
                heureFin,
                null,
                null,
                null,
                null // proprietaireTelephone - non utilisé dans ce fichier désactivé
        );

        publishEvent(event);
    }

    /**
     * Publie l'événement sur le broker RabbitMQ
     */
    private void publishEvent(HoraireSyncEvent event) {
        if (rabbitTemplate == null) {
            log.debug("RabbitMQ non configuré, événement non publié: action={}, uuid={}", 
                    event.action(), event.uuid());
            return;
        }
        
        try {
            log.info("Publication d'événement de synchronisation: action={}, uuid={}, terrainId={}", 
                    event.action(), event.uuid(), event.terrainId());
            // Utilisation de la réflexion pour éviter la dépendance directe
            java.lang.reflect.Method method = rabbitTemplate.getClass()
                    .getMethod("convertAndSend", String.class, String.class, Object.class);
            method.invoke(rabbitTemplate, exchange, routingKey, event);
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement de synchronisation: {}", 
                    e.getMessage(), e);
            // On ne fait pas échouer la transaction principale si la publication échoue
        }
    }
}
