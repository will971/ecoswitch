import { describe, expect, it } from 'vitest'
import { DomainError } from '../common/domain-error'
import { extensionOf, mimeFor, safeFolder, uniqueName } from './media.rules'

describe('regles d upload', () => {
  it('deduit le type MIME de l extension, pas du client', () => {
    expect(mimeFor('.png')).toBe('image/png')
    expect(mimeFor('.jpeg')).toBe('image/jpeg')
    expect(mimeFor('.svg')).toBe('image/svg+xml')
  })
  it('refuse les extensions hors liste blanche', () => {
    expect(() => mimeFor('.html')).toThrow(DomainError)
    expect(() => mimeFor('.exe')).toThrow(DomainError)
  })
  it('extension : minuscule, png par defaut', () => {
    expect(extensionOf('Logo.PNG')).toBe('.png')
    expect(extensionOf('sans-extension')).toBe('.png')
    expect(extensionOf(undefined)).toBe('.png')
  })
  it('neutralise les dossiers malicieux', () => {
    expect(safeFolder('../../etc')).toBe('etc')
    expect(safeFolder('brands,models')).toBe('brands')
    expect(safeFolder('')).toBe('general')
    expect(safeFolder(undefined)).toBe('general')
  })
  it('genere 16 caracteres hexadecimaux, comme le Java', () => {
    expect(uniqueName('.png')).toMatch(/^[0-9a-f]{16}\.png$/)
  })
})
