# EcoSwitch Monorepo

Monorepo du projet EcoSwitch avec :
- une API backend Spring Boot
- une IHM frontend Vue 3 / Vite

## Structure

```text
DEV/
  ecoswitch-api/      # backend Spring Boot
  ecoswitch-ihm/      # frontend Vue + Vite
  docker-compose.yml  # lancement complet conteneurise
  Makefile            # commandes unifiees
  DOCKER-RUN.md       # guide docker detaille
```

> `springboot-app/` n'est pas utilise par le setup Docker actuel.

## Prerequis

- Docker Desktop (ou Docker Engine + Compose plugin)
- Optionnel pour developpement local :
  - Java 24 (API)
  - Node.js 22+ et npm (IHM)

## Demarrage rapide (recommande)

Depuis la racine `DEV` :

```bash
make up
```

Acces :
- IHM: <http://localhost:3000>
- API: <http://localhost:8080>
- Swagger: <http://localhost:8080/swagger-ui/index.html>

Arret :

```bash
make down
```

## Developpement local sans Docker

### API
```bash
make dev-api
```

### IHM
```bash
make dev-ihm
```

Par defaut, l'IHM locale tourne sur `http://localhost:5173` et proxifie `/api` vers l'API locale.

## Commandes utiles

- `make build` : rebuild des images Docker
- `make logs` : logs des services Docker
- `make ps` : etat des conteneurs
- `make test-api` : tests backend
- `make build-ihm` : build frontend

## Documentation projet

- API : `ecoswitch-api/README.md`
- IHM : `ecoswitch-ihm/README.md`
- Docker : `DOCKER-RUN.md`
