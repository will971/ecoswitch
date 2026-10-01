#!/usr/bin/env bash
# Usage : test.sh        -> tests unitaires (sans base)
#         test.sh e2e    -> test de bout en bout contre l'API du workspace

. "$(cd "$(dirname "$0")" && pwd)/lib.sh"

cd "${CONDUCTOR_WORKSPACE_PATH:-$(pwd)}"

if [ "${1:-}" = "e2e" ]; then
  api="http://127.0.0.1:${PORT_API}"
  curl -sf "${api}/health" >/dev/null 2>&1 || die "Aucune API sur ${api}. Lance « dev » d'abord."
  log "Test de bout en bout contre ${api} (ecrit dans la base PARTAGEE puis nettoie)..."
  API="$api" ADMIN_EMAIL="$ECOSWITCH_DEV_ADMIN_EMAIL" ADMIN_PASSWORD="$ECOSWITCH_DEV_ADMIN_PASSWORD" bash ecoswitch-api/test/e2e/smoke.sh
  exit $?
fi

failed=0
log "API : Vitest (dont 4 400+ vecteurs de reference extraits de l'ancien Java)..."
(cd ecoswitch-api && npm test) || failed=1
log "Seeder : tests Python..."
(cd scripts && python3 test_seed_catalog.py) || failed=1
[ "$failed" -eq 0 ] && log "Tout est vert." || die "Des tests ont echoue."
