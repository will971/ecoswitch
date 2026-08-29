# EcoSwitch — Lancement Docker & Déploiement

## 1. Prérequis
- Docker Engine + Docker Compose (ou Docker Desktop)

---

## 2. Environnement de Développement Local
Utilise une base H2 en mémoire et recharge automatiquement les données :

```bash
# Démarrage
make up
# ou : docker compose up --build

# Arrêt
make down
# ou : docker compose down
```

**Accès :**
- IHM Web : [http://localhost:3000](http://localhost:3000)
- API Directe : [http://localhost:8080](http://localhost:8080)
- Swagger UI : [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Console H2 : [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

---

## 3. Environnement de Production (PostgreSQL + Optimisations)
Utilise PostgreSQL persistant, les flags mémoire JVM légers, la compression Gzip et le cache Nginx :

1. **Préparation des variables d'environnement :**
   ```bash
   cp .env.example .env
   # Éditez .env pour définir vos mots de passe et clés
   ```

2. **Démarrage en arrière-plan :**
   ```bash
   make prod-up
   # ou : docker compose -f docker-compose.prod.yml up -d --build
   ```

3. **Suivi des logs :**
   ```bash
   make prod-logs
   # ou : docker compose -f docker-compose.prod.yml logs -f
   ```

4. **Arrêt :**
   ```bash
   make prod-down
   # ou : docker compose -f docker-compose.prod.yml down
   ```

