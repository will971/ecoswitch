/**
 * Migre les comptes de l'ancienne table `app_user` (Spring) vers les tables
 * better-auth (`auth_user`, `auth_account`).
 *
 * - Idempotent : relancable sans doublon (upsert par email).
 * - Les mots de passe ne sont PAS re-haches : le hachage Spring `{bcrypt}...`
 *   est copie tel quel ; le hook de verification retire le prefixe. Les
 *   utilisateurs se reconnectent avec leur mot de passe actuel.
 * - `app_user` n'est ni modifiee ni supprimee : c'est le chemin de rollback.
 * - Echoue si une simulation ou un profil de garage reste rattache a un email
 *   sans compte apres migration.
 *
 * Usage : DATABASE_URL=... npx tsx scripts/migrate-users.ts [--dry-run]
 */
import { randomUUID } from 'node:crypto'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()
const dryRun = process.argv.includes('--dry-run')
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

async function main(): Promise<void> {
  const legacy = await prisma.appUserLegacy.findMany({ orderBy: { id: 'asc' } })
  console.log(`${legacy.length} compte(s) dans app_user${dryRun ? ' (simulation, rien n\'est ecrit)' : ''}`)

  let migrated = 0
  const skipped: string[] = []

  for (const u of legacy) {
    const email = u.email.trim().toLowerCase()
    // Le Java semait un compte « admin » / « admin » : ni un email valide, ni
    // un compte qu'on veut conserver.
    if (!EMAIL.test(email)) {
      skipped.push(`${u.email} (email invalide)`)
      continue
    }
    if (dryRun) {
      migrated++
      continue
    }

    const now = new Date()
    const user = await prisma.user.upsert({
      where: { email },
      update: { role: u.role === 'ADMIN' ? 'ADMIN' : 'USER', plan: u.plan || 'Pro' },
      create: {
        id: randomUUID(),
        email,
        name: u.name || 'Utilisateur',
        emailVerified: u.provider === 'google',
        plan: u.plan || 'Pro',
        role: u.role === 'ADMIN' ? 'ADMIN' : 'USER',
        createdAt: now,
        updatedAt: now,
      },
    })

    const providerId = u.passwordHash ? 'credential' : 'google'
    const existing = await prisma.account.findFirst({ where: { userId: user.id, providerId } })
    if (!existing) {
      await prisma.account.create({
        data: {
          id: randomUUID(),
          userId: user.id,
          providerId,
          // better-auth identifie un compte « credential » par l'id utilisateur.
          accountId: providerId === 'credential' ? user.id : email,
          password: u.passwordHash,
          createdAt: now,
          updatedAt: now,
        },
      })
    }
    migrated++
  }

  console.log(`${migrated} compte(s) migre(s), ${skipped.length} ignore(s)`)
  for (const s of skipped) console.log(`  - ignore : ${s}`)
  if (dryRun) return

  // Verification bloquante : aucune donnee metier orpheline.
  const orphans = await prisma.$queryRaw<{ email: string }[]>`
    SELECT DISTINCT e.email FROM (
      SELECT user_email AS email FROM simulation
      UNION SELECT user_email FROM user_vehicle_profile
    ) e
    LEFT JOIN auth_user u ON u.email = lower(e.email)
    WHERE u.id IS NULL`
  if (orphans.length > 0) {
    console.error(`ECHEC : ${orphans.length} email(s) portent des donnees sans compte :`)
    for (const o of orphans) console.error(`  - ${o.email}`)
    process.exitCode = 1
  } else {
    console.log('Verification OK : toutes les simulations et profils ont un compte.')
  }
}

main()
  .catch((e) => {
    console.error(e)
    process.exitCode = 1
  })
  .finally(() => prisma.$disconnect())
