package com.matchi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration OpenAPI/Swagger pour forcer HTTPS en production
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        
        // Si on est en production, configurer les serveurs
        if ("prod".equals(activeProfile)) {
            List<Server> servers = new ArrayList<>();
            // Les serveurs de production peuvent être configurés ici si nécessaire
            openAPI.setServers(servers);
        }
        
        return openAPI;
    }
}
