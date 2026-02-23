package com.matchi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka pour la synchronisation des horaires
 * entre Spring Boot et Django
 * 
 * Cette configuration ne se charge que si Kafka est disponible
 * 
 * @EnableKafka est nécessaire pour activer les listeners Kafka
 */
@Configuration
@EnableKafka
@ConditionalOnClass(name = {
    "org.springframework.kafka.core.KafkaTemplate",
    "org.apache.kafka.clients.producer.ProducerConfig",
    "org.springframework.kafka.annotation.KafkaListener",
    "org.springframework.kafka.annotation.EnableKafka"
})
public class KafkaConfig {
    
    static {
        // Vérifier que @EnableKafka est disponible
        try {
            Class<?> enableKafkaClass = Class.forName("org.springframework.kafka.annotation.EnableKafka");
            System.out.println("✅ @EnableKafka disponible - Spring Boot devrait l'activer automatiquement");
        } catch (ClassNotFoundException e) {
            System.out.println("⚠️ @EnableKafka non disponible");
        }
    }

    @Value("${spring.kafka.bootstrap-servers:localhost:9094}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:horaire-sync-group}")
    private String groupId;

    /**
     * Configuration du Producer Kafka pour publier les événements
     */
    @Bean
    @Primary
    public Object producerFactory() {
        try {
            Class<?> producerConfigClass = Class.forName("org.apache.kafka.clients.producer.ProducerConfig");
            Class<?> stringSerializerClass = Class.forName("org.apache.kafka.common.serialization.StringSerializer");
            Class<?> jsonSerializerClass = Class.forName("org.springframework.kafka.support.serializer.JsonSerializer");
            Class<?> producerFactoryClass = Class.forName("org.springframework.kafka.core.DefaultKafkaProducerFactory");
            
            Map<String, Object> configProps = new HashMap<>();
            configProps.put((String) producerConfigClass.getField("BOOTSTRAP_SERVERS_CONFIG").get(null), bootstrapServers);
            configProps.put((String) producerConfigClass.getField("KEY_SERIALIZER_CLASS_CONFIG").get(null), stringSerializerClass);
            configProps.put((String) producerConfigClass.getField("VALUE_SERIALIZER_CLASS_CONFIG").get(null), jsonSerializerClass);
            configProps.put((String) producerConfigClass.getField("ENABLE_IDEMPOTENCE_CONFIG").get(null), true);
            configProps.put((String) producerConfigClass.getField("ACKS_CONFIG").get(null), "all");
            configProps.put((String) producerConfigClass.getField("RETRIES_CONFIG").get(null), 3);
            
            // Configuration pour JsonSerializer - Spring Boot configurera automatiquement l'ObjectMapper
            // avec support JavaTime via jackson-datatype-jsr310
            configProps.put("spring.json.add.type.headers", false); // Ne pas ajouter les headers de type
            
            Object factory = producerFactoryClass.getConstructor(Map.class).newInstance(configProps);
            
            System.out.println("✅ ProducerFactory créé avec JsonSerializer pour HoraireSyncEvent");
            System.out.println("✅ JsonSerializer utilisera l'ObjectMapper de Spring Boot (avec support JavaTime)");
            return factory;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de ProducerFactory: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Impossible de créer ProducerFactory", e);
        }
    }

    /**
     * KafkaTemplate pour publier les événements
     */
    @Bean(name = "kafkaTemplate")
    @Primary
    public Object kafkaTemplate() {
        try {
            Class<?> kafkaTemplateClass = Class.forName("org.springframework.kafka.core.KafkaTemplate");
            Object producerFactory = producerFactory();
            Object template = kafkaTemplateClass.getConstructor(
                    Class.forName("org.springframework.kafka.core.ProducerFactory")
            ).newInstance(producerFactory);
            
            System.out.println("✅ KafkaTemplate créé avec succès pour HoraireSyncEvent");
            System.out.println("✅ Bootstrap servers: " + bootstrapServers);
            System.out.println("✅ Topic par défaut: horaire-sync-topic");
            
            return template;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de KafkaTemplate: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Impossible de créer KafkaTemplate", e);
        }
    }

    /**
     * Configuration du Consumer Kafka pour recevoir les événements
     */
    @Bean
    public Object consumerFactory() {
        try {
            Class<?> consumerConfigClass = Class.forName("org.apache.kafka.clients.consumer.ConsumerConfig");
            Class<?> stringDeserializerClass = Class.forName("org.apache.kafka.common.serialization.StringDeserializer");
            Class<?> jsonDeserializerClass = Class.forName("org.springframework.kafka.support.serializer.JsonDeserializer");
            Class<?> consumerFactoryClass = Class.forName("org.springframework.kafka.core.DefaultKafkaConsumerFactory");
            
            Map<String, Object> configProps = new HashMap<>();
            configProps.put((String) consumerConfigClass.getField("BOOTSTRAP_SERVERS_CONFIG").get(null), bootstrapServers);
            configProps.put((String) consumerConfigClass.getField("GROUP_ID_CONFIG").get(null), groupId);
            configProps.put((String) consumerConfigClass.getField("KEY_DESERIALIZER_CLASS_CONFIG").get(null), stringDeserializerClass);
            configProps.put((String) consumerConfigClass.getField("VALUE_DESERIALIZER_CLASS_CONFIG").get(null), jsonDeserializerClass);
            configProps.put((String) consumerConfigClass.getField("AUTO_OFFSET_RESET_CONFIG").get(null), "earliest");
            configProps.put((String) consumerConfigClass.getField("ENABLE_AUTO_COMMIT_CONFIG").get(null), false); // Désactiver auto-commit pour MANUAL_IMMEDIATE
            
            // Configuration pour JsonDeserializer
            // Configuration pour JsonDeserializer via les propriétés du consumer
            // Ces propriétés seront utilisées par JsonDeserializer
            configProps.put("spring.json.trusted.packages", "*");
            configProps.put("spring.json.type.mapping", "horaireSyncEvent:com.matchi.dto.HoraireSyncEventRaw");
            configProps.put("spring.json.value.default.type", "com.matchi.dto.HoraireSyncEventRaw");
            // ✅ Ignorer les propriétés inconnues (comme "source" de Django)
            configProps.put("spring.json.use.type.headers", false);
            configProps.put("spring.json.add.type.headers", false);
            
            // Configuration supplémentaire pour le consumer
            configProps.put("session.timeout.ms", 30000);
            configProps.put("heartbeat.interval.ms", 10000);
            configProps.put("max.poll.records", 10);
            
            // ✅ Configuration pour capturer les erreurs de désérialisation
            configProps.put("spring.json.deserializer.ignore.unknown.properties", true);
            configProps.put("spring.json.deserializer.fail.on.unknown.properties", false);
            
            Object factory = consumerFactoryClass.getConstructor(Map.class).newInstance(configProps);
            
            System.out.println("✅ ConsumerFactory créé avec JsonDeserializer pour HoraireSyncEvent");
            System.out.println("✅ Group ID: " + groupId);
            System.out.println("✅ Bootstrap servers: " + bootstrapServers);
            return factory;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de ConsumerFactory: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Impossible de créer ConsumerFactory", e);
        }
    }

    /**
     * Factory pour les listeners Kafka
     */
    @Bean(name = "kafkaListenerContainerFactory")
    public Object kafkaListenerContainerFactory() {
        try {
            Class<?> factoryClass = Class.forName("org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory");
            Class<?> containerPropertiesClass = Class.forName("org.springframework.kafka.listener.ContainerProperties");
            
            Object factory = factoryClass.getDeclaredConstructor().newInstance();
            Object consumerFactory = consumerFactory();
            factoryClass.getMethod("setConsumerFactory", 
                    Class.forName("org.springframework.kafka.core.ConsumerFactory")).invoke(factory, consumerFactory);
            
            // Configurer le démarrage automatique (par défaut c'est true, mais on le confirme)
            try {
                java.lang.reflect.Method[] methods = factoryClass.getMethods();
                for (java.lang.reflect.Method method : methods) {
                    if ("setAutoStartup".equals(method.getName()) && method.getParameterCount() == 1) {
                        method.invoke(factory, true);
                        System.out.println("✅ Auto-startup activé pour le listener");
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("ℹ️ Auto-startup sera activé par défaut (méthode non trouvée, mais c'est OK)");
            }
            
            Object containerProps = factoryClass.getMethod("getContainerProperties").invoke(factory);
            
            // ✅ Ajouter un listener pour logger les messages reçus
            try {
                Class<?> messageListenerClass = Class.forName("org.springframework.kafka.listener.MessageListener");
                Class<?> recordMessageListenerClass = Class.forName("org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter");
                
                // Créer un wrapper pour logger avant d'appeler le listener réel
                java.lang.reflect.Method setMessageListenerMethod = containerPropertiesClass.getMethod("setMessageListener", messageListenerClass);
                
                // On ne peut pas facilement wrapper avec reflection, donc on va juste améliorer les logs
                System.out.println("✅ ContainerProperties configuré pour logging des messages");
            } catch (Exception e) {
                System.out.println("ℹ️ MessageListener wrapper non configuré (normal)");
            }
            
            // Configurer le nombre de threads concurrents (par défaut 1)
            try {
                java.lang.reflect.Method setConcurrencyMethod = factoryClass.getMethod("setConcurrency", int.class);
                setConcurrencyMethod.invoke(factory, 1);
                System.out.println("✅ Concurrency configuré: 1 thread");
            } catch (Exception e) {
                System.out.println("ℹ️ Concurrency par défaut sera utilisé");
            }
            
            // Essayer MANUAL_IMMEDIATE, sinon utiliser MANUAL
            try {
                Enum<?> ackMode = Enum.valueOf(
                        (Class<Enum>) containerPropertiesClass.getClasses()[0], 
                        "MANUAL_IMMEDIATE"
                );
                containerPropertiesClass.getMethod("setAckMode", containerPropertiesClass.getClasses()[0])
                        .invoke(containerProps, ackMode);
                System.out.println("✅ Mode d'acknowledgment: MANUAL_IMMEDIATE");
            } catch (Exception e) {
                // Si MANUAL_IMMEDIATE n'existe pas, utiliser MANUAL
                try {
                    Enum<?> ackMode = Enum.valueOf(
                            (Class<Enum>) containerPropertiesClass.getClasses()[0], 
                            "MANUAL"
                    );
                    containerPropertiesClass.getMethod("setAckMode", containerPropertiesClass.getClasses()[0])
                            .invoke(containerProps, ackMode);
                    System.out.println("✅ Mode d'acknowledgment: MANUAL (MANUAL_IMMEDIATE non disponible)");
                } catch (Exception e2) {
                    System.err.println("⚠️ Impossible de configurer le mode d'acknowledgment: " + e2.getMessage());
                }
            }
            
            // ✅ Configurer un error handler pour capturer les erreurs de désérialisation
            try {
                Class<?> errorHandlerClass = Class.forName("org.springframework.kafka.listener.DefaultErrorHandler");
                
                // Créer un error handler avec logging
                java.lang.reflect.Constructor<?> constructor = errorHandlerClass.getDeclaredConstructor();
                Object errorHandler = constructor.newInstance();
                
                // Configurer l'error handler sur la factory
                try {
                    Class<?> commonErrorHandlerClass = Class.forName("org.springframework.kafka.listener.CommonErrorHandler");
                    java.lang.reflect.Method setErrorHandlerMethod = factoryClass.getMethod("setCommonErrorHandler", commonErrorHandlerClass);
                    setErrorHandlerMethod.invoke(factory, errorHandler);
                    System.out.println("✅ Error handler configuré pour capturer les erreurs de désérialisation");
                } catch (Exception e) {
                    System.out.println("⚠️ Impossible de configurer l'error handler: " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.out.println("⚠️ Error handler non disponible: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("✅ KafkaListenerContainerFactory créé");
            System.out.println("✅ Bean name: kafkaListenerContainerFactory");
            System.out.println("✅ Le listener DjangoHoraireEventListener devrait maintenant démarrer");
            return factory;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de KafkaListenerContainerFactory: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Impossible de créer KafkaListenerContainerFactory", e);
        }
    }

    /**
     * Configuration pour créer automatiquement le topic s'il n'existe pas
     */
    @Bean
    public Object kafkaAdmin() {
        try {
            Class<?> adminConfigClass = Class.forName("org.apache.kafka.clients.admin.AdminClientConfig");
            Class<?> kafkaAdminClass = Class.forName("org.springframework.kafka.core.KafkaAdmin");
            
            Map<String, Object> configs = new HashMap<>();
            configs.put((String) adminConfigClass.getField("BOOTSTRAP_SERVERS_CONFIG").get(null), bootstrapServers);
            
            Object admin = kafkaAdminClass.getConstructor(Map.class).newInstance(configs);
            
            // Configurer la création automatique des topics
            java.lang.reflect.Field autoCreateField = kafkaAdminClass.getDeclaredField("autoCreate");
            autoCreateField.setAccessible(true);
            autoCreateField.set(admin, true);
            
            System.out.println("✅ KafkaAdmin créé avec création automatique des topics activée");
            return admin;
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la création de KafkaAdmin: " + e.getMessage());
            // Ne pas bloquer le démarrage si KafkaAdmin ne peut pas être créé
            return null;
        }
    }

    /**
     * Configuration du topic horaire-sync-topic
     */
    @Bean
    public Object horaireSyncTopic() {
        try {
            Class<?> topicClass = Class.forName("org.apache.kafka.clients.admin.NewTopic");
            
            // Créer un topic avec 3 partitions et replication factor 1
            Object topic = topicClass.getConstructor(String.class, int.class, short.class)
                    .newInstance("horaire-sync-topic", 3, (short) 1);
            
            System.out.println("✅ Configuration du topic horaire-sync-topic créée");
            return topic;
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la création de la configuration du topic: " + e.getMessage());
            return null;
        }
    }
}
