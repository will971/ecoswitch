# EcoSwitch

Outil d'aide à la décision financière et écologique : il compare le coût de
détention d'un véhicule actuel avec celui d'un véhicule cible (électrique,
hybride ou thermique récent) et calcule le seuil de rentabilité du passage.

Monorepo :

- **`ecoswitch-api/`** — API REST **NestJS 11 + Prisma 6 + better-auth**, PostgreSQL
- **`ecoswitch-ihm/`** — interface web **Vue 3 + Vite**, servie par Nginx

> L'API a été réécrite depuis Spring Boot 4 / Java 26. La procédure de bascule
> d'une base existante est décrite dans [docs/MIGRATION.md](./docs/MIGRATION.md).

## Démarrage rapide

```bash
make up
```

| Service | URL |
|---|---|
| Interface web | http://localhost:3000 |
| API | http://localhost:8080 (`/health`) |
| Console MinIO (images) | http://localhost:9001 — `minioadmin` / `minioadmin` |

Le catalogue est vide au premier lancement. Pour le peupler, il faut un compte
**ADMIN** : inscris-toi depuis l'interface avec un email listé dans
`ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS`, puis :

```bash
ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS=moi@exemple.fr make up
make seed-local ECOSWITCH_ADMIN_EMAIL=moi@exemple.fr ECOSWITCH_ADMIN_PASSWORD=...
```

Depuis **Conductor**, tout est automatisé — voir [.conductor/README.md](./.conductor/README.md).

## Documentation

- 📘 [Fonctionnelle](./docs/FUNCTIONAL.md) — règles métier, formules, aides de l'État
- 📗 [Technique](./docs/TECHNICAL.md) — architecture, sécurité, stockage, tests
- 📙 [Développement](./docs/DEVELOPMENT.md) — installation, commandes, tests
- 📕 [Migration](./docs/MIGRATION.md) — bascule depuis l'ancienne API Java

## Commandes

| Commande | Action |
|---|---|
| `make up` / `make down` | Pile complète en conteneurs (base, MinIO, API, IHM) |
| `make dev-api` / `make dev-ihm` | API ou IHM en local, avec rechargement à chaud |
| `make test` | Tests de l'API (Vitest) et du seeder (Python) |
| `make migrate` | Applique les migrations Prisma |
| `make seed-local` | Peuple le catalogue (compte ADMIN requis) |
| `make prod-up` | Production, à partir de `.env` (cf. `.env.example`) |
