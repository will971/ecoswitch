import { isFuelType, type FuelType, type Vehicule } from './fuel-type'

/**
 * Normalise un `Vehicule` recu en JSON comme le faisait Jackson : un champ
 * numerique absent vaut 0 (primitif Java), une chaine numerique est convertie.
 * Sans cela, un champ manquant propagerait NaN dans tous les calculs.
 *
 * Un carburant invalide devient null : la validation du service renvoie alors
 * le message d'erreur du Java plutot qu'un resultat absurde.
 */
export function toVehiculeInput(raw: unknown): Vehicule | null {
  if (raw === null || typeof raw !== 'object') return null
  const v = raw as Record<string, unknown>
  const num = (x: unknown) => {
    const n = typeof x === 'string' ? Number(x) : x
    return typeof n === 'number' && Number.isFinite(n) ? n : 0
  }
  return {
    id: typeof v.id === 'number' ? v.id : null,
    name: typeof v.name === 'string' ? v.name : '',
    brand: typeof v.brand === 'string' ? v.brand : null,
    model: typeof v.model === 'string' ? v.model : null,
    version: typeof v.version === 'string' ? v.version : null,
    fuelType: (isFuelType(v.fuelType) ? v.fuelType : null) as FuelType,
    consumption: num(v.consumption),
    annualMileage: Math.trunc(num(v.annualMileage)),
    maintenanceCost: num(v.maintenanceCost),
    purchasePrice: num(v.purchasePrice),
    resaleValue: num(v.resaleValue),
  }
}

export function toPrices(raw: unknown): Record<string, number> | null {
  if (raw === null || typeof raw !== 'object') return null
  const out: Record<string, number> = {}
  for (const [k, x] of Object.entries(raw as Record<string, unknown>)) {
    const n = typeof x === 'string' ? Number(x) : x
    if (typeof n === 'number' && Number.isFinite(n)) out[k] = n
  }
  return out
}

export const optNum = (x: unknown): number | null => {
  const n = typeof x === 'string' && x.trim() !== '' ? Number(x) : x
  return typeof n === 'number' && Number.isFinite(n) ? n : null
}

export const optBool = (x: unknown): boolean | null => (typeof x === 'boolean' ? x : null)
