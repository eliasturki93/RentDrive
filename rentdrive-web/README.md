# rentdrive-web

Frontend Angular 17 de la plateforme RentDrive.

## Setup

```bash
npm install
ng serve        # dev server sur http://localhost:4200
ng build        # production build
ng test         # tests unitaires
```

## Structure

```
src/
├── app/
│   ├── core/          # Auth, guards, interceptors, services partagés
│   ├── shared/        # Composants, pipes, directives réutilisables
│   ├── features/
│   │   ├── auth/      # Login, register
│   │   ├── client/    # Recherche, réservations (LOCATAIRE)
│   │   ├── agency/    # Dashboard agence, véhicules, réservations
│   │   └── admin/     # Gestion utilisateurs, agences, validation KYC
│   └── app.routes.ts
└── environments/
```
