# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start infrastructure (MySQL + Redis)
docker-compose up -d

# Run application
./mvnw spring-boot:run

# Build
./mvnw clean package

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=UserServiceTest

# Run a single test method
./mvnw test -Dtest=UserServiceTest#methodName
```

> Windows: use `mvnw.cmd` instead of `./mvnw`

## Tech Stack

- **Java 21**, Spring Boot 4.0.5
- **Spring Data JPA** + Hibernate (ORM), Flyway (migrations — currently `enabled: flase` typo in `application.yml`, must fix to `false` or `true`)
- **MySQL** (primary store), **Redis** (Spring Session)
- **SpringDoc OpenAPI**, Lombok

## Architecture

Layered Spring Boot monolith targeting an Algerian car rental marketplace (comments and messages are in French).

```
controller/       → REST endpoints, request validation, HTTP responses
service/          → interfaces
service/impl/     → business logic, @Transactional boundaries
repository/       → JPA repositories with custom JPQL + @EntityGraph
entity/           → JPA entities (relationships, constraints)
dto/request/      → inbound request bodies
dto/response/     → outbound responses (ApiResponse<T> wrapper, PageResponse<T>)
mapper/           → manual entity→DTO conversion (avoids LazyInitializationException)
exception/        → custom exception hierarchy + GlobalExceptionHandler
enums/            → domain enums (roles, statuses, payment/vehicle attributes)
converter/        → Hibernate AttributeConverters (e.g. JSON map for vehicle features)
validator/        → custom constraint validators
```

## Domain Model

**Core entities and their relationships:**

- **User** (auth credentials) — 1:1 Profile (PII), N:M Roles, 1:1 Store (owner), 1:N Bookings (as renter), 1:N Documents (KYC), 1:N Reviews
- **Store** — owned by one User, contains N Vehicles
- **Vehicle** — belongs to Store, 1:N VehiclePhoto (ordered by index), JSON `features` field via `JsonMapConverter`, `@Version` for optimistic locking
- **Booking** — pivot entity linking Vehicle + renter User + Store; 1:1 Payment; 1:N Reviews (max 2: RENTER_TO_OWNER, OWNER_TO_RENTER). State machine: `PENDING → CONFIRMED → IN_PROGRESS → COMPLETED`, can transition to `CANCELLED` or `DISPUTED`
- **Payment** — one per Booking; deposit lifecycle: `HELD → RELEASED | FORFEITED`; `metadata` JSON field for gateway responses
- **Document** — KYC per User; `PENDING → VERIFIED | REJECTED`; required for `AGENCE` role
- **Role** — `LOCATAIRE` (renter), `BAILLEUR` (private landlord), `AGENCE` (agency), `ADMIN`

User/Profile are split intentionally for GDPR compliance — do not merge them.

## Key Conventions

**JPA / Performance**
- Default `FetchType.LAZY` everywhere; use `@EntityGraph` in repository queries to selectively load associations.
- `@Transactional(readOnly = true)` on all read-only service methods.
- `@Modifying` bulk JPQL updates for password/email/status changes instead of loading entities.
- Composite index `(vehicle_id, start_date, end_date)` exists for booking overlap detection.

**DTOs & Mapping**
- Never return entities directly from controllers — always map through `*Mapper` classes.
- `ApiResponse<T>` wraps all responses. `PageResponse<T>` wraps paginated lists.
- PATCH endpoints use null-checking in service layer to apply only provided fields.

**Exceptions**
- Extend `KarrentException` for domain errors. Subtypes: `ConflictException`, `ForbiddenException`, `ResourceNotFoundException`, `ValidationException`.
- `GlobalExceptionHandler` translates these to HTTP responses automatically.

## Current Implementation Status

**Done:** User registration, Profile CRUD, password change, email/phone verification endpoints, role assignment/revocation, user search with pagination, status management (ACTIVE/SUSPENDED/BANNED), full exception handling, all 10 domain entities + Flyway schema.

**Not yet implemented:** Login/JWT, RabbitMQ event publishing (marked TODO), email/OTP delivery, Store management, Vehicle catalog, Booking workflow, Payment gateway (CIB/Edahabia), Review system, KYC validation, Elasticsearch upgrade for full-text search.

## API Base Path

All endpoints are prefixed `/api/v1`. Current groups: `/auth`, `/users/me`, `/admin/users`.

## Repositories

| Repo | Stack | URL |
|------|-------|-----|
| `RentDrive` | Spring Boot (Java 21) | https://github.com/eliasturki93/RentDrive |
| `rentdrive-web` | Angular 17+ | https://github.com/eliasturki93/rentdrive-web |
| `rentdrive-mobile` | Flutter (Dart) | https://github.com/eliasturki93/rentdrive-mobile |

## Architecture Reference

Full architecture details (domain model, API endpoints, ADR decisions, infrastructure, env vars) are in [`ARCHITECTURE.md`](./ARCHITECTURE.md). **Always update ARCHITECTURE.md when making architectural decisions.**


### 1. Plan Mode Default
- Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions)
- If something goes sideways, STOP and re-plan immediately — don't keep pushing
- Use plan mode for verification steps, not just building
- Write detailed specs upfront to reduce ambiguity

### 2. Subagent Strategy
- Use subagents liberally to keep main context window clean
- Offload research, exploration, and parallel analysis to subagents
- For complex problems, throw more compute at it via subagents
- One task per subagent for focused execution

### 3. Self-Improvement Loop
- After ANY correction from the user: update `tasks/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until mistake rate drops
- Review lessons at session start for relevant project

### 4. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness

### 5. Demand Elegance (Balanced)
- For non-trivial changes: pause and ask "is there a more elegant way?"
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution"
- Skip this for simple, obvious fixes — don't over-engineer
- Challenge your own work before presenting it

### 6. Autonomous Bug Fixing
- When given a bug report: just fix it. Don't ask for hand-holding
- Point at logs, errors, failing tests — then resolve them
- Zero context switching required from the user
- Go fix failing CI tests without being told how

### 7. Security by Default
- Never hardcode secrets, API keys, passwords, or tokens in source code — use environment variables
- Always use parameterized queries — never concatenate user input into SQL/NoSQL queries
- Hash passwords with bcrypt, argon2, or scrypt — never MD5 or SHA1
- Sanitize and validate ALL user input at system boundaries
- Escape output to prevent XSS (no raw innerHTML, dangerouslySetInnerHTML, v-html without sanitization)
- Set security headers: HSTS, X-Frame-Options, X-Content-Type-Options, Content-Security-Policy
- Configure CORS restrictively — never use wildcard `*` in production
- Implement rate limiting on authentication endpoints
- Use CSRF tokens for state-changing operations
- Never expose stack traces, debug info, or verbose errors in production
- Ensure .gitignore covers .env, private keys, credentials, and logs
- Prefer HTTPS everywhere — redirect HTTP to HTTPS
- Apply principle of least privilege on all access controls

## Task Management

1. **Plan First**: Write plan to `tasks/todo.md` with checkable items
2. **Verify Plan**: Check in before starting implementation
3. **Track Progress**: Mark items complete as you go
4. **Explain Changes**: High-level summary at each step
5. **Document Results**: Add review section to `tasks/todo.md`
6. **Capture Lessons**: Update `tasks/lessons.md` after corrections

## Core Principles

- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Secure by Design**: Security is not a phase — it's built into every line of code.

# Prompt pour Claude Code — Plateforme de location de voiture

Tu vas analyser la description complète ci-dessous d'un projet full-stack et générer deux fichiers :
- `ARCHITECTURE.md` — documentation technique complète de l'architecture
- `CLAUDE.md` — fichier de contexte et conventions pour guider Claude Code sur ce projet

---

## Description du projet

### Nom du projet
**CarRent Platform** — Plateforme SaaS de location de voiture multi-agences

### Objectif
Application web et mobile permettant à des agences de location de voiture de présenter leur catalogue en ligne, et à des clients de rechercher, comparer et réserver un véhicule. Un administrateur supervise et valide les agences.

---

## Profils utilisateurs

### 1. Client
- Consulter les agences de location autour de lui (géolocalisation) ou par filtre (ville, région)
- Filtrer les voitures par catégorie, prix, disponibilité, transmission, carburant
- Comparer les prix entre plusieurs agences pour un même créneau
- Réserver une voiture avec sélection des dates
- Recevoir des notifications (confirmation, rappel, statut)
- Accès via web (Angular) ET application mobile (Flutter)

### 2. Agence de location
- Créer et gérer son store (profil public) : nom, description, adresse, coordonnées GPS, logo, photos, horaires
- Ajouter / modifier / supprimer ses voitures : marque, modèle, année, catégorie, prix/jour, photos multiples, disponibilité, caractéristiques
- Gérer les réservations reçues (accepter, refuser, marquer comme terminée)
- Tableau de bord : statistiques de visites, réservations, revenus
- Accès via web uniquement (Angular)

### 3. Administrateur (Admin plateforme)
- Valider ou rejeter les inscriptions d'agences
- Suspendre / réactiver une agence
- Vue globale sur toutes les agences, voitures, réservations
- Gestion des utilisateurs
- Accès via web uniquement (Angular)

---

## Stack technique

### Backend
- **Langage** : Java 21+
- **Framework** : Spring Boot 4.X
- **Architecture** : Microservices (un service Spring Boot par domaine métier)
- **API** : REST (JSON), documentation Swagger/OpenAPI
- **Sécurité** : Spring Security + JWT (access token + refresh token)
- **Gateway** : Spring Cloud Gateway (point d'entrée unique, routing, rate limiting, CORS)
- **ORM** : Spring Data JPA + Hibernate
- **Build** : Maven (projet multi-modules)

### Frontend Web
- **Framework** : Angular 17+ (standalone components)
- **UI** : Angular Material ou TailwindCSS
- **State management** : NgRx ou services + RxJS
- **Maps** : Google Maps JavaScript API
- **Auth** : JWT stocké en HttpOnly cookie ou localStorage

### Application Mobile
- **Framework** : Flutter (Dart)
- **Cible** : iOS + Android
- **Profil ciblé** : Client uniquement (pas d'interface agence ni admin sur mobile)
- **Maps** : Google Maps Flutter plugin
- **Notifications** : Firebase Cloud Messaging (FCM)

### Base de données principale
- **SGBD** : MySQL 8.x
- **Connexion** : Spring Data JPA, HikariCP connection pool
- **Migrations** : Flyway

### Stockage fichiers (photos)
- **Solution** : MinIO (self-hosted, compatible API AWS S3)
- **Utilisation** : stockage des photos de voitures, logos agences, documents
- **Intégration Spring Boot** : AWS SDK v2 (compatible MinIO)
- **Principe** : MySQL stocke uniquement l'URL de la photo, le fichier binaire est dans MinIO

### Cache & sessions
- **Solution** : Redis
- **Utilisation** : cache des résultats de recherche, sessions JWT blacklist (logout), rate limiting

### Moteur de recherche
- **Solution Phase 1** : MySQL FULLTEXT (suffisant pour démarrer)
- **Solution Phase 2+** : Elasticsearch (migration quand les volumes nécessitent de meilleures performances)

### Notifications
- **Email transactionnel** : SMTP (Brevo / Mailgun)
- **Push mobile** : Firebase Cloud Messaging (FCM)

### Services externes
- **Géolocalisation** : Google Maps API (Places, Geocoding, Distance Matrix)
- **Paiement** : Stripe (prévu Phase 2, non implémenté en Phase 1)

---

## Architecture microservices (Backend)

### 5 services Spring Boot indépendants

#### 1. `auth-service` (port 8081)
Responsabilités :
- Inscription / connexion des 3 profils (CLIENT, AGENCY, ADMIN)
- Génération et validation des JWT (access + refresh tokens)
- Gestion des rôles et permissions
- Logout (blacklist token dans Redis)

Entités principales : `User`, `Role`, `RefreshToken`

#### 2. `agency-service` (port 8082)
Responsabilités :
- CRUD agence (store public)
- Workflow validation admin (PENDING → APPROVED / REJECTED)
- Gestion du profil agence : infos, coordonnées GPS, photos, horaires
- Upload logo/photos vers MinIO

Entités principales : `Agency`, `AgencyStatus` (PENDING, APPROVED, REJECTED, SUSPENDED)

#### 3. `car-service` (port 8083)
Responsabilités :
- CRUD véhicule par agence
- Upload photos multiples vers MinIO
- Gestion disponibilité (disponible / indisponible / en location)
- Catalogue public : liste et détail d'un véhicule

Entités principales : `Car`, `CarPhoto`, `CarCategory` (ECONOMY, SUV, LUXURY, VAN...), `CarStatus`

#### 4. `booking-service` (port 8084)
Responsabilités :
- Création réservation (client choisit voiture + dates)
- Vérification disponibilité (pas de double réservation)
- Workflow réservation : PENDING → CONFIRMED → ACTIVE → COMPLETED / CANCELLED
- Historique réservations (client et agence)
- Calcul prix total (prix/jour × nombre de jours)

Entités principales : `Booking`, `BookingStatus`

#### 5. `search-service` (port 8085)
Responsabilités :
- Recherche agences par géolocalisation (lat/lng + rayon en km)
- Recherche et filtrage voitures (ville, catégorie, prix min/max, dates dispo)
- Comparateur de prix : même type de voiture sur une même période entre plusieurs agences
- Intégration Google Maps (geocoding, calcul distances)
- Cache Redis sur les résultats de recherche populaires

---

## API Gateway

- **Port** : 8080 (point d'entrée unique de tous les clients)
- **Routing** : `/api/auth/**` → auth-service, `/api/agencies/**` → agency-service, etc.
- **Sécurité** : validation JWT avant de transmettre la requête
- **CORS** : configuré pour Angular (localhost:4200 en dev, domaine prod)
- **Rate limiting** : via Redis (par IP et par utilisateur)

---

## Schéma de base de données MySQL (tables principales)

```sql
-- Utilisateurs
users (id, email, password_hash, role ENUM('CLIENT','AGENCY','ADMIN'), status, created_at)

-- Agences
agencies (id, owner_id FK users, name, slug, description, address, city, country,
          lat, lng, phone, email, logo_url, status ENUM('PENDING','APPROVED','REJECTED','SUSPENDED'),
          created_at, updated_at)

-- Voitures
cars (id, agency_id FK agencies, brand, model, year, category, seats, transmission,
      fuel_type, price_per_day, description, status ENUM('AVAILABLE','UNAVAILABLE','RENTED'),
      created_at)

-- Photos voitures (URL stockée, fichier dans MinIO)
car_photos (id, car_id FK cars, url, is_primary, display_order)

-- Réservations
bookings (id, client_id FK users, car_id FK cars, agency_id FK agencies,
          start_date, end_date, total_days, total_price,
          status ENUM('PENDING','CONFIRMED','ACTIVE','COMPLETED','CANCELLED'),
          created_at, updated_at)

-- Avis (Phase 2)
reviews (id, client_id FK users, agency_id FK agencies, booking_id FK bookings,
         rating TINYINT, comment TEXT, created_at)
```

---

## Infrastructure & hébergement

### Environnement de développement
- Docker Compose : MySQL, Redis, MinIO, tous les services Spring Boot
- Chaque service a son propre `Dockerfile`
- Variables d'environnement via `.env` (jamais committées)

### Production (Phase 1-2)
- **Hébergeur** : Hetzner Cloud (Europe, meilleur rapport qualité/prix)
- **Serveur** : VPS CX32 (4 vCPU, 8 GB RAM, 80 GB SSD) — ~€12.5/mois
- **Déploiement** : Docker Compose sur VPS
- **Reverse proxy** : Nginx (SSL/TLS Let's Encrypt, compression gzip)
- **CI/CD** : GitHub Actions (build → test → deploy)

### Coût estimé production
| Poste | Coût |
|---|---|
| VPS Hetzner CX32 | €12.5/mois |
| Domaine + SSL (Let's Encrypt) | €1/mois |
| Emails transactionnels (Brevo free) | €0/mois |
| Photos MinIO (inclus VPS) | €0/mois |
| **Total Phase 1-2** | **~€15/mois** |

---

## Phases de développement

### Phase 1 — Fondations (4-6 semaines)
- Setup projet Maven multi-modules + Docker Compose
- Auth Service complet (JWT, 3 rôles)
- Agency Service + workflow validation admin
- Car Service (CRUD + upload photos MinIO)
- Angular : interfaces admin (validation agences) + agence (gestion store & voitures)
- Schéma MySQL + migrations Flyway

### Phase 2 — Recherche & Réservation (4-6 semaines)
- Search Service (filtres, géolocalisation Google Maps)
- Comparateur de prix entre agences
- Booking Service (workflow réservation complet)
- Angular : interface client (recherche, comparaison, réservation)
- Redis (cache recherches, sessions)

### Phase 3 — Mobile & Notifications (4-6 semaines)
- Application Flutter (client mobile)
- Notifications push FCM + emails transactionnels
- Calendrier de disponibilité voitures
- Elasticsearch (remplacement MySQL FULLTEXT si besoin)

### Phase 4 — Finalisation (3-4 semaines)
- Stripe (paiement en ligne)
- Dashboard analytique agence
- Système d'avis et notations
- Optimisation performances
- CI/CD complet + déploiement production

---

## Conventions de développement

### Nommage
- Services Spring Boot : `kebab-case` (ex: `auth-service`, `car-service`)
- Packages Java : `com.carrent.{service}.{layer}` (ex: `com.carrent.car.controller`)
- Tables MySQL : `snake_case` pluriel (ex: `car_photos`, `booking_status`)
- Endpoints REST : `/api/{resource}/{id}` en `kebab-case`
- Branches Git : `feature/`, `fix/`, `release/`

### Structure d'un service Spring Boot
```
{service-name}/
├── src/main/java/com/carrent/{service}/
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic
│   ├── repository/      # JPA repositories
│   ├── entity/          # JPA entities
│   ├── dto/             # Request / Response DTOs
│   ├── mapper/          # Entity ↔ DTO (MapStruct)
│   ├── exception/       # Custom exceptions + handler
│   ├── config/          # Spring config (Security, CORS, etc.)
│   └── {ServiceName}Application.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/    # Flyway scripts (V1__init.sql...)
└── Dockerfile
```

### Règles importantes
- Jamais de logique métier dans les controllers (déléguer aux services)
- Toujours valider les DTOs avec `@Valid` + Bean Validation
- Toujours retourner des DTOs, jamais les entités JPA directement
- Les URLs des photos sont relatives (préfixe MinIO ajouté côté service)
- Elasticsearch est optionnel en Phase 1 : prévoir l'interface mais implémenter avec MySQL FULLTEXT

---

## Instruction pour Claude Code

À partir de tout ce contexte, génère :

### 1. `ARCHITECTURE.md`
Document de référence technique complet incluant :
- Vue d'ensemble de l'architecture (diagramme ASCII)
- Description de chaque service et ses responsabilités
- Schéma de la base de données avec toutes les tables
- Flux de données pour les cas d'usage principaux (inscription agence, recherche voiture, réservation)
- Stack technique complète avec versions
- Infrastructure et déploiement
- Variables d'environnement nécessaires par service

### 2. `CLAUDE.md`
Fichier de contexte pour Claude Code incluant :
- Description courte du projet (3-5 lignes)
- Stack et versions exactes utilisées
- Structure des modules Maven
- Conventions de code à respecter impérativement
- Commandes utiles (build, test, run, docker)
- Points d'attention spécifiques à ce projet (ex: toujours utiliser MinIO et jamais BLOB, Elasticsearch optionnel Phase 1)
- Entités critiques et leurs relations
- Endpoints API principaux par service
