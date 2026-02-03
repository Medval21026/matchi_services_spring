package com.matchi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global pour retourner des erreurs JSON structurées
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e, WebRequest request) {
        logger.error("RuntimeException: {}", e.getMessage(), e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", e.getMessage() != null ? e.getMessage() : "Erreur de validation");
        error.put("path", request.getDescription(false).replace("uri=", ""));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e, WebRequest request) {
        logger.error("Exception non gérée: {}", e.getMessage(), e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "Internal Server Error");
        error.put("message", e.getMessage() != null ? e.getMessage() : "Une erreur inattendue s'est produite");
        error.put("path", request.getDescription(false).replace("uri=", ""));
        
        // En développement, exposer plus de détails
        String activeProfile = System.getProperty("spring.profiles.active", "");
        if (!"prod".equals(activeProfile)) {
            error.put("exception", e.getClass().getName());
            if (e.getCause() != null) {
                error.put("cause", e.getCause().getMessage());
            }
            // Stack trace en développement seulement
            if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
                error.put("stackTrace", e.getStackTrace()[0].toString());
            }
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
