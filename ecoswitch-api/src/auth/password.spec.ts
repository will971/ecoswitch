import { hashSync } from 'bcryptjs'
import { describe, expect, it } from 'vitest'
import { hashPassword, verifyPassword } from './password'

describe('compatibilite des mots de passe Spring', () => {
  it('verifie un hachage Spring prefixe {bcrypt}', async () => {
    const spring = `{bcrypt}${hashSync('motdepasse', 10)}`
    expect(await verifyPassword({ hash: spring, password: 'motdepasse' })).toBe(true)
    expect(await verifyPassword({ hash: spring, password: 'mauvais' })).toBe(false)
  })

  it('verifie un hachage $2a$, la variante emise par Spring BCryptPasswordEncoder', async () => {
    // bcryptjs n'emet que du $2b$. Pour un mot de passe ASCII de moins de 72
    // octets, $2a$ et $2b$ sont le meme calcul : on reconstitue donc exactement
    // la forme stockee par Spring.
    const spring2a = `{bcrypt}${hashSync('admin', 10).replace(/^\$2b\$/, '$2a$')}`
    expect(spring2a.startsWith('{bcrypt}$2a$10$')).toBe(true)
    expect(await verifyPassword({ hash: spring2a, password: 'admin' })).toBe(true)
    expect(await verifyPassword({ hash: spring2a, password: 'Admin' })).toBe(false)
  })

  it('verifie un hachage cree par le NestJS', async () => {
    const h = await hashPassword('nouveau-compte')
    expect(h.startsWith('$2')).toBe(true)
    expect(await verifyPassword({ hash: h, password: 'nouveau-compte' })).toBe(true)
  })

  it('refuse les algorithmes non bcrypt au lieu de lever', async () => {
    expect(await verifyPassword({ hash: '{noop}admin', password: 'admin' })).toBe(false)
    expect(await verifyPassword({ hash: '{pbkdf2}abc', password: 'abc' })).toBe(false)
  })
})
