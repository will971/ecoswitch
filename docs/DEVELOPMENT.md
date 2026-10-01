# EcoSwitch — Développement

## Prérequis

- **Docker Desktop** (PostgreSQL et MinIO tournent en conteneurs)
- **Node 22+**
- **Python 3** (seeder du catalogue)

Java n'est plus nécessaire.

## Trois façons de lancer le projet

### 1. Depuis Conductor (recommandé pour travailler en parallèle)

Le setup du workspace installe tout, et le script **dev** lance l'API et l'IHM
sur des ports propres au workspace. Base et images sont partagées entre
workspaces. Détails : [.conductor/README.md](../.conductor/README.md).

### 2. Tout en conteneurs

```bash
make up
```

### 3. API et IHM en local, avec rechargement à chaud

Il faut une base et un stockage d'images. Le plus simple est de démarrer ceux de
`make up`, puis d'arrêter les conteneurs `api` et `ihm` :

```bash
docker compose up -d postgres minio minio-init
```

Puis, dans `ecoswitch-api/`, créer un `.env` :

```bash
DATABASE_URL="postgresql://ecoswitch:ecoswitch@127.0.0.1:5432/ecoswitch?schema=public"
BETTER_AUTH_SECRET="dev-only-secret-change-me-0123456789abcdef"
ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS="moi@exemple.fr"
S3_ENDPOINT="http://127.0.0.1:9000"
S3_REGION="us-east-1"
S3_BUCKET="ecoswitch-media"
S3_ACCESS_KEY="minioadmin"
S3_SECRET_KEY="minioadmin"
S3_PUBLIC_BASE_URL="http://127.0.0.1:9000/ecoswitch-media"
```

(`docker-compose.yml` n'expose pas Postgres sur l'hôte : ajouter
`ports: ["5432:5432"]` au service `postgres` pour ce mode.)

```bash
make migrate     # applique le schéma
make dev-api     # http://localhost:8080
make dev-ihm     # http://localhost:5173 — proxifie /api vers 8080
```

Le proxy de Vite vise `http://127.0.0.1:8080`, ou `VITE_DEV_API_TARGET` s'il est défini.

## Peupler le catalogue

Les écritures du catalogue exigent un compte **ADMIN**. Inscris-toi (depuis
l'interface ou `POST /api/v1/auth/register`) avec un email présent dans
`ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS`, puis :

```bash
python3 scripts/seed_catalog.py --url http://localhost:8080 \
  --email moi@exemple.fr --password '...'
```

⚠️ Le seeder télécharge les photos des modèles depuis Wikimedia et **abandonne
tout modèle dont il n'obtient pas d'image**. Selon la disponibilité du réseau, il
n'injecte qu'une partie des 20 marques et 61 modèles de son jeu de données.

## Tests

```bash
make test        # API (Vitest) + seeder (Python)
```

- **Vitest** — 69 tests, dont la parité avec l'ancien moteur de calcul Java sur
  4 422 vecteurs de référence (`ecoswitch-api/test/golden/`), sans base de données.
- **Test de bout en bout** — `ecoswitch-api/test/e2e/smoke.sh` joue ~50
  scénarios contre une API vivante (authentification, failles refermées,
  simulations, garage, catalogue, comparaisons) :

  ```bash
  API=http://localhost:8080 ADMIN_EMAIL=moi@exemple.fr ADMIN_PASSWORD=... \
    bash ecoswitch-api/test/e2e/smoke.sh
  ```

- **Playwright** (IHM) — `cd ecoswitch-ihm && npm run test:e2e`.

## Schéma de base de données

```bash
cd ecoswitch-api
npx prisma migrate dev --name <description>   # créer une migration
npx prisma studio                              # explorer les données
```

⚠️ Relire toute migration générée avant de l'appliquer : sur ce projet, le diff
automatique de Prisma proposait de supprimer et recréer la colonne des images,
ce qui les aurait toutes détruites. Voir `prisma/migrations/20260930120000_media_oid_to_bytea/`.
