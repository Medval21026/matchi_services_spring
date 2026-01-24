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
 * ✅ Compatible avec PWA mobile (iOS/Safari) + CORS + credentials
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    /**
     * Configuration de Spring Security
     * ✅ CRITIQUE : Utiliser corsConfigurationSource() pour que CORS fonctionne avec credentials sur mobile
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ✅ CRITIQUE : Configurer CORS avec la source (nécessaire pour mobile + credentials)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Désactiver CSRF pour les API REST (on utilise JWT à la place)
            .csrf(csrf -> csrf.disable())
            // Autoriser toutes les requêtes (peut être modifié plus tard pour protéger certains endpoints)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        
        return http.build();
    }

    /**
     * Bean pour l'encodeur de mot de passe BCrypt
     * Utilisé pour hacher et vérifier les mots de passe
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
