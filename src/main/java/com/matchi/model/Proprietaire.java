package com.matchi.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proprietaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;

    @Column(unique = true)
    private Integer telephone;

    private String password;
    @Column(name = "is_active")
    private Boolean isActive;

    private LocalDateTime createdAt = LocalDateTime.now();
     
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = false;
        }
    }
}
