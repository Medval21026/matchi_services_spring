package com.matchi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * Configuration CORS pour permettre la communication avec le front-end Angular
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Permettre les credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Origines autorisées (Angular en développement et production)
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",     // Angular dev server
                "http://localhost:55264",     // Angular dev server alternatif
                "http://127.0.0.1:4200",     // Alias localhost
                "http://localhost:3000"      // Au cas où
        ));

        // Headers autorisés
        config.setAllowedHeaders(Arrays.asList(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Requested-With"
        ));

        // Méthodes HTTP autorisées
        config.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS",
                "PATCH"
        ));

        // Headers exposés au client
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // Durée de cache de la configuration CORS (en secondes)
        config.setMaxAge(3600L);

        // Appliquer la configuration à tous les endpoints
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
