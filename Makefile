.PHONY: up down build logs ps dev-api dev-ihm test-api build-ihm

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

dev-api:
	cd ecoswitch-api && ./gradlew bootRun

test-api:
	cd ecoswitch-api && ./gradlew test

dev-ihm:
	cd ecoswitch-ihm && npm install && npm run dev

build-ihm:
	cd ecoswitch-ihm && npm install && npm run build
