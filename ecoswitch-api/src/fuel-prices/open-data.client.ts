import { Injectable, Logger } from '@nestjs/common'
import { optionalEnv } from '../common/env'

const DEFAULT_URL =
  'https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records'

/**
 * Moyennes nationales calculees cote serveur Open Data (une seule ligne
 * agregee), en excluant les prix aberrants. Requete identique a
 * FuelPriceOpenDataDao.java.
 */
const QUERY =
  '?where=gazole_prix%20%3E%201.0%20AND%20gazole_prix%20%3C%202.8%20AND%20e10_prix%20%3E%201.0%20AND%20e10_prix%20%3C%202.8' +
  '&select=avg(gazole_prix)%20as%20avg_gazole,%20avg(e10_prix)%20as%20avg_e10,%20avg(sp95_prix)%20as%20avg_sp95,%20avg(sp98_prix)%20as%20avg_sp98,%20avg(e85_prix)%20as%20avg_e85,%20count(id)%20as%20total_stations' +
  '&limit=1'

export interface OpenDataResult {
  prices: Record<string, number>
  totalStationsSurveyed: number
  success: boolean
}

/** Equivalent de Math.round(x * 100.0) / 100.0. */
const round2 = (x: number) => Math.round(x * 100) / 100

/** Reproduit JsonNode.asDouble(defaut) : null ou absent -> defaut. */
function num(v: unknown, fallback: number): number {
  return typeof v === 'number' && Number.isFinite(v) ? v : fallback
}

@Injectable()
export class OpenDataFuelClient {
  private readonly logger = new Logger(OpenDataFuelClient.name)
  private readonly url = optionalEnv('OPEN_DATA_FUEL_URL') ?? DEFAULT_URL

  async fetchNationalAverages(): Promise<OpenDataResult> {
    try {
      const res = await fetch(this.url + QUERY, {
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(12_000),
      })
      if (!res.ok) {
        this.logger.warn(`Open Data : HTTP ${res.status}`)
        return failed()
      }
      const json = (await res.json()) as { results?: Record<string, unknown>[] }
      const r = json.results?.[0]
      if (!r) return failed()

      const avgE10 = num(r.avg_e10, 2.04)
      const prices = {
        PETROL: round2(avgE10),
        DIESEL: round2(num(r.avg_gazole, 2.21)),
        SP95: round2(num(r.avg_sp95, avgE10 + 0.05)),
        SP98: round2(num(r.avg_sp98, avgE10 + 0.12)),
        E85: round2(num(r.avg_e85, 0.88)),
      }
      const totalStationsSurveyed = Math.trunc(num(r.total_stations, 7000))
      this.logger.log(`${totalStationsSurveyed} stations sondees (E10=${prices.PETROL}, Gazole=${prices.DIESEL})`)
      return { prices, totalStationsSurveyed, success: true }
    } catch (e) {
      this.logger.warn(`Echec de recuperation : ${e instanceof Error ? e.message : String(e)}`)
      return failed()
    }
  }
}

function failed(): OpenDataResult {
  return { prices: {}, totalStationsSurveyed: 0, success: false }
}
