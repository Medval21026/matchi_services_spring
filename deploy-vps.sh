#!/bin/bash

# Script de déploiement à exécuter directement sur le VPS
# Usage: ./deploy-vps.sh

JAR_NAME="matchi_service-0.0.1-SNAPSHOT.jar"
APP_DIR="~/matchi_services_spring"

echo "=========================================="
echo "🚀 Déploiement de Matchi Service"
echo "=========================================="

cd ${APP_DIR}

# Arrêter l'ancienne instance
echo ""
echo "🛑 Arrêt de l'ancienne instance..."
pkill -f "${JAR_NAME}" || echo "Aucune instance en cours d'exécution"
sleep 2

# Démarrer la nouvelle instance
echo ""
echo "▶️  Démarrage de la nouvelle instance..."
nohup java -jar ${JAR_NAME} > log.txt 2>&1 &
APP_PID=$!

echo "✅ Application démarrée avec PID: ${APP_PID}"
sleep 3

# Vérifier que l'application est bien démarrée
if ps -p ${APP_PID} > /dev/null; then
    echo "✅ Application en cours d'exécution (PID: ${APP_PID})"
    echo ""
    echo "📝 Pour voir les logs:"
    echo "   tail -f ${APP_DIR}/log.txt"
    echo ""
    echo "🌐 Application accessible sur:"
    echo "   http://187.124.35.219:8085"
    echo "   http://187.124.35.219:8085/swagger-ui.html"
else
    echo "❌ Erreur: L'application ne s'est pas démarrée correctement!"
    echo "📝 Vérifiez les logs:"
    echo "   tail -n 50 ${APP_DIR}/log.txt"
    exit 1
fi
