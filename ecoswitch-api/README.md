# EcoSwitch API

API backend du projet EcoSwitch, developpee avec Spring Boot.

## Prerequis
- Java 24
- Docker (optionnel, pour execution conteneurisee)

## Lancement en local
Depuis le dossier `ecoswitch-api` :

```bash
./gradlew bootRun
```

L'API demarre par defaut sur `http://localhost:8080`.

## Endpoints utiles
- Swagger UI : `http://localhost:8080/swagger-ui/index.html`
- H2 Console : `http://localhost:8080/h2-console`

## Tests
```bash
./gradlew test
```

## Lancement avec Docker (projet complet)
Depuis la racine `DEV` (un niveau au-dessus) :

```bash
docker compose up --build
```

Dans ce mode :
- API : `http://localhost:8080`
- IHM : `http://localhost:3000`
