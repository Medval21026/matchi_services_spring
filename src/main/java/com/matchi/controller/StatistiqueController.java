package com.matchi.controller;

import com.matchi.dto.ClientAbonneDTO;
import com.matchi.service.StatistiqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/statistiques")
@RequiredArgsConstructor
public class StatistiqueController {

    private final StatistiqueService statistiqueService;

    /**
     * Retourne le nombre d'abonnements actifs pour un terrain donné
     * GET /api/statistiques/terrains/{terrainId}/abonnements-actifs
     */
    @GetMapping("/terrains/{terrainId}/abonnements-actifs")
    public Long getNombreAbonnementsActifs(@PathVariable Long terrainId) {
        return statistiqueService.getNombreAbonnementsActifs(terrainId);
    }

    /**
     * Retourne le nombre de réservations ponctuelles d'aujourd'hui pour un terrain donné
     * GET /api/statistiques/terrains/{terrainId}/reservations-aujourdhui
     */
    @GetMapping("/terrains/{terrainId}/reservations-aujourdhui")
    public Long getNombreReservationsAujourdhui(@PathVariable Long terrainId) {
        return statistiqueService.getNombreReservationsAujourdhui(terrainId);
    }

    /**
     * Retourne le revenu total des abonnements actifs pour un terrain donné
     * GET /api/statistiques/terrains/{terrainId}/revenu-abonnements-actifs
     */
    @GetMapping("/terrains/{terrainId}/revenu-abonnements-actifs")
    public BigDecimal getRevenuAbonnementsActifs(@PathVariable Long terrainId) {
        return statistiqueService.getRevenuAbonnementsActifs(terrainId);
    }

    /**
     * Retourne le revenu des réservations ponctuelles d'aujourd'hui pour un terrain donné
     * GET /api/statistiques/terrains/{terrainId}/revenu-reservations-aujourdhui
     */
    @GetMapping("/terrains/{terrainId}/revenu-reservations-aujourdhui")
    public BigDecimal getRevenuReservationsAujourdhui(@PathVariable Long terrainId) {
        return statistiqueService.getRevenuReservationsAujourdhui(terrainId);
    }

    /**
     * Retourne la liste des clients qui ont fait une transaction (abonnement ou réservation ponctuelle)
     * avec un terrain donné
     * GET /api/statistiques/terrains/{terrainId}/clients
     */
    @GetMapping("/terrains/{terrainId}/clients")
    public List<ClientAbonneDTO> getClientsParTerrain(@PathVariable Long terrainId) {
        return statistiqueService.getClientsParTerrain(terrainId);
    }

    @GetMapping("/terrains/{terrainId}/reservations-hier")
    public Long getNombreReservationsHier(@PathVariable Long terrainId) {
        return statistiqueService.getNombreReservationsHier(terrainId);
    }

    /**
     * Revenu des réservations ponctuelles “hier” pour un terrain donné
     * GET /api/statistiques/terrains/{terrainId}/revenu-reservations-hier
     */
    @GetMapping("/terrains/{terrainId}/revenu-reservations-hier")
    public BigDecimal getRevenuReservationsHier(@PathVariable Long terrainId) {
        return statistiqueService.getRevenuReservationsHier(terrainId);
    }
}
