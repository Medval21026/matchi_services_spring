package com.matchi.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientAbonne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private Integer telephone;

    private LocalDateTime createdAt = LocalDateTime.now();


    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}


