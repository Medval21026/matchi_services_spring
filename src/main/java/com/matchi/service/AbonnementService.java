package com.matchi.service;

import com.matchi.model.*;
import com.matchi.repository.*;
import com.matchi.dto.*;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.matchi.event.AbonnementModifieEvent;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbonnementService {

    private final AbonnementRepository abonnementRepository;
    private final ClientAbonneRepository clientAbonneRepository;
    private final TerrainServiceRepository terrainServiceRepository;
    private final AbonnementHoraireRepository abonnementHoraireRepository;
    private final ReservationPonctuelleRepository reservationPonctuelleRepository;
    private final IndisponibleHoraireRepository indisponibleHoraireRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final KafkaAvailabilityService kafkaAvailabilityService;
    
    @PersistenceContext
    private EntityManager entityManager;

    // ======== METHODES HELPER ========
    
    /**
     * Calcule le nombre de semaines entre deux dates
     */
    private long calculerNombreSemaines(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            return 4; // Par défaut 4 semaines
        }
        long jours = java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin);
        long semaines = jours / 7;
        return semaines > 0 ? semaines : 1; // Au minimum 1 semaine
    }
    
    /**
     * Convertit JourSemaine en DayOfWeek
     */
    private DayOfWeek jourSemaineToJavaDayOfWeek(JourSemaine jourSemaine) {
        switch (jourSemaine) {
            case LUNDI: return DayOfWeek.MONDAY;
            case MARDI: return DayOfWeek.TUESDAY;
            case MERCREDI: return DayOfWeek.WEDNESDAY;
            case JEUDI: return DayOfWeek.THURSDAY;
            case VENDREDI: return DayOfWeek.FRIDAY;
            case SAMEDI: return DayOfWeek.SATURDAY;
            case DIMANCHE: return DayOfWeek.SUNDAY;
            default: return DayOfWeek.MONDAY;
        }
    }
    
    /**
     * Calcule la prochaine date pour un jour de la semaine donné à partir d'une date de début
     * et en ajoutant un nombre de semaines
     * 
     * Exemple: dateDebut = 15/01/2026 (jeudi), jourSemaine = JEUDI, numeroSemaine = 0
     * Résultat: 15/01/2026 (le même jour)
     * 
     * Exemple: dateDebut = 15/01/2026 (jeudi), jourSemaine = JEUDI, numeroSemaine = 1
     * Résultat: 22/01/2026 (jeudi de la semaine suivante)
     * 
     * ✅ CORRECTION : Les dates sont calculées de manière séquentielle pour maintenir
     * les paires de jours consécutifs (ex: JEUDI et VENDREDI)
     */
    /**
     * Calcule la prochaine date pour un jour de la semaine donné à partir d'une date de début
     * et en ajoutant un nombre de semaines
     * 
     * Les semaines sont comptées depuis le lundi de la semaine de la date de début :
     * - Semaine 0 : la semaine qui contient la date de début (du lundi au dimanche)
     * - Semaine 1 : la semaine suivante (du lundi au dimanche)
     * - etc.
     * 
     * Exemple: dateDebut = 15/01/2026 (jeudi), jourSemaine = JEUDI, numeroSemaine = 0
     * Résultat: 15/01/2026 (le même jour)
     * 
     * Exemple: dateDebut = 15/01/2026 (jeudi), jourSemaine = JEUDI, numeroSemaine = 1
     * Résultat: 22/01/2026 (jeudi de la semaine suivante)
     * 
     * Exemple: dateDebut = 24/01/2026 (vendredi), jourSemaine = LUNDI, numeroSemaine = 0
     * Résultat: 27/01/2026 (lundi de la semaine 0, qui est la semaine de la date de début)
     * 
     * Exemple: dateDebut = 24/01/2026 (vendredi), jourSemaine = LUNDI, numeroSemaine = 1
     * Résultat: 02/02/2026 (lundi de la semaine 1, calculé depuis la date de début)
     * 
     * ✅ CORRECTION : Les dates sont calculées de manière séquentielle pour maintenir
     * les paires de jours consécutifs (ex: JEUDI et VENDREDI)
     * ✅ Les semaines sont toujours comptées depuis le lundi de la semaine de la date de début
     */
    /**
     * Calcule la prochaine date pour un jour de la semaine donné à partir d'une date de début
     * et en ajoutant un nombre de semaines
     * 
     * Les semaines sont comptées depuis le lundi de la semaine de la date de début :
     * - Semaine 0 : la première occurrence du jour dans la semaine de la date de début (ou après si le jour est avant la date de début)
     * - Semaine 1 : la semaine suivante (7 jours après la semaine 0)
     * - etc.
     * 
     * Exemple: dateDebut = 15/01/2026 (jeudi), jourSemaine = JEUDI, numeroSemaine = 0
     * Résultat: 15/01/2026 (le même jour)
     * 
     * Exemple: dateDebut = 15/01/2026 (jeudi), jourSemaine = JEUDI, numeroSemaine = 1
     * Résultat: 22/01/2026 (jeudi de la semaine suivante)
     * 
     * Exemple: dateDebut = 24/01/2026 (vendredi), jourSemaine = LUNDI, numeroSemaine = 0
     * Résultat: 27/01/2026 (premier lundi après la date de début, dans la semaine de la date de début)
     * 
     * Exemple: dateDebut = 24/01/2026 (vendredi), jourSemaine = LUNDI, numeroSemaine = 1
     * Résultat: 02/02/2026 (lundi de la semaine 1, 7 jours après la semaine 0)
     * 
     * ✅ CORRECTION : Les dates sont calculées de manière séquentielle pour maintenir
     * les paires de jours consécutifs (ex: JEUDI et VENDREDI)
     * ✅ Les semaines sont toujours comptées depuis la première occurrence du jour après ou égal à la date de début
     */
    private LocalDate calculerDateHoraire(LocalDate dateDebut, JourSemaine jourSemaine, int numeroSemaine) {
        if (dateDebut == null) {
            return null;
        }
        
        DayOfWeek jourCible = jourSemaineToJavaDayOfWeek(jourSemaine);
        DayOfWeek jourActuel = dateDebut.getDayOfWeek();
        
        // Calculer le nombre de jours jusqu'au prochain jour cible (ou le même jour si c'est déjà le bon jour)
        int joursJusquauProchainJour = (jourCible.getValue() - jourActuel.getValue() + 7) % 7;
        
        // Si c'est le même jour et qu'on est à la semaine 0, utiliser la date de début
        if (joursJusquauProchainJour == 0 && numeroSemaine == 0) {
            return dateDebut;
        }
        
        // Si c'est le même jour mais qu'on est à une semaine suivante, aller à la semaine suivante
        if (joursJusquauProchainJour == 0 && numeroSemaine > 0) {
            joursJusquauProchainJour = 7; // Aller à la semaine suivante
        }
        
        // Calculer la première occurrence du jour cible (semaine 0)
        LocalDate premiereOccurrence = dateDebut.plusDays(joursJusquauProchainJour);
        
        // Ajouter les semaines supplémentaires à partir de la première occurrence
        LocalDate dateHoraire = premiereOccurrence.plusWeeks(numeroSemaine);
        
        return dateHoraire;
    }
    
    /**
     * Calcule la date de l'horaire et la décale automatiquement vers le futur si elle est dans le passé.
     * Si la date calculée est dans le passé ou si c'est aujourd'hui mais que l'heure de fin est passée,
     * la date est décalée vers la prochaine occurrence valide du même jour de la semaine.
     * Préserve l'écart relatif entre les horaires (si deux horaires sont à 1 semaine d'écart, ils le restent).
     * 
     * @param dateDebut La date de début de l'abonnement
     * @param jourSemaine Le jour de la semaine de l'horaire
     * @param numeroSemaine Le numéro de semaine (0 = première semaine)
     * @param heureFin L'heure de fin de l'horaire (peut être null)
     * @return La date calculée, décalée vers le futur si nécessaire
     */
    private LocalDate calculerDateHoraireAvecDecalage(LocalDate dateDebut, JourSemaine jourSemaine, int numeroSemaine, java.time.LocalTime heureFin) {
        if (dateDebut == null) {
            return null;
        }
        
        // Calculer la date initiale basée sur la date de début
        LocalDate dateHoraire = calculerDateHoraire(dateDebut, jourSemaine, numeroSemaine);
        if (dateHoraire == null) {
            return null;
        }
        
        LocalDate aujourdhui = LocalDate.now();
        java.time.LocalTime maintenant = java.time.LocalTime.now();
        java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
        
        // Si la date est dans le passé, la décale vers le futur en préservant l'écart relatif
        if (dateHoraire.isBefore(aujourdhui)) {
            // Calculer combien de jours se sont écoulés depuis la date calculée
            long joursEcoules = java.time.temporal.ChronoUnit.DAYS.between(dateHoraire, aujourdhui);
            
            // Calculer combien de semaines complètes se sont écoulées
            long semainesEcoulees = joursEcoules / 7;
            
            // Ajouter au moins une semaine supplémentaire pour être dans le futur
            long semainesAAjouter = semainesEcoulees + 1;
            
            // Si on est le même jour de la semaine aujourd'hui, on peut utiliser aujourd'hui
            // (mais on vérifiera l'heure après)
            DayOfWeek jourCible = jourSemaineToJavaDayOfWeek(jourSemaine);
            DayOfWeek jourAujourdhui = aujourdhui.getDayOfWeek();
            
            if (jourCible == jourAujourdhui && semainesEcoulees == 0) {
                // C'est le même jour aujourd'hui et on est dans la même semaine
                dateHoraire = aujourdhui;
            } else {
                // Décaler vers le futur en préservant l'écart relatif
                dateHoraire = dateHoraire.plusWeeks(semainesAAjouter);
            }
        }
        
        // Si c'est aujourd'hui, vérifier que l'heure de fin n'est pas passée
        if (dateHoraire.equals(aujourdhui) && heureFin != null) {
            // Si heureFin = minuit, c'est valide (fin de journée)
            if (!heureFin.equals(minuit) && (heureFin.isBefore(maintenant) || heureFin.equals(maintenant))) {
                // L'heure est passée, décaler vers la semaine suivante (même jour de la semaine)
                dateHoraire = dateHoraire.plusWeeks(1);
            }
        }
        
        return dateHoraire;
    }
    
    /**
     * Calcule le prix total en additionnant tous les prix des horaires hebdomadaires
     * multiplié par le nombre de semaines (calculé à partir des dates ou 4 par défaut)
     * 
     * Exemple: Si vous avez 2 horaires de 50€ chacun et l'abonnement dure 4 semaines,
     * le prix total sera: (50 + 50) * 4 = 400€
     */
    private BigDecimal calculerPrixTotal(List<AbonnementHoraire> horaires, LocalDate dateDebut, LocalDate dateFin) {
        if (horaires == null || horaires.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Calculer le prix hebdomadaire (somme des prix de tous les horaires)
        BigDecimal prixHebdomadaire = horaires.stream()
                .map(AbonnementHoraire::getPrixHeure)
                .filter(prix -> prix != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculer le nombre de semaines
        long nombreSemaines = calculerNombreSemaines(dateDebut, dateFin);
        
        // Prix total = prix hebdomadaire * nombre de semaines
        return prixHebdomadaire.multiply(BigDecimal.valueOf(nombreSemaines));
    }
    
    /**
     * Détermine le statut de l'abonnement en fonction des dates
     * - ACTIF : si aujourd'hui est entre dateDebut et dateFin
     * - TERMINE : si aujourd'hui est après dateFin
     * - Conserve SUSPENDU si déjà défini manuellement
     */
    private StatutAbonnement determinerStatut(LocalDate dateDebut, LocalDate dateFin, StatutAbonnement statusActuel) {
        // Si le statut est déjà SUSPENDU, on le conserve (gestion manuelle)
        if (statusActuel == StatutAbonnement.SUSPENDU) {
            return StatutAbonnement.SUSPENDU;
        }
        
        LocalDate aujourdhui = LocalDate.now();
        
        // Si la date de fin est dépassée
        if (dateFin != null && aujourdhui.isAfter(dateFin)) {
            return StatutAbonnement.TERMINE;
        }
        
        // Si on est dans la période de validité
        if (dateDebut != null && dateFin != null && 
            !aujourdhui.isBefore(dateDebut) && !aujourdhui.isAfter(dateFin)) {
            return StatutAbonnement.ACTIF;
        }
        
        // Par défaut, ACTIF
        return StatutAbonnement.ACTIF;
    }

    // ======== MAPPERS ========
    private AbonnementDTO toDTO(Abonnement abonnement) {
        List<AbonnementHoraireDTO> horaires = abonnement.getHoraires() == null ? List.of() :
            abonnement.getHoraires().stream()
                .map(h -> {
                    AbonnementHoraireDTO dto = new AbonnementHoraireDTO();
                    dto.setId(h.getId());
                    dto.setDate(h.getDate()); // Ajouter la date
                    dto.setJourSemaine(h.getJourSemaine());
                    dto.setHeureDebut(h.getHeureDebut());
                    dto.setHeureFin(h.getHeureFin());
                    dto.setPrixHeure(h.getPrixHeure());
                    dto.setAbonnementId(h.getAbonnement() != null ? h.getAbonnement().getId() : null);
                    return dto;
                })
                .collect(Collectors.toList());

        return new AbonnementDTO(
                abonnement.getId(),
                abonnement.getTerrain() != null ? abonnement.getTerrain().getId() : null,
                abonnement.getClient() != null ? abonnement.getClient().getId() : null,
                abonnement.getDateDebut(),
                abonnement.getDateFin(),
                abonnement.getPrixTotal(),
                abonnement.getStatus(),
                abonnement.getCreatedAt(),
                horaires
        );
    }

    // ======== SERVICES ========
    public List<AbonnementDTO> getAllAbonnements() {
        // ✅ FILTRER : Afficher uniquement les abonnements actifs
        return abonnementRepository.findByStatus(StatutAbonnement.ACTIF)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AbonnementDTO getAbonnementById(Long id) {
        return abonnementRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable"));
    }

    public List<AbonnementDTO> getAbonnementsByClientId(Long clientId) {
        // ✅ FILTRER : Afficher uniquement les abonnements actifs
        return abonnementRepository.findByClientId(clientId)
                .stream()
                .filter(abonnement -> abonnement.getStatus() == StatutAbonnement.ACTIF)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Valide que la date de début n'est pas dans le passé
     */
    private void validerDateDebutNonPassee(LocalDate dateDebut) {
        if (dateDebut == null) {
            return; // Pas de validation si date non fournie
        }
        
        LocalDate aujourdhui = LocalDate.now();
        
        // Vérifier si la date de début est dans le passé
        if (dateDebut.isBefore(aujourdhui)) {
            throw new IllegalArgumentException(
                String.format("Impossible de créer un abonnement avec une date de début passée. La date %s est antérieure à aujourd'hui (%s)", 
                    dateDebut, aujourdhui)
            );
        }
    }
    
    /**
     * ✅ Valide qu'un créneau horaire d'abonnement ne chevauche pas avec des réservations existantes
     * Vérifie les conflits avec :
     * 1. Les réservations ponctuelles existantes
     * 2. Les autres abonnements actifs (via IndisponibleHoraire)
     */
    private void validerConflitHoraireAbonnement(Long terrainId, LocalDate date, 
                                                 java.time.LocalTime heureDebut, java.time.LocalTime heureFin,
                                                 Long abonnementIdExclu) {
        if (terrainId == null || date == null || heureDebut == null || heureFin == null) {
            return; // Pas de validation si données incomplètes
        }
        
        // 1. Vérifier les conflits avec les réservations ponctuelles existantes
        List<ReservationPonctuelle> reservationsExistantes = reservationPonctuelleRepository.findByTerrain_IdAndDate(terrainId, date);
        
        for (ReservationPonctuelle existante : reservationsExistantes) {
            // Vérifier si les créneaux se chevauchent
            if (creneauxSeChevauchent(heureDebut, heureFin, 
                                     existante.getHeureDebut(), existante.getHeureFin())) {
                throw new IllegalArgumentException(
                    String.format("Conflit avec une réservation ponctuelle : Le créneau %s-%s est déjà réservé pour ce terrain le %s. " +
                                 "Réservation existante : %s-%s",
                        heureDebut, heureFin, date,
                        existante.getHeureDebut(), existante.getHeureFin())
                );
            }
        }
        
        // 2. Vérifier les conflits avec les autres abonnements actifs (via IndisponibleHoraire)
        List<IndisponibleHoraire> horairesIndisponibles = indisponibleHoraireRepository.findByTerrainIdAndDate(terrainId, date);
        
        // ✅ CORRECTION : Récupérer tous les IDs des horaires de l'abonnement en cours de création/modification
        // Le sourceId dans indisponible_horaire est l'ID de l'AbonnementHoraire, pas l'ID de l'Abonnement
        java.util.Set<Long> horaireIdsExclus = new java.util.HashSet<>();
        if (abonnementIdExclu != null) {
            List<AbonnementHoraire> horairesAbonnement = abonnementHoraireRepository.findByAbonnementId(abonnementIdExclu);
            horaireIdsExclus = horairesAbonnement.stream()
                    .map(AbonnementHoraire::getId)
                    .collect(java.util.stream.Collectors.toSet());
        }
        
        for (IndisponibleHoraire indispo : horairesIndisponibles) {
            // Exclure les horaires de l'abonnement en cours de création/modification
            // Le sourceId est l'ID de l'AbonnementHoraire
            if (indispo.getTypeReservation() == TypeReservation.ABONNEMENT && 
                indispo.getSourceId() != null && 
                horaireIdsExclus.contains(indispo.getSourceId())) {
                continue; // Ignorer les horaires de l'abonnement actuel
            }
            
            // Vérifier si les créneaux se chevauchent
            if (creneauxSeChevauchent(heureDebut, heureFin, 
                                     indispo.getHeureDebut(), indispo.getHeureFin())) {
                throw new IllegalArgumentException(
                    String.format("Conflit avec un abonnement existant : Le créneau %s-%s est déjà occupé par un autre abonnement pour ce terrain le %s. " +
                                 "Créneau occupé : %s-%s",
                        heureDebut, heureFin, date,
                        indispo.getHeureDebut(), indispo.getHeureFin())
                );
            }
        }
    }
    
    /**
     * ✅ Vérifie si deux créneaux horaires se chevauchent
     * Gère le cas spécial où heureFin = 00:00 (minuit = fin de journée)
     */
    private boolean creneauxSeChevauchent(java.time.LocalTime debut1, java.time.LocalTime fin1, 
                                          java.time.LocalTime debut2, java.time.LocalTime fin2) {
        if (debut1 == null || fin1 == null || debut2 == null || fin2 == null) {
            return false;
        }
        
        // Cas spécial : si les deux créneaux ont la même heure de début, ils se chevauchent
        if (debut1.equals(debut2)) {
            return true;
        }
        
        // Cas spécial : si fin1 ou fin2 est 00h (minuit), on doit gérer différemment
        java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
        boolean fin1EstMinuit = fin1.equals(minuit);
        boolean fin2EstMinuit = fin2.equals(minuit);
        
        if (fin1EstMinuit) {
            // fin1 est minuit, donc le créneau 1 va jusqu'à la fin de la journée
            return debut1.isBefore(fin2);
        } else if (fin2EstMinuit) {
            // fin2 est minuit, donc le créneau 2 va jusqu'à la fin de la journée
            return debut2.isBefore(fin1);
        } else {
            // Cas normal : aucun créneau ne se termine à minuit
            // Deux créneaux se chevauchent si : debut1 < fin2 && fin1 > debut2
            return (debut1.isBefore(fin2) && fin1.isAfter(debut2));
        }
    }
    
    // ======== CREATE ========
    @Transactional
    public AbonnementDTO createAbonnement(AbonnementCreateDTO dto) {
        // ✅ VALIDATION : Vérifier que Kafka est disponible avant de créer un abonnement
        if (!kafkaAvailabilityService.isKafkaAvailable()) {
            throw new IllegalStateException("Impossible de créer un abonnement : Kafka n'est pas démarré ou n'est pas disponible. Veuillez démarrer Kafka avant de créer un abonnement.");
        }
        
        // ✅ VALIDATION : Vérifier les données obligatoires
        if (dto == null) {
            throw new IllegalArgumentException("Les données de l'abonnement ne peuvent pas être nulles");
        }
        
        if (dto.getTerrainId() == null) {
            throw new IllegalArgumentException("L'ID du terrain est obligatoire");
        }
        
        if (dto.getClientTelephone() == null) {
            throw new IllegalArgumentException("Le numéro de téléphone du client est obligatoire");
        }
        
        if (dto.getDateDebut() == null) {
            throw new IllegalArgumentException("La date de début est obligatoire");
        }
        
        // ✅ CALCUL AUTOMATIQUE : Si dateFin est null, calculer automatiquement dateDebut + 28 jours
        LocalDate dateFin = dto.getDateFin();
        if (dateFin == null) {
            dateFin = dto.getDateDebut().plusDays(28);
        }
        
        if (dateFin.isBefore(dto.getDateDebut())) {
            throw new IllegalArgumentException(
                String.format("La date de fin (%s) ne peut pas être antérieure à la date de début (%s)",
                    dateFin, dto.getDateDebut())
            );
        }
        
        if (dto.getHoraires() == null || dto.getHoraires().isEmpty()) {
            throw new IllegalArgumentException("Au moins un horaire est obligatoire pour créer un abonnement");
        }
        
        // ✅ CORRECTION : Permettre la création d'abonnement avec une date de début passée
        // Seuls les horaires non passés seront générés

        Abonnement abonnement = new Abonnement();

        // Terrain
        TerrainService terrain = terrainServiceRepository.findById(dto.getTerrainId())
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Terrain introuvable avec l'ID %d", dto.getTerrainId())
                ));
        abonnement.setTerrain(terrain);

        // Client - ✅ Recherche par numéro de téléphone au lieu de l'ID
        ClientAbonne client = clientAbonneRepository.findByTelephone(dto.getClientTelephone())
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Client introuvable avec le numéro de téléphone %d", dto.getClientTelephone())
                ));
        abonnement.setClient(client);

        abonnement.setDateDebut(dto.getDateDebut());
        abonnement.setDateFin(dateFin); // Utiliser la date de fin calculée automatiquement si nécessaire

        // *** GENERATION DES HORAIRES REPETITIFS ***
        // Calculer le nombre de semaines
        long nombreSemaines = calculerNombreSemaines(dto.getDateDebut(), dateFin);
        
        // Générer tous les horaires répétés pour toutes les semaines
        List<AbonnementHoraire> horairesRepetitifs = new ArrayList<>();
        
        if (dto.getHoraires() != null && !dto.getHoraires().isEmpty()) {
            // ✅ CORRECTION : Calculer d'abord toutes les premières occurrences (semaine 0)
            // pour préserver l'ordre séquentiel des jours dans chaque semaine
            java.util.Map<JourSemaine, LocalDate> premieresOccurrences = new java.util.HashMap<>();
            for (AbonnementHoraireDTO hdto : dto.getHoraires()) {
                if (!premieresOccurrences.containsKey(hdto.getJourSemaine())) {
                    // Calculer la première occurrence de ce jour à partir de la date de début
                    LocalDate premiereOccurrence = calculerDateHoraire(dto.getDateDebut(), hdto.getJourSemaine(), 0);
                    premieresOccurrences.put(hdto.getJourSemaine(), premiereOccurrence);
                }
            }
            
            // Pour chaque semaine (de 0 à nombreSemaines-1)
            for (int semaine = 0; semaine < nombreSemaines; semaine++) {
                // Pour chaque horaire de base fourni (dans l'ordre fourni)
                for (AbonnementHoraireDTO hdto : dto.getHoraires()) {
                    // ✅ CALCUL AUTOMATIQUE DE HEURE FIN
                    java.time.LocalTime heureDebut = hdto.getHeureDebut();
                    java.time.LocalTime heureFin = hdto.getHeureFin();
                    if (heureFin == null && heureDebut != null) {
                        heureFin = heureDebut.plusHours(1);
                    }
                    
                    // ✅ VALIDATION : vérifier que les horaires sont dans l'intervalle d'ouverture/fermeture
                    if (abonnement.getTerrain() != null) {
                        validerHoraires(abonnement.getTerrain(), heureDebut, heureFin);
                    }
                    
                    // *** CALCUL DE LA DATE PRECISE ***
                    // ✅ CORRECTION : Utiliser la première occurrence calculée et ajouter les semaines
                    // Cela garantit que l'ordre séquentiel des jours est préservé dans chaque semaine
                    LocalDate premiereOccurrence = premieresOccurrences.get(hdto.getJourSemaine());
                    LocalDate dateHoraire = premiereOccurrence != null ? premiereOccurrence.plusWeeks(semaine) : null;
                    
                    // ✅ VALIDATION : Ne générer que les horaires non passés
                    if (dateHoraire != null) {
                        LocalDate aujourdhui = LocalDate.now();
                        java.time.LocalTime maintenant = java.time.LocalTime.now();
                        java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
                        
                        // Si la date est dans le passé, ne pas créer cet horaire
                        if (dateHoraire.isBefore(aujourdhui)) {
                            continue;
                        }
                        
                        // Si c'est aujourd'hui, vérifier que l'heure de fin n'est pas passée
                        if (dateHoraire.equals(aujourdhui) && heureFin != null) {
                            // Si heureFin = minuit, c'est valide (fin de journée)
                            if (!heureFin.equals(minuit) && (heureFin.isBefore(maintenant) || heureFin.equals(maintenant))) {
                                // L'heure est passée, ne pas créer cet horaire
                                continue;
                            }
                        }
                    }
                    
                    // ✅ VALIDATION : Ne générer que les horaires dans la période valide (dateDebut <= date <= dateFin)
                    if (dateHoraire != null && dateFin != null) {
                        if (dateHoraire.isAfter(dateFin)) {
                            // Cette date dépasse la date de fin, ne pas créer cet horaire
                            continue;
                        }
                    }
                    
                    // ✅ VALIDATION : Vérifier les conflits avec les réservations existantes
                    if (abonnement.getTerrain() != null && dateHoraire != null && heureDebut != null && heureFin != null) {
                        try {
                            validerConflitHoraireAbonnement(
                                abonnement.getTerrain().getId(),
                                dateHoraire,
                                heureDebut,
                                heureFin,
                                null // Pas d'exclusion pour une nouvelle création
                            );
                        } catch (IllegalArgumentException e) {
                            // Enrichir le message d'erreur avec plus de contexte
                            throw new IllegalArgumentException(
                                String.format("Erreur lors de la création de l'horaire pour le %s (semaine %d) : %s",
                                    hdto.getJourSemaine(), semaine, e.getMessage()),
                                e
                            );
                        }
                    }
                    
                    AbonnementHoraire h = new AbonnementHoraire();
                    h.setAbonnement(abonnement);
                    h.setJourSemaine(hdto.getJourSemaine());
                    h.setDate(dateHoraire);
                    h.setHeureDebut(heureDebut);
                    h.setHeureFin(heureFin);
                    h.setPrixHeure(hdto.getPrixHeure());
                    horairesRepetitifs.add(h);
                }
            }
        }

        abonnement.setHoraires(horairesRepetitifs);

        // *** CALCUL AUTOMATIQUE DU PRIX TOTAL ***
        // Le prix total = somme de TOUS les horaires répétés
        BigDecimal prixTotal = horairesRepetitifs.stream()
                .map(AbonnementHoraire::getPrixHeure)
                .filter(prix -> prix != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        abonnement.setPrixTotal(prixTotal);

        // *** DETERMINATION AUTOMATIQUE DU STATUT ***
        StatutAbonnement statutInitial = dto.getStatus() != null ? dto.getStatus() : StatutAbonnement.ACTIF;
        StatutAbonnement statutFinal = determinerStatut(dto.getDateDebut(), dateFin, statutInitial);
        abonnement.setStatus(statutFinal);

        Abonnement saved = abonnementRepository.save(abonnement);

        // Enregistrer TOUS les horaires répétitifs
        if (!horairesRepetitifs.isEmpty()) {
            horairesRepetitifs.forEach(h -> h.setAbonnement(saved));
            abonnementHoraireRepository.saveAll(horairesRepetitifs);
        }

        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
        // L'événement AFTER_COMMIT garantit que les données sont déjà commitées en base
        if (saved.getTerrain() != null) {
            eventPublisher.publishEvent(new AbonnementModifieEvent(saved.getTerrain().getId()));
        }

        return toDTO(saved);
    }

    // ======== UPDATE ========
    @Transactional
    public AbonnementDTO updateAbonnement(Long id, AbonnementUpdateDTO dto) {
        Abonnement abonnement = abonnementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable"));

        // Terrain
        if (dto.getTerrainId() != null) {
            TerrainService terrain = terrainServiceRepository.findById(dto.getTerrainId())
                    .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));
            abonnement.setTerrain(terrain);
        }

        // Client - ✅ Recherche par numéro de téléphone au lieu de l'ID
        if (dto.getClientTelephone() != null) {
            ClientAbonne client = clientAbonneRepository.findByTelephone(dto.getClientTelephone())
                    .orElseThrow(() -> new IllegalArgumentException("Client non trouvé avec le téléphone: " + dto.getClientTelephone()));
            abonnement.setClient(client);
        }

        // Garder trace des dates modifiées
        boolean datesModifiees = false;
        boolean dateDebutModifiee = false;
        
        LocalDate ancienneDateDebut = abonnement.getDateDebut();
        
        if (dto.getDateDebut() != null) {
        
            // ❌ SUPPRIMER la validation bloquante
            // validerDateDebutNonPassee(dto.getDateDebut());
        
            // Vérifier si la date de début a vraiment changé
            if (!dto.getDateDebut().equals(ancienneDateDebut)) {
                dateDebutModifiee = true;
                datesModifiees = true;
                abonnement.setDateDebut(dto.getDateDebut());
            }
        }
        
        if (dto.getDateFin() != null) {
            abonnement.setDateFin(dto.getDateFin());
            datesModifiees = true;
        } else {
            // ✅ CALCUL AUTOMATIQUE : Si dateFin n'est pas fournie, calculer automatiquement dateDebut + 28 jours
            // Utiliser la date de début actuelle (qui peut avoir été modifiée)
            LocalDate dateDebutActuelle = abonnement.getDateDebut();
            LocalDate nouvelleDateFin = dateDebutActuelle.plusDays(28);
            abonnement.setDateFin(nouvelleDateFin);
            datesModifiees = true;
        }
        
        
        // ✅ NOUVEAU : Si la date de début a changé mais les horaires ne sont pas fournis,
        // mettre à jour les dates des horaires existants avec la nouvelle date de début
        if (dateDebutModifiee && dto.getHoraires() == null && !abonnement.getHoraires().isEmpty()) {
            // Récupérer les horaires existants et les trier par date pour préserver la séquence
            List<AbonnementHoraire> horairesExistants = new ArrayList<>(abonnement.getHoraires());
            horairesExistants.sort((h1, h2) -> {
                if (h1.getDate() == null && h2.getDate() == null) return 0;
                if (h1.getDate() == null) return 1;
                if (h2.getDate() == null) return -1;
                return h1.getDate().compareTo(h2.getDate());
            });
            
            // Extraire les configurations uniques (jour + heure) dans l'ordre d'apparition
            java.util.List<java.util.Map.Entry<JourSemaine, java.time.LocalTime>> configurationsUniques = new java.util.ArrayList<>();
            java.util.Set<String> configurationsVues = new java.util.HashSet<>();
            for (AbonnementHoraire horaire : horairesExistants) {
                if (horaire.getJourSemaine() != null && horaire.getHeureDebut() != null) {
                    String cle = horaire.getJourSemaine().name() + "_" + horaire.getHeureDebut().toString();
                    if (!configurationsVues.contains(cle)) {
                        configurationsUniques.add(new java.util.AbstractMap.SimpleEntry<>(
                            horaire.getJourSemaine(), horaire.getHeureDebut()));
                        configurationsVues.add(cle);
                    }
                }
            }
            
            // Mettre à jour les dates de chaque horaire existant en préservant la séquence
            // On parcourt les horaires dans l'ordre et on calcule la semaine en fonction de leur position
            int indexGlobal = 0;
            int nombreConfigs = configurationsUniques.size();
            
            for (AbonnementHoraire horaire : horairesExistants) {
                if (horaire.getJourSemaine() != null && horaire.getHeureDebut() != null) {
                    JourSemaine jour = horaire.getJourSemaine();
                    
                    // Calculer le numéro de semaine : chaque cycle complet de toutes les configurations = 1 semaine
                    int semaine = nombreConfigs > 0 ? (indexGlobal / nombreConfigs) : 0;
                    
                    // Calculer la nouvelle date basée sur la nouvelle date de début
                    // ✅ CORRECTION : Utiliser la méthode avec décalage automatique pour les dates passées
                    LocalDate nouvelleDate = calculerDateHoraireAvecDecalage(
                        abonnement.getDateDebut(), 
                        jour, 
                        semaine, 
                        horaire.getHeureFin()
                    );
                    
                    // ✅ VALIDATION : Vérifier les conflits (seulement si la date change vraiment)
                    if (horaire.getDate() == null || !horaire.getDate().equals(nouvelleDate)) {
                        if (abonnement.getTerrain() != null && nouvelleDate != null && 
                            horaire.getHeureDebut() != null && horaire.getHeureFin() != null) {
                            validerConflitHoraireAbonnement(
                                abonnement.getTerrain().getId(),
                                nouvelleDate,
                                horaire.getHeureDebut(),
                                horaire.getHeureFin(),
                                id // Exclure l'abonnement en cours de modification
                            );
                        }
                    }
                    
                    // ✅ Mettre à jour la date de l'horaire existant
                    horaire.setDate(nouvelleDate);
                    
                    indexGlobal++;
                }
            }
            
            // ✅ Sauvegarder les horaires modifiés
            abonnementHoraireRepository.saveAll(horairesExistants);
            
            // Recalculer le prix total (au cas où)
            BigDecimal prixTotal = horairesExistants.stream()
                    .map(AbonnementHoraire::getPrixHeure)
                    .filter(p -> p != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            abonnement.setPrixTotal(prixTotal);
            
            // ✅ FLUSH : Forcer l'écriture des horaires modifiés en base
            entityManager.flush();
            entityManager.clear(); // Vider le cache pour forcer la relecture
        }
        
        // Horaires
        if (dto.getHoraires() != null && !dto.getHoraires().isEmpty()) {
            // ✅ CORRECTION : Ajouter les nouveaux horaires aux horaires existants
            // ❌ INTERDIT : Ne jamais utiliser setHoraires() avec orphanRemoval = true
            // ✅ OBLIGATOIRE : Toujours modifier la liste existante avec add()
            // Les horaires existants sont conservés, on ajoute seulement les nouveaux

            // *** GENERATION DES HORAIRES REPETITIFS ***
            // Calculer le nombre de semaines
            long nombreSemaines = calculerNombreSemaines(abonnement.getDateDebut(), abonnement.getDateFin());
            
            // Pour chaque semaine (de 0 à nombreSemaines-1)
            for (int semaine = 0; semaine < nombreSemaines; semaine++) {
                // Pour chaque horaire de base fourni (dans l'ordre fourni)
                for (AbonnementHoraireDTO hdto : dto.getHoraires()) {
                    // ✅ CALCUL AUTOMATIQUE DE HEURE FIN
                    java.time.LocalTime heureDebut = hdto.getHeureDebut();
                    java.time.LocalTime heureFin = hdto.getHeureFin();
                    if (heureFin == null && heureDebut != null) {
                        heureFin = heureDebut.plusHours(1);
                    }
                    
                    // ✅ VALIDATION : vérifier que les horaires sont dans l'intervalle d'ouverture/fermeture
                    if (abonnement.getTerrain() != null) {
                        validerHoraires(abonnement.getTerrain(), heureDebut, heureFin);
                    }
                    
                    // *** CALCUL DE LA DATE PRECISE ***
                    // ✅ CORRECTION : Calculer directement la date pour chaque semaine depuis la date de début
                    // Cela garantit que les dates sont correctement réparties dans chaque semaine successive
                    // et que l'ordre séquentiel des jours est préservé dans chaque semaine
                    LocalDate dateHoraire = calculerDateHoraire(abonnement.getDateDebut(), hdto.getJourSemaine(), semaine);
                    
                    // ✅ VALIDATION : Ne créer que les horaires non passés
                    // On utilise toujours la date de début comme référence, mais on ne crée pas les horaires passés
                    if (dateHoraire != null) {
                        LocalDate aujourdhui = LocalDate.now();
                        java.time.LocalTime maintenant = java.time.LocalTime.now();
                        java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
                        
                        // Si la date est dans le passé, ne pas créer cet horaire
                        if (dateHoraire.isBefore(aujourdhui)) {
                            continue; // Ne pas créer les horaires passés
                        }
                        
                        // Si c'est aujourd'hui, vérifier que l'heure de fin n'est pas passée
                        if (dateHoraire.equals(aujourdhui) && heureFin != null) {
                            // Si heureFin = minuit, c'est valide (fin de journée)
                            if (!heureFin.equals(minuit) && (heureFin.isBefore(maintenant) || heureFin.equals(maintenant))) {
                                // L'heure est passée, ne pas créer cet horaire
                                continue;
                            }
                        }
                    }
                    
                    // ✅ VALIDATION : Ne créer que les horaires dans la période valide (dateDebut <= date <= dateFin)
                    if (dateHoraire != null && abonnement.getDateFin() != null) {
                        if (dateHoraire.isAfter(abonnement.getDateFin())) {
                            // Cette date dépasse la date de fin, ne pas créer cet horaire
                            continue;
                        }
                    }
                    
                    // ✅ VALIDATION : Vérifier les conflits avec les réservations existantes
                    if (abonnement.getTerrain() != null && dateHoraire != null && heureDebut != null && heureFin != null) {
                        try {
                            validerConflitHoraireAbonnement(
                                abonnement.getTerrain().getId(),
                                dateHoraire,
                                heureDebut,
                                heureFin,
                                id // Exclure l'abonnement en cours de modification
                            );
                        } catch (IllegalArgumentException e) {
                            // Enrichir le message d'erreur avec plus de contexte
                            throw new IllegalArgumentException(
                                String.format("Erreur lors de la modification de l'horaire pour le %s (semaine %d) : %s",
                                    hdto.getJourSemaine(), semaine, e.getMessage()),
                                e
                            );
                        }
                    }
                    
                    // ✅ CRÉER le nouvel horaire
                    AbonnementHoraire h = new AbonnementHoraire();
                    // 🔴 TRÈS IMPORTANT : Définir la relation bidirectionnelle
                    h.setAbonnement(abonnement);
                    h.setJourSemaine(hdto.getJourSemaine());
                    h.setDate(dateHoraire);
                    h.setHeureDebut(heureDebut);
                    h.setHeureFin(heureFin);
                    h.setPrixHeure(hdto.getPrixHeure());
                    
                    // ✅ AJOUTER directement à la liste existante (NE JAMAIS utiliser setHoraires())
                    // ❌ INTERDIT : abonnement.setHoraires(horairesRepetitifs);
                    // ✅ OBLIGATOIRE : Utiliser add() sur la liste existante
                    abonnement.getHoraires().add(h);
                }
            }

            // *** RECALCUL AUTOMATIQUE DU PRIX TOTAL ***
            // Le prix total = somme de TOUS les horaires répétés
            BigDecimal prixTotal = abonnement.getHoraires().stream()
                    .map(AbonnementHoraire::getPrixHeure)
                    .filter(prix -> prix != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            abonnement.setPrixTotal(prixTotal);
        }
        // Si seulement les dates ont changé sans modifier les horaires, ne pas recalculer le prix
        // Le prix total reste inchangé

        // *** GESTION DU STATUT ***
        // Si l'utilisateur a fourni un statut explicite, l'utiliser directement
        if (dto.getStatus() != null) {
            abonnement.setStatus(dto.getStatus());
        } else if (datesModifiees) {
            // Seulement si les dates ont changé et qu'aucun statut n'est fourni, recalculer le statut
            StatutAbonnement statutFinal = determinerStatut(
                abonnement.getDateDebut(), 
                abonnement.getDateFin(), 
                abonnement.getStatus()
            );
            abonnement.setStatus(statutFinal);
        }
        // Sinon, garder le statut actuel inchangé

        // ✅ Sauvegarder l'abonnement (les horaires seront sauvegardés via cascade)
        Abonnement saved = abonnementRepository.save(abonnement);
        
        // ✅ IMPORTANT : Sauvegarder explicitement les horaires pour garantir qu'ils sont persistés
        if (saved.getHoraires() != null && !saved.getHoraires().isEmpty()) {
            // S'assurer que tous les horaires sont bien liés à l'abonnement sauvegardé
            saved.getHoraires().forEach(h -> h.setAbonnement(saved));
            abonnementHoraireRepository.saveAll(saved.getHoraires());
            // ✅ FLUSH IMMÉDIAT : Forcer l'écriture des horaires en base
            entityManager.flush();
        }
        
        // ✅ FLUSH : Forcer l'écriture en base avant la publication de l'événement
        // Flush pour s'assurer que toutes les modifications sont bien écrites
        entityManager.flush();
        
        // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
        entityManager.clear();
        
        // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
        entityManager.flush();
        
        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
        // La synchronisation sera déclenchée automatiquement après le commit de la transaction
        if (saved.getTerrain() != null) {
            eventPublisher.publishEvent(new AbonnementModifieEvent(saved.getTerrain().getId()));
        }
        
        return toDTO(saved);
    }

    // ======== DELETE ========
    @Transactional
    public void deleteAbonnement(Long id) {
        // Récupérer l'abonnement avant suppression pour synchroniser le terrain
        Abonnement abonnement = abonnementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable"));
        Long terrainId = abonnement.getTerrain() != null ? abonnement.getTerrain().getId() : null;
        
        abonnementRepository.deleteById(id);
        
        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
        if (terrainId != null) {
            eventPublisher.publishEvent(new AbonnementModifieEvent(terrainId));
        }
    }
    
    /**
     * Valide que les horaires sont dans l'intervalle d'ouverture/fermeture du terrain
     */
    private void validerHoraires(TerrainService terrain, java.time.LocalTime heureDebut, java.time.LocalTime heureFin) {
        if (terrain.getHeureOuverture() == null || terrain.getHeureFermeture() == null) {
            // Si les heures ne sont pas définies, on ne valide pas
            return;
        }
        
        if (heureDebut == null || heureFin == null) {
            throw new IllegalArgumentException("Les heures de début et de fin doivent être renseignées");
        }
        
        // ✅ GESTION DES CRÉNEAUX TRAVERSANT MINUIT
        // Si heureFin < heureDebut, le créneau traverse minuit (ex: 18h -> 2h)
        boolean creneauTraverseMinuit = heureFin.isBefore(heureDebut);
        java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
        
        if (!creneauTraverseMinuit && heureFin.equals(minuit)) {
            // Si heureFin est minuit et heureDebut < minuit, c'est considéré comme la fin de la journée (24:00)
            // Pas besoin de vérifier heureDebut < heureFin dans ce cas
        } else if (!creneauTraverseMinuit) {
            // Créneau normal : vérifier que heureDebut < heureFin
            if (heureDebut.isAfter(heureFin) || heureDebut.equals(heureFin)) {
                throw new IllegalArgumentException(
                    String.format("L'heure de début (%s) doit être avant l'heure de fin (%s)", 
                        heureDebut, heureFin)
                );
            }
        }
        // Si creneauTraverseMinuit = true, on valide différemment (voir plus bas)
        
        java.time.LocalTime heureOuverture = terrain.getHeureOuverture();
        java.time.LocalTime heureFermeture = terrain.getHeureFermeture();
        
        // Vérifier si le terrain ferme après minuit (ex: 18h -> 2h)
        boolean terrainFermeApresMinuit = heureFermeture.isBefore(heureOuverture);
        
        if (creneauTraverseMinuit) {
            // ✅ CRÉNEAU TRAVERSANT MINUIT (ex: 18h -> 2h)
            // Si le terrain ferme après minuit, les heures après minuit sont valides
            if (terrainFermeApresMinuit) {
                // Vérifier que heureDebut est dans la plage valide :
                // - Soit >= heureOuverture (ex: 18h, 19h, ..., 23h)
                // - Soit <= heureFermeture (ex: 00h, 01h, 02h)
                boolean heureDebutValide = (heureDebut.isAfter(heureOuverture) || heureDebut.equals(heureOuverture)) ||
                                          (heureDebut.isBefore(heureOuverture) && (heureDebut.isBefore(heureFermeture) || heureDebut.equals(heureFermeture)));
                
                if (!heureDebutValide) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de début (%s) n'est pas dans la plage d'ouverture du terrain (%s - %s)", 
                            heureDebut, heureOuverture, heureFermeture)
                    );
                }
                
                // Vérifier que heureFin (du lendemain) est avant ou égale à l'heure de fermeture
                if (heureFin.isAfter(heureFermeture)) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de fin (%s) est après l'heure de fermeture du terrain (%s). " +
                                     "Pour un créneau traversant minuit, l'heure de fin doit être <= heure de fermeture.", 
                            heureFin, heureFermeture)
                    );
                }
            } else {
                // Terrain normal mais créneau traverse minuit (ne devrait pas arriver normalement)
                if (heureDebut.isBefore(heureOuverture)) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de début (%s) est avant l'heure d'ouverture du terrain (%s)", 
                            heureDebut, heureOuverture)
                    );
                }
            }
        } else {
            // Créneau normal : vérifier que heureDebut est dans la plage d'ouverture
            if (terrainFermeApresMinuit) {
                // Terrain ferme après minuit : heureDebut peut être >= heureOuverture OU <= heureFermeture
                boolean heureDebutValide = (heureDebut.isAfter(heureOuverture) || heureDebut.equals(heureOuverture)) ||
                                          (heureDebut.isBefore(heureOuverture) && (heureDebut.isBefore(heureFermeture) || heureDebut.equals(heureFermeture)));
                
                if (!heureDebutValide) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de début (%s) n'est pas dans la plage d'ouverture du terrain (%s - %s)", 
                            heureDebut, heureOuverture, heureFermeture)
                    );
                }
                
                // Vérifier que heureFin est dans la plage
                if (heureFin.isAfter(heureFermeture) && heureFin.isBefore(heureOuverture)) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de fin (%s) n'est pas dans la plage d'ouverture du terrain (%s - %s)", 
                            heureFin, heureOuverture, heureFermeture)
                    );
                }
            } else if (heureFermeture.equals(minuit)) {
                // ✅ GESTION DU CAS : heureFermeture = 00h (minuit)
                // Si fermeture à minuit, permettre les réservations jusqu'à 23h (qui se terminent à 00h)
                // Exemple : heureDebut 18h → peut réserver 19h, 20h, 21h, 22h, 23h (qui se terminent à 00h)
                java.time.LocalTime vingtTroisHeures = java.time.LocalTime.of(23, 0);
                
                // Vérifier que heureDebut est au maximum 23h
                if (heureDebut.isAfter(vingtTroisHeures)) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de début (%s) ne peut pas être après 23h lorsque le terrain ferme à minuit", 
                            heureDebut)
                    );
                }
                
                // Si heureFin = 00h, c'est valide
                // Sinon, vérifier que heureFin <= 23h
                if (!heureFin.equals(minuit) && heureFin.isAfter(vingtTroisHeures)) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de fin (%s) ne peut pas être après 23h lorsque le terrain ferme à minuit", 
                            heureFin)
                    );
                }
            } else {
                // Cas normal : heureFermeture n'est pas minuit et créneau ne traverse pas minuit
                // Vérifier que heureDebut est avant ou égale à l'heure de fermeture
                if (heureDebut.isAfter(heureFermeture)) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de début (%s) est après l'heure de fermeture du terrain (%s)", 
                            heureDebut, heureFermeture)
                    );
                }
                
                // Vérifier que heureFin est avant ou égale à l'heure de fermeture
                if (heureFin.isAfter(heureFermeture)) {
                    throw new IllegalArgumentException(
                        String.format("L'heure de fin (%s) est après l'heure de fermeture du terrain (%s)", 
                            heureFin, heureFermeture)
                    );
                }
            }
        }
    }
}