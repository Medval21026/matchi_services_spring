# 🔗 Guide Étape par Étape : Créer des Variable References sur Railway

## ⚠️ Problème Actuel

L'erreur `Connection refused` signifie que les variables MySQL ne sont **pas accessibles** dans le service Spring Boot.

Les variables existent dans le service MySQL, mais elles doivent être **référencées** dans le service Spring Boot.

## ✅ Solution : Créer des Variable References

### Étape 1 : Accéder aux Variables du Service Spring Boot

1. Allez sur [Railway.app](https://railway.app)
2. Ouvrez votre projet "refreshing-dream"
3. **Cliquez sur le service "matchi_services_spring"** (celui qui crash, pas MySQL)
4. Allez dans l'onglet **"Variables"**

### Étape 2 : Créer la Première Variable Reference

1. Cliquez sur **"+ New Variable"** ou **"Add Variable"**

2. **Pour MYSQLHOST :**
   - **Name:** `MYSQLHOST`
   - **Value:** Au lieu de taper une valeur, cherchez un bouton **"Reference"** ou une icône de **chaîne/lien**
   - Cliquez sur **"Reference"** ou **"Select from service"**
   - Dans le menu déroulant :
     - **Service:** Sélectionnez **"MySQL"**
     - **Variable:** Sélectionnez **"MYSQLHOST"**
   - Cliquez sur **"Save"** ou **"Add"**

### Étape 3 : Répéter pour les Autres Variables

Répétez l'étape 2 pour chaque variable :

- **MYSQLPORT** → Référence vers `MYSQLPORT` du service MySQL
- **MYSQLDATABASE** → Référence vers `MYSQLDATABASE` du service MySQL  
- **MYSQLUSER** → Référence vers `MYSQLUSER` du service MySQL
- **MYSQLPASSWORD** → Référence vers `MYSQLPASSWORD` du service MySQL

### Étape 4 : Ajouter SPRING_PROFILES_ACTIVE

1. Cliquez sur **"+ New Variable"**
2. **Name:** `SPRING_PROFILES_ACTIVE`
3. **Value:** `prod` (valeur directe, **PAS une référence**)
4. Cliquez sur **"Save"**

### Étape 5 : Vérifier

Dans l'onglet "Variables" du service "matchi_services_spring", vous devriez maintenant voir :

```
✅ MYSQLHOST          [Référence → MySQL.MYSQLHOST]
✅ MYSQLPORT          [Référence → MySQL.MYSQLPORT]
✅ MYSQLDATABASE      [Référence → MySQL.MYSQLDATABASE]
✅ MYSQLUSER          [Référence → MySQL.MYSQLUSER]
✅ MYSQLPASSWORD      [Référence → MySQL.MYSQLPASSWORD]
✅ SPRING_PROFILES_ACTIVE = prod
```

### Étape 6 : Redéployer

1. Railway redéploiera automatiquement
2. Ou allez dans "Deployments" → Cliquez sur "Redeploy"

### Étape 7 : Vérifier les Logs

Après le redéploiement, dans les logs, vous devriez voir :

```
=== DIAGNOSTIC VARIABLES ENVIRONNEMENT ===
Profils Spring actifs: prod
Variables MySQL Railway:
  MYSQLHOST: [une valeur, pas "❌ NON DÉFINI"]
  MYSQLPORT: [une valeur]
  MYSQLDATABASE: [une valeur]
  ...
```

## 🎯 Alternative : Si vous ne voyez pas "Reference"

Si Railway n'affiche pas l'option "Reference", essayez :

1. **Dans le service MySQL** → "Variables"
2. Pour chaque variable (ex: `MYSQLHOST`), cliquez sur les **trois points (⋮)** à droite
3. Sélectionnez **"Add Reference"** ou **"Share with service"**
4. Sélectionnez le service **"matchi_services_spring"**
5. Railway créera automatiquement la référence

## ✅ Checklist Finale

- [ ] 5 Variable References créées (MYSQLHOST, MYSQLPORT, MYSQLDATABASE, MYSQLUSER, MYSQLPASSWORD)
- [ ] SPRING_PROFILES_ACTIVE=prod ajouté
- [ ] Service redéployé
- [ ] Logs montrent les variables définies (pas "NON DÉFINI")
- [ ] Service passe à "Online" (point vert)

## 🚨 Si ça ne fonctionne toujours pas

1. **Vérifiez que les deux services sont dans le même environnement**
   - MySQL doit être en "production"
   - matchi_services_spring doit être en "production"

2. **Vérifiez les noms exacts des services**
   - Le service MySQL doit s'appeler exactement "MySQL"
   - Le service Spring Boot doit s'appeler "matchi_services_spring"

3. **Essayez de déconnecter et reconnecter**
   - Supprimez toutes les Variable References
   - Recréez-les une par une

4. **Contactez le support Railway**
   - Si l'option "Reference" n'apparaît pas, c'est peut-être un problème de l'interface Railway
