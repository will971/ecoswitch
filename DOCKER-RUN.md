# Déploiement Docker

## Développement

```bash
make up            # docker compose up --build
```

Démarre PostgreSQL, RustFS (stockage d'images compatible S3), l'API et l'IHM.
L'API crée le bucket `ecoswitch-media` et l'ouvre en lecture au démarrage. Les
images sont servies sous http://localhost:3000/media/, comme en production.

Si 3000 ou 8080 sont déjà pris : `IHM_PORT=3300 API_PORT=8180 make up`.

## Production

```bash
cp .env.example .env     # renseigner les secrets
make prod-up             # docker compose -f docker-compose.prod.yml up -d --build
```

`docker-compose.prod.yml` démarre PostgreSQL, **RustFS**, l'API et l'IHM.

Les images sont stockées dans RustFS (compatible S3), dans le volume `s3_data`.
RustFS remplace MinIO, dont les images Docker (`minio/minio`, `minio/mc`) ne
sont plus distribuées. Il n'est pas exposé : Nginx sert les images sous
`https://<site>/media/`, en lecture seule, avec une CSP qui neutralise le script
éventuel d'un SVG. L'API crée le bucket et sa lecture publique au démarrage
(`S3_AUTO_CREATE_BUCKET`) ; seule la lecture d'objet est ouverte, ni l'écriture
ni le listing.

**Variante OVH** : renseigner `S3_ENDPOINT`, `S3_BUCKET`, etc. et
`S3_AUTO_CREATE_BUCKET=false` dans `.env` (cf. `.env.example`) ; le service `s3`
peut alors être retiré du fichier.

Variables **obligatoires** (le conteneur refuse de démarrer sans) :
`POSTGRES_*`, `BETTER_AUTH_SECRET`, `BETTER_AUTH_URL`, `S3_ACCESS_KEY`,
`S3_SECRET_KEY`, `S3_PUBLIC_BASE_URL`, `CORS_ALLOWED_ORIGINS`.

**Sauvegardes** : deux volumes, `postgres_data` et `s3_data`.

L'API applique ses migrations au démarrage (`prisma migrate deploy`). Elles sont
additives : une base existante n'est jamais vidée.

Empreinte mémoire mesurée : ~100 Mo pour l'API, ~65 Mo pour RustFS. Limites
fixées : 256 Mo pour l'API (contre 512 Mo et un réglage fin de la JVM
pour l'ancienne API Java), 256 Mo pour PostgreSQL, 256 Mo pour RustFS, 128 Mo
pour Nginx.

Pour basculer une base de production issue de l'API Java, suivre
[docs/MIGRATION.md](./docs/MIGRATION.md).
