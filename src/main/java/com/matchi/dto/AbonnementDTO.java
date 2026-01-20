package com.matchi.dto;

import com.matchi.model.StatutAbonnement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AbonnementDTO {

    private Long id;
    private Long terrainId;
    private Long clientId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal prixTotal;
    private StatutAbonnement status;
    private LocalDateTime createdAt;
    private List<AbonnementHoraireDTO> horaires;

    public AbonnementDTO() {}

    public AbonnementDTO(Long id, Long terrainId, Long clientId, LocalDate dateDebut, LocalDate dateFin,
                         BigDecimal prixTotal, StatutAbonnement status, LocalDateTime createdAt,
                         List<AbonnementHoraireDTO> horaires) {
        this.id = id;
        this.terrainId = terrainId;
        this.clientId = clientId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.prixTotal = prixTotal;
        this.status = status;
        this.createdAt = createdAt;
        this.horaires = horaires;
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTerrainId() { return terrainId; }
    public void setTerrainId(Long terrainId) { this.terrainId = terrainId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getPrixTotal() { return prixTotal; }
    public void setPrixTotal(BigDecimal prixTotal) { this.prixTotal = prixTotal; }

    public StatutAbonnement getStatus() { return status; }
    public void setStatus(StatutAbonnement status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<AbonnementHoraireDTO> getHoraires() { return horaires; }
    public void setHoraires(List<AbonnementHoraireDTO> horaires) { this.horaires = horaires; }
}
