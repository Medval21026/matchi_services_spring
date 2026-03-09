# Guide de Déploiement - Matchi Service

## Scripts de déploiement disponibles

### 1. Script Bash (Linux/Mac/Git Bash)
```bash
./deploy.sh
```

### 2. Script Batch (Windows)
```cmd
deploy.bat
```

## Déploiement manuel

Si vous préférez déployer manuellement, suivez ces étapes :

### Étape 1: Compiler le projet
```bash
./mvnw clean package -DskipTests
```

### Étape 2: Copier le JAR vers le VPS
```bash
scp target/matchi_service-0.0.1-SNAPSHOT.jar root@187.124.35.219:~/matchi_services_spring/
```

### Étape 3: Déployer sur le VPS
```bash
ssh root@187.124.35.219
cd ~/matchi_services_spring
pkill -f 'matchi_service-0.0.1-SNAPSHOT.jar'
nohup java -jar matchi_service-0.0.1-SNAPSHOT.jar > log.txt 2>&1 &
```

## Vérification

### Vérifier que l'application est démarrée
```bash
ssh root@187.124.35.219 "ps aux | grep matchi_service"
```

### Voir les logs
```bash
ssh root@187.124.35.219 "tail -f ~/matchi_services_spring/log.txt"
```

### Tester l'API
```bash
curl http://187.124.35.219:8085/api/clients
```

## URLs d'accès

- **API Base**: http://187.124.35.219:8085
- **Swagger UI**: http://187.124.35.219:8085/swagger-ui.html
- **API Docs**: http://187.124.35.219:8085/v3/api-docs

## Configuration

Assurez-vous que les fichiers de configuration sont corrects :
- `application.properties` : Configuration de développement
- `application-prod.properties` : Configuration de production

## Arrêter l'application

```bash
ssh root@187.124.35.219 "pkill -f 'matchi_service-0.0.1-SNAPSHOT.jar'"
```
