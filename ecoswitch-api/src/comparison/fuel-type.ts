/** Miroir de FuelType.java — valeurs persistees telles quelles en varchar(30). */
export const FUEL_TYPES = ['PETROL', 'DIESEL', 'HYBRID', 'PLUGIN_HYBRID', 'ELECTRIC'] as const

export type FuelType = (typeof FUEL_TYPES)[number]

export function isFuelType(value: unknown): value is FuelType {
  return typeof value === 'string' && (FUEL_TYPES as readonly string[]).includes(value)
}

/**
 * Forme de DTO heritee de Vehicule.java. La table `vehicule` n'est pas portee
 * (le catalogue structure la remplace), mais le type reste le vocabulaire des
 * endpoints /profitability/*.
 */
export interface Vehicule {
  id?: number | null
  name: string
  brand?: string | null
  model?: string | null
  generation?: string | null
  version?: string | null
  purchasePrice: number
  fuelType: FuelType
  /** L/100km pour thermique/hybride, kWh/100km pour electrique. */
  consumption: number
  annualMileage: number
  maintenanceCost: number
  resaleValue: number
  url?: string | null
}

export type FuelPricesByType = Record<string, number>
