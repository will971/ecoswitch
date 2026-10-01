# Bascule de la production : API Java → API NestJS

Procédure pour reprendre une base PostgreSQL alimentée par l'ancienne API
Spring Boot. Indisponibilité visée : **moins de 20 minutes**.

## Ce qui rend la bascule sûre

- **Aucune donnée métier n'est réécrite.** `simulation` et `user_vehicle_profile`
  référencent l'utilisateur par email, sans clé étrangère : l'identité change de
  table sans toucher à ces lignes.
- **Les migrations sont additives.** `0_init` reproduit à l'identique le schéma
  produit par Hibernate et est marquée comme déjà appliquée. Les suivantes
  ajoutent des tables et un index, et convertissent une colonne.
- **Rien n'est supprimé.** `app_user` (anciens comptes), `vehicule` et
  `media_files` restent en place : c'est le chemin de retour arrière.
- **Les mots de passe sont conservés.** Les hachages Spring sont copiés tels
  quels ; chacun se reconnecte avec son mot de passe actuel.

## Conséquences visibles pour les utilisateurs

- **Tout le monde est déconnecté une fois** : les anciens jetons JWT ne sont pas
  reconnus. Le front gère ce cas (retour à l'écran de connexion).
- **Les nouveaux mots de passe font 8 caractères minimum.** Les comptes existants
  ne sont pas concernés.
- Le compte `admin` / `admin` semé par l'ancienne API **n'est pas repris**.

## Étapes

### J-7 — Répétition générale

Rejouer toute la procédure sur une copie de la base de production. Ne pas sauter
cette étape : c'est elle qui valide la reprise du schéma réel.

### J0

1. **Page de maintenance**, puis sauvegarde complète :

   ```bash
   pg_dump -Fc "$DATABASE_URL" > ecoswitch-avant-bascule.dump
   ```

   Le format `-Fc` embarque les *large objects* où l'API Java stockait les
   images ; un dump partiel par table ne les contiendrait pas.

2. **Appliquer les migrations** — automatique au démarrage du conteneur :

   ```bash
   docker compose -f docker-compose.prod.yml run --rm api \
     sh -c "node scripts/baseline-if-needed.cjs && npx prisma migrate deploy"
   ```

   `baseline-if-needed.cjs` reconnaît une base issue de l'API Java (schéma
   présent, pas d'historique Prisma) et marque `0_init` comme déjà appliquée.
   La migration `media_oid_to_bytea` copie ensuite les images des *large
   objects* vers une colonne `bytea`, vérifie qu'aucune n'est perdue, puis
   libère les *large objects*.

3. **Migrer les comptes** (idempotent, relançable) :

   ```bash
   docker compose -f docker-compose.prod.yml run --rm api npm run migrate:users -- --dry-run
   docker compose -f docker-compose.prod.yml run --rm api npm run migrate:users
   ```

   Le script échoue si une simulation ou un profil de garage reste rattaché à un
   email sans compte.

4. **Démarrer l'API**, puis extraire les images vers le bucket OVH :

   ```bash
   make prod-up
   make migrate-media    # téléverse, réécrit les URL du catalogue, vérifie
   ```

5. **Vérifications** (le test de bout en bout écrit des données de test, puis les supprime) :

   ```bash
   curl -s https://api.example.fr/health
   API=https://api.example.fr ADMIN_EMAIL=... ADMIN_PASSWORD=... \
     bash ecoswitch-api/test/e2e/smoke.sh
   ```

   Contrôler à la main : connexion avec un compte existant, une simulation
   sauvegardée, un logo de marque, et qu'un `POST /api/v1/catalog/brands` sans
   jeton renvoie bien **401**.

6. **Retirer la page de maintenance.**

### Retour arrière

Toutes les migrations étant additives, l'ancienne image Java fonctionne sur la
base migrée, à une exception près : la colonne `media_files.data` est passée de
`oid` à `bytea`, que Hibernate ne sait pas lire. Le retour arrière complet
consiste donc à **restaurer le dump** de l'étape 1. Les sessions et comptes créés
entre-temps sur la nouvelle API sont perdus. Garder ce dump au moins deux semaines.

### J+14 — Nettoyage (facultatif)

Une fois la nouvelle API validée : supprimer `app_user`, `vehicule`, puis
`media_files` lorsque plus aucune simulation sauvegardée n'en dépend.
