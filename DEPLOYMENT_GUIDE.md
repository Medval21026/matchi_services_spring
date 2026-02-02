# 🚀 Guide de Déploiement - Matchi Service Backend

## ⚠️ IMPORTANT : Vercel ne supporte PAS Spring Boot

**Vercel** est conçu pour :
- ✅ Applications frontend (Angular, React, Vue)
- ✅ Fonctions serverless (Node.js, Python)
- ❌ **PAS pour les applications Java/Spring Boot**

## 🎯 Solutions Recommandées

### Option 1 : Railway (⭐ Recommandé - Le plus simple)

**Avantages :**
- ✅ Supporte Java/Spring Boot nativement
- ✅ Base de données MySQL incluse
- ✅ Déploiement automatique depuis GitHub
- ✅ Gratuit pour commencer

**Étapes :**

1. **Créer un compte sur [Railway.app](https://railway.app)**

2. **Connecter votre repository GitHub**

3. **Ajouter une base de données MySQL :**
   - Cliquez sur "New" → "Database" → "MySQL"
   - Railway créera automatiquement les variables d'environnement

4. **Configurer les variables d'environnement :**
   ```
   SPRING_PROFILES_ACTIVE=prod
   DATABASE_URL=<fourni par Railway>
   DB_USERNAME=<fourni par Railway>
   DB_PASSWORD=<fourni par Railway>
   PORT=8080
   ```

5. **Déployer :**
   - Railway détectera automatiquement votre projet Java
   - Il construira avec Maven et déploiera

6. **Mettre à jour CorsConfig.java :**
   - Ajoutez l'URL Railway dans les origines autorisées

---

### Option 2 : Render

**Avantages :**
- ✅ Supporte Spring Boot
- ✅ Base de données MySQL disponible
- ✅ Plan gratuit disponible

**Étapes :**

1. Créer un compte sur [Render.com](https://render.com)

2. Créer un nouveau "Web Service"

3. Connecter votre repository GitHub

4. Configuration :
   - **Build Command :** `./mvnw clean package -DskipTests`
   - **Start Command :** `java -jar target/matchi_service-0.0.1-SNAPSHOT.jar`
   - **Environment :** Java

5. Ajouter une base de données MySQL

6. Configurer les variables d'environnement

---

### Option 3 : Heroku

**Avantages :**
- ✅ Très populaire
- ✅ Supporte Spring Boot
- ⚠️ Plan gratuit limité

**Étapes :**

1. Installer Heroku CLI

2. Créer un fichier `Procfile` :
   ```
   web: java -jar target/matchi_service-0.0.1-SNAPSHOT.jar
   ```

3. Créer un fichier `system.properties` :
   ```
   java.runtime.version=17
   ```

4. Déployer :
   ```bash
   heroku create matchi-service
   heroku addons:create cleardb:ignite
   git push heroku main
   ```

---

### Option 4 : AWS / Azure / Google Cloud

**Avantages :**
- ✅ Très puissant et scalable
- ⚠️ Plus complexe à configurer
- ⚠️ Peut être coûteux

---

## 📝 Configuration Requise

### 1. Mettre à jour CorsConfig.java

Ajoutez l'URL de votre backend déployé :

```java
config.setAllowedOriginPatterns(Arrays.asList(
    // Développement
    "http://localhost:*",
    "http://172.20.10.*",
    // Production
    "https://matchi-services-angular-afyy.vercel.app",  // Votre frontend Vercel
    "https://*.vercel.app",
    "https://votre-backend.railway.app",  // Votre backend Railway
    "https://*.railway.app"
));
```

### 2. Variables d'Environnement

Sur votre plateforme de déploiement, configurez :

```
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:mysql://host:port/database
DB_USERNAME=votre_username
DB_PASSWORD=votre_password
PORT=8080
```

### 3. Mettre à jour l'URL du Backend dans Angular

Dans votre application Angular, mettez à jour `environment.prod.ts` :

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://votre-backend.railway.app/api'  // URL de votre backend déployé
};
```

---

## 🔍 Vérification

Après le déploiement, testez :

1. **Health Check :**
   ```
   curl https://votre-backend.railway.app/api/clients
   ```

2. **Swagger UI :**
   ```
   https://votre-backend.railway.app/swagger-ui.html
   ```

3. **Depuis votre frontend Angular :**
   - L'application devrait pouvoir communiquer avec le backend

---

## ❓ Pourquoi Vercel ne fonctionne pas ?

**Vercel** utilise un modèle serverless :
- Les fonctions s'exécutent à la demande
- Pas de serveur qui tourne en continu
- Optimisé pour Node.js/Python serverless

**Spring Boot** nécessite :
- Un serveur Java qui tourne en continu
- Un environnement JVM stable
- Support des connexions longues (WebSocket, etc.)

→ **Incompatibilité fondamentale**

---

## ✅ Recommandation Finale

**Utilisez Railway** pour déployer votre backend Spring Boot :
1. Simple à configurer
2. Supporte Java nativement
3. Base de données incluse
4. Déploiement automatique

**Gardez Vercel** pour votre frontend Angular (c'est parfait pour ça !)

---

**Architecture Recommandée :**
```
┌─────────────────────────────────┐
│  Angular Frontend               │
│  (Vercel)                       │
│  https://matchi-services-...   │
└──────────────┬──────────────────┘
               │ HTTPS
               │ CORS
               ▼
┌─────────────────────────────────┐
│  Spring Boot Backend            │
│  (Railway)                      │
│  https://matchi-backend.railway │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│  MySQL Database                 │
│  (Railway)                      │
└─────────────────────────────────┘
```
