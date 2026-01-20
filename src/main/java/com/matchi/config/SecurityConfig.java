package com.matchi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configuration de Spring Security pour désactiver l'authentification automatique
     * et permettre l'accès à tous les endpoints
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http)) // ✅ Activer CORS
            .csrf(csrf -> csrf.disable()) // Désactiver CSRF pour les API REST
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Permettre toutes les requêtes sans authentification
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
