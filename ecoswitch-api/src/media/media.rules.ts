import { randomBytes } from 'node:crypto'
import { DomainError } from '../common/domain-error'

export const MAX_UPLOAD_BYTES = Number(process.env.MAX_UPLOAD_MB ?? 5) * 1024 * 1024

/**
 * Type MIME deduit de l'extension, JAMAIS du client : le Java stockait le
 * Content-Type annonce par le navigateur, qu'un attaquant controle.
 */
const MIME_BY_EXT: Record<string, string> = {
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
}

export function extensionOf(originalName: string | undefined): string {
  if (!originalName || !originalName.includes('.')) return '.png'
  return originalName.slice(originalName.lastIndexOf('.')).toLowerCase()
}

export function mimeFor(ext: string): string {
  const mime = MIME_BY_EXT[ext]
  if (!mime) {
    throw new DomainError('Format de fichier non supporté. Formats autorisés : png, jpg, jpeg, svg, webp, gif.')
  }
  return mime
}

/** Dossier nettoye comme le Java : premier element, [a-zA-Z0-9_-] seulement. */
export function safeFolder(folder: string | undefined): string {
  const first = (folder ?? '').split(',')[0].trim()
  const clean = first.replace(/[^a-zA-Z0-9_-]/g, '')
  return clean || 'general'
}

export function uniqueName(ext: string): string {
  return randomBytes(8).toString('hex') + ext
}
