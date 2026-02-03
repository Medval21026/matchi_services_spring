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
        
        // Si on est en production, forcer HTTPS
        if ("prod".equals(activeProfile)) {
            List<Server> servers = new ArrayList<>();
            // Ajouter le serveur HTTPS Railway
            servers.add(new Server()
                .url("https://matchiservicesspring-production.up.railway.app")
                .description("Production Server (HTTPS)"));
            // Garder aussi HTTP pour compatibilité
            servers.add(new Server()
                .url("http://matchiservicesspring-production.up.railway.app")
                .description("Production Server (HTTP - redirects to HTTPS)"));
            openAPI.setServers(servers);
        }
        
        return openAPI;
    }
}
