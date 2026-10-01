import { BadRequestException, Inject, Injectable, Logger, NotFoundException } from '@nestjs/common'
import { CACHE_MANAGER } from '@nestjs/cache-manager'
import type { Cache } from 'cache-manager'
import fallbackPlates from './fallback-plates.json'

export interface PlateVehicle {
  name: string
  fuelType: string
  consumption: number
  annualMileage: number
  maintenanceCost: number
  resaleValue: number
  purchasePrice?: number
  source: 'OSCARO' | 'LOCAL_FALLBACK'
}

const CACHE_TTL_MS = 24 * 60 * 60 * 1000
const OSCARO_URL = 'https://www.oscaro.com/catalog/vehicles/by_registration?registration='

/** Valeurs par defaut selon le carburant annonce par Oscaro (repris du Java). */
const PROFILE_BY_FUEL = {
  ELECTRIC: { consumption: 16.5, maintenanceCost: 200, resaleValue: 15000 },
  HYBRID: { consumption: 4.4, maintenanceCost: 320, resaleValue: 12000 },
  DIESEL: { consumption: 4.9, maintenanceCost: 450, resaleValue: 8000 },
  PETROL: { consumption: 6.2, maintenanceCost: 400, resaleValue: 6000 },
} as const

/**
 * Recherche d'un vehicule par plaque d'immatriculation.
 *
 * ⚠️ Oscaro est interroge par scraping, sans convention, avec un User-Agent de
 * navigateur. C'est fragile (un changement de leur page casse la fonction) et
 * juridiquement discutable en usage commercial. Le dictionnaire local sert de
 * repli. A remplacer par l'API du SIV ou un fournisseur sous contrat.
 */
@Injectable()
export class ImmatriculationService {
  private readonly logger = new Logger(ImmatriculationService.name)
  private readonly fallback = new Map<string, Record<string, unknown>>(
    Object.entries(fallbackPlates as Record<string, Record<string, unknown>>).map(([k, v]) => [clean(k), v]),
  )

  constructor(@Inject(CACHE_MANAGER) private readonly cache: Cache) {}

  async lookup(plaque: string): Promise<PlateVehicle> {
    if (!plaque || plaque.trim() === '') {
      throw new BadRequestException('Le numero de plaque est manquant.')
    }
    const plate = clean(plaque)
    // Garde-fou absent du Java : pas d'appel sortant pour une saisie qui ne
    // peut pas etre une plaque (SIV « AB123CD » ou FNI « 1234AB56 »).
    if (!/^[A-Z0-9]{5,10}$/.test(plate)) {
      throw new BadRequestException('Format de plaque invalide.')
    }

    const key = `immatriculation:${plate}`
    const cached = await this.cache.get<PlateVehicle>(key)
    if (cached) return cached

    const found = (await this.fromOscaro(plate)) ?? this.fromFallback(plate)
    if (found === null) {
      throw new NotFoundException(
        "Plaque d'immatriculation introuvable. Veuillez saisir manuellement les informations.",
      )
    }
    await this.cache.set(key, found, CACHE_TTL_MS)
    return found
  }

  private async fromOscaro(plate: string): Promise<PlateVehicle | null> {
    try {
      const res = await fetch(OSCARO_URL + encodeURIComponent(plate), {
        headers: {
          'User-Agent':
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          Accept: 'application/json',
          Referer: 'https://www.oscaro.com/',
          'Accept-Language': 'fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7',
        },
        signal: AbortSignal.timeout(3_000),
      })
      if (!res.ok) {
        this.logger.warn(`Oscaro : HTTP ${res.status}, bascule sur le dictionnaire local`)
        return null
      }
      const vehicles = (await res.json()) as Record<string, unknown>[]
      const v = Array.isArray(vehicles) ? vehicles[0] : undefined
      if (!v) return null

      const name = [v.make ?? 'Marque inconnue', v.model ?? 'Modele inconnu', v.version ?? '']
        .map(String)
        .join(' ')
        .trim()
        .replace(/ +/g, ' ')
      const fuelType = mapFuel(typeof v.fuel === 'string' ? v.fuel : null)
      return { name, fuelType, annualMileage: 15000, ...PROFILE_BY_FUEL[fuelType], source: 'OSCARO' }
    } catch (e) {
      this.logger.warn(`Oscaro injoignable (${e instanceof Error ? e.message : e}), bascule sur le dictionnaire local`)
      return null
    }
  }

  private fromFallback(plate: string): PlateVehicle | null {
    const entry = this.fallback.get(plate)
    return entry ? ({ ...entry, source: 'LOCAL_FALLBACK' } as unknown as PlateVehicle) : null
  }
}

export function clean(plaque: string): string {
  return plaque.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
}

export function mapFuel(fuel: string | null): keyof typeof PROFILE_BY_FUEL {
  const f = (fuel ?? '').toLowerCase()
  if (f.includes('elec') || f.includes('volt')) return 'ELECTRIC'
  if (f.includes('hybr')) return 'HYBRID'
  if (f.includes('dies') || f.includes('gazo')) return 'DIESEL'
  return 'PETROL'
}
