import { PutObjectCommand, S3Client } from '@aws-sdk/client-s3'
import { Injectable, Logger, ServiceUnavailableException } from '@nestjs/common'
import { optionalEnv } from '../common/env'

/**
 * Stockage objet compatible S3 : OVH Object Storage en production, MinIO en
 * developpement. La base ne conserve que des URL absolues.
 *
 * Le bucket doit etre lisible publiquement (politique de bucket) : le
 * navigateur charge les images directement, sans passer par l'API.
 */
@Injectable()
export class StorageService {
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
