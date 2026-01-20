package com.matchi.dto;

import com.matchi.model.JourSemaine;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDate;

public class AbonnementHoraireDTO {

    private Long id;
    private Long abonnementId; 
    private LocalDate date; // Date précise de l'horaire
    private JourSemaine jourSemaine;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private BigDecimal prixHeure;

    public AbonnementHoraireDTO() {}

    public AbonnementHoraireDTO(Long id, Long abonnementId, LocalDate date,
                                JourSemaine jourSemaine, LocalTime heureDebut, LocalTime heureFin,
                                BigDecimal prixHeure) {
        this.id = id;
        this.abonnementId = abonnementId;
        this.date = date;
        this.jourSemaine = jourSemaine;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.prixHeure = prixHeure;
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAbonnementId() { return abonnementId; }
    public void setAbonnementId(Long abonnementId) { this.abonnementId = abonnementId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public JourSemaine getJourSemaine() { return jourSemaine; }
    public void setJourSemaine(JourSemaine jourSemaine) { this.jourSemaine = jourSemaine; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public BigDecimal getPrixHeure() { return prixHeure; }
    public void setPrixHeure(BigDecimal prixHeure) { this.prixHeure = prixHeure; }
}
