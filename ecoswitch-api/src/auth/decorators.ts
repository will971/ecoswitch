import { createParamDecorator, ExecutionContext, SetMetadata } from '@nestjs/common'
import type { AuthUser, Role } from './auth.types'

export const IS_PUBLIC_KEY = 'isPublic'
export const ROLES_KEY = 'roles'

/** Route accessible sans authentification. Tout le reste est protege. */
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true)

/** Route reservee a certains roles (implique l'authentification). */
export const Roles = (...roles: Role[]) => SetMetadata(ROLES_KEY, roles)

/** Injecte l'utilisateur authentifie. */
export const CurrentUser = createParamDecorator((_data: unknown, ctx: ExecutionContext): AuthUser => {
  return ctx.switchToHttp().getRequest().user
})
