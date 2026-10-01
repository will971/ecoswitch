import { compare, hash } from 'bcryptjs'

/**
 * Hachage compatible avec l'existant Spring.
 *
 * Spring Security utilisait un DelegatingPasswordEncoder : les hachages
 * stockes sont prefixes de l'algorithme, `{bcrypt}$2a$10$...`. On retire ce
 * prefixe a la verification ; les nouveaux comptes sont haches en bcrypt nu.
 * Les comptes migres gardent donc leur mot de passe sans reinitialisation.
 */
const SPRING_BCRYPT_PREFIX = /^\{bcrypt\}/
const BCRYPT_COST = 10

export async function hashPassword(password: string): Promise<string> {
  return hash(password, BCRYPT_COST)
}

export async function verifyPassword(params: { hash: string; password: string }): Promise<boolean> {
  const stored = params.hash.replace(SPRING_BCRYPT_PREFIX, '')
  // Un hachage d'un autre algorithme Spring ({noop}, {pbkdf2}...) n'est pas un
  // bcrypt : on refuse plutot que de laisser bcryptjs lever.
  if (!stored.startsWith('$2')) return false
  return compare(params.password, stored)
}
