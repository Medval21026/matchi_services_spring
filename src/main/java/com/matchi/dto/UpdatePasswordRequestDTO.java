package com.matchi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordRequestDTO {
    private String motDePasse;      // Ancien mot de passe
    private String newMotDePasse;   // Nouveau mot de passe
}
