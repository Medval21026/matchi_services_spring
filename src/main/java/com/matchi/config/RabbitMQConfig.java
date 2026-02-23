package com.matchi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ pour la synchronisation des horaires
 * entre Spring Boot et Django
 * 
 * Cette configuration ne se charge que si RabbitMQ est configuré et disponible
 */
@Configuration
@ConditionalOnClass(name = {
    "org.springframework.amqp.core.TopicExchange",
    "org.springframework.amqp.rabbit.core.RabbitTemplate"
})
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.exchange:horaire-sync-exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.queue:horaire-sync-queue}")
    private String queue;

    @Value("${spring.rabbitmq.routing-key:horaire-sync}")
    private String routingKey;

    /**
     * Crée l'exchange pour les événements de synchronisation
     */
    @Bean
    public Object horaireSyncExchange() {
        try {
            Class<?> clazz = Class.forName("org.springframework.amqp.core.TopicExchange");
            return clazz.getConstructor(String.class).newInstance(exchange);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer TopicExchange", e);
        }
    }

    /**
     * Crée la queue pour recevoir les événements de Django
     */
    @Bean
    public Object horaireSyncQueue() {
        try {
            Class<?> queueBuilderClass = Class.forName("org.springframework.amqp.core.QueueBuilder");
            Class<?> queueClass = Class.forName("org.springframework.amqp.core.Queue");
            java.lang.reflect.Method durableMethod = queueBuilderClass.getMethod("durable", String.class);
            Object builder = durableMethod.invoke(null, queue);
            java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
            return buildMethod.invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer Queue", e);
        }
    }

    /**
     * Lie la queue à l'exchange avec la routing key
     */
    @Bean
    public Object horaireSyncBinding() {
        try {
            Class<?> bindingBuilderClass = Class.forName("org.springframework.amqp.core.BindingBuilder");
            java.lang.reflect.Method bindMethod = bindingBuilderClass.getMethod("bind", 
                    Class.forName("org.springframework.amqp.core.Queue"));
            Object binding = bindMethod.invoke(null, horaireSyncQueue());
            java.lang.reflect.Method toMethod = binding.getClass().getMethod("to", 
                    Class.forName("org.springframework.amqp.core.TopicExchange"));
            binding = toMethod.invoke(binding, horaireSyncExchange());
            java.lang.reflect.Method withMethod = binding.getClass().getMethod("with", String.class);
            return withMethod.invoke(binding, routingKey);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer Binding", e);
        }
    }

    /**
     * Configure le convertisseur JSON pour les messages
     */
    @Bean
    public Object jsonMessageConverter() {
        try {
            Class<?> clazz = Class.forName("org.springframework.amqp.support.converter.Jackson2JsonMessageConverter");
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer Jackson2JsonMessageConverter", e);
        }
    }

    /**
     * Configure le RabbitTemplate avec le convertisseur JSON
     */
    @Bean
    public Object rabbitTemplate(Object connectionFactory) {
        try {
            Class<?> templateClass = Class.forName("org.springframework.amqp.rabbit.core.RabbitTemplate");
            Object template = templateClass.getConstructor(
                    Class.forName("org.springframework.amqp.rabbit.connection.ConnectionFactory"))
                    .newInstance(connectionFactory);
            java.lang.reflect.Method setConverterMethod = templateClass.getMethod("setMessageConverter", 
                    Class.forName("org.springframework.amqp.support.converter.MessageConverter"));
            setConverterMethod.invoke(template, jsonMessageConverter());
            return template;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer RabbitTemplate", e);
        }
    }
}
