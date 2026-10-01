#!/usr/bin/env bash
# Pilotage de la base PARTAGEE entre tous les workspaces.
# Usage : db.sh {up|down|status|psql|seed|reset|studio|dump|migrate|migrate-users}

. "$(cd "$(dirname "$0")" && pwd)/lib.sh"

cd "${CONDUCTOR_WORKSPACE_PATH:-$(pwd)}"

case "${1:-status}" in
  up)
    ensure_shared_infra
    ;;

  down)
    warn "Infrastructure PARTAGEE : l'arreter coupe tous les workspaces."
    docker stop "$ECOSWITCH_DB_CONTAINER" "$ECOSWITCH_S3_CONTAINER" >/dev/null 2>&1 || true
    log "Base et stockage d'images arretes."
    ;;

  status)
    require_docker
    if docker container inspect "$ECOSWITCH_DB_CONTAINER" >/dev/null 2>&1; then
      state="$(docker container inspect -f '{{.State.Status}}' "$ECOSWITCH_DB_CONTAINER")"
    else
      state="absent"
    fi
    log "Conteneur : ${ECOSWITCH_DB_CONTAINER} (${state}) sur le port ${ECOSWITCH_DB_PORT}"
    log "Volume    : ${ECOSWITCH_DB_VOLUME}"
    log "Images    : ${S3_PUBLIC_BASE_URL} (console http://localhost:${ECOSWITCH_S3_CONSOLE_PORT})"
    log "URL       : ${DATABASE_URL}"
    if [ "$state" = "running" ]; then
      psql_shared -c "
        SELECT (SELECT count(*) FROM brands)                 AS marques,
               (SELECT count(*) FROM vehicle_models)         AS modeles,
               (SELECT count(*) FROM finition_motorisations) AS variantes,
               (SELECT count(*) FROM simulation)             AS simulations;" 2>/dev/null \
        || warn "Schema absent. Lance :  .conductor/scripts/db.sh migrate"
      echo
      log "Migrations appliquees :"
      psql_shared -tAc "SELECT migration_name FROM _prisma_migrations ORDER BY finished_at" 2>/dev/null \
        | sed 's/^/  - /' || warn "  (aucun historique Prisma)"
    fi
    ;;

  psql)
    ensure_shared_db
    shift
    docker exec -it "$ECOSWITCH_DB_CONTAINER" psql -U "$ECOSWITCH_DB_USER" -d "$ECOSWITCH_DB_NAME" "$@"
    ;;

  migrate)
    ensure_shared_db
    warn "La base est PARTAGEE : cette migration s'applique a TOUS les workspaces."
    (cd ecoswitch-api && npx prisma migrate deploy)
    ;;

  migrate-users)
    ensure_shared_db
    warn "Migration des comptes app_user -> better-auth (base PARTAGEE)."
    (cd ecoswitch-api && DATABASE_URL="$DATABASE_URL" npm run migrate:users)
    ;;

  seed)
    ensure_shared_infra
    api="${ECOSWITCH_SEED_API:-http://127.0.0.1:${PORT_API}}"
    curl -sf "${api}/health" >/dev/null 2>&1 || die "Aucune API sur ${api}. Lance « dev » d'abord."

    # Compte admin de dev : cree au premier passage, promu ADMIN par l'API.
    curl -s -o /dev/null -X POST "${api}/api/v1/auth/register" -H 'Content-Type: application/json' \
      -d "{\"email\":\"${ECOSWITCH_DEV_ADMIN_EMAIL}\",\"password\":\"${ECOSWITCH_DEV_ADMIN_PASSWORD}\",\"name\":\"Admin dev\"}" || true

    log "Peuplement du catalogue via ${api}..."
    warn "Rappel : le seeder telecharge ses images depuis Wikimedia et abandonne"
    warn "les modeles sans photo — il n'injecte aujourd'hui qu'une partie du catalogue."
    python3 scripts/seed_catalog.py --url "$api" \
      --email "$ECOSWITCH_DEV_ADMIN_EMAIL" --password "$ECOSWITCH_DEV_ADMIN_PASSWORD"
    ;;

  reset)
    warn "DESTRUCTIF — la base est PARTAGEE par tous les workspaces."
    printf 'Taper « reset » pour confirmer : '
    read -r confirm
    [ "$confirm" = "reset" ] || die "Annule."
    docker rm -f "$ECOSWITCH_DB_CONTAINER" "$ECOSWITCH_S3_CONTAINER" >/dev/null 2>&1 || true
    docker volume rm "$ECOSWITCH_DB_VOLUME" "$ECOSWITCH_S3_VOLUME" >/dev/null 2>&1 || true
    log "Base, images et volumes supprimes. Relance le setup du workspace."
    ;;

  studio)
    ensure_shared_db
    log "Prisma Studio -> http://localhost:${PORT_PRISMA_STUDIO}"
    (cd ecoswitch-api && DATABASE_URL="$DATABASE_URL" npx prisma studio --port "$PORT_PRISMA_STUDIO")
    ;;

  dump)
    ensure_shared_db
    out="${2:-.context/ecoswitch-$(date +%Y%m%d-%H%M%S).dump}"
    mkdir -p "$(dirname "$out")"
    # -Fc embarque les large objects, ce que ne fait pas un dump partiel.
    docker exec "$ECOSWITCH_DB_CONTAINER" pg_dump -U "$ECOSWITCH_DB_USER" -d "$ECOSWITCH_DB_NAME" -Fc > "$out"
    log "Dump ecrit : ${out} ($(du -h "$out" | cut -f1))"
    ;;

  *)
    die "Usage : db.sh {up|down|status|psql|seed|reset|studio|dump|migrate|migrate-users}"
    ;;
esac
