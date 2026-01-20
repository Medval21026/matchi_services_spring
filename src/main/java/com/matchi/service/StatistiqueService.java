package com.matchi.service;

import com.matchi.dto.ClientAbonneDTO;
import com.matchi.model.Abonnement;
import com.matchi.model.ClientAbonne;
import com.matchi.model.ReservationPonctuelle;
import com.matchi.model.StatutAbonnement;
import com.matchi.repository.AbonnementRepository;
import com.matchi.repository.ClientAbonneRepository;
import com.matchi.repository.ReservationPonctuelleRepository;
import com.matchi.repository.TerrainServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatistiqueService {

    private final AbonnementRepository abonnementRepository;
    private final ReservationPonctuelleRepository reservationPonctuelleRepository;
    private final TerrainServiceRepository terrainServiceRepository;
    private final ClientAbonneRepository clientAbonneRepository;

    /**
     * Retourne le nombre d'abonnements actifs pour un terrain donné
     */
    public Long getNombreAbonnementsActifs(Long terrainId) {
        // Vérifier que le terrain existe
        terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain introuvable avec l'ID: " + terrainId));
        
        List<Abonnement> abonnements = abonnementRepository.findByTerrainId(terrainId);
        
        // Filtrer les abonnements actifs
        return abonnements.stream()
                .filter(abonnement -> abonnement.getStatus() == StatutAbonnement.ACTIF)
                .count();
    }

    /**
     * Retourne le nombre de réservations ponctuelles d'aujourd'hui pour un terrain donné
     */
    public Long getNombreReservationsAujourdhui(Long terrainId) {
        // Vérifier que le terrain existe
        terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain introuvable avec l'ID: " + terrainId));
        
        LocalDate aujourdhui = LocalDate.now();
        List<ReservationPonctuelle> reservations = reservationPonctuelleRepository.findByTerrain_Id(terrainId);
        
        // Filtrer les réservations d'aujourd'hui
        return reservations.stream()
                .filter(reservation -> reservation.getDate() != null && reservation.getDate().equals(aujourdhui))
                .count();
    }

    /**
     * Retourne le revenu total des abonnements actifs pour un terrain donné
     */
    public BigDecimal getRevenuAbonnementsActifs(Long terrainId) {
        // Vérifier que le terrain existe
        terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain introuvable avec l'ID: " + terrainId));
        
        List<Abonnement> abonnements = abonnementRepository.findByTerrainId(terrainId);
        
        // Calculer le revenu total des abonnements actifs
        return abonnements.stream()
                .filter(abonnement -> abonnement.getStatus() == StatutAbonnement.ACTIF)
                .map(Abonnement::getPrixTotal)
                .filter(prix -> prix != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retourne le revenu des réservations ponctuelles d'aujourd'hui pour un terrain donné
     * Pour le calcul du revenu du jour X, inclut toutes les heures comprises entre 00:00 et l'heure d'ouverture
     * du terrain du jour X+1, mais les attribue au jour X.
     */
    public BigDecimal getRevenuReservationsAujourdhui(Long terrainId) {
        // Vérifier que le terrain existe
        com.matchi.model.TerrainService terrain = terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain introuvable avec l'ID: " + terrainId));
        
        LocalDate aujourdhui = LocalDate.now();
        LocalDate demain = aujourdhui.plusDays(1);
        List<ReservationPonctuelle> reservations = reservationPonctuelleRepository.findByTerrain_Id(terrainId);
        
        java.time.LocalTime heureOuverture = terrain.getHeureOuverture();
        java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
        
        // Calculer le revenu total des réservations d'aujourd'hui
        BigDecimal revenuAujourdhui = reservations.stream()
                .filter(reservation -> reservation.getDate() != null && reservation.getDate().equals(aujourdhui))
                .map(ReservationPonctuelle::getPrix)
                .filter(prix -> prix != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Inclure aussi les réservations du jour suivant (X+1) qui ont une heure de début entre 00:00 et l'heure d'ouverture
        // mais les attribuer au jour X (aujourd'hui)
        if (heureOuverture != null) {
            BigDecimal revenuDemainMatin = reservations.stream()
                    .filter(reservation -> reservation.getDate() != null && reservation.getDate().equals(demain))
                    .filter(reservation -> {
                        if (reservation.getHeureDebut() == null) {
                            return false;
                        }
                        java.time.LocalTime heureDebut = reservation.getHeureDebut();
                        // Inclure les réservations qui commencent entre 00:00 et l'heure d'ouverture
                        // Si l'heure d'ouverture est après minuit (ex: terrain ouvre à 18h et ferme à 2h), 
                        // on inclut les heures entre 00:00 et heureOuverture (ex: 00:00 à 02:00)
                        // Si l'heure d'ouverture est avant minuit (ex: 8h), on inclut les heures entre 00:00 et heureOuverture (ex: 00:00 à 08:00)
                        // Vérifier si l'heure de début est >= 00:00 et < heureOuverture
                        return (heureDebut.equals(minuit) || heureDebut.isAfter(minuit)) && 
                               heureDebut.isBefore(heureOuverture);
                    })
                    .map(ReservationPonctuelle::getPrix)
                    .filter(prix -> prix != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return revenuAujourdhui.add(revenuDemainMatin);
        }
        
        return revenuAujourdhui;
    }
    
    /**
     * Retourne le nombre de réservations ponctuelles d'hier pour un terrain donné
     */
    public Long getNombreReservationsHier(Long terrainId) {
        // Vérifier que le terrain existe
        terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain introuvable avec l'ID: " + terrainId));
        
        LocalDate hier = LocalDate.now().minusDays(1);
        List<ReservationPonctuelle> reservations = reservationPonctuelleRepository.findByTerrain_Id(terrainId);
        
        // Filtrer les réservations d'hier
        return reservations.stream()
                .filter(reservation -> reservation.getDate() != null && reservation.getDate().equals(hier))
                .count();
    }
    
    /**
     * Retourne le revenu des réservations ponctuelles d'hier pour un terrain donné
     * Pour le calcul du revenu du jour X, inclut toutes les heures comprises entre 00:00 et l'heure d'ouverture
     * du terrain du jour X+1, mais les attribue au jour X.
     */
    public BigDecimal getRevenuReservationsHier(Long terrainId) {
        // Vérifier que le terrain existe
        com.matchi.model.TerrainService terrain = terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain introuvable avec l'ID: " + terrainId));
        
        LocalDate hier = LocalDate.now().minusDays(1);
        LocalDate aujourdhui = LocalDate.now();
        List<ReservationPonctuelle> reservations = reservationPonctuelleRepository.findByTerrain_Id(terrainId);
        
        java.time.LocalTime heureOuverture = terrain.getHeureOuverture();
        java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
        
        // Calculer le revenu total des réservations d'hier
        BigDecimal revenuHier = reservations.stream()
                .filter(reservation -> reservation.getDate() != null && reservation.getDate().equals(hier))
                .map(ReservationPonctuelle::getPrix)
                .filter(prix -> prix != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Inclure aussi les réservations d'aujourd'hui (X+1) qui ont une heure de début entre 00:00 et l'heure d'ouverture
        // mais les attribuer au jour X (hier)
        if (heureOuverture != null) {
            BigDecimal revenuAujourdhuiMatin = reservations.stream()
                    .filter(reservation -> reservation.getDate() != null && reservation.getDate().equals(aujourdhui))
                    .filter(reservation -> {
                        if (reservation.getHeureDebut() == null) {
                            return false;
                        }
                        java.time.LocalTime heureDebut = reservation.getHeureDebut();
                        // Inclure les réservations qui commencent entre 00:00 et l'heure d'ouverture
                        // Si l'heure d'ouverture est après minuit (ex: terrain ouvre à 18h et ferme à 2h), 
                        // on inclut les heures entre 00:00 et heureOuverture (ex: 00:00 à 02:00)
                        // Si l'heure d'ouverture est avant minuit (ex: 8h), on inclut les heures entre 00:00 et heureOuverture (ex: 00:00 à 08:00)
                        // Vérifier si l'heure de début est >= 00:00 et < heureOuverture
                        return (heureDebut.equals(minuit) || heureDebut.isAfter(minuit)) && 
                               heureDebut.isBefore(heureOuverture);
                    })
                    .map(ReservationPonctuelle::getPrix)
                    .filter(prix -> prix != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return revenuHier.add(revenuAujourdhuiMatin);
        }
        
        return revenuHier;
    }

    /**
     * Retourne la liste des clients qui ont fait une transaction (abonnement ou réservation ponctuelle)
     * avec un terrain donné
     */
    public List<ClientAbonneDTO> getClientsParTerrain(Long terrainId) {
        // Vérifier que le terrain existe
        terrainServiceRepository.findById(terrainId)
                .orElseThrow(() -> new IllegalArgumentException("Terrain introuvable avec l'ID: " + terrainId));
        
        Set<Long> clientIds = new HashSet<>();
        Set<Integer> clientTelephones = new HashSet<>();
        
        // 1. Récupérer les clients depuis les abonnements
        List<Abonnement> abonnements = abonnementRepository.findByTerrainId(terrainId);
        for (Abonnement abonnement : abonnements) {
            if (abonnement.getClient() != null) {
                clientIds.add(abonnement.getClient().getId());
                if (abonnement.getClient().getTelephone() != null) {
                    clientTelephones.add(abonnement.getClient().getTelephone());
                }
            }
        }
        
        // 2. Récupérer les clients depuis les réservations ponctuelles (via téléphone)
        List<ReservationPonctuelle> reservations = reservationPonctuelleRepository.findByTerrain_Id(terrainId);
        for (ReservationPonctuelle reservation : reservations) {
            if (reservation.getClientTelephone() != null) {
                clientTelephones.add(reservation.getClientTelephone());
            }
        }
        
        // 3. Récupérer les clients par ID
        List<ClientAbonne> clientsParId = clientIds.stream()
                .map(clientAbonneRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
        
        // 4. Récupérer les clients par téléphone (pour les réservations ponctuelles)
        List<ClientAbonne> clientsParTelephone = clientTelephones.stream()
                .map(clientAbonneRepository::findByTelephone)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
        
        // 5. Combiner et dédupliquer les clients
        Set<Long> idsUniques = new HashSet<>();
        List<ClientAbonne> clientsUniques = new java.util.ArrayList<>();
        
        // Ajouter les clients par ID
        for (ClientAbonne client : clientsParId) {
            if (!idsUniques.contains(client.getId())) {
                clientsUniques.add(client);
                idsUniques.add(client.getId());
            }
        }
        
        // Ajouter les clients par téléphone (qui ne sont pas déjà dans la liste)
        for (ClientAbonne client : clientsParTelephone) {
            if (!idsUniques.contains(client.getId())) {
                clientsUniques.add(client);
                idsUniques.add(client.getId());
            }
        }
        
        // 6. Convertir en DTO
        return clientsUniques.stream()
                .map(client -> new ClientAbonneDTO(
                    client.getId(),
                    client.getNom(),
                    client.getPrenom(),
                    client.getTelephone()
                ))
                .collect(Collectors.toList());
    }
}
