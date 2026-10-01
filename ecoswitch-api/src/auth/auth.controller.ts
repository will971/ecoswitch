import { Body, Controller, Get, HttpCode, Post } from '@nestjs/common'
import { Throttle } from '@nestjs/throttler'
import { AuthService } from './auth.service'
import type { AuthUser } from './auth.types'
import { CurrentUser, Public } from './decorators'

/**
 * Contrat historique consomme par api.js : memes routes, memes corps, memes
 * codes. Le jeton renvoye est stocke cote front sous `saas_token` et renvoye
 * en `Authorization: Bearer`.
 *
 * Limite a 10 tentatives par minute et par IP : le Java n'avait aucune
 * protection contre le bourrage d'identifiants.
 */
@Controller('api/v1/auth')
@Throttle({ default: { limit: 10, ttl: 60_000 } })
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  @Public()
  @Post('register')
  @HttpCode(201)
  register(@Body() body: { email?: string; password?: string; name?: string }) {
    return this.auth.register(body?.email, body?.name, body?.password)
  }

  @Public()
  @Post('login')
  @HttpCode(200)
  login(@Body() body: { email?: string; password?: string }) {
    return this.auth.login(body?.email, body?.password)
  }

  @Public()
  @Post('google-login')
  @HttpCode(200)
  googleLogin(@Body() body: { credential?: string }) {
    return this.auth.googleLogin(body?.credential)
  }

  /** 401 si la session est absente ou expiree : le front deconnecte alors l'utilisateur. */
  @Get('me')
  me(@CurrentUser() user: AuthUser) {
    return { name: user.name, email: user.email, plan: user.plan, role: user.role }
  }
}
