# API EcoSwitch — NestJS + Prisma.
# Remplace l'image gradle:jdk26 / temurin:26 : ~150 Mo de RAM au lieu de 512.

FROM node:22-alpine AS deps
WORKDIR /app
# openssl : requis par le moteur de requetes Prisma sur Alpine.
RUN apk add --no-cache openssl
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM deps AS build
COPY . .
RUN npx prisma generate && npm run build && npm prune --omit=dev --no-audit --no-fund

FROM node:22-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache openssl && addgroup -S app && adduser -S app -G app
ENV NODE_ENV=production PORT=8080
COPY --from=build --chown=app:app /app/node_modules ./node_modules
COPY --from=build --chown=app:app /app/dist ./dist
COPY --from=build --chown=app:app /app/prisma ./prisma
COPY --from=build --chown=app:app /app/package.json ./package.json
# Scripts de bascule : npm run migrate:users / migrate:media dans le conteneur.
COPY --from=build --chown=app:app /app/scripts ./scripts
COPY --from=build --chown=app:app /app/tsconfig.json ./tsconfig.json
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health || exit 1
# Les migrations sont additives et idempotentes : on les applique au demarrage.
CMD ["sh", "-c", "node scripts/baseline-if-needed.cjs && npx prisma migrate deploy && node dist/main.js"]
