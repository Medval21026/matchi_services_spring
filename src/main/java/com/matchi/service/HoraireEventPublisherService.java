package com.matchi.service;

import com.matchi.dto.HoraireSyncEvent;
import com.matchi.model.IndisponibleHoraire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Service pour publier les événements de synchronisation des horaires
 * sur le broker Kafka
 * 
 * Ce service ne fonctionne que si Kafka est configuré
 */
@Service
@ConditionalOnBean(name = "kafkaTemplate")
public class HoraireEventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(HoraireEventPublisherService.class);

    @Autowired(required = false)
    @Qualifier("kafkaTemplate")
    private Object kafkaTemplate;
    
    @PostConstruct
    public void init() {
        if (kafkaTemplate != null) {
            log.info("✅ HoraireEventPublisherService initialisé avec KafkaTemplate. Topic: {}", topic);
            log.info("📋 Type du kafkaTemplate: {}", kafkaTemplate.getClass().getName());
        } else {
            log.warn("⚠️ HoraireEventPublisherService initialisé SANS KafkaTemplate - les événements ne seront pas publiés");
        }
    }

    @Value("${spring.kafka.topic.horaire-sync:horaire-sync-topic}")
    private String topic;

    /**
     * Extrait le numéro de téléphone du propriétaire depuis le terrain
     */
    private Integer getProprietaireTelephone(IndisponibleHoraire horaire) {
        if (horaire.getTerrain() != null && horaire.getTerrain().getProprietaire() != null) {
            return horaire.getTerrain().getProprietaire().getTelephone();
        }
        return null;
    }

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
                getProprietaireTelephone(horaire)
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
                getProprietaireTelephone(horaire)
        );

        publishEvent(event);
    }

    /**
     * Publie un événement de suppression d'horaire
     */
    public void publishDeletedEvent(UUID uuid, Long terrainId, LocalDate date, 
                                     LocalTime heureDebut, LocalTime heureFin, Integer proprietaireTelephone) {
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
                proprietaireTelephone
        );

        publishEvent(event);
    }

    /**
     * Publie l'événement sur le topic Kafka
     */
    private void publishEvent(HoraireSyncEvent event) {
        if (kafkaTemplate == null) {
            log.warn("Kafka non configuré, événement non publié: action={}, uuid={}", 
                    event.action(), event.uuid());
            return;
        }
        
        try {
            log.info("📤 Publication d'événement de synchronisation sur Kafka: action={}, uuid={}, terrainId={}, topic={}", 
                    event.action(), event.uuid(), event.terrainId(), topic);
            log.debug("📤 Contenu de l'événement: {}", event);
            
            // Vérifier que l'événement n'est pas null
            if (event == null) {
                log.error("❌ L'événement est NULL - impossible de publier");
                return;
            }
            
            // Trouver la méthode send avec la bonne signature
            // KafkaTemplate a plusieurs surcharges de send(), on cherche celle qui accepte (String, String, Object)
            java.lang.reflect.Method sendMethod = null;
            java.lang.reflect.Method[] methods = kafkaTemplate.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                if ("send".equals(method.getName())) {
                    java.lang.Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 3 && 
                        paramTypes[0] == String.class && 
                        paramTypes[1] == String.class &&
                        paramTypes[2] == Object.class) {
                        sendMethod = method;
                        break;
                    }
                }
            }
            
            // Si on ne trouve pas la méthode avec 3 paramètres, essayer avec 2 paramètres (topic, value)
            if (sendMethod == null) {
                for (java.lang.reflect.Method method : methods) {
                    if ("send".equals(method.getName())) {
                        java.lang.Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length == 2 && 
                            paramTypes[0] == String.class && 
                            paramTypes[1] == Object.class) {
                            sendMethod = method;
                            log.debug("📤 Utilisation de send(topic, value) au lieu de send(topic, key, value)");
                            break;
                        }
                    }
                }
            }
            
            if (sendMethod == null) {
                log.error("❌ Méthode send() non trouvée dans KafkaTemplate");
                return;
            }
            
            log.debug("📤 Envoi du message sur le topic: {} avec la méthode: {}", topic, sendMethod);
            Object result;
            if (sendMethod.getParameterCount() == 3) {
                result = sendMethod.invoke(kafkaTemplate, topic, event.uuid().toString(), event);
            } else {
                // Utiliser send(topic, value) si la méthode avec key n'existe pas
                result = sendMethod.invoke(kafkaTemplate, topic, event);
            }
            log.debug("📤 Résultat de l'envoi: {}", result);
            
            // Attendre la confirmation pour s'assurer que le message est bien envoyé
            if (result != null) {
                try {
                    java.lang.reflect.Method getMethod = result.getClass().getMethod("get");
                    Object sendResult = getMethod.invoke(result);
                    log.info("✅ Événement publié avec succès sur Kafka. Résultat: {}", sendResult);
                } catch (Exception e) {
                    log.warn("⚠️ Impossible d'obtenir le résultat synchrone, envoi asynchrone: {}", e.getMessage());
                    log.info("✅ Événement envoyé sur Kafka (mode asynchrone)");
                }
            } else {
                log.warn("⚠️ Résultat de l'envoi est NULL");
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de la publication de l'événement de synchronisation sur Kafka: {}", 
                    e.getMessage(), e);
            log.error("❌ Type d'erreur: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("❌ Cause: {}", e.getCause().getMessage());
            }
            e.printStackTrace();
            // On ne fait pas échouer la transaction principale si la publication échoue
        }
    }
}
