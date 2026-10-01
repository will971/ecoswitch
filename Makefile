.PHONY: up down build logs ps prod-up prod-down prod-logs dev-api dev-ihm test test-api test-scripts migrate migrate-users migrate-media studio seed-local seed-prod

# ── Pile complete en conteneurs (base, MinIO, API, IHM) ─────────────────
up:
	docker compose up --build

down:
	docker compose down

build:
	docker compose build

logs:
	docker compose logs -f

ps:
	docker compose ps

# ── Production ──────────────────────────────────────────────────────────
prod-up:
	docker compose -f docker-compose.prod.yml up -d --build

prod-down:
	docker compose -f docker-compose.prod.yml down

prod-logs:
	docker compose -f docker-compose.prod.yml logs -f

# Bascule depuis l'ancienne API Java (cf. docs/MIGRATION.md)
migrate-users:
	docker compose -f docker-compose.prod.yml exec api npm run migrate:users

migrate-media:
	docker compose -f docker-compose.prod.yml exec api npm run migrate:media

# ── Developpement hors conteneurs (base et MinIO via `make up` ou Conductor)
dev-api:
	cd ecoswitch-api && npm run start:dev

dev-ihm:
	cd ecoswitch-ihm && npm install && npm run dev

migrate:
	cd ecoswitch-api && npx prisma migrate deploy

studio:
	cd ecoswitch-api && npx prisma studio

# ── Tests ───────────────────────────────────────────────────────────────
test: test-api test-scripts

test-api:
	cd ecoswitch-api && npm test

test-scripts:
	cd scripts && python3 test_seed_catalog.py

# ── Catalogue (exige un compte ADMIN) ───────────────────────────────────
# Exemple : make seed-local ECOSWITCH_ADMIN_EMAIL=moi@x.fr ECOSWITCH_ADMIN_PASSWORD=...
seed-local:
	python3 scripts/seed_catalog.py --url http://localhost:8080

seed-prod:
	python3 scripts/seed_catalog.py --url $(API_URL)
