package com.matchi.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * Configuration CORS pour Angular, PWA mobile et outils de test (Swagger UI)
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration config = new CorsConfiguration();
                
                String origin = request.getHeader("Origin");
                
                // Si pas d'origine (requêtes directes, outils de test), autoriser toutes les origines sans credentials
                if (origin == null || origin.isEmpty()) {
                    config.setAllowCredentials(false);
                    config.setAllowedOriginPatterns(Collections.singletonList("*"));
                } else {
                    // Pour les requêtes avec origine, vérifier si elle est autorisée
                    config.setAllowCredentials(true);
                    config.setAllowedOrigins(Arrays.asList(
                        // Frontend Angular (Vercel)
                        "https://matchi-services-angular-afyy.vercel.app",
                        // Backend Railway (pour Swagger UI et outils de test)
                        "http://matchiservicesspring-production.up.railway.app",
                        "https://matchiservicesspring-production.up.railway.app",
                        // Développement local
                        "http://localhost:4200",
                        "http://localhost:4201",
                        "http://127.0.0.1:4200",
                        "http://localhost:8080",
                        "http://127.0.0.1:8080"
                    ));
                }
                
                // Configuration commune
                config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
                config.setAllowedHeaders(Arrays.asList(
                        "Origin",
                        "Content-Type",
                        "Accept",
                        "Authorization",
                        "X-Requested-With",
                        "Access-Control-Request-Method",
                        "Access-Control-Request-Headers"
                ));
                config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
                config.setMaxAge(3600L);
                
                return config;
            }
        };
    }

    // Filtre supplémentaire (optionnel, Spring Security utilise corsConfigurationSource)
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
