# ARCHITECTURE.md — RentDrive Platform

## Vue d'ensemble

RentDrive est une plateforme SaaS de location de voiture ciblant le marché algérien. Elle connecte trois acteurs : **Clients** (locataires), **Bailleurs/Agences** (propriétaires de véhicules), et **Admins** (plateforme).

**Stratégie d'architecture :**
- **Phase 1-2 :** Monolithe Spring Boot (modules internes bien séparés)
- **Phase 4+ :** Migration microservices (découpage en services indépendants)

---

## Repositories

| Repo | Stack | Rôle |
|------|-------|------|
| `RentDrive` | Spring Boot (Java 21) | https://github.com/eliasturki93/RentDrive |
| `rentdrive-web` | Angular 17+ | https://github.com/eliasturki93/rentdrive-web |
| `rentdrive-mobile` | Flutter (Dart) | https://github.com/eliasturki93/rentdrive-mobile |

---

## Architecture globale (Phase 1-2 — Monolithe)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Clients                                   │
│   Angular Web (4200)          Flutter Mobile (iOS/Android)       │
└──────────────┬────────────────────────────┬─────────────────────┘
               │  REST/JSON + JWT            │  REST/JSON + JWT + FCM
               ▼                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Spring Boot Monolith (:8080)                     │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │   Auth   │  │  Store   │  │ Vehicle  │  │    Booking    │  │
│  │  module  │  │  module  │  │  module  │  │    module     │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │  Search  │  │ Payment  │  │  Review  │  │   Document    │  │
│  │  module  │  │  module  │  │  module  │  │  (KYC) module │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘  │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                 Couches transversales                    │   │
│  │  Security (JWT) │ GlobalExceptionHandler │ Flyway │ Docs │   │
│  └─────────────────────────────────────────────────────────┘   │
└────────────────┬──────────────────────────────────────────────-─┘
                 │
     ┌───────────┼───────────┬──────────────┐
     ▼           ▼           ▼              ▼
  MySQL 8.x   Redis       MinIO        SMTP/FCM
 (données)  (sessions   (fichiers     (notifs)
            + cache)     + photos)
```

---

## Architecture cible (Phase 4+ — Microservices)

```
┌──────────────────────────────────────────────────────────────┐
│                        Clients                                │
│        Angular Web              Flutter Mobile                │
└──────────────┬───────────────────────────┬───────────────────┘
               │                           │
               ▼                           ▼
┌─────────────────────────────────────────────────────────────┐
│              API Gateway (Spring Cloud Gateway :8080)        │
│     Routing │ JWT validation │ Rate limiting │ CORS          │
└──┬──────────┬──────────┬────────────┬────────────┬──────────┘
   │          │          │            │            │
   ▼          ▼          ▼            ▼            ▼
:8081      :8082      :8083        :8084        :8085
auth-    agency-     car-        booking-     search-
service  service    service      service      service
```

| Service | Port | Responsabilités |
|---------|------|-----------------|
| `auth-service` | 8081 | JWT, login, logout, rôles |
| `agency-service` | 8082 | CRUD store, workflow validation admin |
| `car-service` | 8083 | CRUD véhicule, upload photos MinIO |
| `booking-service` | 8084 | Réservations, machine à états |
| `search-service` | 8085 | Filtres, géolocalisation, comparateur |

---

## Domaine métier — Modèle de données

### Entités et relations

```
User (credentials auth)
  ├─ 1:1  → Profile          (PII : nom, wilaya, avatar)
  ├─ N:M  → Role[]           (via user_roles)
  ├─ 1:1  → Store            (si BAILLEUR ou AGENCE)
  ├─ 1:N  → Booking[]        (en tant que locataire)
  ├─ 1:N  → Document[]       (KYC)
  └─ 1:N  → Review[]         (avis laissés)

Store (profil public d'un bailleur/agence)
  ├─ 1:1  ← User             (owner, UNIQUE)
  └─ 1:N  ← Vehicle[]

Vehicle
  ├─ N:1  → Store
  ├─ 1:N  ← VehiclePhoto[]   (triées par display_order)
  ├─ JSON : features          (équipements, caractéristiques)
  └─ @Version                 (verrouillage optimiste)

Booking  ← entité pivot centrale
  ├─ N:1  → Vehicle
  ├─ N:1  → User             (locataire)
  ├─ N:1  → Store
  ├─ 1:1  ← Payment
  └─ 1:N  ← Review[]         (max 2 : RENTER_TO_OWNER + OWNER_TO_RENTER)

Payment
  ├─ 1:1  ← Booking
  └─ JSON : metadata          (réponses gateway)

Document (KYC)
  └─ N:1  → User

Review
  ├─ N:1  → Booking
  ├─ N:1  → User             (auteur)
  └─ N:1  → User             (cible)
```

### Schéma MySQL (tables principales)

```sql
users          (id, email, phone, password_hash, status, created_at)
user_roles     (user_id FK, role_id FK)
roles          (id, name ENUM: LOCATAIRE/BAILLEUR/AGENCE/ADMIN)
profiles       (id, user_id FK UNIQUE, first_name, last_name, wilaya, avatar_url)
documents      (id, user_id FK, type, file_url, status: PENDING/VERIFIED/REJECTED)

stores         (id, owner_id FK UNIQUE, name, slug, description, address, city,
                lat, lng, phone, logo_url, status: PENDING/APPROVED/REJECTED/SUSPENDED,
                rating, created_at)

vehicles       (id, store_id FK, brand, model, year, category, seats, transmission,
                fuel_type, price_per_day, features JSON, status: AVAILABLE/UNAVAILABLE/RENTED,
                version, created_at)
vehicle_photos (id, vehicle_id FK, url, is_primary, display_order)

bookings       (id, vehicle_id FK, renter_id FK, store_id FK,
                start_date, end_date, total_days, total_price,
                status: PENDING/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED/DISPUTED,
                version, created_at, updated_at)
payments       (id, booking_id FK UNIQUE, amount, deposit_amount,
                method: CASH/CIB/EDAHABIA/STRIPE,
                payment_status, deposit_status: HELD/RELEASED/FORFEITED,
                metadata JSON)
reviews        (id, booking_id FK, author_id FK, target_id FK,
                type: RENTER_TO_OWNER/OWNER_TO_RENTER,
                rating TINYINT, comment TEXT, created_at)
```

---

## Machine à états — Booking

```
                    ┌──────────┐
                    │ PENDING  │  ← création par le client
                    └────┬─────┘
           agence confirme│         client ou agence annule
                    ┌────▼─────┐  ─────────────────────────→ CANCELLED
                    │CONFIRMED │
                    └────┬─────┘
           date début atteinte│
                    ┌────▼─────┐
                    │IN_PROGRESS│
                    └────┬─────┘
                         │  fin normale        litige
                    ┌────▼──────┐         ┌──────────┐
                    │ COMPLETED │         │ DISPUTED │
                    └───────────┘         └──────────┘
```

---

## Stockage fichiers — MinIO

- MySQL stocke **uniquement l'URL** de la photo/document
- Le fichier binaire est dans **MinIO** (compatible API AWS S3)
- Buckets : `vehicles-photos`, `store-logos`, `kyc-documents`
- Intégration : AWS SDK v2 via `MinioService`
- Ne jamais stocker de binaires en BLOB MySQL

---

## Sécurité — JWT

- **Access token** : durée courte (15 min), transporté en `Authorization: Bearer`
- **Refresh token** : durée longue (7 jours), stocké en HttpOnly cookie
- **Logout** : blacklist du token dans Redis (clé = `blacklist:{jti}`, TTL = expiration restante)
- **Rôles** : vérifiés via `@PreAuthorize` sur chaque endpoint
- **BCrypt** : hashage des mots de passe (encoder configurable)

---

## APIs — Endpoints par module

### Auth
```
POST   /api/v1/auth/register         Public
POST   /api/v1/auth/login            Public
POST   /api/v1/auth/refresh          Public (cookie)
POST   /api/v1/auth/logout           Authentifié
```

### User / Profile
```
GET    /api/v1/users/me              Authentifié
PATCH  /api/v1/users/me/profile      Authentifié
PUT    /api/v1/users/me/password     Authentifié
DELETE /api/v1/users/me             Authentifié
POST   /api/v1/users/me/verify-email Authentifié
POST   /api/v1/users/me/verify-phone Authentifié
```

### Admin — Users
```
GET    /api/v1/admin/users           ADMIN
GET    /api/v1/admin/users/{id}      ADMIN
PATCH  /api/v1/admin/users/{id}/status ADMIN
POST   /api/v1/admin/users/{id}/roles  ADMIN
DELETE /api/v1/admin/users/{id}/roles/{role} ADMIN
```

### Store *(à implémenter)*
```
POST   /api/v1/stores                BAILLEUR/AGENCE
GET    /api/v1/stores/{id}           Public
PATCH  /api/v1/stores/me             BAILLEUR/AGENCE
PATCH  /api/v1/admin/stores/{id}/status  ADMIN
```

### Vehicle *(à implémenter)*
```
POST   /api/v1/vehicles              BAILLEUR/AGENCE
GET    /api/v1/vehicles              Public (paginé, filtrable)
GET    /api/v1/vehicles/{id}         Public
PATCH  /api/v1/vehicles/{id}         BAILLEUR/AGENCE (owner)
DELETE /api/v1/vehicles/{id}         BAILLEUR/AGENCE (owner)
POST   /api/v1/vehicles/{id}/photos  BAILLEUR/AGENCE (owner)
```

### Booking *(à implémenter)*
```
POST   /api/v1/bookings              LOCATAIRE
GET    /api/v1/bookings/me           LOCATAIRE
GET    /api/v1/stores/me/bookings    BAILLEUR/AGENCE
PATCH  /api/v1/bookings/{id}/confirm BAILLEUR/AGENCE
PATCH  /api/v1/bookings/{id}/cancel  LOCATAIRE/BAILLEUR/AGENCE
PATCH  /api/v1/bookings/{id}/complete BAILLEUR/AGENCE
```

### Search *(à implémenter)*
```
GET    /api/v1/search/vehicles       Public (filtres + géoloc)
GET    /api/v1/search/compare        Public (comparateur de prix)
```

---

## Infrastructure

### Développement (Docker Compose)
```yaml
services:
  mysql:   port 3306  (données principales)
  redis:   port 6379  (sessions + cache + JWT blacklist)
  minio:   port 9000  (stockage fichiers)
```

### Production (Phase 4)
- **Hébergeur :** Hetzner Cloud VPS CX32 (4 vCPU, 8 GB RAM) — ~€12.5/mois
- **Reverse proxy :** Nginx (SSL Let's Encrypt, gzip)
- **CI/CD :** GitHub Actions → build → test → deploy
- **Coût total estimé :** ~€15/mois (Phase 1-2)

---

## Variables d'environnement

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/rentdrive
DB_USERNAME=
DB_PASSWORD=

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=
JWT_ACCESS_EXPIRATION=900000      # 15 min en ms
JWT_REFRESH_EXPIRATION=604800000  # 7 jours en ms

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=
MINIO_SECRET_KEY=
MINIO_BUCKET_VEHICLES=vehicles-photos
MINIO_BUCKET_STORES=store-logos
MINIO_BUCKET_DOCS=kyc-documents

# Google Maps
GOOGLE_MAPS_API_KEY=

# Email (SMTP)
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

# Firebase (Phase 3)
FIREBASE_SERVER_KEY=
```

---

## Conventions de nommage

| Élément | Convention | Exemple |
|---------|-----------|---------|
| Packages Java | `com.rentdrive.{module}.{couche}` | `com.rentdrive.vehicle.service` |
| Tables MySQL | `snake_case` pluriel | `vehicle_photos` |
| Endpoints REST | `kebab-case` | `/api/v1/vehicle-photos` |
| Branches Git | `feature/`, `fix/`, `release/` | `feature/auth-jwt` |
| Repos | `kebab-case` | `rentdrive-web`, `rentdrive-mobile` |

---

## Décisions d'architecture (ADR)

| Date | Décision | Raison |
|------|----------|--------|
| 2026-04-19 | Monolithe d'abord, microservices en Phase 4 | MVP plus rapide, découpage propre possible grâce aux modules internes |
| 2026-04-19 | MinIO pour les fichiers (pas de BLOB MySQL) | Scalabilité, séparation des responsabilités |
| 2026-04-19 | User/Profile séparés | Conformité GDPR |
| 2026-04-19 | Mapper manuel (pas MapStruct) | Évite `LazyInitializationException`, contrôle total |
