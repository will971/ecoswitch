import { BadRequestException, NotFoundException } from '@nestjs/common'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clean, ImmatriculationService, mapFuel } from './immatriculation.service'

function memoryCache() {
  const m = new Map<string, unknown>()
  return { get: async (k: string) => m.get(k), set: async (k: string, v: unknown) => void m.set(k, v) }
}

describe('ImmatriculationService', () => {
  let svc: ImmatriculationService
  beforeEach(() => {
    svc = new ImmatriculationService(memoryCache() as never)
  })
  afterEach(() => vi.unstubAllGlobals())

  it('retombe sur le dictionnaire local quand Oscaro est injoignable — jamais d appel reel en test', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new Error('reseau coupe') }))
    const v = await svc.lookup('AB-123-CD')
    expect(v.name).toBe('Peugeot 208 II 1.2 PureTech 100 (2020)')
    expect(v.source).toBe('LOCAL_FALLBACK')
  })

  it('normalise une reponse Oscaro', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify([
      { make: 'RENAULT', model: 'ZOE', version: 'R110  Life', fuel: 'Electrique' },
    ]))))
    const v = await svc.lookup('XY 456 ZT')
    expect(v).toMatchObject({ name: 'RENAULT ZOE R110 Life', fuelType: 'ELECTRIC', consumption: 16.5, source: 'OSCARO' })
  })

  it('met en cache : le second appel ne sort pas', async () => {
    const fetchMock = vi.fn(async () => { throw new Error('reseau coupe') })
    vi.stubGlobal('fetch', fetchMock)
    await svc.lookup('GG-555-EL')
    await svc.lookup('gg 555 el')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('404 quand la plaque est inconnue partout', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('[]')))
    await expect(svc.lookup('ZZ-999-ZZ')).rejects.toBeInstanceOf(NotFoundException)
  })

  it('400 sans appel sortant pour une saisie qui ne peut pas etre une plaque', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    await expect(svc.lookup('ab')).rejects.toBeInstanceOf(BadRequestException)
    await expect(svc.lookup('   ')).rejects.toBeInstanceOf(BadRequestException)
    expect(fetchMock).not.toHaveBeenCalled()
  })
})

describe('utilitaires', () => {
  it('nettoie les formats SIV et FNI', () => {
    expect(clean('ab-123-cd')).toBe('AB123CD')
    expect(clean('1234 AB 56')).toBe('1234AB56')
  })
  it('mappe les libelles de carburant Oscaro', () => {
    expect(mapFuel('Gazole')).toBe('DIESEL')
    expect(mapFuel('Hybride essence')).toBe('HYBRID')
    expect(mapFuel('Essence')).toBe('PETROL')
    expect(mapFuel(null)).toBe('PETROL')
  })
})
