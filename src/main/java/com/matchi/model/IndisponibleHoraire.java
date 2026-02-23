package com.matchi.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "indisponible_horaire", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_horaire_source", columnNames = {"terrain_id", "type_reservation", "source_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndisponibleHoraire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID global unique pour la synchronisation entre Spring Boot et Django
    @Column(unique = true, nullable = false, updatable = false)
    private UUID uuid;

    /**
     * Génère automatiquement un UUID avant la persistance si non défini
     */
    @PrePersist
    protected void generateUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    // Relation vers le terrain
    @ManyToOne
    @JoinColumn(name = "terrain_id")
    private TerrainService terrain;

    // Date et horaires
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;

    // Type de réservation
    @Enumerated(EnumType.STRING)
    private TypeReservation typeReservation;

    // Référence vers la source (ID de l'abonnement horaire ou réservation ponctuelle)
    // ✅ CONTRAINTE UNIQUE : Un seul horaire par sourceId et typeReservation pour éviter les doublons
    @Column(name = "source_id")
    private Long sourceId;

    // Informations supplémentaires optionnelles
    private String description;
}
