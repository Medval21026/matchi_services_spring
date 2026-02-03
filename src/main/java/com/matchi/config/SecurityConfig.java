package com.matchi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuration Spring Security
 * ✅ Compatible avec PWA + CORS + credentials
 * ✅ Support HTTPS via Railway proxy (configuré dans application-prod.properties)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Utiliser notre configuration CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Désactiver CSRF pour les API REST
            .csrf(csrf -> csrf.disable())
            // Autoriser toutes les requêtes (à sécuriser plus tard)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }

    // Bean pour encoder les mots de passe
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
