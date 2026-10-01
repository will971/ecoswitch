import { Injectable, Logger, OnApplicationBootstrap } from '@nestjs/common'
import { Cron } from '@nestjs/schedule'
import { geminiConfigured, geminiGenerate } from '../common/gemini'
import { OpenDataFuelClient } from './open-data.client'

export interface FuelPricesLiveResponse {
  prices: Record<string, number>
  lastUpdatedIso: string
  aiSummary: string
  totalStationsSurveyed: number
  /** `isLive` et non `live` : composante de record Java, Jackson garde le nom. */
  isLive: boolean
}

/**
 * Prix carburants nationaux, rafraichis a 6 h, 12 h et 18 h.
 *
 * Note : l'electricite n'est jamais mise a jour (0,2516 €/kWh fixe), et les
 * hybrides suivent le prix de l'E10 — comportement repris du Java.
 */
@Injectable()
export class FuelPricesService implements OnApplicationBootstrap {
  private readonly logger = new Logger(FuelPricesService.name)

  private prices: Record<string, number> = {
    PETROL: 1.88,
    DIESEL: 1.76,
    ELECTRIC: 0.2516,
    HYBRID: 1.88,
    PLUGIN_HYBRID: 1.88,
    E85: 0.88,
    SP98: 1.96,
  }
  private lastUpdatedIso = new Date().toISOString()
  private aiSummary = 'Tarifs moyens nationaux indicatifs en vigueur en France.'
  private totalStationsSurveyed = 0

  constructor(private readonly openData: OpenDataFuelClient) {}

  /** Premier chargement en tache de fond : le demarrage n'attend pas le reseau. */
  onApplicationBootstrap(): void {
    if (process.env.FUEL_PRICES_SYNC_ON_BOOT === 'false') return
    void this.refresh()
  }

  @Cron('0 0 6,12,18 * * *')
  async refresh(): Promise<void> {
    const result = await this.openData.fetchNationalAverages()
    if (!result.success || Object.keys(result.prices).length === 0) {
      this.logger.warn('Aucun resultat Open Data valide, conservation du cache existant.')
      return
    }

    const p = result.prices
    const next = { ...this.prices }
    if (p.PETROL !== undefined) {
      next.PETROL = p.PETROL
      next.HYBRID = p.PETROL
      next.PLUGIN_HYBRID = p.PETROL
    }
    for (const k of ['DIESEL', 'SP95', 'SP98', 'E85'] as const) {
      if (p[k] !== undefined) next[k] = p[k]
    }
    this.prices = next
    this.totalStationsSurveyed = result.totalStationsSurveyed
    this.lastUpdatedIso = new Date().toISOString()
    this.aiSummary = await this.buildSummary(next.PETROL, next.DIESEL, next.ELECTRIC)
  }

  getLive(): FuelPricesLiveResponse {
    return {
      prices: { ...this.prices },
      lastUpdatedIso: this.lastUpdatedIso,
      aiSummary: this.aiSummary,
      totalStationsSurveyed: this.totalStationsSurveyed,
      isLive: true,
    }
  }

  private async buildSummary(petrol: number, diesel: number, electric: number): Promise<string> {
    const stations = this.totalStationsSurveyed > 0 ? this.totalStationsSurveyed : 7000
    if (!geminiConfigured()) {
      return `Prix moyens observés sur ${stations} stations en France : SP95-E10 à ${petrol.toFixed(2)} €/L, Gazole à ${diesel.toFixed(2)} €/L et Électricité à ${electric.toFixed(4)} €/kWh (Tarif Bleu EDF).`
    }
    const prompt =
      "En tant qu'analyste des mobilités et énergies en France, résume en 2 phrases concises et percutantes " +
      `la situation actuelle du coût des énergies : Essence SP95-E10 = ${petrol.toFixed(2)} €/L, Gazole = ${diesel.toFixed(2)} €/L, Électricité = ${electric.toFixed(4)} €/kWh. ` +
      'Mets en avant le différentiel de coût aux 100 km entre thermique et électrique en France.'
    const ai = await geminiGenerate(prompt)
    return (
      ai?.text ??
      `SP95-E10 : ${petrol.toFixed(2)} €/L | Gazole : ${diesel.toFixed(2)} €/L | Électricité : ${electric.toFixed(4)} €/kWh (Relevé sur ${this.totalStationsSurveyed} stations).`
    )
  }
}
