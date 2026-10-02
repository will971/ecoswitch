# Déploiement Docker

## Développement

```bash
make up            # docker compose up --build
```

Démarre PostgreSQL, MinIO (stockage d'images compatible S3, bucket
`ecoswitch-media` créé et rendu public automatiquement), l'API et l'IHM. Les
images sont servies sous http://localhost:3000/media/, comme en production.

## Production

```bash
cp .env.example .env     # renseigner les secrets
make prod-up             # docker compose -f docker-compose.prod.yml up -d --build
```

`docker-compose.prod.yml` démarre PostgreSQL, **MinIO**, l'API et l'IHM.

Les images sont stockées dans MinIO (compatible S3), dans le volume
`minio_data`. MinIO n'est pas exposé : Nginx sert les images sous
`https://<site>/media/`, en lecture seule, avec une CSP qui neutralise le script
éventuel d'un SVG. Le bucket est créé au premier démarrage par `minio-init`.

**Variante OVH** : renseigner les variables `S3_*` dans `.env` (cf.
`.env.example`) ; MinIO tourne alors à vide et peut être retiré du fichier.

Variables **obligatoires** (le conteneur refuse de démarrer sans) :
`POSTGRES_*`, `BETTER_AUTH_SECRET`, `BETTER_AUTH_URL`, `MINIO_ROOT_USER`,
`MINIO_ROOT_PASSWORD` (8 caractères minimum), `S3_PUBLIC_BASE_URL`,
`CORS_ALLOWED_ORIGINS`.

**Sauvegardes** : il y a désormais deux volumes à sauvegarder, `postgres_data`
et `minio_data`.

L'API applique ses migrations au démarrage (`prisma migrate deploy`). Elles sont
additives : une base existante n'est jamais vidée.

Empreinte mémoire : 256 Mo pour l'API (contre 512 Mo et un réglage fin de la JVM
pour l'ancienne API Java), 256 Mo pour PostgreSQL, 256 Mo pour MinIO, 128 Mo
pour Nginx.

Pour basculer une base de production issue de l'API Java, suivre
[docs/MIGRATION.md](./docs/MIGRATION.md).
