package com.matchi.rabbitmq_disabled;

import com.matchi.dto.HoraireSyncEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service pour écouter les événements de synchronisation des horaires
 * provenant de Django via RabbitMQ
 * 
 * ⚠️ FICHIER DÉSACTIVÉ - Ce fichier est dans rabbitmq_disabled et ne devrait pas être utilisé
 * Ce service ne fonctionne que si RabbitMQ est configuré
 */
// @Service // DÉSACTIVÉ - Fichier dans rabbitmq_disabled
// @RequiredArgsConstructor // DÉSACTIVÉ
// @ConditionalOnProperty(name = "spring.rabbitmq.host") // DÉSACTIVÉ
public class DjangoHoraireEventListener {

    private static final Logger log = LoggerFactory.getLogger(DjangoHoraireEventListener.class);

    // DÉSACTIVÉ - Fichier dans rabbitmq_disabled
    // private final IndisponibleHoraireRepository indisponibleHoraireRepository;
    // private final TerrainServiceRepository terrainServiceRepository;

    /**
     * Écoute les événements de synchronisation des horaires provenant de Django
     * et applique les changements de manière idempotente
     * 
     * ⚠️ FICHIER DÉSACTIVÉ - Ce fichier est dans rabbitmq_disabled et ne devrait pas être utilisé
     */
    // @KafkaListener(queues = "${spring.rabbitmq.queue:horaire-sync-queue}") // DÉSACTIVÉ
    // @Transactional // DÉSACTIVÉ
    public void handleHoraireSyncEvent(HoraireSyncEvent event) {
        // DÉSACTIVÉ - Fichier dans rabbitmq_disabled
        log.warn("⚠️ Ce service est désactivé (fichier dans rabbitmq_disabled)");
        // Tout le code ci-dessous est commenté car ce fichier est désactivé
        /*
        log.info("Réception d'un événement de synchronisation: action={}, uuid={}, terrainId={}", 
                event.action(), event.uuid(), event.terrainId());

        try {
            switch (event.action()) {
                case "created":
                    handleCreatedEvent(event);
                    break;
                case "updated":
                    handleUpdatedEvent(event);
                    break;
                case "deleted":
                    handleDeletedEvent(event);
                    break;
                default:
                    log.warn("Action inconnue dans l'événement de synchronisation: {}", event.action());
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement de synchronisation: {}", 
                    e.getMessage(), e);
            throw e; // Rejeter le message pour qu'il soit retraité
        }
        */
    }

    /*
    // Toutes les méthodes ci-dessous sont commentées car ce fichier est désactivé
    
    private void handleCreatedEvent(HoraireSyncEvent event) {
        // Code désactivé
    }

    private void handleUpdatedEvent(HoraireSyncEvent event) {
        // Code désactivé
    }

    private void handleDeletedEvent(HoraireSyncEvent event) {
        // Code désactivé
    }
    */
}
