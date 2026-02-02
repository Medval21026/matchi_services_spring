package com.matchi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration et diagnostic de la base de données pour Railway
 * Affiche les variables d'environnement au démarrage pour le débogage
 */
@Configuration
@Profile("prod")
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${MYSQLHOST:}")
    private String mysqlHost;

    @Value("${MYSQLPORT:}")
    private String mysqlPort;

    @Value("${MYSQLDATABASE:}")
    private String mysqlDatabase;

    @Value("${MYSQLUSER:}")
    private String mysqlUser;

    @Value("${MYSQLPASSWORD:}")
    private String mysqlPassword;

    private final DataSource dataSource;

    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void checkEnvironmentVariables() {
        logger.error("==========================================");
        logger.error("=== DIAGNOSTIC VARIABLES ENVIRONNEMENT ===");
        logger.error("==========================================");
        
        boolean allVariablesPresent = true;
        
        // Vérifier chaque variable
        if (mysqlHost == null || mysqlHost.isEmpty()) {
            logger.error("❌ MYSQLHOST: NON DÉFINI");
            allVariablesPresent = false;
        } else {
            logger.info("✅ MYSQLHOST: {}", mysqlHost);
        }
        
        if (mysqlPort == null || mysqlPort.isEmpty()) {
            logger.error("❌ MYSQLPORT: NON DÉFINI");
            allVariablesPresent = false;
        } else {
            logger.info("✅ MYSQLPORT: {}", mysqlPort);
        }
        
        if (mysqlDatabase == null || mysqlDatabase.isEmpty()) {
            logger.error("❌ MYSQLDATABASE: NON DÉFINI");
            allVariablesPresent = false;
        } else {
            logger.info("✅ MYSQLDATABASE: {}", mysqlDatabase);
        }
        
        if (mysqlUser == null || mysqlUser.isEmpty()) {
            logger.error("❌ MYSQLUSER: NON DÉFINI");
            allVariablesPresent = false;
        } else {
            logger.info("✅ MYSQLUSER: {}", mysqlUser);
        }
        
        if (mysqlPassword == null || mysqlPassword.isEmpty()) {
            logger.error("❌ MYSQLPASSWORD: NON DÉFINI");
            allVariablesPresent = false;
        } else {
            logger.info("✅ MYSQLPASSWORD: *** (défini)");
        }
        
        if (!allVariablesPresent) {
            logger.error("");
            logger.error("⚠️  ATTENTION: Variables MySQL manquantes !");
            logger.error("⚠️  Sur Railway, vous devez connecter MySQL au service Spring Boot.");
            logger.error("⚠️  Voir le guide: RAILWAY_MYSQL_CONNECTION_GUIDE.md");
            logger.error("");
            logger.error("Étapes à suivre:");
            logger.error("1. Cliquez sur 'matchi_services_spring' → Settings");
            logger.error("2. Cherchez 'Connect Database' ou 'Add Service'");
            logger.error("3. Sélectionnez votre service MySQL");
            logger.error("4. Railway ajoutera automatiquement les variables");
            logger.error("");
        }
        
        logger.error("==========================================");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseConfiguration() {
        if (dataSource == null) {
            logger.warn("DataSource non disponible pour le test de connexion");
            return;
        }
        logger.info("==========================================");
        logger.info("=== CONFIGURATION BASE DE DONNÉES ===");
        logger.info("==========================================");
        
        // Afficher les variables d'environnement MySQL
        logger.info("Variables MySQL Railway:");
        logger.info("  MYSQLHOST: {}", mysqlHost.isEmpty() ? "NON DÉFINI" : mysqlHost);
        logger.info("  MYSQLPORT: {}", mysqlPort.isEmpty() ? "NON DÉFINI" : mysqlPort);
        logger.info("  MYSQLDATABASE: {}", mysqlDatabase.isEmpty() ? "NON DÉFINI" : mysqlDatabase);
        logger.info("  MYSQLUSER: {}", mysqlUser.isEmpty() ? "NON DÉFINI" : mysqlUser);
        logger.info("  MYSQLPASSWORD: {}", mysqlPassword.isEmpty() ? "NON DÉFINI" : "***");
        
        // Afficher la configuration Spring
        logger.info("Configuration Spring DataSource:");
        logger.info("  URL: {}", datasourceUrl.isEmpty() ? "NON DÉFINI" : maskPassword(datasourceUrl));
        logger.info("  Username: {}", datasourceUsername.isEmpty() ? "NON DÉFINI" : datasourceUsername);
        
        // Tester la connexion
        testConnection();
        
        logger.info("==========================================");
    }

    private void testConnection() {
        try {
            logger.info("Test de connexion à la base de données...");
            try (Connection connection = dataSource.getConnection()) {
                boolean valid = connection.isValid(5);
                if (valid) {
                    logger.info("✅ Connexion à la base de données RÉUSSIE");
                    logger.info("  Database: {}", connection.getCatalog());
                    logger.info("  URL: {}", connection.getMetaData().getURL());
                } else {
                    logger.error("❌ Connexion à la base de données ÉCHOUÉE - isValid() retourne false");
                }
            }
        } catch (SQLException e) {
            logger.error("❌ ERREUR de connexion à la base de données:", e);
            logger.error("   Message: {}", e.getMessage());
            logger.error("   SQL State: {}", e.getSQLState());
            logger.error("   Error Code: {}", e.getErrorCode());
        }
    }

    private String maskPassword(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        // Masquer le mot de passe dans l'URL JDBC
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}
