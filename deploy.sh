#!/bin/bash

# Script de déploiement pour Matchi Service
# Usage: ./deploy.sh

# Configuration
VPS_IP="187.124.35.219"
VPS_USER="root"
VPS_PATH="~/matchi_services_spring"
JAR_NAME="matchi_service-0.0.1-SNAPSHOT.jar"
PROJECT_NAME="matchi_service"

echo "=========================================="
echo "🚀 Déploiement de Matchi Service"
echo "=========================================="

# Étape 1: Compiler le projet
echo ""
echo "📦 Étape 1: Compilation du projet..."
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation!"
    exit 1
fi

echo "✅ Compilation réussie!"

# Étape 2: Copier le JAR vers le VPS
echo ""
echo "📤 Étape 2: Copie du JAR vers le VPS..."
scp target/${JAR_NAME} ${VPS_USER}@${VPS_IP}:${VPS_PATH}/

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la copie!"
    exit 1
fi

echo "✅ JAR copié avec succès!"

# Étape 3: Arrêter l'ancienne instance et démarrer la nouvelle
echo ""
echo "🔄 Étape 3: Déploiement sur le VPS..."
ssh ${VPS_USER}@${VPS_IP} << EOF
    cd ${VPS_PATH}
    echo "Arrêt de l'ancienne instance..."
    pkill -f '${JAR_NAME}' || echo "Aucune instance en cours d'exécution"
    sleep 2
    echo "Démarrage de la nouvelle instance..."
    nohup java -jar ${JAR_NAME} > log.txt 2>&1 &
    echo "✅ Application démarrée!"
    echo "📋 PID: \$!"
    sleep 3
    echo "Vérification du démarrage..."
    ps aux | grep ${JAR_NAME} | grep -v grep
EOF

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors du déploiement!"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ Déploiement terminé avec succès!"
echo "=========================================="
echo ""
echo "📝 Pour voir les logs:"
echo "   ssh ${VPS_USER}@${VPS_IP} 'tail -f ${VPS_PATH}/log.txt'"
echo ""
echo "🌐 Application accessible sur:"
echo "   http://${VPS_IP}:8085"
echo "   http://${VPS_IP}:8085/swagger-ui.html"
echo ""
