package com.matchi.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation vers TerrainService
    @ManyToOne
    @JoinColumn(name = "terrain_id")
    private TerrainService terrain;

    // Relation vers AbonnementHoraire
    @OneToMany(mappedBy = "abonnement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AbonnementHoraire> horaires = new ArrayList<>();

    // Relation vers ClientAbonne
    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientAbonne client;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private BigDecimal prixTotal;

    @Enumerated(EnumType.STRING)
    private StatutAbonnement status = StatutAbonnement.ACTIF; 

    private LocalDateTime createdAt;

    // ======== Lifecycle Callback ========
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
