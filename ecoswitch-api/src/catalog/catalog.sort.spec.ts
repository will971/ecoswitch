import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { caseInsensitiveCompare } from './catalog.sort'

/**
 * Attendus extraits de String.CASE_INSENSITIVE_ORDER par SortGoldenGenerator.java.
 * On valide a la fois l'ordre trie et la matrice des comparaisons deux a deux,
 * cette derniere etant plus discriminante qu'un simple tri.
 */
const golden = JSON.parse(
  readFileSync(join(__dirname, '..', '..', 'test', 'golden', 'case-insensitive-order.golden.json'), 'utf8'),
)

describe('caseInsensitiveCompare — parite avec String.CASE_INSENSITIVE_ORDER', () => {
  it.each(golden.cases.map((c: { input: string[] }, i: number) => [i, c]))(
    'cas %i reproduit l ordre du Java',
    (_i, c: { input: string[]; sorted: string[] }) => {
      expect([...c.input].sort(caseInsensitiveCompare)).toEqual(c.sorted)
    },
  )

  it('reproduit chaque comparaison deux a deux', () => {
    const mismatches: string[] = []
    for (const c of golden.cases) {
      for (const p of c.pairs) {
        const actual = Math.sign(caseInsensitiveCompare(p.a, p.b))
        if (actual !== p.sign) {
          mismatches.push(`compare(${JSON.stringify(p.a)}, ${JSON.stringify(p.b)}): attendu ${p.sign}, obtenu ${actual}`)
        }
      }
    }
    expect(mismatches).toEqual([])
  })

  it('ne se comporte pas comme localeCompare', () => {
    // Garde-fou explicite : si quelqu'un remplace l'implementation par
    // localeCompare, ce test tombe.
    expect(caseInsensitiveCompare('Skoda', 'Škoda')).toBeLessThan(0)
    expect('Škoda'.localeCompare('Skoda', 'fr')).toBeGreaterThan(0)
    expect(caseInsensitiveCompare('2008', '208')).toBeLessThan(0)
  })
})
