package com.matchi.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

/**
 * Désérialiseur personnalisé pour LocalTime depuis un tableau [heure, minute, seconde]
 * ou depuis une chaîne ISO
 */
public class LocalTimeArrayDeserializer extends JsonDeserializer<LocalTime> {
    
    private static final Logger log = LoggerFactory.getLogger(LocalTimeArrayDeserializer.class);
    
    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonToken token = p.getCurrentToken();
            if (token == null || token == JsonToken.VALUE_NULL) {
                log.debug("🔍 LocalTimeArrayDeserializer: token null ou VALUE_NULL");
                return null;
            }
            
            // Si c'est un tableau [heure, minute, seconde]
            if (token == JsonToken.START_ARRAY) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Object> array = (List<Object>) p.readValueAs(List.class);
                    if (array != null && array.size() >= 2) {
                        int hour = ((Number) array.get(0)).intValue();
                        int minute = ((Number) array.get(1)).intValue();
                        int second = array.size() > 2 ? ((Number) array.get(2)).intValue() : 0;
                        LocalTime time = LocalTime.of(hour, minute, second);
                        log.debug("✅ LocalTimeArrayDeserializer: tableau [{}] converti en {}", array, time);
                        return time;
                    } else {
                        log.warn("⚠️ LocalTimeArrayDeserializer: tableau invalide (taille: {})", array != null ? array.size() : 0);
                    }
                } catch (Exception e) {
                    log.error("❌ LocalTimeArrayDeserializer: erreur lors de la lecture du tableau: {}", e.getMessage());
                    throw new IOException("Erreur lors de la désérialisation du tableau LocalTime", e);
                }
            }
            
            // Si c'est une chaîne ISO
            if (token.isScalarValue()) {
                String value = p.getValueAsString();
                if (value == null || value.trim().isEmpty()) {
                    log.debug("🔍 LocalTimeArrayDeserializer: chaîne vide");
                    return null;
                }
                try {
                    // Essayer avec secondes d'abord
                    try {
                        LocalTime time = LocalTime.parse(value);
                        log.debug("✅ LocalTimeArrayDeserializer: chaîne '{}' convertie en {}", value, time);
                        return time;
                    } catch (Exception e) {
                        // Essayer sans secondes
                        LocalTime time = LocalTime.parse(value + ":00");
                        log.debug("✅ LocalTimeArrayDeserializer: chaîne '{}' convertie en {} (secondes ajoutées)", value, time);
                        return time;
                    }
                } catch (Exception e) {
                    log.warn("⚠️ LocalTimeArrayDeserializer: impossible de parser la chaîne '{}': {}", value, e.getMessage());
                    return null;
                }
            }
            
            log.warn("⚠️ LocalTimeArrayDeserializer: token non supporté: {}", token);
            return null;
        } catch (Exception e) {
            log.error("❌ LocalTimeArrayDeserializer: erreur inattendue: {}", e.getMessage(), e);
            throw new IOException("Erreur lors de la désérialisation de LocalTime", e);
        }
    }
}
