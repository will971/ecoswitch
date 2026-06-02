# EcoSwitch - lancement Docker

## Prerequis
- Docker Desktop installe (ou moteur Docker + plugin Compose)

## Demarrage
Depuis la racine du workspace (`DEV`) :

```bash
docker compose up --build
```

## Acces
- IHM: http://localhost:3000
- API directe: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

L'IHM appelle l'API via le proxy nginx sur le chemin `/api/*`.

## Arret
```bash
docker compose down
```
