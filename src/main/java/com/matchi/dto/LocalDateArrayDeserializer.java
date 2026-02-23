package com.matchi.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Désérialiseur personnalisé pour LocalDate depuis un tableau [année, mois, jour]
 * ou depuis une chaîne ISO
 */
public class LocalDateArrayDeserializer extends JsonDeserializer<LocalDate> {
    
    private static final Logger log = LoggerFactory.getLogger(LocalDateArrayDeserializer.class);
    
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonToken token = p.getCurrentToken();
            if (token == null || token == JsonToken.VALUE_NULL) {
                log.debug("🔍 LocalDateArrayDeserializer: token null ou VALUE_NULL");
                return null;
            }
            
            // Si c'est un tableau [année, mois, jour]
            if (token == JsonToken.START_ARRAY) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Object> array = (List<Object>) p.readValueAs(List.class);
                    if (array != null && array.size() >= 3) {
                        int year = ((Number) array.get(0)).intValue();
                        int month = ((Number) array.get(1)).intValue();
                        int day = ((Number) array.get(2)).intValue();
                        LocalDate date = LocalDate.of(year, month, day);
                        log.debug("✅ LocalDateArrayDeserializer: tableau [{}] converti en {}", array, date);
                        return date;
                    } else {
                        log.warn("⚠️ LocalDateArrayDeserializer: tableau invalide (taille: {})", array != null ? array.size() : 0);
                    }
                } catch (Exception e) {
                    log.error("❌ LocalDateArrayDeserializer: erreur lors de la lecture du tableau: {}", e.getMessage());
                    throw new IOException("Erreur lors de la désérialisation du tableau LocalDate", e);
                }
            }
            
            // Si c'est une chaîne ISO
            if (token.isScalarValue()) {
                String value = p.getValueAsString();
                if (value == null || value.trim().isEmpty()) {
                    log.debug("🔍 LocalDateArrayDeserializer: chaîne vide");
                    return null;
                }
                try {
                    LocalDate date = LocalDate.parse(value);
                    log.debug("✅ LocalDateArrayDeserializer: chaîne '{}' convertie en {}", value, date);
                    return date;
                } catch (Exception e) {
                    log.warn("⚠️ LocalDateArrayDeserializer: impossible de parser la chaîne '{}': {}", value, e.getMessage());
                    return null;
                }
            }
            
            log.warn("⚠️ LocalDateArrayDeserializer: token non supporté: {}", token);
            return null;
        } catch (Exception e) {
            log.error("❌ LocalDateArrayDeserializer: erreur inattendue: {}", e.getMessage(), e);
            throw new IOException("Erreur lors de la désérialisation de LocalDate", e);
        }
    }
}
