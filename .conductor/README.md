# Lancer EcoSwitch depuis Conductor

## Le principe

**Les ports sont isolés par workspace. La base et les images sont partagées.**

Conductor alloue dix ports consécutifs à chaque workspace local, à partir de
`$CONDUCTOR_PORT`. Plusieurs agents tournent ainsi en parallèle sans collision :

| Décalage | Service |
|---|---|
| `+0` | IHM (Vite) |
| `+1` | API NestJS |
| `+2` | Prisma Studio |
| `+3`…`+9` | libres |

L'infrastructure, elle, est **commune à tous les workspaces** et vit hors des
worktrees — archiver un workspace ne détruit aucune donnée :

| Service | Conteneur | Port | Volume |
|---|---|---|---|
| PostgreSQL | `ecoswitch-dev-db` | 5433 | `ecoswitch-dev-pgdata` |
| Images (MinIO, équivalent local du bucket OVH) | `ecoswitch-dev-minio` | 9100 (console 9101) | `ecoswitch-dev-miniodata` |

## Au quotidien

| Script | Rôle |
|---|---|
| **dev** | IHM + API *(défaut)* |
| **API seule** / **IHM seule** | un seul service |
| **tests** | Vitest (dont 4 422 vecteurs de parité avec l'ancien Java) + seeder |
| **tests e2e** | ~50 scénarios contre l'API du workspace (lancer **dev** d'abord) |
| **base : état** | conteneurs, volumétrie, migrations appliquées |
| **base : peupler** | lance le seeder contre l'API du workspace |
| **base : Prisma Studio** | explorateur de données |

En ligne de commande, `.conductor/scripts/db.sh` expose aussi `up`, `down`,
`psql`, `migrate`, `migrate-users`, `dump` et `reset`.

**Compte admin de développement** : `admin@ecoswitch.local` / `admin-dev-123`.
Il est créé par **base : peupler** et promu ADMIN par l'API au démarrage — il
permet d'éditer le catalogue depuis l'interface.

## La contrepartie de l'infrastructure partagée

1. **Un seul schéma pour tous les workspaces.** Deux branches aux migrations
   divergentes se marchent dessus : Prisma pose un verrou, donc les migrations
   ne s'entrelacent pas, mais si le workspace A applique une migration que la
   branche du workspace B ignore, B tourne sur un schéma qu'il ne connaît pas.
   Travaille sur une migration dans un seul workspace à la fois ; **base : état**
   affiche les migrations appliquées.

2. **Les données sont communes.** Une simulation créée dans un workspace est
   visible depuis les autres. Pour repartir d'un état vierge : `db.sh dump`, puis
   `db.sh reset`.

3. **Le cache catalogue retarde la propagation.** Chaque API garde le catalogue
   en mémoire (10 minutes en production). En développement, `dev.sh` abaisse ce
   délai à **5 secondes** (`CATALOG_CACHE_TTL_MS`) : une modification faite dans
   un workspace apparaît presque aussitôt dans les autres.

## Première mise en service

Le setup crée la base et le stockage s'ils n'existent pas, installe les
dépendances, génère le client Prisma et applique les migrations (y compris sur
une base héritée de l'ancienne API Java). Seul geste manuel : lancer **dev**,
puis **base : peupler**.

## Le seeder n'est pas fiable

`scripts/seed_catalog.py` télécharge les photos des modèles depuis Wikimedia et
**abandonne tout modèle dont il n'obtient pas d'image**. Son jeu de données
compte 20 marques et 61 modèles, mais il n'en injecte souvent qu'une fraction.
D'où l'intérêt d'une base partagée et persistante : une fois peuplée, on n'y
revient plus. Le correctif de fond serait de versionner les images.

## Prérequis

- **Docker Desktop** démarré
- **Node 22+**
- **Python 3** pour le seeder

## Activation

Conductor lit ce fichier `settings.toml` **depuis la branche par défaut du
dépôt distant** : les scripts n'apparaissent dans l'interface qu'une fois la
configuration mergée sur `main`. En attendant, ils s'exécutent depuis le terminal
du workspace (`.conductor/scripts/dev.sh`).
