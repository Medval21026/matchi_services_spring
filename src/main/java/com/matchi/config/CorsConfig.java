package com.matchi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * Configuration CORS pour Angular et PWA mobile
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Autoriser credentials (cookies, JWT, Authorization headers)
        config.setAllowCredentials(true);

        // Autoriser les origines Angular / Vercel
        // ⚠️ IMPORTANT: Avec allowCredentials(true), on doit utiliser setAllowedOrigins (pas setAllowedOriginPatterns)
        // et spécifier les origines exactes (pas de wildcards)
        config.setAllowedOrigins(Arrays.asList(
            "https://matchi-services-angular-afyy.vercel.app",
            "http://localhost:4200",
            "http://localhost:4201",
            "http://127.0.0.1:4200"
        ));

        // Méthodes HTTP autorisées
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","PATCH"));

        // Headers autorisés
        config.setAllowedHeaders(Arrays.asList(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "X-Requested-With"
        ));

        // Headers exposés au frontend
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));

        // Durée du cache pour preflight (OPTIONS)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    // Filtre supplémentaire (optionnel, Spring Security utilise corsConfigurationSource)
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
