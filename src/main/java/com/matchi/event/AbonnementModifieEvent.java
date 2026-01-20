package com.matchi.event;

/**
 * Événement publié lorsqu'un abonnement est modifié ou supprimé
 * pour déclencher la synchronisation des horaires indisponibles APRÈS le commit
 */
public record AbonnementModifieEvent(Long terrainId) {}
