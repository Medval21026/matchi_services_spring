# 🔧 Guide : Connecter MySQL au Service Spring Boot sur Railway

## ⚠️ Problème Actuel

Les logs montrent : `Database JDBC URL [undefined/unknown]`

Cela signifie que les variables d'environnement MySQL ne sont **pas disponibles** dans votre service Spring Boot.

## ✅ Solution : Connecter MySQL au Service Spring Boot

### Méthode 1 : Via l'Interface Railway (Recommandé)

1. **Allez sur Railway** → Votre projet "refreshing-dream"

2. **Cliquez sur le service "matchi_services_spring"** (pas MySQL)

3. **Allez dans l'onglet "Settings"**

4. **Cherchez la section "Networking" ou "Connected Services"**

5. **Cliquez sur "Connect Database" ou "Add Service"**

6. **Sélectionnez votre service MySQL** dans la liste

7. **Railway ajoutera automatiquement les variables :**
   - `MYSQLHOST`
   - `MYSQLPORT`
   - `MYSQLDATABASE`
   - `MYSQLUSER`
   - `MYSQLPASSWORD`

### Méthode 2 : Via les Variables d'Environnement (Manuel)

Si la méthode 1 ne fonctionne pas :

1. **Cliquez sur le service MySQL** → "Settings" → "Variables"
2. **Notez les valeurs de :**
   - `MYSQLHOST`
   - `MYSQLPORT`
   - `MYSQLDATABASE`
   - `MYSQLUSER`
   - `MYSQLPASSWORD`

3. **Cliquez sur le service "matchi_services_spring"** → "Variables"
4. **Ajoutez manuellement chaque variable** avec les valeurs notées

### Méthode 3 : Utiliser l'URL Privée Railway

Si les variables ne sont pas disponibles, Railway peut utiliser l'URL privée :

1. **Cliquez sur le service MySQL** → "Settings" → "Networking"
2. **Notez l'URL privée** (ex: `mysql.railway.internal`)
3. **Dans le service Spring Boot** → "Variables", ajoutez :
   ```
   MYSQLHOST=mysql.railway.internal
   MYSQLPORT=3306
   MYSQLDATABASE=railway
   MYSQLUSER=root
   MYSQLPASSWORD=<votre mot de passe MySQL>
   ```

## 🔍 Vérification

Après avoir connecté MySQL :

1. **Vérifiez les variables** dans "matchi_services_spring" → "Variables"
   - Toutes les variables `MYSQL*` doivent être présentes

2. **Vérifiez SPRING_PROFILES_ACTIVE**
   - Doit être défini à `prod`

3. **Redéployez le service**
   - Railway redéploiera automatiquement ou cliquez sur "Redeploy"

4. **Vérifiez les logs**
   - Vous devriez voir dans les logs :
     ```
     Variables MySQL Railway:
       MYSQLHOST: [une valeur, pas "NON DÉFINI"]
       MYSQLPORT: [une valeur]
       MYSQLDATABASE: [une valeur]
       ...
     ```

## 📝 Checklist

- [ ] MySQL est "Online" sur Railway
- [ ] MySQL est connecté au service "matchi_services_spring"
- [ ] Les variables `MYSQL*` sont présentes dans "matchi_services_spring" → "Variables"
- [ ] `SPRING_PROFILES_ACTIVE=prod` est défini
- [ ] Le service a été redéployé après la connexion
- [ ] Les logs montrent les variables MySQL définies (pas "NON DÉFINI")

## 🚨 Si ça ne fonctionne toujours pas

1. **Vérifiez que vous êtes dans le bon environnement**
   - Le projet doit être en "production"
   - Les deux services doivent être dans le même environnement

2. **Vérifiez les noms des services**
   - Le service MySQL doit s'appeler "MySQL" ou similaire
   - Le service Spring Boot doit s'appeler "matchi_services_spring"

3. **Essayez de déconnecter et reconnecter**
   - Dans "Settings" du service Spring Boot
   - Déconnectez MySQL
   - Reconnectez MySQL

4. **Vérifiez les logs MySQL**
   - Cliquez sur MySQL → "Logs"
   - Vérifiez que MySQL est bien démarré

## ✅ Une fois connecté

Après avoir connecté MySQL et redéployé :

1. **Les tables seront créées automatiquement** par Hibernate
2. **Le service passera à "Online"** (point vert)
3. **L'API sera accessible** via l'URL Railway
