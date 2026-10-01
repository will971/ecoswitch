/**
 * Sort les images de la base (`media_files`) vers le bucket S3 (OVH / MinIO)
 * et reecrit les URL du catalogue.
 *
 * 1. Televerse chaque ligne de media_files sous la cle `<dossier>/<fichier>`
 * 2. Reecrit brands.logo_url, vehicle_models.image_url, finitions.image_url :
 *    `/uploads/<dossier>/<fichier>`  ->  `<S3_PUBLIC_BASE_URL>/<dossier>/<fichier>`
 * 3. Verifie qu'il ne reste aucune URL `/uploads/` dans le catalogue
 *
 * La table media_files n'est PAS supprimee : la route GET /uploads/* continue
 * de servir les anciennes URL presentes dans les simulations sauvegardees
 * (blob JSON jamais reecrit). La supprimer est une decision ulterieure.
 *
 * Idempotent : un objet deja present est ecrase a l'identique.
 *
 * Usage : DATABASE_URL=... S3_*=... npx tsx scripts/extract-media-to-bucket.ts [--dry-run]
 */
import { PutObjectCommand, S3Client } from '@aws-sdk/client-s3'
import { PrismaClient } from '@prisma/client'

const prisma = new PrismaClient()
const dryRun = process.argv.includes('--dry-run')

function env(name: string): string {
  const v = process.env[name]
  if (!v) throw new Error(`Variable manquante : ${name}`)
  return v
}

/** `/uploads/brands/a.png` ou `/uploads/a.png` -> cle de bucket, sinon null. */
export function legacyKey(url: string | null): string | null {
  if (!url) return null
  const m = /^\/uploads\/(?:([A-Za-z0-9_-]+)\/)?([^/]+)$/.exec(url)
  if (!m) return null
  return `${m[1] ?? 'general'}/${m[2]}`
}

async function main(): Promise<void> {
  const base = env('S3_PUBLIC_BASE_URL').replace(/\/$/, '')
  const bucket = env('S3_BUCKET')
  const s3 = new S3Client({
    endpoint: env('S3_ENDPOINT'),
    region: process.env.S3_REGION ?? 'gra',
    credentials: { accessKeyId: env('S3_ACCESS_KEY'), secretAccessKey: env('S3_SECRET_KEY') },
    forcePathStyle: process.env.S3_FORCE_PATH_STYLE !== 'false',
  })

  // 1. Televersement, une image a la fois : evite de charger toute la table en memoire.
  const ids = await prisma.mediaFile.findMany({ select: { id: true }, orderBy: { id: 'asc' } })
  console.log(`${ids.length} image(s) dans media_files${dryRun ? ' (simulation)' : ''}`)
  let bytes = 0
  for (const { id } of ids) {
    const m = await prisma.mediaFile.findUniqueOrThrow({ where: { id } })
    bytes += m.data.length
    if (dryRun) continue
    await s3.send(
      new PutObjectCommand({
        Bucket: bucket,
        Key: `${m.folder}/${m.fileName}`,
        Body: Buffer.from(m.data),
        ContentType: m.contentType,
        CacheControl: 'public, max-age=31536000, immutable',
      }),
    )
  }
  console.log(`${(bytes / 1024).toFixed(0)} Ko ${dryRun ? 'a televerser' : 'televerses'}`)

  // 2. Reecriture des URL du catalogue.
  const rewrite = async (label: string, rows: { id: bigint; url: string | null }[], save: (id: bigint, url: string) => Promise<unknown>) => {
    let n = 0
    for (const r of rows) {
      const key = legacyKey(r.url)
      if (key === null) continue
      n++
      if (!dryRun) await save(r.id, `${base}/${key}`)
    }
    console.log(`${label} : ${n} URL ${dryRun ? 'a reecrire' : 'reecrite(s)'}`)
  }

  await rewrite(
    'brands.logo_url',
    (await prisma.brand.findMany()).map((b) => ({ id: b.id, url: b.logoUrl })),
    (id, url) => prisma.brand.update({ where: { id }, data: { logoUrl: url } }),
  )
  await rewrite(
    'vehicle_models.image_url',
    (await prisma.vehicleModel.findMany()).map((m) => ({ id: m.id, url: m.imageUrl })),
    (id, url) => prisma.vehicleModel.update({ where: { id }, data: { imageUrl: url } }),
  )
  await rewrite(
    'finitions.image_url',
    (await prisma.finition.findMany()).map((f) => ({ id: f.id, url: f.imageUrl })),
    (id, url) => prisma.finition.update({ where: { id }, data: { imageUrl: url } }),
  )
  if (dryRun) return

  // 3. Verification bloquante.
  const [{ remaining }] = await prisma.$queryRaw<{ remaining: number }[]>`
    SELECT (
      (SELECT count(*) FROM brands WHERE logo_url LIKE '/uploads/%') +
      (SELECT count(*) FROM vehicle_models WHERE image_url LIKE '/uploads/%') +
      (SELECT count(*) FROM finitions WHERE image_url LIKE '/uploads/%')
    )::int AS remaining`
  if (remaining > 0) {
    console.error(`ECHEC : ${remaining} URL /uploads/ restante(s) dans le catalogue.`)
    process.exitCode = 1
  } else {
    console.log('Verification OK : le catalogue ne reference plus que le bucket.')
  }
}

if (require.main === module) {
  main()
    .catch((e) => {
      console.error(e)
      process.exitCode = 1
    })
    .finally(() => prisma.$disconnect())
}
