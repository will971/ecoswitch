import { ExecutionContext, ForbiddenException, UnauthorizedException } from '@nestjs/common'
import { Reflector } from '@nestjs/core'
import { describe, expect, it, vi } from 'vitest'
import { AuthGuard } from './auth.guard'
import type { AuthUser } from './auth.types'
import { IS_PUBLIC_KEY, ROLES_KEY } from './decorators'
import { extractBearer, SessionService } from './session.service'

const USER: AuthUser = { id: 'u1', email: 'a@b.fr', name: 'A', plan: 'Pro', role: 'USER' }
const ADMIN: AuthUser = { ...USER, id: 'u2', role: 'ADMIN' }
const VALID = 'jeton-valide-0123456789'

function setup(meta: { public?: boolean; roles?: string[] }, resolved: AuthUser | null) {
  const reflector = {
    getAllAndOverride: vi.fn((key: string) =>
      key === IS_PUBLIC_KEY ? meta.public : key === ROLES_KEY ? meta.roles : undefined,
    ),
  } as unknown as Reflector
  const sessions = { resolve: vi.fn(async () => resolved) } as unknown as SessionService
  return { guard: new AuthGuard(reflector, sessions), sessions }
}

function ctx(authorization?: string) {
  const request: Record<string, unknown> = { headers: authorization ? { authorization } : {} }
  const context = {
    getHandler: () => null,
    getClass: () => null,
    switchToHttp: () => ({ getRequest: () => request }),
  } as unknown as ExecutionContext
  return { context, request }
}

describe('AuthGuard', () => {
  it('laisse passer une route @Public sans jeton, sans meme interroger la base', async () => {
    const { guard, sessions } = setup({ public: true }, null)
    expect(await guard.canActivate(ctx().context)).toBe(true)
    expect(sessions.resolve).not.toHaveBeenCalled()
  })

  it('refuse une route protegee sans jeton (securise par defaut)', async () => {
    const { guard } = setup({}, USER)
    await expect(guard.canActivate(ctx().context)).rejects.toBeInstanceOf(UnauthorizedException)
  })

  it('refuse un jeton inconnu ou expire', async () => {
    const { guard } = setup({}, null)
    await expect(guard.canActivate(ctx(`Bearer ${VALID}`).context)).rejects.toBeInstanceOf(UnauthorizedException)
  })

  it('attache l utilisateur a la requete', async () => {
    const { guard } = setup({}, USER)
    const { context, request } = ctx(`Bearer ${VALID}`)
    expect(await guard.canActivate(context)).toBe(true)
    expect(request.user).toEqual(USER)
  })

  it('refuse un USER sur une route @Roles(ADMIN)', async () => {
    const { guard } = setup({ roles: ['ADMIN'] }, USER)
    await expect(guard.canActivate(ctx(`Bearer ${VALID}`).context)).rejects.toBeInstanceOf(ForbiddenException)
  })

  it('accepte un ADMIN sur une route @Roles(ADMIN)', async () => {
    const { guard } = setup({ roles: ['ADMIN'] }, ADMIN)
    expect(await guard.canActivate(ctx(`Bearer ${VALID}`).context)).toBe(true)
  })
})

describe('extractBearer', () => {
  it('extrait le jeton', () => expect(extractBearer(`Bearer ${VALID}`)).toBe(VALID))
  it('ignore la casse du schema', () => expect(extractBearer(`bearer ${VALID}`)).toBe(VALID))
  it('rejette les valeurs que le front peut envoyer par erreur', () => {
    expect(extractBearer('Bearer undefined')).toBeNull()
    expect(extractBearer('Bearer null')).toBeNull()
    expect(extractBearer('Bearer court')).toBeNull()
    expect(extractBearer(VALID)).toBeNull()
    expect(extractBearer(undefined)).toBeNull()
  })
})
