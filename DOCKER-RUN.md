# Déploiement Docker

## Développement

```bash
make up            # docker compose up --build
```

Démarre PostgreSQL, MinIO (stockage d'images compatible S3, bucket
`ecoswitch-media` créé et rendu public automatiquement), l'API et l'IHM.

## Production

```bash
cp .env.example .env     # renseigner les secrets
make prod-up             # docker compose -f docker-compose.prod.yml up -d --build
```

`docker-compose.prod.yml` démarre PostgreSQL, l'API et l'IHM. Les images sont
stockées dans un **bucket OVH Object Storage** (compatible S3), qui doit être en
lecture publique : le navigateur charge les images directement.

Variables **obligatoires** (le conteneur refuse de démarrer sans) :
`POSTGRES_*`, `BETTER_AUTH_SECRET`, `BETTER_AUTH_URL`, `S3_ENDPOINT`,
`S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_PUBLIC_BASE_URL`,
`CORS_ALLOWED_ORIGINS`.

L'API applique ses migrations au démarrage (`prisma migrate deploy`). Elles sont
additives : une base existante n'est jamais vidée.

Empreinte mémoire : 256 Mo pour l'API (contre 512 Mo et un réglage fin de la JVM
pour l'ancienne API Java), 256 Mo pour PostgreSQL, 128 Mo pour Nginx.

Pour basculer une base de production issue de l'API Java, suivre
[docs/MIGRATION.md](./docs/MIGRATION.md).
