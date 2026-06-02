# EcoSwitch IHM

Interface web du projet EcoSwitch, developpee avec Vue 3 et Vite.

## Prerequis
- Node.js 22+
- npm
- Docker (optionnel, pour execution conteneurisee)

## Installation
Depuis le dossier `ecoswitch-ihm` :

```bash
npm install
```

## Lancement en local (developpement)
```bash
npm run dev
```

L'IHM demarre par defaut sur `http://localhost:5173`.

## Build de production
```bash
npm run build
```

## Preview locale du build
```bash
npm run preview
```

## Lancement avec Docker (projet complet)
Depuis la racine `DEV` (un niveau au-dessus) :

```bash
docker compose up --build
```

Dans ce mode :
- IHM : `http://localhost:3000`
- API : `http://localhost:8080`
