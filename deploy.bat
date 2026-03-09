@echo off
REM Script de déploiement pour Matchi Service (Windows)
REM Usage: deploy.bat

REM Configuration
set VPS_IP=187.124.35.219
set VPS_USER=root
set VPS_PATH=~/matchi_services_spring
set JAR_NAME=matchi_service-0.0.1-SNAPSHOT.jar

echo ==========================================
echo 🚀 Déploiement de Matchi Service
echo ==========================================
echo.

REM Étape 1: Compiler le projet
echo 📦 Étape 1: Compilation du projet...
call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation!
    exit /b 1
)

echo ✅ Compilation réussie!
echo.

REM Étape 2: Copier le JAR vers le VPS
echo 📤 Étape 2: Copie du JAR vers le VPS...
scp target\%JAR_NAME% %VPS_USER%@%VPS_IP%:%VPS_PATH%/

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la copie!
    exit /b 1
)

echo ✅ JAR copié avec succès!
echo.

REM Étape 3: Arrêter l'ancienne instance et démarrer la nouvelle
echo 🔄 Étape 3: Déploiement sur le VPS...
ssh %VPS_USER%@%VPS_IP% "cd %VPS_PATH% && pkill -f '%JAR_NAME%' || echo 'Aucune instance en cours' && sleep 2 && nohup java -jar %JAR_NAME% > log.txt 2>&1 & && echo '✅ Application démarrée!' && sleep 3 && ps aux | grep %JAR_NAME% | grep -v grep"

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors du déploiement!
    exit /b 1
)

echo.
echo ==========================================
echo ✅ Déploiement terminé avec succès!
echo ==========================================
echo.
echo 📝 Pour voir les logs:
echo    ssh %VPS_USER%@%VPS_IP% "tail -f %VPS_PATH%/log.txt"
echo.
echo 🌐 Application accessible sur:
echo    http://%VPS_IP%:8085
echo    http://%VPS_IP%:8085/swagger-ui.html
echo.
