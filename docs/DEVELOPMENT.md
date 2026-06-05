# EcoSwitch — Guide de Développement & d'Intégration

Ce guide décrit les étapes nécessaires pour installer, exécuter, tester et contribuer au projet EcoSwitch sur votre machine de développement.

---

## 1. Prérequis

Avant de commencer, assurez-vous d'avoir installé les outils suivants sur votre système :

*   **Docker Desktop** (ou Docker Engine + Docker Compose).
*   **Java 24** (nécessaire pour exécuter l'API en local sans Docker).
*   **Node.js 22+** et **npm** (nécessaires pour exécuter l'IHM en local sans Docker).

---

## 2. Lancement Rapide (Environnement Docker)

C'est la méthode recommandée pour démarrer l'ensemble du projet (API + IHM + Reverse Proxy) en une seule commande.

Depuis la racine du projet (`DEV`) :

1.  **Démarrer les conteneurs** :
    ```bash
    make up
    ```
    *(Équivalent à `docker compose up --build`)*

2.  **Arrêter les conteneurs** :
    ```bash
    make down
    ```
    *(Équivalent à `docker compose down`)*

### URL d'accès dans ce mode :
*   **Interface Utilisateur (IHM)** : [http://localhost:3000](http://localhost:3000)
*   **API Spring Boot (Directe)** : [http://localhost:8080](http://localhost:8080)
*   **Console d'administration** : [http://localhost:8080/admin](http://localhost:8080/admin) (identifiants: `admin` / `admin`)
*   **Swagger UI** : [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
*   **Console H2** : [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (voir détails de connexion ci-dessous)

---

## 3. Lancement Local pour le Développement (Sans Docker)

Pour un cycle de développement plus rapide avec rechargement à chaud (Hot Reload), vous pouvez lancer les deux services indépendamment sur votre machine.

### A. Démarrer le Backend (API Spring Boot)
Depuis la racine du projet :
```bash
make dev-api
```
*(Équivalent à `cd ecoswitch-api && ./gradlew bootRun`)*

L'API démarre par défaut sur **`http://localhost:8080`**.

### B. Démarrer le Frontend (IHM Vue 3 + Vite)
Depuis la racine du projet :
```bash
make dev-ihm
```
*(Équivalent à `cd ecoswitch-ihm && npm install && npm run dev`)*

Le serveur de développement de Vite démarre sur **`http://localhost:5173`**.

> [!NOTE]
> En mode développement local, Vite est configuré via son fichier `vite.config.js` pour agir comme proxy de développement. Tout appel vers `/api/*` sur le port `5173` est automatiquement redirigé vers `http://localhost:8080/api/*`, évitant ainsi les erreurs de sécurité de type CORS.

---

## 4. Accès aux Outils de Diagnostic

### A. Console de Base de Données H2
La base de données s'exécute en mémoire. Vous pouvez accéder à la console d'administration H2 à l'adresse suivante : [http://localhost:8080/h2-console](http://localhost:8080/h2-console).

Saisissez les paramètres de connexion suivants :
*   **Saved Settings** : Generic H2 (Embedded)
*   **Driver Class** : `org.h2.Driver`
*   **JDBC URL** : `jdbc:h2:mem:testdb`
*   **User Name** : `sa`
*   **Password** : *(laisser vide)*

### B. Documentation de l'API (Swagger UI / OpenAPI)
L'application expose une documentation interactive de ses points d'accès à l'adresse : [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).
Vous y trouverez tous les schémas de requêtes et de réponses pour les contrôleurs d'authentification, d'immatriculation, de comparaison de véhicules et de simulation.

---

## 5. Peuplement de la Base de Données (Seeding)

Au démarrage du backend, l'application initialise sa base de données à l'aide de trois sources complémentaires de données définies dans [AdemeService.java](../ecoswitch-api/src/main/java/com/example/springbootapp/service/AdemeService.java) et [VehiculeSeedLoader.java](../ecoswitch-api/src/main/java/com/example/springbootapp/service/VehiculeSeedLoader.java) :

1.  **Données Historiques (`vehicules-seed.csv`)** : Fichier local contenant des véhicules d'anciennes générations (années 2000 à 2015) avec des codes de génération détaillés (ex: BMW E46, Clio II) permettant d'alimenter les profils de véhicules actuels des utilisateurs.
2.  **Base de Données ADEME (`ademe-car-labelling.csv`)** : Un extrait officiel du jeu de données ADEME (Car Labelling 2022/2023) contenant environ 10 000 versions de véhicules neufs avec leurs consommations WLTP réelles et leurs types d'énergie associés.
3.  **JSON Distant (Optionnel)** : Chargement de véhicules de référence via une URL GitHub paramétrée sous la clé `app.seed.vehicles-url` dans `application.yaml`.

---

## 6. Lancement des Tests

Le backend intègre des suites de tests unitaires et d'intégration Spring Boot pour valider la logique des calculs et des contrôleurs.

Pour exécuter les tests depuis la racine du projet :
```bash
make test-api
```
*(Équivalent à `cd ecoswitch-api && ./gradlew test`)*

Les rapports de test JUnit sont générés dans le dossier :
`ecoswitch-api/build/reports/tests/test/index.html`.
