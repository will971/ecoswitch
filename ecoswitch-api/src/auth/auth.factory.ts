import type { PrismaClient } from '@prisma/client'
import { betterAuth } from 'better-auth'
import { prismaAdapter } from 'better-auth/adapters/prisma'
import { bearer } from 'better-auth/plugins'
import { optionalEnv, requireEnv } from '../common/env'
import { hashPassword, verifyPassword } from './password'

/** Duree de session : 7 jours, comme l'expiration des JWT Java. */
export const SESSION_TTL_SECONDS = 60 * 60 * 24 * 7

export function createAuth(prisma: PrismaClient) {
  const googleClientId = optionalEnv('GOOGLE_CLIENT_ID')

  return betterAuth({
    secret: requireEnv('BETTER_AUTH_SECRET'),
    baseURL: optionalEnv('BETTER_AUTH_URL') ?? 'http://localhost:8080',
    // Le gestionnaire HTTP de better-auth n'est PAS monte : le front n'appelle
    // que la facade /api/v1/auth. On n'expose pas de surface inutile.
    basePath: '/api/auth',
    database: prismaAdapter(prisma, { provider: 'postgresql' }),

    emailAndPassword: {
      enabled: true,
      // Le Java acceptait n'importe quel mot de passe non vide. Les comptes
      // existants restent utilisables (la connexion ne controle pas la
      // longueur) ; seules les nouvelles inscriptions sont concernees.
      minPasswordLength: 8,
      password: { hash: hashPassword, verify: verifyPassword },
    },

    socialProviders: googleClientId
      ? {
          google: {
            clientId: googleClientId,
            // Inutile pour verifier un ID token, requis par le typage.
            clientSecret: optionalEnv('GOOGLE_CLIENT_SECRET') ?? '',
          },
        }
      : {},

    // Le Java rattachait silencieusement une connexion Google a un compte
    // email/mot de passe existant. On conserve ce comportement.
    account: {
      accountLinking: { enabled: true, trustedProviders: ['google'] },
    },

    user: {
      additionalFields: {
        // input: false — ni le plan ni le role ne sont jamais fixes par le client.
        plan: { type: 'string', defaultValue: 'Pro', input: false },
        role: { type: 'string', defaultValue: 'USER', input: false },
      },
    },

    session: { expiresIn: SESSION_TTL_SECONDS },
    plugins: [bearer()],
  })
}

export type Auth = ReturnType<typeof createAuth>
