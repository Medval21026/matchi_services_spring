package com.matchi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * Configuration CORS pour permettre la communication avec le front-end Angular et PWA mobile
 * ✅ Compatible avec Spring Security + credentials + mobile (iOS/Safari)
 */
@Configuration
public class CorsConfig {

    /**
     * Configuration CORS source pour Spring Security
     * ⚠️ IMPORTANT : Utiliser setAllowedOriginPatterns au lieu de setAllowedOrigins
     * pour supporter les PWA mobiles avec des origines dynamiques
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // ✅ CRITIQUE : Permettre les credentials (cookies, authorization headers, JWT)
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Arrays.asList(
            // ✅ Développement local
            "http://172.20.10.*",      // ✅ Autorise toutes les IPs du réseau 172.20.10.x avec n'importe quel port
            "http://localhost:*",      // ✅ Localhost avec n'importe quel port
            "http://127.0.0.1:*",      // ✅ 127.0.0.1 avec n'importe quel port
            "http://192.168.*.*",      // ✅ Si vous utilisez un autre réseau local (optionnel)
            // ✅ Production - Vercel
            "https://matchi-services-angular-afyy.vercel.app",  // ✅ URL Vercel principale
            "https://*.vercel.app"     // ✅ Toutes les sous-domaines Vercel (pour preview deployments)
        ));

        // ✅ Headers autorisés (essentiels pour mobile + credentials)
        config.setAllowedHeaders(Arrays.asList(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",           // ✅ Critique pour JWT
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-CSRF-TOKEN"            // Si CSRF est activé plus tard
        ));

        // ✅ Méthodes HTTP autorisées
        config.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS",
                "PATCH",
                "HEAD"
        ));

        // ✅ Headers exposés au client (importants pour mobile)
        config.setExposedHeaders(Arrays.asList(
                "Authorization",          // ✅ Pour récupérer le JWT token
                "Content-Disposition",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // ✅ Durée de cache pour preflight requests (mobile-friendly)
        config.setMaxAge(3600L);

        // Appliquer la configuration à tous les endpoints
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    /**
     * Filtre CORS supplémentaire (optionnel, pour compatibilité)
     * Spring Security utilisera corsConfigurationSource() en priorité
     */
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
