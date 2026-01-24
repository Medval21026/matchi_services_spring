package com.matchi.service;

import com.matchi.model.IndisponibleHoraire;
import com.matchi.model.ReservationPonctuelle;
import com.matchi.model.TerrainService;
import com.matchi.repository.IndisponibleHoraireRepository;
import com.matchi.repository.ReservationPonctuelleRepository;
import com.matchi.repository.TerrainServiceRepository;
import com.matchi.dto.ReservationPonctuelleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.matchi.event.ReservationModifieEvent;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationPonctuelleService {

    private final ReservationPonctuelleRepository reservationRepository;
    private final TerrainServiceRepository terrainServiceRepository;
    private final IndisponibleHoraireRepository indisponibleHoraireRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @PersistenceContext
    private EntityManager entityManager;

    // ======== Mapper ========
    private ReservationPonctuelleDTO toDTO(ReservationPonctuelle reservation) {
        return new ReservationPonctuelleDTO(
                reservation.getId(),
                reservation.getDate(),
                reservation.getHeureDebut(),
                reservation.getHeureFin(),
                reservation.getPrix(),
                reservation.getClientTelephone(),
                reservation.getTerrain() != null ? reservation.getTerrain().getId() : null
        );
    }

    private ReservationPonctuelle toEntity(ReservationPonctuelleDTO dto) {
        ReservationPonctuelle reservation = new ReservationPonctuelle();
        reservation.setId(dto.id());
        reservation.setDate(dto.date());
        reservation.setHeureDebut(dto.heureDebut());
        // Calcul automatique de heureFin = heureDebut + 1 heure
        reservation.setHeureFin(dto.heureDebut() != null ? dto.heureDebut().plusHours(1) : null);
        reservation.setPrix(dto.prix());
        reservation.setClientTelephone(dto.clientTelephone());

        if (dto.terrainId() != null) {
            TerrainService terrain = terrainServiceRepository.findById(dto.terrainId())
                    .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));
            
            // ✅ VALIDATION : vérifier que les horaires sont dans l'intervalle d'ouverture/fermeture
            validerHoraires(terrain, reservation.getHeureDebut(), reservation.getHeureFin());
            
            reservation.setTerrain(terrain);
        }

        return reservation;
    }
    
    /**
     * ✅ Valide que la date et l'heure ne sont pas dans le passé
     */
    private void validerDateEtHeureNonPassees(java.time.LocalDate date, java.time.LocalTime heureDebut, java.time.LocalTime heureFin) {
        if (date == null || heureDebut == null || heureFin == null) {
            return; // Pas de validation si données incomplètes
        }
        
        java.time.LocalDate aujourdhui = java.time.LocalDate.now();
        java.time.LocalTime maintenant = java.time.LocalTime.now();
        
        // Vérifier si la date est dans le passé
        if (date.isBefore(aujourdhui)) {
            throw new IllegalArgumentException(
                String.format("Impossible de réserver pour une date passée. La date %s est antérieure à aujourd'hui (%s)", 
                    date, aujourdhui)
            );
        }
        
        // Si c'est aujourd'hui, vérifier que l'heure de fin n'est pas encore passée
        // On peut réserver un créneau même si l'heure de début est passée, tant que l'heure de fin n'est pas passée
        if (date.equals(aujourdhui)) {
            java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
            
            // Gérer le cas où heureFin = minuit (fin de journée)
            if (heureFin.equals(minuit)) {
                // Si heureFin est minuit, le créneau se termine à la fin de la journée
                // On peut réserver tant qu'on n'est pas encore à minuit
                return; // Valide
            }
            
            // Vérifier que l'heure de fin n'est pas encore passée
            if (heureFin.isBefore(maintenant) || heureFin.equals(maintenant)) {
                throw new IllegalArgumentException(
                    String.format("Impossible de réserver un créneau dont l'heure de fin (%s) est déjà passée. Il est actuellement %s", 
                        heureFin, maintenant)
                );
            }
        }
    }
    
    /**
     * ✅ Valide qu'il n'y a pas de conflit avec une réservation existante ou un abonnement
     */
    private void validerConflitReservation(Long terrainId, java.time.LocalDate date, 
                                           java.time.LocalTime heureDebut, java.time.LocalTime heureFin, 
                                           Long reservationIdExclue) {
        if (terrainId == null || date == null || heureDebut == null || heureFin == null) {
            return; // Pas de validation si données incomplètes
        }
        
        // 1. Vérifier les conflits avec les réservations ponctuelles existantes
        List<ReservationPonctuelle> reservationsExistantes = reservationRepository.findByTerrain_IdAndDate(terrainId, date);
        
        for (ReservationPonctuelle existante : reservationsExistantes) {
            // Exclure la réservation en cours de modification
            if (reservationIdExclue != null && existante.getId().equals(reservationIdExclue)) {
                continue;
            }
            
            // Vérifier si les créneaux se chevauchent
            if (creneauxSeChevauchent(heureDebut, heureFin, 
                                     existante.getHeureDebut(), existante.getHeureFin())) {
                throw new IllegalArgumentException(
                    String.format("Conflit de réservation : Le créneau %s-%s est déjà réservé pour ce terrain à cette date (%s). " +
                                 "Réservation existante : %s-%s",
                        heureDebut, heureFin, date,
                        existante.getHeureDebut(), existante.getHeureFin())
                );
            }
        }
        
        // 2. Vérifier les conflits avec les abonnements (via IndisponibleHoraire)
        List<IndisponibleHoraire> horairesIndisponibles = indisponibleHoraireRepository.findByTerrainIdAndDate(terrainId, date);
        
        for (IndisponibleHoraire indispo : horairesIndisponibles) {
            // Vérifier si les créneaux se chevauchent
            if (creneauxSeChevauchent(heureDebut, heureFin, 
                                     indispo.getHeureDebut(), indispo.getHeureFin())) {
                throw new IllegalArgumentException(
                    String.format("Conflit avec un abonnement : Le créneau %s-%s est déjà occupé par un abonnement pour ce terrain à cette date (%s). " +
                                 "Créneau occupé : %s-%s",
                        heureDebut, heureFin, date,
                        indispo.getHeureDebut(), indispo.getHeureFin())
                );
            }
        }
    }
    
    /**
     * Vérifie si deux créneaux horaires se chevauchent
     */
    private boolean creneauxSeChevauchent(LocalTime debut1, LocalTime fin1, 
                                          LocalTime debut2, LocalTime fin2) {
        // Cas spécial : si les deux créneaux sont identiques (même début et même fin), ils se chevauchent
        if (debut1.equals(debut2) && fin1.equals(fin2)) {
            return true;
        }
        
        // Cas spécial : si fin1 ou fin2 est 00h (minuit), on doit gérer différemment
        LocalTime minuit = LocalTime.of(0, 0);
        boolean fin1EstMinuit = fin1.equals(minuit);
        boolean fin2EstMinuit = fin2.equals(minuit);
        
        // Si fin1 est minuit, on considère que le créneau 1 se termine à 24h (fin de journée)
        // Si fin2 est minuit, on considère que le créneau 2 se termine à 24h (fin de journée)
        
        if (fin1EstMinuit && fin2EstMinuit) {
            // Les deux créneaux se terminent à minuit
            // Si les heures de début sont identiques, ils se chevauchent (déjà géré plus haut avec return true)
            // Sinon, ils se chevauchent si l'un commence avant que l'autre ne se termine
            // Puisque les deux se terminent à minuit, ils se chevauchent si leurs débuts se chevauchent
            // Exemple : 22:00-00:00 et 23:00-00:00 se chevauchent car 22:00 < 23:00 < 00:00
            // Donc : debut1 < debut2 (le premier commence avant le second) OU debut2 < debut1 (le second commence avant le premier)
            // Mais si debut1 = debut2, on a déjà retourné true plus haut
            // Donc ici, on vérifie si les créneaux se chevauchent : debut1 < debut2 (le premier commence avant le second)
            // ET fin1 > debut2 (le premier se termine après que le second ne commence) -> toujours vrai car fin1 = minuit
            // OU debut2 < debut1 (le second commence avant le premier) ET fin2 > debut1 (le second se termine après que le premier ne commence) -> toujours vrai car fin2 = minuit
            // Donc si debut1 != debut2, ils se chevauchent toujours car les deux se terminent à minuit
            return true; // Si les deux se terminent à minuit et ont des débuts différents, ils se chevauchent toujours
        } else if (fin1EstMinuit) {
            // fin1 est minuit, donc le créneau 1 va jusqu'à la fin de la journée
            // Il chevauche le créneau 2 si debut1 < fin2 ET (debut2 < fin1 OU debut2 = debut1)
            // Mais fin1 = minuit, donc on compare avec minuit
            // Le créneau 1 va de debut1 à minuit (24h)
            // Il chevauche le créneau 2 si : debut1 < fin2 (le créneau 1 commence avant que le créneau 2 ne se termine)
            // ET (debut2 < minuit OU debut2 = debut1)
            // Puisque minuit = 00:00, debut2 < minuit est toujours faux sauf si debut2 est aussi minuit
            // Donc on simplifie : debut1 < fin2
            return debut1.isBefore(fin2);
        } else if (fin2EstMinuit) {
            // fin2 est minuit, donc le créneau 2 va jusqu'à la fin de la journée
            // Il chevauche le créneau 1 si debut2 < fin1
            return debut2.isBefore(fin1);
        } else {
            // Cas normal : aucun créneau ne se termine à minuit
            // Deux créneaux se chevauchent si : debut1 < fin2 && fin1 > debut2
            return (debut1.isBefore(fin2) && fin1.isAfter(debut2));
        }
    }
    
    /**
     * Valide que les horaires de réservation sont dans l'intervalle d'ouverture/fermeture du terrain
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

    // ======== CRUD ========
    public List<ReservationPonctuelleDTO> getAllReservations() {
        java.time.LocalDate aujourdhui = java.time.LocalDate.now();
        java.time.LocalTime maintenant = java.time.LocalTime.now();
        
        return reservationRepository.findAll()
                .stream()
                // ✅ FILTRER : Afficher toutes les réservations d'aujourd'hui (même si heure début passée) 
                // tant que l'heure de fin n'est pas dépassée, et toutes les réservations futures
                .filter(reservation -> {
                    if (reservation.getDate() == null) {
                        return false; // Exclure si pas de date
                    }
                    
                    // Si la date est dans le passé, exclure
                    if (reservation.getDate().isBefore(aujourdhui)) {
                        return false;
                    }
                    
                    // Si c'est aujourd'hui, vérifier que l'heure de fin n'est pas encore passée
                    if (reservation.getDate().equals(aujourdhui)) {
                        if (reservation.getHeureFin() != null) {
                            // ✅ CORRECTION : Gérer le cas spécial où heureFin = 00:00:00 (minuit)
                            java.time.LocalTime minuit = java.time.LocalTime.of(0, 0);
                            if (reservation.getHeureFin().equals(minuit)) {
                                // Si heureFin est minuit, c'est considéré comme la fin de la journée (24h)
                                // La réservation est valide pour aujourd'hui si l'heure de fin (minuit) n'est pas encore atteinte
                                // Puisque minuit n'arrive qu'après 23h59, si on est avant minuit, la réservation est toujours valide
                                return true; // Réservation valide jusqu'à minuit
                            } else if (reservation.getHeureFin().isBefore(maintenant) || reservation.getHeureFin().equals(maintenant)) {
                                // L'heure de fin est passée ou égale à maintenant, exclure
                                return false;
                            }
                            // Si heureFin > maintenant, la réservation est encore en cours ou future, l'inclure
                        }
                        // Si pas d'heure de fin, inclure la réservation d'aujourd'hui
                    }
                    
                    return true; // Inclure les réservations futures ou en cours
                })
                // ✅ TRI : Ordre croissant par date puis par heure
                .sorted(Comparator
                        .comparing(ReservationPonctuelle::getDate)
                        .thenComparing(ReservationPonctuelle::getHeureDebut))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ReservationPonctuelleDTO getReservationById(Long id) {
        return reservationRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Réservation introuvable"));
    }

    @Transactional
    public ReservationPonctuelleDTO createReservation(ReservationPonctuelleDTO dto) {
        // ✅ VALIDATION : Vérifier que la date et l'heure ne sont pas dans le passé
        // Calculer heureFin si non fournie
        java.time.LocalTime heureFin = dto.heureFin();
        if (heureFin == null && dto.heureDebut() != null) {
            heureFin = dto.heureDebut().plusHours(1);
        }
        validerDateEtHeureNonPassees(dto.date(), dto.heureDebut(), heureFin);
        
        ReservationPonctuelle reservation = toEntity(dto);
        
        // ✅ VALIDATION : Vérifier les conflits avant de sauvegarder
        if (reservation.getTerrain() != null && reservation.getDate() != null 
            && reservation.getHeureDebut() != null && reservation.getHeureFin() != null) {
            validerConflitReservation(
                reservation.getTerrain().getId(),
                reservation.getDate(),
                reservation.getHeureDebut(),
                reservation.getHeureFin(),
                null // Pas d'exclusion pour une nouvelle réservation
            );
        }
        
        ReservationPonctuelle saved = reservationRepository.save(reservation);
        
        // ✅ FLUSH : Forcer l'écriture en base avant la publication de l'événement
        entityManager.flush();
        
        // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
        entityManager.clear();
        
        // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
        entityManager.flush();
        
        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
        // L'événement AFTER_COMMIT garantit que les données sont déjà commitées en base
        if (saved.getTerrain() != null) {
            eventPublisher.publishEvent(new ReservationModifieEvent(saved.getTerrain().getId()));
        }
        
        return toDTO(saved);
    }

    @Transactional
    public ReservationPonctuelleDTO updateReservation(Long id, ReservationPonctuelleDTO dto) {
        ReservationPonctuelle existing = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Réservation introuvable"));

        // Sauvegarder les valeurs originales pour détecter les changements de créneau
        java.time.LocalDate dateOriginale = existing.getDate();
        java.time.LocalTime heureDebutOriginale = existing.getHeureDebut();
        java.time.LocalTime heureFinOriginale = existing.getHeureFin();
        Long terrainIdOriginal = existing.getTerrain() != null ? existing.getTerrain().getId() : null;
        
        // Flag pour savoir si le créneau a changé
        boolean creneauModifie = false;

        // Mise à jour partielle - uniquement les champs non-null
        if (dto.date() != null && !dto.date().equals(dateOriginale)) {
            existing.setDate(dto.date());
            creneauModifie = true;
        }
        if (dto.heureDebut() != null && !dto.heureDebut().equals(heureDebutOriginale)) {
            existing.setHeureDebut(dto.heureDebut());
            // Calcul automatique de heureFin = heureDebut + 1 heure
            existing.setHeureFin(dto.heureDebut().plusHours(1));
            creneauModifie = true;
            
            // ✅ VALIDATION : vérifier les horaires si le terrain existe
            if (existing.getTerrain() != null) {
                validerHoraires(existing.getTerrain(), existing.getHeureDebut(), existing.getHeureFin());
            }
        }
        
        // ✅ VALIDATION : Vérifier que la date et l'heure ne sont pas dans le passé (après mise à jour)
        // Utiliser l'heure de fin pour valider (on peut réserver si l'heure de fin n'est pas encore passée)
        validerDateEtHeureNonPassees(existing.getDate(), existing.getHeureDebut(), existing.getHeureFin());
        
        if (dto.prix() != null) {
            existing.setPrix(dto.prix());
        }
        if (dto.clientTelephone() != null) {
            existing.setClientTelephone(dto.clientTelephone());
        }

        if (dto.terrainId() != null && !dto.terrainId().equals(terrainIdOriginal)) {
            TerrainService terrain = terrainServiceRepository.findById(dto.terrainId())
                    .orElseThrow(() -> new IllegalArgumentException("Terrain non trouvé"));
            
            // ✅ VALIDATION : vérifier les horaires lors du changement de terrain
            validerHoraires(terrain, existing.getHeureDebut(), existing.getHeureFin());
            
            existing.setTerrain(terrain);
            creneauModifie = true;
        }

        // ✅ VALIDATION : Vérifier les conflits SEULEMENT si le créneau a été modifié
        // Si on modifie seulement le prix ou le téléphone, pas besoin de vérifier les conflits
        if (creneauModifie && existing.getTerrain() != null && existing.getDate() != null 
            && existing.getHeureDebut() != null && existing.getHeureFin() != null) {
            validerConflitReservation(
                existing.getTerrain().getId(),
                existing.getDate(),
                existing.getHeureDebut(),
                existing.getHeureFin(),
                id // Exclure la réservation en cours de modification
            );
        }

        ReservationPonctuelle saved = reservationRepository.save(existing);
        
        // ✅ FLUSH : Forcer l'écriture en base avant la publication de l'événement
        entityManager.flush();
        
        // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
        entityManager.clear();
        
        // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
        entityManager.flush();
        
        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
        if (saved.getTerrain() != null) {
            eventPublisher.publishEvent(new ReservationModifieEvent(saved.getTerrain().getId()));
        }
        
        return toDTO(saved);
    }

    @Transactional
    public void deleteReservation(Long id) {
        // Récupérer la réservation avant suppression pour synchroniser le terrain
        ReservationPonctuelle reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Réservation introuvable"));
        Long terrainId = reservation.getTerrain() != null ? reservation.getTerrain().getId() : null;
        
        reservationRepository.deleteById(id);
        
        // ✅ FLUSH : Forcer l'écriture de la suppression en base
        entityManager.flush();
        
        // ✅ CLEAR : Vider le cache L1 pour forcer la relecture lors de la synchronisation
        entityManager.clear();
        
        // ✅ FLUSH FINAL : S'assurer que tout est bien écrit avant de publier l'événement
        entityManager.flush();
        
        // ✅ PUBLIER UN ÉVÉNEMENT pour déclencher la synchronisation APRÈS le commit
        if (terrainId != null) {
            eventPublisher.publishEvent(new ReservationModifieEvent(terrainId));
        }
    }

    public List<ReservationPonctuelleDTO> getReservationsByTerrain(Long terrainId) {
        java.time.LocalDate aujourdhui = java.time.LocalDate.now();
        
        return reservationRepository.findByTerrain_Id(terrainId)
                .stream()
                // ✅ FILTRER : Afficher TOUTES les réservations d'aujourd'hui (même si heures passées)
                // et toutes les réservations futures
                .filter(reservation -> {
                    if (reservation.getDate() == null) {
                        return false; // Exclure si pas de date
                    }
                    
                    // Si la date est dans le passé (avant aujourd'hui), exclure
                    if (reservation.getDate().isBefore(aujourdhui)) {
                        return false;
                    }
                    
                    // ✅ AFFICHER TOUTES les réservations d'aujourd'hui, même si l'heure est passée
                    // Si c'est aujourd'hui, inclure toutes les réservations sans vérifier l'heure
                    if (reservation.getDate().equals(aujourdhui)) {
                        return true; // Inclure toutes les réservations d'aujourd'hui
                    }
                    
                    // Inclure toutes les réservations futures
                    return true;
                })
                // ✅ TRI : Ordre croissant par date puis par heure
                .sorted(Comparator
                        .comparing(ReservationPonctuelle::getDate)
                        .thenComparing(ReservationPonctuelle::getHeureDebut))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
