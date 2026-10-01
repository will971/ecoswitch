import { Injectable } from '@nestjs/common'
import { PrismaService } from '../common/prisma/prisma.service'
import type { AuthUser, Role } from './auth.types'

/**
 * Resolution d'un jeton bearer vers un utilisateur.
 *
 * Le jeton renvoye au front est le `token` d'une ligne de auth_session. On le
 * resout directement en base plutot que de passer par le pipeline HTTP de
 * better-auth : c'est deterministe, testable, et sans cout de plugin a chaque
 * requete. Une session expire 7 jours apres sa creation, comme les JWT Java.
 */
@Injectable()
export class SessionService {
  constructor(private readonly prisma: PrismaService) {}

  async resolve(token: string, now: Date = new Date()): Promise<AuthUser | null> {
    const session = await this.prisma.session.findUnique({
      where: { token },
      include: { user: true },
    })
    if (session === null || session.expiresAt <= now) return null

    const u = session.user
    return {
      id: u.id,
      email: u.email,
      name: u.name,
      plan: u.plan,
      role: (u.role === 'ADMIN' ? 'ADMIN' : 'USER') as Role,
    }
  }
}

/** Extrait le jeton d'un en-tete `Authorization: Bearer <jeton>`. */
export function extractBearer(header: string | string[] | undefined): string | null {
  const value = Array.isArray(header) ? header[0] : header
  if (!value) return null
  const match = /^Bearer\s+(.+)$/i.exec(value.trim())
  if (!match) return null
  const token = match[1].trim()
  // Memes garde-fous que api.js:getToken() cote front.
  if (token.length < 10 || token === 'undefined' || token === 'null') return null
  return token
}
