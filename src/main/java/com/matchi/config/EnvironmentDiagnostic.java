package com.matchi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * Diagnostic des variables d'environnement - s'exécute AVANT la création de la DataSource
 * pour afficher les variables même si la connexion échoue
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnvironmentDiagnostic implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentDiagnostic.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        
        logger.error("==========================================");
        logger.error("=== DIAGNOSTIC VARIABLES ENVIRONNEMENT ===");
        logger.error("==========================================");
        
        // Vérifier le profil actif
        String[] activeProfiles = env.getActiveProfiles();
        logger.error("Profils Spring actifs: {}", activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "AUCUN");
        
        if (activeProfiles.length == 0 || !java.util.Arrays.asList(activeProfiles).contains("prod")) {
            logger.error("⚠️  ATTENTION: Le profil 'prod' n'est PAS actif !");
            logger.error("⚠️  Ajoutez SPRING_PROFILES_ACTIVE=prod dans les variables d'environnement Railway");
        }
        
        // Vérifier les variables MySQL
        logger.error("");
        logger.error("Variables MySQL Railway:");
        
        String mysqlHost = env.getProperty("MYSQLHOST");
        String mysqlPort = env.getProperty("MYSQLPORT");
        String mysqlDatabase = env.getProperty("MYSQLDATABASE");
        String mysqlUser = env.getProperty("MYSQLUSER");
        String mysqlPassword = env.getProperty("MYSQLPASSWORD");
        
        logger.error("  MYSQLHOST: {}", mysqlHost != null && !mysqlHost.isEmpty() ? mysqlHost : "❌ NON DÉFINI");
        logger.error("  MYSQLPORT: {}", mysqlPort != null && !mysqlPort.isEmpty() ? mysqlPort : "❌ NON DÉFINI");
        logger.error("  MYSQLDATABASE: {}", mysqlDatabase != null && !mysqlDatabase.isEmpty() ? mysqlDatabase : "❌ NON DÉFINI");
        logger.error("  MYSQLUSER: {}", mysqlUser != null && !mysqlUser.isEmpty() ? mysqlUser : "❌ NON DÉFINI");
        logger.error("  MYSQLPASSWORD: {}", mysqlPassword != null && !mysqlPassword.isEmpty() ? "*** (défini)" : "❌ NON DÉFINI");
        
        // Vérifier la configuration Spring DataSource
        logger.error("");
        logger.error("Configuration Spring DataSource:");
        String datasourceUrl = env.getProperty("spring.datasource.url");
        String datasourceUsername = env.getProperty("spring.datasource.username");
        
        logger.error("  spring.datasource.url: {}", datasourceUrl != null ? maskPassword(datasourceUrl) : "❌ NON DÉFINI");
        logger.error("  spring.datasource.username: {}", datasourceUsername != null ? datasourceUsername : "❌ NON DÉFINI");
        
        // Afficher toutes les variables d'environnement qui commencent par MYSQL
        logger.error("");
        logger.error("Toutes les variables MYSQL* disponibles:");
        boolean foundAny = false;
        for (String key : System.getenv().keySet()) {
            if (key.startsWith("MYSQL")) {
                logger.error("  {} = {}", key, key.contains("PASSWORD") ? "***" : System.getenv(key));
                foundAny = true;
            }
        }
        if (!foundAny) {
            logger.error("  ❌ AUCUNE variable MYSQL* trouvée dans l'environnement");
        }
        
        // Instructions si variables manquantes
        if (mysqlHost == null || mysqlHost.isEmpty() || 
            mysqlPort == null || mysqlPort.isEmpty() ||
            mysqlDatabase == null || mysqlDatabase.isEmpty()) {
            logger.error("");
            logger.error("⚠️  ATTENTION: Variables MySQL manquantes !");
            logger.error("⚠️  Sur Railway, vous devez créer des 'Variable References'.");
            logger.error("");
            logger.error("Étapes à suivre:");
            logger.error("1. Allez sur Railway → 'matchi_services_spring' → Variables");
            logger.error("2. Cliquez sur '+ New Variable'");
            logger.error("3. Pour chaque variable MySQL:");
            logger.error("   - Name: MYSQLHOST");
            logger.error("   - Value: Cliquez sur 'Reference' → Sélectionnez service 'MySQL' → Variable 'MYSQLHOST'");
            logger.error("4. Répétez pour: MYSQLPORT, MYSQLDATABASE, MYSQLUSER, MYSQLPASSWORD");
            logger.error("5. Ajoutez aussi: SPRING_PROFILES_ACTIVE = prod");
            logger.error("");
        }
        
        logger.error("==========================================");
    }
    
    private String maskPassword(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}
