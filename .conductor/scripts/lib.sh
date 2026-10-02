#!/usr/bin/env bash
# Helpers partages par les scripts Conductor.
# Source-le, ne l'execute pas : `. "$(dirname "$0")/lib.sh"`

set -euo pipefail

# ── Base de donnees PARTAGEE entre tous les workspaces ───────────────────
#
# Choix assume : un seul Postgres, une seule base, pour tous les workspaces.
# On evite ainsi de re-seeder le catalogue a chaque nouveau workspace.
# Le conteneur et son volume vivent HORS des worktrees : archiver un
# workspace ne detruit pas les donnees.
#
# Consequence a connaitre : deux workspaces sur des branches aux migrations
# divergentes se marchent dessus. Voir .conductor/README.md.
export ECOSWITCH_DB_CONTAINER="${ECOSWITCH_DB_CONTAINER:-ecoswitch-dev-db}"
export ECOSWITCH_DB_VOLUME="${ECOSWITCH_DB_VOLUME:-ecoswitch-dev-pgdata}"
export ECOSWITCH_DB_PORT="${ECOSWITCH_DB_PORT:-5433}"
export ECOSWITCH_DB_NAME="${ECOSWITCH_DB_NAME:-ecoswitch}"
export ECOSWITCH_DB_USER="${ECOSWITCH_DB_USER:-ecoswitch}"
export ECOSWITCH_DB_PASSWORD="${ECOSWITCH_DB_PASSWORD:-ecoswitch}"
export ECOSWITCH_DB_IMAGE="${ECOSWITCH_DB_IMAGE:-postgres:16-alpine}"

export DATABASE_URL="postgresql://${ECOSWITCH_DB_USER}:${ECOSWITCH_DB_PASSWORD}@127.0.0.1:${ECOSWITCH_DB_PORT}/${ECOSWITCH_DB_NAME}?schema=public"

# ── Stockage d'images PARTAGE (RustFS, compatible S3) ───────────────────
# MinIO n'est plus distribue en image Docker ; RustFS le remplace. Le bucket
# et sa lecture publique sont crees par l'API au demarrage.
export ECOSWITCH_S3_CONTAINER="${ECOSWITCH_S3_CONTAINER:-ecoswitch-dev-s3}"
export ECOSWITCH_S3_VOLUME="${ECOSWITCH_S3_VOLUME:-ecoswitch-dev-s3data}"
export ECOSWITCH_S3_IMAGE="${ECOSWITCH_S3_IMAGE:-rustfs/rustfs:1.0.0}"
export ECOSWITCH_S3_PORT="${ECOSWITCH_S3_PORT:-9100}"
export S3_ENDPOINT="http://127.0.0.1:${ECOSWITCH_S3_PORT}"
export S3_REGION="us-east-1"
export S3_BUCKET="ecoswitch-media"
export S3_ACCESS_KEY="ecoswitch"
export S3_SECRET_KEY="ecoswitch-dev-secret"
export S3_AUTO_CREATE_BUCKET="true"
export S3_PUBLIC_BASE_URL="http://127.0.0.1:${ECOSWITCH_S3_PORT}/${S3_BUCKET}"

# ── Authentification de developpement ────────────────────────────────────
# Secret de dev (jamais en production). Le compte ci-dessous est promu ADMIN
# au demarrage de l'API ; db.sh seed le cree au besoin.
export BETTER_AUTH_SECRET="${BETTER_AUTH_SECRET:-dev-only-secret-not-for-production-0123456789}"
export ECOSWITCH_DEV_ADMIN_EMAIL="${ECOSWITCH_DEV_ADMIN_EMAIL:-admin@ecoswitch.local}"
export ECOSWITCH_DEV_ADMIN_PASSWORD="${ECOSWITCH_DEV_ADMIN_PASSWORD:-admin-dev-123}"
export ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS="${ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS:-$ECOSWITCH_DEV_ADMIN_EMAIL}"

# ── Ports ISOLES par workspace ──────────────────────────────────────────
#
# Conductor alloue 10 ports consecutifs a partir de $CONDUCTOR_PORT. Chaque
# workspace a donc ses propres ports : plusieurs agents tournent en parallele
# sans collision.
#
# +0 IHM (Vite)   +1 API NestJS   +2 Prisma Studio   +3..+9 libres
base_port() {
  if [ -n "${CONDUCTOR_PORT:-}" ]; then
    echo "$CONDUCTOR_PORT"
  else
    # Hors Conductor (terminal classique) : ports historiques du projet.
    echo "3000"
  fi
}

export PORT_IHM="$(( $(base_port) + 0 ))"
export PORT_API="$(( $(base_port) + 1 ))"
export PORT_PRISMA_STUDIO="$(( $(base_port) + 2 ))"

export WORKSPACE_LABEL="${CONDUCTOR_WORKSPACE_NAME:-local}"

# ── Utilitaires ─────────────────────────────────────────────────────────

log()  { printf '\033[1;36m[ecoswitch]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[ecoswitch]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[ecoswitch]\033[0m %s\n' "$*" >&2; exit 1; }

require_docker() {
  command -v docker >/dev/null 2>&1 || die "Docker est requis mais introuvable dans le PATH."
  docker info >/dev/null 2>&1 || die "Le demon Docker ne repond pas. Lance Docker Desktop."
}

# Demarre la base partagee si besoin. Idempotent : peut etre appele par
# chaque workspace sans effet de bord.
ensure_shared_db() {
  require_docker

  # `docker inspect` ecrit une ligne vide sur stdout quand le conteneur manque :
  # on teste donc son existence avant de lire son etat.
  local state
  if docker container inspect "$ECOSWITCH_DB_CONTAINER" >/dev/null 2>&1; then
    state="$(docker container inspect -f '{{.State.Status}}' "$ECOSWITCH_DB_CONTAINER")"
  else
    state="absent"
  fi

  case "$state" in
    running)
      log "Base partagee deja active (${ECOSWITCH_DB_CONTAINER}:${ECOSWITCH_DB_PORT})."
      ;;
    absent)
      log "Creation de la base partagee sur le port ${ECOSWITCH_DB_PORT}..."
      docker volume create "$ECOSWITCH_DB_VOLUME" >/dev/null
      docker run -d \
        --name "$ECOSWITCH_DB_CONTAINER" \
        --restart unless-stopped \
        -e POSTGRES_DB="$ECOSWITCH_DB_NAME" \
        -e POSTGRES_USER="$ECOSWITCH_DB_USER" \
        -e POSTGRES_PASSWORD="$ECOSWITCH_DB_PASSWORD" \
        -v "$ECOSWITCH_DB_VOLUME":/var/lib/postgresql/data \
        -p "${ECOSWITCH_DB_PORT}":5432 \
        "$ECOSWITCH_DB_IMAGE" >/dev/null
      ;;
    *)
      log "Redemarrage de la base partagee (etat: ${state})..."
      docker start "$ECOSWITCH_DB_CONTAINER" >/dev/null
      ;;
  esac

  wait_for_db
}

wait_for_db() {
  local i
  for i in $(seq 1 30); do
    if docker exec "$ECOSWITCH_DB_CONTAINER" pg_isready -U "$ECOSWITCH_DB_USER" -d "$ECOSWITCH_DB_NAME" >/dev/null 2>&1; then
      log "Base prete."
      return 0
    fi
    sleep 1
  done
  die "La base n'a pas repondu apres 30 s. Inspecte : docker logs ${ECOSWITCH_DB_CONTAINER}"
}

psql_shared() {
  docker exec -i "$ECOSWITCH_DB_CONTAINER" psql -U "$ECOSWITCH_DB_USER" -d "$ECOSWITCH_DB_NAME" "$@"
}

# Demarre le stockage d'images partage. Idempotent.
ensure_shared_storage() {
  require_docker
  local state
  if docker container inspect "$ECOSWITCH_S3_CONTAINER" >/dev/null 2>&1; then
    state="$(docker container inspect -f '{{.State.Status}}' "$ECOSWITCH_S3_CONTAINER")"
  else
    state="absent"
  fi

  case "$state" in
    running) ;;
    absent)
      log "Creation du stockage d'images partage (RustFS) sur le port ${ECOSWITCH_S3_PORT}..."
      docker volume create "$ECOSWITCH_S3_VOLUME" >/dev/null
      docker run -d \
        --name "$ECOSWITCH_S3_CONTAINER" \
        --restart unless-stopped \
        -e RUSTFS_ACCESS_KEY="$S3_ACCESS_KEY" \
        -e RUSTFS_SECRET_KEY="$S3_SECRET_KEY" \
        -v "$ECOSWITCH_S3_VOLUME":/data \
        -p "${ECOSWITCH_S3_PORT}":9000 \
        "$ECOSWITCH_S3_IMAGE" >/dev/null
      ;;
    *)
      docker start "$ECOSWITCH_S3_CONTAINER" >/dev/null
      ;;
  esac

  local i
  for i in $(seq 1 30); do
    if curl -sf "${S3_ENDPOINT}/health" >/dev/null 2>&1; then
      log "Stockage d'images pret (${S3_ENDPOINT}, bucket ${S3_BUCKET})."
      return 0
    fi
    sleep 1
  done
  die "Le stockage d'images n'a pas repondu apres 30 s. Inspecte : docker logs ${ECOSWITCH_S3_CONTAINER}"
}

ensure_shared_infra() {
  ensure_shared_db
  ensure_shared_storage
}
