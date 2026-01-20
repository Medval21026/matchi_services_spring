package com.matchi.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndisponibleHoraire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private Long sourceId;

    // Informations supplémentaires optionnelles
    private String description;
}
