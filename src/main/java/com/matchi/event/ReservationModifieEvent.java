package com.matchi.event;

/**
 * Événement publié lorsqu'une réservation ponctuelle est modifiée ou supprimée
 * pour déclencher la synchronisation des horaires indisponibles APRÈS le commit
 */
public record ReservationModifieEvent(Long terrainId) {}
