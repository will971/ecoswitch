# EcoSwitch — Monorepo de Transition Écologique

EcoSwitch est un outil d'aide à la décision financière et écologique permettant de comparer le coût de détention de différents véhicules et d'identifier le seuil de rentabilité lors du passage au véhicule électrique ou hybride.

Ce dépôt est un monorepo contenant :
*   **ecoswitch-api** : Le serveur backend REST (Spring Boot / Java 24 / H2).
*   **ecoswitch-ihm** : L'interface utilisateur Web monopage (Vue 3 / Vite).

---

## 📂 Structure du Projet

```text
DEV/
  ├── ecoswitch-api/      # Backend Spring Boot (API & Admin)
  ├── ecoswitch-ihm/      # Frontend Vue.js (Interface Web & Proxy Nginx)
  ├── docs/               # Documentations détaillées (Fonctionnelle, Technique, Dév)
  ├── Makefile            # Raccourcis de commandes unifiées pour le projet
  ├── docker-compose.yml  # Fichier d'orchestration multi-conteneurs
  └── DOCKER-RUN.md       # Guide de déploiement Docker simplified
```

---

## 📖 Documentations Détaillées

Pour faciliter l'intégration et la contribution au projet, la documentation est découpée en trois guides spécifiques :

*   📘 **[Documentation Fonctionnelle](./docs/FUNCTIONAL.md)** : Règles métier, calcul du seuil de rentabilité (Break-Even), amortissement LOA/LLD, subventions gouvernementales (Bonus Écologique et Prime à la Conversion), émissions CO₂ et recherche de plaques d'immatriculation.
*   📗 **[Documentation Technique](./docs/TECHNICAL.md)** : Architecture logicielle, double chaîne de sécurité Spring Security (JWT stateless pour l'API et session standard pour l'Admin), proxy Nginx, et solutions d'observabilité (AspectJ, JVM MXBeans, modification dynamique du niveau de log).
*   📙 **[Guide de Développement & Intégration](./docs/DEVELOPMENT.md)** : Prérequis d'installation, lancement local avec/sans Docker, accès aux consoles Swagger & H2, peuplement initial des bases de données et lancement des tests JUnit.

---

## 🚀 Démarrage Rapide

Le moyen le plus simple de démarrer l'ensemble des services est d'utiliser Docker Compose via le `Makefile`.

### Lancement Complet
Depuis la racine du projet, exécutez :
```bash
make up
```

### URL Utiles
*   **Application Web (IHM)** : [http://localhost:3000](http://localhost:3000)
*   **API Swagger (OpenAPI)** : [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
*   **Dashboard d'Administration** : [http://localhost:8080/admin](http://localhost:8080/admin) *(User: `admin` / Password: `admin`)*
*   **Console Base de Données H2** : [http://localhost:8080/h2-console](http://localhost:8080/h2-console) *(JDBC URL : `jdbc:h2:mem:testdb`)*

### Arrêt de l'Environnement
```bash
make down
```

---

## 🛠️ Commandes Utiles (Raccourcis Makefile)

| Commande | Action |
| :--- | :--- |
| `make up` | Démarre et reconstruit l'ensemble des conteneurs Docker (API + IHM + Proxy) |
| `make down` | Arrête et nettoie tous les conteneurs et réseaux Docker associés |
| `make dev-api` | Lance le serveur backend en local sur le port `8080` (sans Docker) |
| `make dev-ihm` | Installe les dépendances et lance le front en local sur le port `5173` |
| `make test-api` | Exécute l'ensemble des tests automatisés JUnit du backend |
| `make build` | Reconstruit uniquement les images Docker |
| `make logs` | Affiche en continu les logs des conteneurs Docker |
| `make ps` | Liste l'état des conteneurs en cours d'exécution |
