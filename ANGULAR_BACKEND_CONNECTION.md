# 🔗 Configuration Backend-Frontend - Matchi Service

## ✅ Configuration Effectuée

### 1. **CorsConfig.java** ✅
Fichier de configuration CORS créé dans `src/main/java/com/matchi/config/CorsConfig.java`

**Fonctionnalités:**
- ✅ Autorise les requêtes depuis `http://localhost:4200` (Angular)
- ✅ Autorise tous les headers nécessaires
- ✅ Autorise toutes les méthodes HTTP (GET, POST, PUT, DELETE, etc.)
- ✅ Active les credentials (pour JWT tokens)

---

### 2. **SecurityConfig.java** ✅
Mis à jour pour activer CORS dans Spring Security

---

### 3. **application.properties** ✅
Configuration du serveur sur le port 8080 avec encodage UTF-8

---

## 🚀 Comment Utiliser depuis Angular

### 1. Configuration Environment Angular

**Fichier:** `src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

---

### 2. Exemple de Service Angular avec HttpClient

**Fichier:** `src/app/core/services/abonnement.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AbonnementService {
  private apiUrl = `${environment.apiUrl}/abonnements`;

  constructor(private http: HttpClient) {}

  // GET - Récupérer tous les abonnements
  getAllAbonnements(): Observable<AbonnementDTO[]> {
    return this.http.get<AbonnementDTO[]>(this.apiUrl);
  }

  // POST - Créer un abonnement
  createAbonnement(data: AbonnementCreateDTO): Observable<AbonnementDTO> {
    return this.http.post<AbonnementDTO>(this.apiUrl, data, {
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      })
    });
  }

  // PUT - Modifier un abonnement
  updateAbonnement(id: number, data: AbonnementUpdateDTO): Observable<AbonnementDTO> {
    return this.http.put<AbonnementDTO>(`${this.apiUrl}/${id}`, data);
  }

  // DELETE - Supprimer un abonnement
  deleteAbonnement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
```

---

### 3. Configuration HttpClient dans Angular

**Fichier:** `src/app/app.config.ts` (Angular 17+)

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient()
  ]
};
```

**OU pour Angular versions antérieures:**

**Fichier:** `src/app/app.module.ts`

```typescript
import { HttpClientModule } from '@angular/common/http';

@NgModule({
  imports: [
    BrowserModule,
    HttpClientModule,
    // ... autres modules
  ]
})
export class AppModule { }
```

---

### 4. Exemple d'Utilisation dans un Component

**Fichier:** `src/app/features/abonnement/abonnement-list/abonnement-list.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { AbonnementService } from '../../../core/services/abonnement.service';

@Component({
  selector: 'app-abonnement-list',
  templateUrl: './abonnement-list.component.html'
})
export class AbonnementListComponent implements OnInit {
  abonnements: AbonnementDTO[] = [];
  loading = false;
  error: string | null = null;

  constructor(private abonnementService: AbonnementService) {}

  ngOnInit(): void {
    this.loadAbonnements();
  }

  loadAbonnements(): void {
    this.loading = true;
    this.error = null;

    this.abonnementService.getAllAbonnements().subscribe({
      next: (data) => {
        this.abonnements = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement:', err);
        this.error = 'Impossible de charger les abonnements';
        this.loading = false;
      }
    });
  }

  createAbonnement(): void {
    const newAbonnement: AbonnementCreateDTO = {
      terrainId: 1,
      clientTelephone: 42345678,
      dateDebut: '2026-01-15',
      dateFin: '2026-02-15',
      horaires: [
        {
          jourSemaine: 'LUNDI',
          heureDebut: '08:00',
          prixHeure: 1500
        }
      ]
    };

    this.abonnementService.createAbonnement(newAbonnement).subscribe({
      next: (created) => {
        console.log('Abonnement créé:', created);
        this.loadAbonnements(); // Recharger la liste
      },
      error: (err) => {
        console.error('Erreur création:', err);
        alert('Erreur lors de la création');
      }
    });
  }
}
```

---

## 🧪 Tester la Connexion

### 1. Démarrer le Backend Spring Boot

```bash
cd C:\Users\HP\Desktop\matchi_service
.\mvnw spring-boot:run
```

**Vérifier que le serveur est démarré:**
- URL: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

---

### 2. Démarrer Angular

```bash
cd votre-projet-angular
ng serve
```

**Angular sera disponible sur:**
- http://localhost:4200

---

### 3. Test Simple avec Angular

Créez un composant de test:

```typescript
// test-connection.component.ts
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-test-connection',
  template: `
    <div>
      <h2>Test Connexion Backend</h2>
      <button (click)="testConnection()">Tester</button>
      <div *ngIf="result">
        <pre>{{ result | json }}</pre>
      </div>
    </div>
  `
})
export class TestConnectionComponent {
  result: any;

  constructor(private http: HttpClient) {}

  testConnection(): void {
    this.http.get('http://localhost:8080/api/terrains')
      .subscribe({
        next: (data) => {
          console.log('✅ Connexion réussie!', data);
          this.result = data;
        },
        error: (err) => {
          console.error('❌ Erreur de connexion:', err);
          this.result = { error: err.message };
        }
      });
  }
}
```

---

## 📝 Endpoints Disponibles

### Base URL
```
http://localhost:8080/api
```

### Tous les endpoints

| Module | Méthode | Endpoint | Description |
|--------|---------|----------|-------------|
| **Auth** | POST | `/proprietaires/login` | Connexion |
| **Proprietaire** | POST | `/proprietaires` | Inscription |
| **Proprietaire** | GET | `/proprietaires/{id}` | Profil |
| **Terrain** | GET | `/terrains` | Liste terrains |
| **Terrain** | POST | `/terrains` | Créer terrain |
| **Terrain** | GET | `/terrains/{id}` | Détails |
| **Client** | GET | `/clients-abonnes` | Liste clients |
| **Client** | POST | `/clients-abonnes` | Créer client |
| **Abonnement** | GET | `/abonnements` | Liste |
| **Abonnement** | POST | `/abonnements` | Créer |
| **Abonnement** | PUT | `/abonnements/{id}` | Modifier |
| **Abonnement** | DELETE | `/abonnements/{id}` | Supprimer |
| **Réservation** | GET | `/reservations-ponctuelles` | Liste |
| **Réservation** | POST | `/reservations-ponctuelles` | Créer |
| **Réservation** | PUT | `/reservations-ponctuelles/{id}` | Modifier |
| **Réservation** | DELETE | `/reservations-ponctuelles/{id}` | Supprimer |
| **Disponibilité** | GET | `/disponibilites/horaires-occupes` | Horaires occupés |
| **Indisponible** | GET | `/indisponibles/terrain/{id}` | Horaires indisponibles |

---

## 🔒 Gestion de l'Authentification JWT (Futur)

### 1. Interceptor Angular pour JWT

**Fichier:** `src/app/core/interceptors/auth.interceptor.ts`

```typescript
import { HttpInterceptorFn } from '@angular/core';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  
  if (token) {
    const cloned = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    return next(cloned);
  }
  
  return next(req);
};
```

**Ajout dans app.config.ts:**
```typescript
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
```

---

### 2. Service Auth

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/proprietaires`;

  constructor(private http: HttpClient) {}

  login(telephone: number, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, {
      telephone,
      password
    }).pipe(
      tap(response => {
        // Sauvegarder le token
        localStorage.setItem('token', response.token);
        localStorage.setItem('user', JSON.stringify(response.proprietaire));
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}
```

---

## ⚠️ Résolution de Problèmes

### Erreur CORS

**Symptôme:** 
```
Access to XMLHttpRequest at 'http://localhost:8080/api/...' from origin 'http://localhost:4200' 
has been blocked by CORS policy
```

**Solution:**
1. Vérifier que `CorsConfig.java` est bien présent
2. Redémarrer le serveur Spring Boot
3. Vider le cache du navigateur (Ctrl+Shift+Delete)

---

### Erreur 404

**Symptôme:**
```
GET http://localhost:8080/api/terrains 404 (Not Found)
```

**Solution:**
1. Vérifier que le backend est bien démarré
2. Vérifier l'URL exacte dans Swagger
3. Vérifier les logs du backend

---

### Erreur de connexion refusée

**Symptôme:**
```
Http failure response for http://localhost:8080/...: 0 Unknown Error
```

**Solution:**
1. Le backend n'est pas démarré → Lancer `mvnw spring-boot:run`
2. Mauvais port → Vérifier `application.properties` (port=8080)

---

## 📦 Checklist Finale

- [x] ✅ `CorsConfig.java` créé
- [x] ✅ `SecurityConfig.java` mis à jour
- [x] ✅ `application.properties` configuré
- [ ] Backend Spring Boot démarré sur port 8080
- [ ] Angular démarré sur port 4200
- [ ] Test de connexion réussi
- [ ] Tous les services Angular créés
- [ ] Interceptor JWT configuré (si nécessaire)

---

## 🎯 Prochaines Étapes

1. **Démarrer le backend** → `mvnw spring-boot:run`
2. **Créer le projet Angular** → Suivre `FRONTEND_ANGULAR_GUIDE.md`
3. **Créer les services** → Un service par module (terrain, client, etc.)
4. **Créer les composants** → Liste, formulaires, détails
5. **Tester chaque fonctionnalité**

---

**Configuration créée le:** 2026-01-13  
**Backend:** Spring Boot 3.x sur port 8080  
**Frontend:** Angular 17+ sur port 4200  

✅ **La configuration est prête pour la communication !**
