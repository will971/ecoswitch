#!/usr/bin/env bash
# Lance l'IHM et/ou l'API sur les ports isoles du workspace.
# Usage : dev.sh [all|api|ihm]

. "$(cd "$(dirname "$0")" && pwd)/lib.sh"

cd "${CONDUCTOR_WORKSPACE_PATH:-$(pwd)}"

MODE="${1:-all}"

ensure_shared_infra

# Tue tout le groupe de processus a la sortie : pas de serveur orphelin qui
# garderait le port du workspace.
cleanup() {
  trap - TERM INT EXIT
  kill -- -$$ 2>/dev/null || true
}
trap cleanup TERM INT EXIT

start_api() {
  log "API -> http://localhost:${PORT_API}   (admin dev : ${ECOSWITCH_DEV_ADMIN_EMAIL})"
  (
    cd ecoswitch-api
    PORT="$PORT_API" \
    DATABASE_URL="$DATABASE_URL" \
    BETTER_AUTH_SECRET="$BETTER_AUTH_SECRET" \
    BETTER_AUTH_URL="http://localhost:${PORT_API}" \
    ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS="$ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS" \
    S3_ENDPOINT="$S3_ENDPOINT" S3_REGION="$S3_REGION" S3_BUCKET="$S3_BUCKET" \
    S3_ACCESS_KEY="$S3_ACCESS_KEY" S3_SECRET_KEY="$S3_SECRET_KEY" \
    S3_PUBLIC_BASE_URL="$S3_PUBLIC_BASE_URL" \
    CATALOG_CACHE_TTL_MS="${CATALOG_CACHE_TTL_MS:-5000}" \
    npm run start:dev
  ) &
}

start_ihm() {
  log "IHM -> http://localhost:${PORT_IHM}"
  (
    cd ecoswitch-ihm
    VITE_DEV_API_TARGET="http://127.0.0.1:${PORT_API}" npm run dev -- --port "$PORT_IHM" --strictPort
  ) &
}

case "$MODE" in
  all) start_api; start_ihm ;;
  api) start_api ;;
  ihm) start_ihm ;;
  *)   die "Mode inconnu : ${MODE} (attendu : all | api | ihm)" ;;
esac

# Rend la main des qu'un process meurt, pour que Conductor voie l'echec.
wait -n
