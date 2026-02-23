package com.matchi.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service utilitaire pour appeler le backend Django
 * une fois que la synchronisation des horaires indisponibles
 * côté Spring est terminée.
 *
 * Utilisation attendue :
 *  - Injecter ce service dans IndisponibleHoraireService
 *  - Appeler notifierDjangoSynchronisation(terrainId)
 *    à la fin de synchroniserHorairesIndisponibles(...)
 */
@Service
@RequiredArgsConstructor
public class DjangoSyncService {

    private static final Logger log = LoggerFactory.getLogger(DjangoSyncService.class);

    /**
     * URL de base du backend Django (configurable dans application.properties).
     * Exemple :
     *   django.sync.base-url=http://localhost:8000
     */
    @Value("${django.sync.base-url:http://localhost:8000}")
    private String djangoBaseUrl;

    // RestTemplate simple pour appeler Django
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Appelle l'endpoint Django après la synchro Spring :
     *
     *   GET {djangoBaseUrl}/synchroniser-horaires/{terrainId}/
     *
     * Côté Django, cette URL doit lancer la mise à jour de la table
     * Indisponibilites à partir de l'API Spring.
     */
    public void notifierDjangoSynchronisation(Long terrainIdSpring) {
        if (terrainIdSpring == null) {
            return;
        }

        String url = String.format("%s/synchroniser-horaires/%d/", djangoBaseUrl, terrainIdSpring);

        try {
            log.info("Appel de la synchronisation Django pour le terrain {}", terrainIdSpring);
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            // On log l'erreur mais on ne bloque pas la logique côté Spring
            log.error("Erreur lors de l'appel à Django pour la synchro des horaires (terrain {}): {}",
                    terrainIdSpring, e.getMessage(), e);
        }
    }
}

