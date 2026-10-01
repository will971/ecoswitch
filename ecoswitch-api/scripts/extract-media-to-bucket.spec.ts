import { describe, expect, it } from 'vitest'
import { legacyKey } from './extract-media-to-bucket'

describe('legacyKey', () => {
  it('convertit les URL relatives historiques', () => {
    expect(legacyKey('/uploads/brands/a1b2.png')).toBe('brands/a1b2.png')
    expect(legacyKey('/uploads/a1b2.png')).toBe('general/a1b2.png')
  })
  it('laisse intactes les URL deja absolues ou vides', () => {
    expect(legacyKey('https://cdn.example/brands/a.png')).toBeNull()
    expect(legacyKey('')).toBeNull()
    expect(legacyKey(null)).toBeNull()
  })
  it('refuse les chemins douteux', () => {
    expect(legacyKey('/uploads/../etc/passwd')).toBeNull()
    expect(legacyKey('/uploads/a/b/c.png')).toBeNull()
  })
})
