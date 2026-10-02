import {
  CreateBucketCommand,
  HeadBucketCommand,
  PutBucketPolicyCommand,
  PutObjectCommand,
  S3Client,
} from '@aws-sdk/client-s3'
import { Injectable, Logger, OnApplicationBootstrap, ServiceUnavailableException } from '@nestjs/common'
import { optionalEnv } from '../common/env'

/**
 * Stockage objet compatible S3 : RustFS, integre a la stack Docker (dev et
 * prod), ou un bucket OVH si les variables S3_* le designent. La base ne
 * conserve que des URL absolues.
 *
 * Le bucket doit etre lisible anonymement : le navigateur charge les images
 * sans passer par l'API (via Nginx sous /media/ dans la stack Docker).
 */
@Injectable()
export class StorageService implements OnApplicationBootstrap {
  private readonly logger = new Logger(StorageService.name)
  private readonly bucket = optionalEnv('S3_BUCKET')
  private readonly publicBaseUrl = optionalEnv('S3_PUBLIC_BASE_URL')?.replace(/\/$/, '')
  private readonly client: S3Client | null

  constructor() {
    const endpoint = optionalEnv('S3_ENDPOINT')
    const accessKeyId = optionalEnv('S3_ACCESS_KEY')
    const secretAccessKey = optionalEnv('S3_SECRET_KEY')
    this.client =
      endpoint && accessKeyId && secretAccessKey && this.bucket
        ? new S3Client({
            endpoint,
            region: optionalEnv('S3_REGION') ?? 'gra',
            credentials: { accessKeyId, secretAccessKey },
            // Style chemin : requis par MinIO, accepte par OVH.
            forcePathStyle: optionalEnv('S3_FORCE_PATH_STYLE') !== 'false',
          })
        : null
    if (this.client === null) {
      this.logger.warn('Stockage S3 non configure : les envois d images seront refuses.')
    }
  }

  /**
   * Avec S3_AUTO_CREATE_BUCKET=true (stacks Docker), cree le bucket s'il manque
   * et l'ouvre en lecture anonyme — lecture d'objet uniquement : ni ecriture
   * ni listing. Remplace le conteneur d'init base sur `mc`, dont l'image n'est
   * plus distribuee. A laisser a false pour un bucket gere ailleurs (OVH).
   *
   * Un echec ne bloque pas le demarrage : seul l'envoi d'images en patira.
   */
  async onApplicationBootstrap(): Promise<void> {
    if (this.client === null || !this.bucket || optionalEnv('S3_AUTO_CREATE_BUCKET') !== 'true') return
    try {
      await this.ensureBucket(this.client, this.bucket)
    } catch (e) {
      this.logger.error(`Preparation du bucket ${this.bucket} impossible : ${e instanceof Error ? e.message : e}`)
    }
  }

  private async ensureBucket(client: S3Client, bucket: string): Promise<void> {
    try {
      await client.send(new HeadBucketCommand({ Bucket: bucket }))
    } catch {
      await client.send(new CreateBucketCommand({ Bucket: bucket }))
      this.logger.log(`Bucket ${bucket} cree.`)
    }
    await client.send(
      new PutBucketPolicyCommand({ Bucket: bucket, Policy: JSON.stringify(publicReadPolicy(bucket)) }),
    )
  }

  /** URL publique d'un objet, ou null si aucune base publique n'est configuree. */
  publicUrl(key: string): string | null {
    return this.publicBaseUrl ? `${this.publicBaseUrl}/${key}` : null
  }

  async put(key: string, body: Buffer, contentType: string): Promise<string> {
    if (this.client === null || !this.bucket) {
      throw new ServiceUnavailableException("Le stockage d'images n'est pas configuré.")
    }
    await this.client.send(
      new PutObjectCommand({
        Bucket: this.bucket,
        Key: key,
        Body: body,
        ContentType: contentType,
        CacheControl: 'public, max-age=31536000, immutable',
      }),
    )
    const url = this.publicUrl(key)
    if (url === null) throw new ServiceUnavailableException('S3_PUBLIC_BASE_URL manquant.')
    return url
  }
}

/** Lecture anonyme des objets, et rien d'autre (pas de listing, pas d'ecriture). */
export function publicReadPolicy(bucket: string) {
  return {
    Version: '2012-10-17',
    Statement: [
      {
        Effect: 'Allow',
        Principal: { AWS: ['*'] },
        Action: ['s3:GetObject'],
        Resource: [`arn:aws:s3:::${bucket}/*`],
      },
    ],
  }
}
