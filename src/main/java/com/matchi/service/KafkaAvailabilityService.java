package com.matchi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Service pour vérifier si Kafka est disponible et démarré
 */
@Service
public class KafkaAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAvailabilityService.class);

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    /**
     * Vérifie si Kafka est disponible en vérifiant si le bean kafkaTemplate existe
     * ET si on peut réellement se connecter au broker Kafka
     * @return true si Kafka est disponible et accessible, false sinon
     */
    public boolean isKafkaAvailable() {
        if (applicationContext == null) {
            log.warn("⚠️ ApplicationContext non disponible - Kafka considéré comme non disponible");
            return false;
        }

        try {
            // Vérifier si le bean kafkaTemplate existe
            if (!applicationContext.containsBean("kafkaTemplate")) {
                log.warn("⚠️ Kafka n'est pas disponible - kafkaTemplate bean non trouvé");
                return false;
            }
            
            Object kafkaTemplate = applicationContext.getBean("kafkaTemplate");
            if (kafkaTemplate == null) {
                log.warn("⚠️ Kafka n'est pas disponible - kafkaTemplate est null");
                return false;
            }
            
            // ✅ VÉRIFICATION RÉELLE : Tester si on peut réellement se connecter au broker Kafka
            // En utilisant la réflexion pour tester une opération qui nécessite une connexion
            try {
                java.lang.reflect.Method[] methods = kafkaTemplate.getClass().getMethods();
                
                // ✅ TEST DE CONNEXION : Essayer d'obtenir les partitions d'un topic
                // Cette opération nécessite une connexion au broker Kafka
                // Si Kafka n'est pas démarré, cette opération échouera avec une erreur de connexion
                java.lang.reflect.Method partitionsForMethod = null;
                for (java.lang.reflect.Method method : methods) {
                    if ("partitionsFor".equals(method.getName()) && method.getParameterCount() == 1) {
                        partitionsForMethod = method;
                        break;
                    }
                }
                
                if (partitionsForMethod != null) {
                    try {
                        // Cette opération nécessite une connexion au broker Kafka
                        // Si Kafka n'est pas démarré, cela lèvera une exception de connexion
                        partitionsForMethod.invoke(kafkaTemplate, "__kafka_availability_test_topic__");
                        log.debug("✅ Kafka est disponible et accessible - connexion au broker réussie");
                        return true;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause != null) {
                            String causeMessage = cause.getMessage();
                            String causeClassName = cause.getClass().getName();
                            
                            // Si c'est une erreur de connexion, Kafka n'est pas disponible
                            if (causeMessage != null && (
                                causeMessage.contains("Connection refused") ||
                                causeMessage.contains("Unable to connect") ||
                                causeMessage.contains("Broker may not be available") ||
                                causeMessage.contains("Timeout") ||
                                causeMessage.contains("NetworkException") ||
                                causeMessage.contains("java.net.ConnectException") ||
                                causeMessage.contains("org.apache.kafka.common.errors.TimeoutException")
                            ) || causeClassName.contains("ConnectException") ||
                               causeClassName.contains("TimeoutException") ||
                               causeClassName.contains("NetworkException")) {
                                log.warn("⚠️ Kafka n'est pas disponible - impossible de se connecter au broker: {} ({})", 
                                        causeMessage != null ? causeMessage : causeClassName, causeClassName);
                                return false;
                            }
                        }
                        // Si c'est une autre erreur (comme topic inexistant), c'est OK, Kafka est accessible
                        log.debug("✅ Kafka est accessible (erreur attendue pour topic de test): {}", 
                                cause != null ? cause.getMessage() : e.getMessage());
                        return true;
                    }
                } else {
                    // Si la méthode partitionsFor n'existe pas, on ne peut pas tester la connexion
                    // Par sécurité, on considère que Kafka n'est pas disponible
                    log.warn("⚠️ Kafka n'est pas disponible - impossible de tester la connexion (méthode partitionsFor non trouvée)");
                    return false;
                }
                
            } catch (Exception e) {
                // Si on ne peut pas tester la connexion, on considère que Kafka n'est pas disponible
                log.warn("⚠️ Impossible de tester la connexion Kafka: {}", e.getMessage());
                return false;
            }
            
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            log.warn("⚠️ Kafka n'est pas disponible - kafkaTemplate bean non défini");
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification de la disponibilité de Kafka: {}", e.getMessage());
            return false;
        }
    }
}
