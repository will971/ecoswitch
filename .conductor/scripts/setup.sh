#!/usr/bin/env bash
# Prepare un workspace fraichement cree. Idempotent : relancable sans risque.

. "$(cd "$(dirname "$0")" && pwd)/lib.sh"

cd "${CONDUCTOR_WORKSPACE_PATH:-$(pwd)}"

log "Workspace « ${WORKSPACE_LABEL} » — ports ${PORT_IHM} (IHM) / ${PORT_API} (API)"

# ── 1. Infrastructure partagee : base + stockage d'images ───────────────
ensure_shared_infra

# ── 2. Dependances ──────────────────────────────────────────────────────
install_npm() {
  local dir="$1"
  [ -d "$dir" ] || return 0
  log "npm ci dans ${dir}..."
  (cd "$dir" && npm ci --no-audit --no-fund)
}
install_npm ecoswitch-ihm
install_npm ecoswitch-api

# ── 3. Client Prisma et migrations ──────────────────────────────────────
log "Generation du client Prisma..."
(cd ecoswitch-api && npx prisma generate >/dev/null)

# Base heritee du Java : schema present sans historique Prisma -> baseline.
if ! psql_shared -tAc "SELECT 1 FROM information_schema.tables WHERE table_name='_prisma_migrations'" 2>/dev/null | grep -q 1 \
   && psql_shared -tAc "SELECT 1 FROM information_schema.tables WHERE table_name='brands'" 2>/dev/null | grep -q 1; then
  log "Schema existant sans historique Prisma : marquage de la baseline 0_init."
  (cd ecoswitch-api && npx prisma migrate resolve --applied 0_init >/dev/null)
fi

log "Application des migrations (base PARTAGEE)..."
(cd ecoswitch-api && npx prisma migrate deploy)

# ── 4. Catalogue ────────────────────────────────────────────────────────
count="$(psql_shared -tAc "SELECT count(*) FROM brands" 2>/dev/null | tr -d ' ')"
if [ "${count:-0}" = "0" ]; then
  warn "Catalogue vide. Lance « dev », puis « base : peupler »."
else
  log "Catalogue deja peuple (${count} marques)."
fi

log "Workspace pret. Lance « dev » depuis Conductor."
