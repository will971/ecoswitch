import {
  BadRequestException,
  ConflictException,
  Injectable,
  Logger,
  OnApplicationBootstrap,
  UnauthorizedException,
} from '@nestjs/common'
import { emailListEnv } from '../common/env'
import { PrismaService } from '../common/prisma/prisma.service'
import { Auth, createAuth } from './auth.factory'
import type { AuthResponse, Role } from './auth.types'

/**
 * Facade entre le contrat historique /api/v1/auth et better-auth.
 *
 * Messages et codes HTTP reprennent ceux de AuthController.java : le front les
 * affiche tels quels (`data.error`).
 */
@Injectable()
export class AuthService implements OnApplicationBootstrap {
  private readonly logger = new Logger(AuthService.name)
  private readonly auth: Auth
  /** Remplace la liste d'emails codee en dur cote Java (UserService.isAdminEmail). */
  private readonly bootstrapAdmins = emailListEnv('ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS')

  constructor(private readonly prisma: PrismaService) {
    this.auth = createAuth(prisma)
  }

  async onApplicationBootstrap(): Promise<void> {
    if (this.bootstrapAdmins.length === 0) return
    const { count } = await this.prisma.user.updateMany({
      where: { email: { in: this.bootstrapAdmins }, role: { not: 'ADMIN' } },
      data: { role: 'ADMIN' },
    })
    if (count > 0) this.logger.log(`${count} compte(s) promu(s) ADMIN via ECOSWITCH_BOOTSTRAP_ADMIN_EMAILS`)
  }

  async register(email?: string, name?: string, password?: string): Promise<AuthResponse> {
    const normalized = this.requireCredentials(email, password)
    try {
      const res = await this.auth.api.signUpEmail({
        body: { email: normalized, password: password!, name: name?.trim() || 'Utilisateur' },
      })
      return this.respond(res.token, res.user.id)
    } catch (e) {
      const code = errorCode(e)
      if (code === 'USER_ALREADY_EXISTS' || code === 'USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL') {
        throw new ConflictException('Un compte existe déjà avec cet email.')
      }
      if (code === 'PASSWORD_TOO_SHORT') {
        throw new BadRequestException('Le mot de passe doit contenir au moins 8 caractères.')
      }
      if (code === 'PASSWORD_TOO_LONG') {
        throw new BadRequestException('Le mot de passe est trop long.')
      }
      if (code === 'INVALID_EMAIL' || code === 'VALIDATION_ERROR') {
        throw new BadRequestException('Adresse email invalide.')
      }
      throw e
    }
  }

  async login(email?: string, password?: string): Promise<AuthResponse> {
    const normalized = this.requireCredentials(email, password)
    try {
      const res = await this.auth.api.signInEmail({ body: { email: normalized, password: password! } })
      return this.respond(res.token, res.user.id)
    } catch (e) {
      // Toute erreur d'identification donne le meme message, pour ne pas
      // reveler si l'email existe — comme le Java.
      if (errorCode(e) !== null) throw new UnauthorizedException('Email ou mot de passe incorrect.')
      throw e
    }
  }

  async googleLogin(credential?: string): Promise<AuthResponse> {
    if (!credential || credential.trim() === '') {
      throw new BadRequestException('Le jeton Google (credential) est manquant.')
    }
    try {
      // better-auth verifie la signature ET l'audience (client_id) du jeton —
      // le Java se contentait de journaliser une audience incorrecte.
      const res = await this.auth.api.signInSocial({
        body: { provider: 'google', idToken: { token: credential } },
      })
      if (!('token' in res) || !res.token || !('user' in res)) {
        throw new UnauthorizedException('Authentification Google échouée.')
      }
      return this.respond(res.token, res.user.id)
    } catch (e) {
      if (e instanceof UnauthorizedException) throw e
      if (errorCode(e) !== null) throw new UnauthorizedException('Authentification Google échouée.')
      this.logger.error('Erreur validation SSO Google', e instanceof Error ? e.stack : String(e))
      throw e
    }
  }

  private requireCredentials(email?: string, password?: string): string {
    if (!email || email.trim() === '' || !password || password.trim() === '') {
      throw new BadRequestException('Email et mot de passe obligatoires.')
    }
    return email.trim().toLowerCase()
  }

  /** Relit l'utilisateur pour renvoyer role et plan a jour (apres promotion eventuelle). */
  private async respond(token: string | null | undefined, userId: string): Promise<AuthResponse> {
    if (!token) throw new UnauthorizedException('Session non créée.')
    let user = await this.prisma.user.findUniqueOrThrow({ where: { id: userId } })
    if (this.bootstrapAdmins.includes(user.email) && user.role !== 'ADMIN') {
      user = await this.prisma.user.update({ where: { id: userId }, data: { role: 'ADMIN' } })
    }
    return {
      token,
      name: user.name,
      email: user.email,
      plan: user.plan,
      role: (user.role === 'ADMIN' ? 'ADMIN' : 'USER') as Role,
    }
  }
}

/** Code d'erreur better-auth (APIError), ou null si l'erreur n'en vient pas. */
function errorCode(e: unknown): string | null {
  if (typeof e !== 'object' || e === null) return null
  const body = (e as { body?: { code?: unknown } }).body
  return typeof body?.code === 'string' ? body.code : null
}
