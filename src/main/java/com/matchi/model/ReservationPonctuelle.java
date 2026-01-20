package com.matchi.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationPonctuelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private BigDecimal prix;

    private Integer clientTelephone;

    @ManyToOne
    private TerrainService terrain;
}
