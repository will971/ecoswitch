import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common'
import { Reflector } from '@nestjs/core'
import type { Role } from './auth.types'
import { IS_PUBLIC_KEY, ROLES_KEY } from './decorators'
import { extractBearer, SessionService } from './session.service'

/**
 * Guard global, SECURISE PAR DEFAUT : une route exige une session valide sauf
 * si elle porte @Public(). C'est l'inverse du Java, dont le `permitAll()` par
 * defaut a laisse ouverts le CRUD catalogue et les profils de garage.
 */
@Injectable()
export class AuthGuard implements CanActivate {
  constructor(
    private readonly reflector: Reflector,
    private readonly sessions: SessionService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const targets = [context.getHandler(), context.getClass()]
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, targets)
    if (isPublic) return true

    const request = context.switchToHttp().getRequest()
    const token = extractBearer(request.headers?.authorization)
    const user = token ? await this.sessions.resolve(token) : null
    if (user === null) throw new UnauthorizedException('Non authentifié')
    request.user = user

    const roles = this.reflector.getAllAndOverride<Role[] | undefined>(ROLES_KEY, targets)
    if (roles && roles.length > 0 && !roles.includes(user.role)) {
      throw new ForbiddenException('Accès réservé aux administrateurs.')
    }
    return true
  }
}
