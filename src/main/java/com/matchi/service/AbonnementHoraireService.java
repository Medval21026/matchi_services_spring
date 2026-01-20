package com.matchi.service;

import com.matchi.dto.AbonnementHoraireDTO;
import com.matchi.model.Abonnement;
import com.matchi.model.AbonnementHoraire;
import com.matchi.model.IndisponibleHoraire;
import com.matchi.model.JourSemaine;
import com.matchi.model.ReservationPonctuelle;
import com.matchi.model.TypeReservation;
import com.matchi.repository.AbonnementHoraireRepository;
import com.matchi.repository.AbonnementRepository;
import com.matchi.repository.IndisponibleHoraireRepository;
import com.matchi.repository.ReservationPonctuelleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.matchi.event.AbonnementModifieEvent;
import com.matchi.event.AbonnementHoraireChangeEvent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbonnementHoraireService {

    private final AbonnementHoraireRepository horaireRepository;
    private final AbonnementRepository abonnementRepository;
    private final ReservationPonctuelleRepository reservationPonctuelleRepository;
    private final IndisponibleHoraireRepository indisponibleHoraireRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @PersistenceContext
    private EntityManager entityManager;

    // ======== METHODES HELPER ========
    
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
     * ✅ Valide qu'un créneau horaire d'abonnement ne chevauche pas avec des réservations existantes
     * Vérifie les conflits avec :
     * 1. Les réservations ponctuelles existantes
     * 2. Les autres abonnements actifs (via IndisponibleHoraire)
     */
    private void validerConflitHoraireAbonnement(Long terrainId, LocalDate date, 
                                                 LocalTime heureDebut, LocalTime heureFin,
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
        
        // ✅ CORRECTION : Récupérer tous les IDs des horaires de l'abonnement en cours de modification
        // Le sourceId dans indisponible_horaire est l'ID de l'AbonnementHoraire, pas l'ID de l'Abonnement
        java.util.Set<Long> horaireIdsExclus = new java.util.HashSet<>();
        if (abonnementIdExclu != null) {
            List<AbonnementHoraire> horairesAbonnement = horaireRepository.findByAbonnementId(abonnementIdExclu);
            horaireIdsExclus = horairesAbonnement.stream()
                    .map(AbonnementHoraire::getId)
                    .collect(java.util.stream.Collectors.toSet());
        }
        
        for (IndisponibleHoraire indispo : horairesIndisponibles) {
            // Exclure les horaires de l'abonnement en cours de modification
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
    private boolean creneauxSeChevauchent(LocalTime debut1, LocalTime fin1, 
                                          LocalTime debut2, LocalTime fin2) {
        if (debut1 == null || fin1 == null || debut2 == null || fin2 == null) {
            return false;
        }
        
        // Cas spécial : si les deux créneaux ont la même heure de début, ils se chevauchent
        if (debut1.equals(debut2)) {
            return true;
        }
        
        // Cas spécial : si fin1 ou fin2 est 00h (minuit), on doit gérer différemment
        LocalTime minuit = LocalTime.of(0, 0);
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
    
    /**
     * Calcule la nouvelle date pour un horaire quand on change le jour de la semaine
     * en gardant la même semaine relative
     */
    private LocalDate calculerNouvelleDate(LocalDate dateActuelle, JourSemaine nouveauJour, LocalDate dateDebutAbonnement) {
        if (dateActuelle == null || dateDebutAbonnement == null) {
            return null;
        }
        
        // Calculer le nombre de semaines écoulées depuis le début de l'abonnement
        long joursEcoules = java.time.temporal.ChronoUnit.DAYS.between(dateDebutAbonnement, dateActuelle);
        int numeroSemaine = (int) (joursEcoules / 7);
        
        // Calculer la nouvelle date pour le nouveau jour dans la même semaine
        DayOfWeek jourCible = jourSemaineToJavaDayOfWeek(nouveauJour);
        DayOfWeek jourDebut = dateDebutAbonnement.getDayOfWeek();
        
        // Calculer le nombre de jours jusqu'au jour cible
        int joursJusquauProchainJour = (jourCible.getValue() - jourDebut.getValue() + 7) % 7;
        
        // Calculer la date finale
        LocalDate nouvelleDate = dateDebutAbonnement.plusDays(joursJusquauProchainJour).plusWeeks(numeroSemaine);
        
        return nouvelleDate;
    }

    // ======== MAPPERS ========
    private AbonnementHoraireDTO toDTO(AbonnementHoraire horaire) {
        return new AbonnementHoraireDTO(
                horaire.getId(),
                horaire.getAbonnement() != null ? horaire.getAbonnement().getId() : null,
                horaire.getDate(), // Inclure la date maintenant
                horaire.getJourSemaine(),
                horaire.getHeureDebut(),
                horaire.getHeureFin(),
                horaire.getPrixHeure()
        );
    }

    private AbonnementHoraire toEntity(AbonnementHoraireDTO dto) {
        AbonnementHoraire horaire = new AbonnementHoraire();
        horaire.setId(dto.getId());
        horaire.setDate(dto.getDate()); // Inclure la date
        horaire.setJourSemaine(dto.getJourSemaine());
        horaire.setHeureDebut(dto.getHeureDebut());
        horaire.setHeureFin(dto.getHeureFin());
        horaire.setPrixHeure(dto.getPrixHeure());

        if (dto.getAbonnementId() != null) {
            Abonnement abonnement = abonnementRepository.findById(dto.getAbonnementId()).orElse(null);
            horaire.setAbonnement(abonnement);
        }

        return horaire;
    }

    // ======== SERVICES CRUD ========
    public List<AbonnementHoraireDTO> getAllHoraires() {
        return horaireRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public AbonnementHoraireDTO trouverHoraireParId(Long id) {
        return horaireRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    public List<AbonnementHoraireDTO> getHorairesParAbonnement(Long abonnementId) {
        return horaireRepository.findByAbonnementId(abonnementId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * ✅ Valide que la date et l'heure ne sont pas dans le passé
     * @param date La date à valider
     * @param heureDebut L'heure de début
     * @param heureFin L'heure de fin
     * @param estModification Si true, permet la modification même si la date est dans le passé (remplacement de jour/heure)
     */
    private void validerDateEtHeureNonPassees(LocalDate date, java.time.LocalTime heureDebut, java.time.LocalTime heureFin, boolean estModification) {
        if (date == null || heureDebut == null || heureFin == null) {
            return; // Pas de validation si données incomplètes
        }
        
        LocalDate aujourdhui = LocalDate.now();
        java.time.LocalTime maintenant = java.time.LocalTime.now();
        
        // Si c'est une modification, on permet de remplacer un jour/heure même si la nouvelle date est dans le passé
        // car on remplace juste le jour/heure d'un horaire existant
        if (estModification) {
            // Pour une modification, on valide seulement que si c'est aujourd'hui, l'heure de fin n'est pas encore passée
            if (date.equals(aujourdhui)) {
                java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
                
                // Gérer le cas où heureFin = minuit (fin de journée)
                if (heureFin.equals(minuit)) {
                    return; // Valide
                }
                
                // Vérifier que l'heure de fin n'est pas encore passée
                if (heureFin.isBefore(maintenant) || heureFin.equals(maintenant)) {
                    throw new IllegalArgumentException(
                        String.format("Impossible de modifier un horaire dont l'heure de fin (%s) est déjà passée. Il est actuellement %s", 
                            heureFin, maintenant)
                    );
                }
            }
            // Si la date est dans le passé mais c'est une modification, on permet (remplacement de jour/heure)
            return;
        }
        
        // Pour une création, on valide strictement que la date n'est pas dans le passé
        if (date.isBefore(aujourdhui)) {
            throw new IllegalArgumentException(
                String.format("Impossible de créer un horaire pour une date passée. La date %s est antérieure à aujourd'hui (%s)", 
                    date, aujourdhui)
            );
        }
        
        // Si c'est aujourd'hui, vérifier que l'heure de fin n'est pas encore passée
        // On peut créer un horaire même si l'heure de début est passée, tant que l'heure de fin n'est pas passée
        if (date.equals(aujourdhui)) {
            java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
            
            // Gérer le cas où heureFin = minuit (fin de journée)
            if (heureFin.equals(minuit)) {
                // Si heureFin est minuit, le créneau se termine à la fin de la journée
                // On peut créer l'horaire tant qu'on n'est pas encore à minuit
                return; // Valide
            }
            
            // Vérifier que l'heure de fin n'est pas encore passée
            if (heureFin.isBefore(maintenant) || heureFin.equals(maintenant)) {
                throw new IllegalArgumentException(
                    String.format("Impossible de créer un horaire dont l'heure de fin (%s) est déjà passée. Il est actuellement %s", 
                        heureFin, maintenant)
                );
            }
        }
    }
    
    /**
     * ✅ Valide que la date et l'heure ne sont pas dans le passé (pour les créations)
     */
    private void validerDateEtHeureNonPassees(LocalDate date, java.time.LocalTime heureDebut, java.time.LocalTime heureFin) {
        validerDateEtHeureNonPassees(date, heureDebut, heureFin, false);
    }
    
    @Transactional
    public AbonnementHoraireDTO ajouterHoraire(AbonnementHoraireDTO dto) {
        // Vérifier et gérer l'exception de chevauchement horaire
        if (dto.getHeureFin() == null && dto.getHeureDebut() != null) {
            // Heure fin automatique = heure debut + 1h
            dto.setHeureFin(dto.getHeureDebut().plusHours(1));
        }

        // Vérifier s'il y a déjà un AbonnementHoraire sur le même abonnement, jour et créneau
        boolean conflit = horaireRepository.findByAbonnementId(dto.getAbonnementId()).stream()
                .anyMatch(existing ->
                        existing.getJourSemaine() == dto.getJourSemaine() &&
                        !(
                            dto.getHeureFin().isBefore(existing.getHeureDebut()) ||
                            dto.getHeureDebut().isAfter(existing.getHeureFin())
                        )
                );
        if (conflit) {
            throw new IllegalArgumentException("Impossible d'enregistrer ce créneau : conflit horaire avec une réservation existante.");
        }

        // *** CREATION DE 4 OCCURRENCES (4 SEMAINES) ***
        Abonnement abonnement = abonnementRepository.findById(dto.getAbonnementId()).orElse(null);
        if (abonnement == null) {
            throw new IllegalArgumentException("Abonnement introuvable");
        }
        
        // ✅ VALIDATION : vérifier que les horaires sont dans l'intervalle d'ouverture/fermeture
        if (abonnement.getTerrain() != null) {
            validerHoraires(abonnement.getTerrain(), dto.getHeureDebut(), dto.getHeureFin());
        }
        
        LocalDate dateDebut = abonnement.getDateDebut();
        LocalDate dateFin = abonnement.getDateFin();
        
        // Calculer le nombre de semaines
        long nombreSemaines = 4; // Par défaut 4 semaines
        if (dateDebut != null && dateFin != null) {
            long jours = java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin);
            nombreSemaines = Math.max(1, jours / 7);
        }
        
        // Créer les horaires pour chaque semaine
        List<AbonnementHoraire> horairesRepetitifs = new java.util.ArrayList<>();
        for (int semaine = 0; semaine < nombreSemaines; semaine++) {
            AbonnementHoraire horaire = toEntity(dto);
            horaire.setAbonnement(abonnement);
            
            // Calculer la date pour cette semaine
            DayOfWeek jourCible = jourSemaineToJavaDayOfWeek(dto.getJourSemaine());
            DayOfWeek jourDebut = dateDebut.getDayOfWeek();
            int joursJusquauProchainJour = (jourCible.getValue() - jourDebut.getValue() + 7) % 7;
            LocalDate dateHoraire = dateDebut.plusDays(joursJusquauProchainJour).plusWeeks(semaine);
            horaire.setDate(dateHoraire);
            
            // ✅ VALIDATION : Vérifier que la date et l'heure ne sont pas dans le passé
            // Calculer heureFin si non fournie
            java.time.LocalTime heureFin = dto.getHeureFin();
            if (heureFin == null && dto.getHeureDebut() != null) {
                heureFin = dto.getHeureDebut().plusHours(1);
            }
            validerDateEtHeureNonPassees(dateHoraire, dto.getHeureDebut(), heureFin);
            
            horairesRepetitifs.add(horaire);
        }
        
        // Sauvegarder tous les horaires
        List<AbonnementHoraire> saved = horaireRepository.saveAll(horairesRepetitifs);
        
        // Recalculer le prix total de l'abonnement
        List<AbonnementHoraire> tousLesHoraires = horaireRepository.findByAbonnementId(dto.getAbonnementId());
        java.math.BigDecimal prixTotal = tousLesHoraires.stream()
                .map(AbonnementHoraire::getPrixHeure)
                .filter(prix -> prix != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        abonnement.setPrixTotal(prixTotal);
        abonnementRepository.save(abonnement);
        
        // ✅ FLUSH : Forcer l'écriture en base avant la publication de l'événement
        entityManager.flush();
        
        // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
        entityManager.clear();
        
        // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
        entityManager.flush();
        
        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
        if (abonnement.getTerrain() != null) {
            eventPublisher.publishEvent(new AbonnementModifieEvent(abonnement.getTerrain().getId()));
            eventPublisher.publishEvent(new AbonnementHoraireChangeEvent(abonnement.getTerrain().getId()));
        }
        
        // Retourner le premier horaire créé
        return toDTO(saved.get(0));
    }

    @Transactional
    public AbonnementHoraireDTO mettreAJourHoraire(Long id, AbonnementHoraireDTO dto) {
        return horaireRepository.findById(id)
                .map(existant -> {
                    // Récupérer le jour de semaine original avant modification
                    JourSemaine jourSemaineOriginal = existant.getJourSemaine();
                    Long abonnementId = existant.getAbonnement() != null ? existant.getAbonnement().getId() : null;
                    
                    // *** CALCUL AUTOMATIQUE DE HEURE FIN ***
                    // Si heureDebut est modifiée, calculer automatiquement heureFin = heureDebut + 1
                    if (dto.getHeureDebut() != null) {
                        dto.setHeureFin(dto.getHeureDebut().plusHours(1));
                        
                        // ✅ VALIDATION : vérifier les horaires si on modifie l'heure
                        if (existant.getAbonnement() != null && existant.getAbonnement().getTerrain() != null) {
                            validerHoraires(existant.getAbonnement().getTerrain(), dto.getHeureDebut(), dto.getHeureFin());
                        }
                    }
                    
                    // *** APPLIQUER LA MODIFICATION A TOUS LES HORAIRES DU MEME JOUR DE LA SEMAINE ***
                    if (abonnementId != null) {
                        // Récupérer l'abonnement pour avoir la date de début
                        Abonnement abonnement = abonnementRepository.findById(abonnementId).orElse(null);
                        LocalDate dateDebutAbonnement = abonnement != null ? abonnement.getDateDebut() : null;
                        
                        // Trouver tous les horaires du même jour de la semaine dans le même abonnement
                        List<AbonnementHoraire> horairesAModifier = horaireRepository.findByAbonnementId(abonnementId)
                                .stream()
                                .filter(h -> h.getJourSemaine() == jourSemaineOriginal)
                                .toList();
                        
                        // Appliquer les modifications à tous ces horaires
                        for (AbonnementHoraire horaire : horairesAModifier) {
                            LocalDate dateHoraire = horaire.getDate();
                            LocalTime heureDebutHoraire = horaire.getHeureDebut();
                            LocalTime heureFinHoraire = horaire.getHeureFin();
                            
                            if (dto.getJourSemaine() != null) {
                                horaire.setJourSemaine(dto.getJourSemaine());
                                // *** RECALCULER LA DATE AUTOMATIQUEMENT ***
                                LocalDate nouvelleDate = calculerNouvelleDate(
                                    horaire.getDate(), 
                                    dto.getJourSemaine(), 
                                    dateDebutAbonnement
                                );
                                horaire.setDate(nouvelleDate);
                                dateHoraire = nouvelleDate;
                            }
                            if (dto.getHeureDebut() != null) {
                                horaire.setHeureDebut(dto.getHeureDebut());
                                // Appliquer aussi heureFin calculée
                                horaire.setHeureFin(dto.getHeureFin());
                                heureDebutHoraire = dto.getHeureDebut();
                                heureFinHoraire = dto.getHeureFin();
                            }
                            if (dto.getPrixHeure() != null) {
                                horaire.setPrixHeure(dto.getPrixHeure());
                            }
                            
                            // ✅ VALIDATION : Vérifier que la date et l'heure ne sont pas dans le passé (après modification)
                            // Pour une modification, on permet de remplacer le jour/heure même si la date est dans le passé
                            // car on remplace juste le jour/heure d'un horaire existant
                            validerDateEtHeureNonPassees(dateHoraire, heureDebutHoraire, heureFinHoraire, true);
                            
                            // ✅ VALIDATION : Vérifier les conflits avec les réservations existantes AVANT de sauvegarder
                            if (abonnement != null && abonnement.getTerrain() != null && 
                                dateHoraire != null && heureDebutHoraire != null && heureFinHoraire != null) {
                                try {
                                    validerConflitHoraireAbonnement(
                                        abonnement.getTerrain().getId(),
                                        dateHoraire,
                                        heureDebutHoraire,
                                        heureFinHoraire,
                                        abonnementId // Exclure l'abonnement en cours de modification
                                    );
                                } catch (IllegalArgumentException e) {
                                    // Enrichir le message d'erreur avec plus de contexte
                                    throw new IllegalArgumentException(
                                        String.format("Erreur lors de la modification de l'horaire pour le %s (date: %s) : %s",
                                            horaire.getJourSemaine(), dateHoraire, e.getMessage()),
                                        e
                                    );
                                }
                            }
                        }
                        
                        // Sauvegarder tous les horaires modifiés
                        horaireRepository.saveAll(horairesAModifier);
                        
                        // ✅ FLUSH : Forcer l'écriture en base avant la synchronisation
                        entityManager.flush();
                        
                        // ✅ CLEAR : Vider le cache L1 pour forcer la relecture
                        entityManager.clear();
                        
                        // *** RECALCULER LE PRIX TOTAL DE L'ABONNEMENT ***
                        if (dto.getPrixHeure() != null && abonnementId != null) {
                            // Ne pas redéclarer la variable abonnement, elle existe déjà plus haut
                            if (abonnement != null) {
                                // Récupérer tous les horaires de l'abonnement
                                List<AbonnementHoraire> tousLesHoraires = horaireRepository.findByAbonnementId(abonnementId);
                                
                                // Calculer le nouveau prix total
                                java.math.BigDecimal prixTotal = tousLesHoraires.stream()
                                        .map(AbonnementHoraire::getPrixHeure)
                                        .filter(prix -> prix != null)
                                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                                
                                // Mettre à jour le prix total de l'abonnement
                                abonnement.setPrixTotal(prixTotal);
                                abonnementRepository.save(abonnement);
                                entityManager.flush(); // Flush aussi pour l'abonnement
                            }
                        }
                        
                        // ✅ FLUSH FINAL : Forcer l'écriture de toutes les modifications en base avant la publication de l'événement
                        entityManager.flush();
                        
                        // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
                        entityManager.clear();
                        
                        // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
                        entityManager.flush();
                        
                        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
                        if (abonnement != null && abonnement.getTerrain() != null) {
                            eventPublisher.publishEvent(new AbonnementModifieEvent(abonnement.getTerrain().getId()));
                            eventPublisher.publishEvent(new AbonnementHoraireChangeEvent(abonnement.getTerrain().getId()));
                        }
                    } else {
                        // Si pas d'abonnement, modifier seulement l'horaire actuel
                        LocalDate dateHoraire = existant.getDate();
                        LocalTime heureDebutHoraire = existant.getHeureDebut();
                        LocalTime heureFinHoraire = existant.getHeureFin();
                        
                        if (dto.getJourSemaine() != null) {
                            existant.setJourSemaine(dto.getJourSemaine());
                        }
                        if (dto.getHeureDebut() != null) {
                            existant.setHeureDebut(dto.getHeureDebut());
                            existant.setHeureFin(dto.getHeureFin());
                            heureDebutHoraire = dto.getHeureDebut();
                            heureFinHoraire = dto.getHeureFin();
                        }
                        if (dto.getPrixHeure() != null) {
                            existant.setPrixHeure(dto.getPrixHeure());
                        }
                        
                        // ✅ VALIDATION : Vérifier les conflits avec les réservations existantes AVANT de sauvegarder
                        if (existant.getAbonnement() != null && existant.getAbonnement().getTerrain() != null && 
                            dateHoraire != null && heureDebutHoraire != null && heureFinHoraire != null) {
                            Long abonnementIdExclu = existant.getAbonnement().getId();
                            try {
                                validerConflitHoraireAbonnement(
                                    existant.getAbonnement().getTerrain().getId(),
                                    dateHoraire,
                                    heureDebutHoraire,
                                    heureFinHoraire,
                                    abonnementIdExclu // Exclure l'abonnement en cours de modification
                                );
                            } catch (IllegalArgumentException e) {
                                // Enrichir le message d'erreur avec plus de contexte
                                throw new IllegalArgumentException(
                                    String.format("Erreur lors de la modification de l'horaire (date: %s) : %s",
                                        dateHoraire, e.getMessage()),
                                    e
                                );
                            }
                        }
                        
                        horaireRepository.save(existant);
                        
                        // ✅ FLUSH : Forcer l'écriture en base avant la publication de l'événement
                        entityManager.flush();
                        
                        // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
                        entityManager.clear();
                        
                        // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
                        entityManager.flush();
                        
                        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
                        if (existant.getAbonnement() != null && existant.getAbonnement().getTerrain() != null) {
                            eventPublisher.publishEvent(new AbonnementModifieEvent(existant.getAbonnement().getTerrain().getId()));
                            eventPublisher.publishEvent(new AbonnementHoraireChangeEvent(existant.getAbonnement().getTerrain().getId()));
                        }
                    }

                    return toDTO(existant);
                })
                .orElse(null);
    }

    @Transactional
    public boolean supprimerHoraire(Long id) {
        // Trouver l'horaire à supprimer
        AbonnementHoraire horaireASupprimer = horaireRepository.findById(id).orElse(null);
        if (horaireASupprimer == null) {
            return false;
        }
        
        // Récupérer le terrain avant suppression pour la synchronisation
        Long terrainId = null;
        if (horaireASupprimer.getAbonnement() != null && 
            horaireASupprimer.getAbonnement().getTerrain() != null) {
            terrainId = horaireASupprimer.getAbonnement().getTerrain().getId();
        }
        
        // Récupérer le jour de semaine et l'abonnement
        JourSemaine jourSemaine = horaireASupprimer.getJourSemaine();
        Long abonnementId = horaireASupprimer.getAbonnement() != null ? 
                            horaireASupprimer.getAbonnement().getId() : null;
        
        if (abonnementId != null) {
            // *** SUPPRIMER TOUTES LES OCCURRENCES DU MEME JOUR DE LA SEMAINE ***
            List<AbonnementHoraire> horairesASupprimer = horaireRepository.findByAbonnementId(abonnementId)
                    .stream()
                    .filter(h -> h.getJourSemaine() == jourSemaine)
                    .toList();
            
            // Supprimer tous ces horaires
            horaireRepository.deleteAll(horairesASupprimer);
            
            // ✅ FLUSH : Forcer l'écriture de la suppression en base
            entityManager.flush();
            
            // Recalculer le prix total de l'abonnement
            Abonnement abonnement = abonnementRepository.findById(abonnementId).orElse(null);
            if (abonnement != null) {
                List<AbonnementHoraire> horairesRestants = horaireRepository.findByAbonnementId(abonnementId);
                java.math.BigDecimal prixTotal = horairesRestants.stream()
                        .map(AbonnementHoraire::getPrixHeure)
                        .filter(prix -> prix != null)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                abonnement.setPrixTotal(prixTotal);
                abonnementRepository.save(abonnement);
                
                // ✅ FLUSH : Forcer l'écriture en base avant la publication de l'événement
                entityManager.flush();
                
                // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
                entityManager.clear();
                
                // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
                entityManager.flush();
                
                // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
                if (abonnement.getTerrain() != null) {
                    eventPublisher.publishEvent(new AbonnementModifieEvent(abonnement.getTerrain().getId()));
                    eventPublisher.publishEvent(new AbonnementHoraireChangeEvent(abonnement.getTerrain().getId()));
                }
            }
        } else {
            // Si pas d'abonnement, supprimer seulement cet horaire
            horaireRepository.deleteById(id);
            
            // ✅ FLUSH : Forcer l'écriture de la suppression en base
            entityManager.flush();
            
            // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
            entityManager.clear();
            
            // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
            entityManager.flush();
            
            // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
            if (terrainId != null) {
                eventPublisher.publishEvent(new AbonnementHoraireChangeEvent(terrainId));
            }
        }
        
        return true;
    }
    
    /**
     * Valide que les horaires sont dans l'intervalle d'ouverture/fermeture du terrain
     */
    private void validerHoraires(com.matchi.model.TerrainService terrain, java.time.LocalTime heureDebut, java.time.LocalTime heureFin) {
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
                boolean heureDebutValide = heureDebut.isAfter(heureOuverture) || heureDebut.equals(heureOuverture) ||
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