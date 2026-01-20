package com.matchi.event;

/**
 * Événement déclenché lorsqu'un horaire d'abonnement est ajouté ou modifié.
 * Utilisé pour synchroniser automatiquement la table indisponible_horaire.
 */
public record AbonnementHoraireChangeEvent(Long terrainId) {}
