package com.matchi.dto;

import com.matchi.model.StatutAbonnement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AbonnementUpdateDTO {

    private Long terrainId;
    private Integer clientTelephone;  // ✅ Changé de clientId à clientTelephone
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal prixTotal;
    private StatutAbonnement status;
    private List<AbonnementHoraireDTO> horaires;

    // ===== Constructeurs =====
    public AbonnementUpdateDTO() {}

    public AbonnementUpdateDTO(Long terrainId, Integer clientTelephone, LocalDate dateDebut, LocalDate dateFin,
                               BigDecimal prixTotal, StatutAbonnement status, List<AbonnementHoraireDTO> horaires) {
        this.terrainId = terrainId;
        this.clientTelephone = clientTelephone;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.prixTotal = prixTotal;
        this.status = status;
        this.horaires = horaires;
    }

    // ===== Getters & Setters =====
    public Long getTerrainId() { return terrainId; }
    public void setTerrainId(Long terrainId) { this.terrainId = terrainId; }

    public Integer getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(Integer clientTelephone) { this.clientTelephone = clientTelephone; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getPrixTotal() { return prixTotal; }
    public void setPrixTotal(BigDecimal prixTotal) { this.prixTotal = prixTotal; }

    public StatutAbonnement getStatus() { return status; }
    public void setStatus(StatutAbonnement status) { this.status = status; }

    public List<AbonnementHoraireDTO> getHoraires() { return horaires; }
    public void setHoraires(List<AbonnementHoraireDTO> horaires) { this.horaires = horaires; }
}
