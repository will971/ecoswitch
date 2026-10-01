#!/usr/bin/env bash
# Test de fumee de bout en bout contre une API vivante.
# Usage : API=http://127.0.0.1:8080 ADMIN_EMAIL=admin@test.fr test/e2e/smoke.sh
# L'API doit avoir ADMIN_EMAIL dans ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS.
set -uo pipefail
API="${API:-http://127.0.0.1:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@test.fr}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-motdepasse-admin}"
RUN="$(date +%s)"
pass=0; fail=0

# check <libelle> <code attendu> <methode> <chemin> [corps] [jeton]
check() {
  local label="$1" expect="$2" method="$3" path="$4" body="${5:-}" token="${6:-}"
  local args=(-s -o /tmp/smoke.body -w '%{http_code}' -X "$method" "$API$path" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-d "$body")
  local code; code="$(curl "${args[@]}")"
  if [ "$code" = "$expect" ]; then pass=$((pass+1)); printf '  ok   %-58s %s\n' "$label" "$code"
  else fail=$((fail+1)); printf '  KO   %-58s %s (attendu %s) %s\n' "$label" "$code" "$expect" "$(head -c 160 /tmp/smoke.body)"; fi
}
field() { python3 -c "import json,sys; d=json.load(open('/tmp/smoke.body')); print($1)" 2>/dev/null; }

# Verifie le message renvoye au front (affiche tel quel dans l'interface).
msg() {
  local label="$1" expected="$2" got; got="$(field "d.get('error')")"
  if [ "$got" = "$expected" ]; then pass=$((pass+1)); printf '  ok   %-58s\n' "$label"
  else fail=$((fail+1)); printf '  KO   %-58s « %s » (attendu « %s »)\n' "$label" "$got" "$expected"; fi
}

USER="user$RUN@test.fr"

echo "── Auth"
check "register : creation"                      201 POST /api/v1/auth/register "{\"email\":\"$USER\",\"password\":\"motdepasse1\",\"name\":\"Alice\"}"
TOKEN="$(field "d['token']")"
[ "$(field "d['role']")" = "USER" ] && echo "       role USER, plan $(field "d['plan']")"
check "register : doublon -> 409"                  409 POST /api/v1/auth/register "{\"email\":\"$USER\",\"password\":\"motdepasse1\"}"
msg "  message 409 identique au Java" "Un compte existe déjà avec cet email."
check "register : mot de passe court -> 400"       400 POST /api/v1/auth/register "{\"email\":\"x$RUN@test.fr\",\"password\":\"court\"}"
check "register : champs manquants -> 400"         400 POST /api/v1/auth/register '{"email":""}'
check "login : ok"                                 200 POST /api/v1/auth/login "{\"email\":\"$USER\",\"password\":\"motdepasse1\"}"
check "login : mauvais mot de passe -> 401"        401 POST /api/v1/auth/login "{\"email\":\"$USER\",\"password\":\"faux-faux-faux\"}"
msg "  message 401 identique au Java" "Email ou mot de passe incorrect."
check "login : email inconnu -> 401"               401 POST /api/v1/auth/login "{\"email\":\"inconnu$RUN@test.fr\",\"password\":\"motdepasse1\"}"
check "google-login : credential manquant -> 400"  400 POST /api/v1/auth/google-login '{}'
check "me : avec jeton"                            200 GET  /api/v1/auth/me "" "$TOKEN"
check "me : sans jeton -> 401"                     401 GET  /api/v1/auth/me
msg "  message 401 identique au Java" "Non authentifié"
check "me : jeton bidon -> 401"                    401 GET  /api/v1/auth/me "" "jeton-totalement-invalide-123"

echo "── Failles Java refermees"
check "garage anonyme -> 401 (Java: 200)"          401 GET  /api/v1/users/me/vehicle-profiles
check "creation marque anonyme -> 401 (Java: 201)" 401 POST /api/v1/catalog/brands '{"name":"Pirate"}'
check "creation marque USER -> 403"                403 POST /api/v1/catalog/brands '{"name":"Pirate"}' "$TOKEN"
check "upload anonyme -> 401 (Java: 200)"          401 POST /api/v1/uploads/image

echo "── Simulations"
check "liste vide"                                 200 GET  /api/v1/simulations "" "$TOKEN"
check "sauvegarde -> 201"                          201 POST /api/v1/simulations '{"name":"Ma simu","simulationData":"{\"a\":1}"}' "$TOKEN"
SIM_ID="$(field "d['id']")"; SAVED_AT="$(field "d['savedAt']")"
echo "       savedAt=$SAVED_AT (format LocalDateTime, sans Z)"
check "sauvegarde sans nom -> 400"                 400 POST /api/v1/simulations '{"simulationData":"{}"}' "$TOKEN"
msg "  message 400 identique au Java" "Le nom de la simulation est obligatoire."
check "suppression -> 204"                         204 DELETE "/api/v1/simulations/$SIM_ID" "" "$TOKEN"
check "suppression deja faite -> 404"              404 DELETE "/api/v1/simulations/$SIM_ID" "" "$TOKEN"

echo "── Garage (champ « default », pas « isDefault »)"
check "creation 1er profil -> 200, favori"         200 POST /api/v1/users/me/vehicle-profiles '{"name":"Clio","fuelType":"PETROL","consumption":6.5}' "$TOKEN"
P1="$(field "d['id']")"; echo "       default=$(field "d['default']") petrolPrice=$(field "d['petrolPrice']") (defaut 1.88)"
check "creation 2e profil favori"                  200 POST /api/v1/users/me/vehicle-profiles '{"name":"Zoe","fuelType":"ELECTRIC","default":true}' "$TOKEN"
P2="$(field "d['id']")"
check "liste"                                      200 GET  /api/v1/users/me/vehicle-profiles "" "$TOKEN"
echo "       favoris : $(field "[(p['name'],p['default']) for p in d]")"
check "carburant invalide -> 400"                  400 POST /api/v1/users/me/vehicle-profiles '{"name":"X","fuelType":"KEROSENE"}' "$TOKEN"
check "suppression du favori"                      204 DELETE "/api/v1/users/me/vehicle-profiles/$P2" "" "$TOKEN"
check "liste apres suppression"                    200 GET  /api/v1/users/me/vehicle-profiles "" "$TOKEN"
echo "       favori promu : $(field "[(p['name'],p['default']) for p in d]")"

echo "── Catalogue (ADMIN)"
# Idempotent : connexion si le compte admin existe deja, inscription sinon.
if ! curl -sf -o /tmp/smoke.body -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' \
     -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"; then
  curl -s -o /tmp/smoke.body -X POST "$API/api/v1/auth/register" -H 'Content-Type: application/json' \
     -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\",\"name\":\"Admin\"}"
fi
ADMIN="$(field "d['token']")"
[ "$(field "d['role']")" = "ADMIN" ] && pass=$((pass+1)) && printf '  ok   %-58s\n' "promotion ADMIN via ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS" \
  || { fail=$((fail+1)); printf '  KO   promotion ADMIN (role=%s)\n' "$(field "d.get('role')")"; }
echo "       role admin : $(field "d['role']")"
check "creer marque -> 201"                        201 POST /api/v1/catalog/brands "{\"name\":\"Renault$RUN\",\"logoUrl\":\"/l.png\"}" "$ADMIN"
B="$(field "d['id']")"
check "marque en doublon (casse) -> 400"           400 POST /api/v1/catalog/brands "{\"name\":\"renault$RUN\"}" "$ADMIN"
check "creer modele"                               201 POST "/api/v1/catalog/models?brandId=$B" '{"name":"Zoe","category":"Citadine"}' "$ADMIN"
M="$(field "d['id']")"
check "creer motorisation"                         201 POST "/api/v1/catalog/motorisations?modelId=$M" '{"name":"R110","fuelType":"ELECTRIC","consumptionWltp":17.2,"autonomieWltpKm":395}' "$ADMIN"
MO="$(field "d['id']")"
check "creer finition"                             201 POST "/api/v1/catalog/finitions?modelId=$M" '{"name":"Life"}' "$ADMIN"
F="$(field "d['id']")"
check "creer variante"                             201 POST "/api/v1/catalog/variants?finitionId=$F&motorisationId=$MO" '{"purchasePrice":33000,"monthlyLoa":299,"defaultMaintenanceCost":200,"estimatedResaleValue":12000}' "$ADMIN"
V="$(field "d['id']")"
check "upsert variante (meme couple)"              201 POST "/api/v1/catalog/variants?finitionId=$F&motorisationId=$MO" '{"purchasePrice":31000}' "$ADMIN"
[ "$(field "d['id']")" = "$V" ] && echo "       meme id $V, prix $(field "d['purchasePrice']")"
check "variante detaillee (graphe)"                200 GET  "/api/v1/catalog/variants/$V"
echo "       $(field "d['finition']['model']['brand']['name'] + ' / ' + d['motorisation']['fuelType']")"
check "hierarchie"                                 200 GET  /api/v1/catalog/hierarchy
check "liste marques (seeder)"                     200 GET  /api/v1/catalog/brands
echo "       modelCount=$(field "[b['modelCount'] for b in d if b['name']=='Renault$RUN'][0]")"

echo "── Comparaisons"
check "simulateur direct"                          200 POST /api/v1/comparisons/profitability/direct '{"currentVehicle":{"name":"Clio","fuelType":"PETROL","consumption":6.5,"annualMileage":15000,"maintenanceCost":500,"resaleValue":8000},"targetVehicle":{"name":"Zoe","fuelType":"ELECTRIC","consumption":17,"annualMileage":15000,"maintenanceCost":250,"purchasePrice":30000},"fuelPricesByType":{"PETROL":1.9,"ELECTRIC":0.25},"homeChargingRatio":0.8,"scrapVehicle":true}'
echo "       breakEven=$(field "d['breakEvenYear']") subsides=$(field "d['totalSubsidies']") recos=$(field "len(d['recommendations'])")"
check "simulateur : sans prix -> 400"              400 POST /api/v1/comparisons/profitability/direct '{"currentVehicle":{"fuelType":"PETROL"},"targetVehicle":{"fuelType":"ELECTRIC"}}'
check "comparateur catalogue"                      200 POST /api/v1/comparisons/profitability/custom "{\"currentVehicle\":{\"name\":\"Clio\",\"fuelType\":\"PETROL\",\"consumption\":6.5,\"annualMileage\":15000,\"resaleValue\":8000},\"targetVehicleIds\":[$V],\"fuelPricesByType\":{\"PETROL\":1.9,\"ELECTRIC\":0.25}}"
check "conseiller IA (forme plate du front)"       200 POST /api/v1/comparisons/ai-advisor '{"currentVehicleName":"Clio","currentFuelType":"PETROL","targetVehicleName":"Zoe","targetFuelType":"ELECTRIC","targetConsumption":17,"annualMileage":15000,"annualFuelSavings":1500,"breakEvenYear":4,"isLeasing":false}'
echo "       moteur=$(field "d['aiEngine']") statut=$(field "d['status']")"
check "prix carburants"                            200 GET  /api/v1/comparisons/fuel-prices/live
echo "       isLive=$(field "d['isLive']") PETROL=$(field "d['prices']['PETROL']")"

echo "── Nettoyage"
check "suppression marque en cascade"              204 DELETE "/api/v1/catalog/brands/$B" "" "$ADMIN"
check "variante disparue avec la marque"           404 GET  "/api/v1/catalog/variants/$V"

echo
echo "Resultat : $pass ok, $fail KO"
[ "$fail" -eq 0 ]
