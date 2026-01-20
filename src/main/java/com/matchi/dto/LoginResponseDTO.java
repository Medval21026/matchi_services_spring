package com.matchi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private Long id;
    private String nom;
    private String prenom;
    private Integer telephone;
    private Boolean isActive;
    private String token;
    private String tokenType = "Bearer";
    private List<Long> terrainIds; // ✅ IDs des terrains associés au propriétaire
    
    // Constructeur avec terrainIds
    public LoginResponseDTO(Long id, String nom, String prenom, Integer telephone, Boolean isActive, String token, List<Long> terrainIds) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.isActive = isActive;
        this.token = token;
        this.tokenType = "Bearer";
        this.terrainIds = terrainIds;
    }
    
    // Constructeur sans tokenType ni terrainIds (pour compatibilité, terrainIds sera null)
    public LoginResponseDTO(Long id, String nom, String prenom, Integer telephone, Boolean isActive, String token) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.isActive = isActive;
        this.token = token;
        this.tokenType = "Bearer";
        this.terrainIds = null;
    }
}
