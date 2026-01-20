package com.matchi.model;



import com.matchi.model.JourSemaine;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifTerrain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private TerrainService terrain;

    @Enumerated(EnumType.STRING)
    private JourSemaine jourSemaine;

    private LocalTime heureDebut;
    private LocalTime heureFin;

    private BigDecimal prixParHeure;
}

