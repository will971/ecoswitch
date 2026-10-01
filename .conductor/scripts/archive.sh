#!/usr/bin/env bash
# Appele a l'archivage d'un workspace.
#
# On ne touche NI au conteneur de base NI a son volume : ils sont partages
# par tous les workspaces. Archiver un workspace ne doit jamais detruire les
# donnees des autres.

. "$(cd "$(dirname "$0")" && pwd)/lib.sh"

log "Workspace « ${WORKSPACE_LABEL} » archive. Base partagee laissee intacte."
log "Pour arreter la base (tous workspaces) :  .conductor/scripts/db.sh down"
