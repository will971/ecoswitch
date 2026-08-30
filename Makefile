.PHONY: up down build logs ps dev-api dev-ihm test-api build-ihm prod-up prod-down prod-logs

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

prod-up:
	docker compose -f docker-compose.prod.yml up -d --build

prod-down:
	docker compose -f docker-compose.prod.yml down

prod-logs:
	docker compose -f docker-compose.prod.yml logs -f

dev-api:
	cd ecoswitch-api && ./gradlew bootRun

test-api:
	cd ecoswitch-api && ./gradlew test

dev-ihm:
	cd ecoswitch-ihm && npm install && npm run dev

build-ihm:
	cd ecoswitch-ihm && npm install && npm run build

seed-local:
	python3 scripts/seed_catalog.py local

seed-local-reset:
	python3 scripts/seed_catalog.py local --reset

seed-prod:
	python3 scripts/seed_catalog.py prod

seed-prod-reset:
	python3 scripts/seed_catalog.py prod --reset

test-scripts:
	python3 scripts/test_seed_catalog.py


