package com.matchi.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Clé secrète pour signer les tokens (à mettre dans application.properties en production)
    private static final String SECRET_KEY = "matchi_service_secret_key_2024_super_secure_key_for_jwt_token_generation";
    
    // Durée de validité du token : 24 heures
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 heures en millisecondes

    /**
     * Génère un token JWT pour un propriétaire
     */
    public String generateToken(Long proprietaireId, String telephone, String nom, String prenom) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", proprietaireId);
        claims.put("telephone", telephone);
        claims.put("nom", nom);
        claims.put("prenom", prenom);
        
        return createToken(claims, telephone.toString());
    }

    /**
     * Crée le token JWT avec les informations fournies
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + EXPIRATION_TIME);

        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extrait toutes les claims d'un token
     */
    public Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrait l'ID du propriétaire du token
     */
    public Long extractProprietaireId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("id", Long.class);
    }

    /**
     * Extrait le téléphone du token
     */
    public String extractTelephone(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Vérifie si le token est expiré
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Valide le token
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
