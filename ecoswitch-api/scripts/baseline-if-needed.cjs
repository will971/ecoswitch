/**
 * Execute au demarrage du conteneur, avant `prisma migrate deploy`.
 *
 * Une base issue de l'API Java contient deja le schema (cree par Hibernate)
 * mais pas d'historique Prisma. Sans ce script, `migrate deploy` tenterait de
 * recreer les tables existantes et echouerait. On marque alors la migration
 * 0_init — qui reproduit exactement ce schema — comme deja appliquee.
 *
 * Sans effet sur une base vierge ou deja suivie par Prisma.
 */
const { execSync } = require('node:child_process')
const { PrismaClient } = require('@prisma/client')

async function main() {
  const prisma = new PrismaClient()
  try {
    const [row] = await prisma.$queryRaw`
      SELECT to_regclass('public._prisma_migrations') IS NOT NULL AS "hasHistory",
             to_regclass('public.brands') IS NOT NULL AS "hasSchema"`
    if (!row.hasHistory && row.hasSchema) {
      console.log("Base issue de l'API Java : marquage de la baseline 0_init.")
      execSync('npx prisma migrate resolve --applied 0_init', { stdio: 'inherit' })
    }
  } finally {
    await prisma.$disconnect()
  }
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
