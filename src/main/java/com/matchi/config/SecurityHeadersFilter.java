package com.matchi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filtre pour supprimer les headers de sécurité qui causent des problèmes en HTTP
 * (Cross-Origin-Opener-Policy, Cross-Origin-Embedder-Policy, etc.)
 * Ces headers ne sont pas fiables en HTTP (seulement en HTTPS ou localhost)
 */
@Component
@Order(1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        // Vérifier si c'est HTTP (pas HTTPS) et pas localhost
        boolean isHttp = !request.isSecure() 
            && !request.getServerName().equals("localhost") 
            && !request.getServerName().equals("127.0.0.1");
        
        if (isHttp) {
            // Utiliser un wrapper pour supprimer les headers problématiques
            HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response) {
                private final Set<String> headersToRemove = Set.of(
                    "Cross-Origin-Opener-Policy",
                    "Cross-Origin-Embedder-Policy",
                    "Cross-Origin-Resource-Policy"
                );
                
                @Override
                public void setHeader(String name, String value) {
                    if (!headersToRemove.contains(name)) {
                        super.setHeader(name, value);
                    }
                }
                
                @Override
                public void addHeader(String name, String value) {
                    if (!headersToRemove.contains(name)) {
                        super.addHeader(name, value);
                    }
                }
            };
            
            filterChain.doFilter(request, wrappedResponse);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
