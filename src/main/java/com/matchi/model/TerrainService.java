package com.matchi.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerrainService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String adresse;

    // Relation vers le propriétaire
    @ManyToOne
    @JoinColumn(name = "proprietaire_id")
    private Proprietaire proprietaire;

    // Heures d'ouverture et de fermeture
    private LocalTime heureOuverture;
    
    private LocalTime heureFermeture;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
