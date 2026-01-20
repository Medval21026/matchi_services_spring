package com.matchi.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonnementHoraire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation ManyToOne vers Abonnement
    @ManyToOne
    @JoinColumn(name = "abonnement_id")
    private Abonnement abonnement;


    @Enumerated(EnumType.STRING)
    private JourSemaine jourSemaine;

    // Date précise de l'horaire (ex: 13/01/2026 pour un LUNDI)
    private LocalDate date;

    private LocalTime heureDebut;
    private LocalTime heureFin;

    private BigDecimal prixHeure;
}
