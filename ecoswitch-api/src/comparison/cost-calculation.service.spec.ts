import { describe, expect, it } from 'vitest'
import { CostCalculationService } from './cost-calculation.service'
import type { FuelType, Vehicule } from './fuel-type'

/**
 * Portage 1:1 de CostCalculationServiceTest.java.
 *
 * Ces tests documentent l'INTENTION metier (barèmes, falaises, replis) ; la
 * couverture exhaustive est assuree par cost-calculation.golden.spec.ts, qui
 * compare 3 446 vecteurs extraits du Java.
 */

function buildVehicule(
  id: number,
  name: string,
  fuelType: FuelType,
  annualMileage: number,
  consumption: number,
  maintenanceCost: number,
  purchasePrice: number,
  resaleValue: number,
): Vehicule {
  return { id, name, fuelType, annualMileage, consumption, maintenanceCost, purchasePrice, resaleValue }
}

const svc = new CostCalculationService()

describe('CostCalculationService', () => {
  it('trouve le seuil de rentabilite quand la cible devient moins chere', () => {
    const current = buildVehicule(1, 'Current', 'PETROL', 18_000, 7.5, 600, 0, 12_000)
    const target = buildVehicule(2, 'Target', 'ELECTRIC', 18_000, 16.5, 350, 25_000, 0)
    const prices = { PETROL: 1.9, ELECTRIC: 0.23 }

    expect(svc.calculateBreakEvenYearFromPrices(current, target, 10, prices)).toBe(7)
  })

  it('rend null quand la cible ne devient jamais moins chere', () => {
    const current = buildVehicule(1, 'Current', 'PETROL', 12_000, 5.8, 450, 0, 8_000)
    const target = buildVehicule(2, 'Target', 'PETROL', 12_000, 5.6, 430, 30_000, 0)
    const prices = { PETROL: 1.9 }

    expect(svc.calculateBreakEvenYearFromPrices(current, target, 10, prices)).toBeNull()
  })

  it('integre un cout de reparation immediat dans le seuil de rentabilite', () => {
    const current = buildVehicule(1, 'BMW 114i', 'PETROL', 12_000, 6.5, 500, 0, 4_500)
    const target = buildVehicule(2, 'New Hybrid Car', 'HYBRID', 12_000, 4.2, 350, 20_000, 0)
    const prices = { PETROL: 1.9, HYBRID: 1.85 }

    expect(svc.calculateBreakEvenYearFromPrices(current, target, 20, prices, 3000.0)).toBe(18)
  })

  it('pondere le prix de l electricite entre domicile et borne publique', () => {
    const electric = buildVehicule(1, 'Tesla', 'ELECTRIC', 18000, 15.0, 300, 40000, 0)

    // 80 % a 0,25 €/kWh + 20 % a 0,55 €/kWh (tarif borne rapide) = 0,31
    expect(svc.resolveWeightedFuelPrice(electric, 0.25, 0.8)).toBeCloseTo(0.31, 3)
  })

  it('applique le bareme du bonus ecologique', () => {
    const electricCheap = buildVehicule(1, 'Zoe', 'ELECTRIC', 15000, 17.2, 200, 35000, 0)
    const electricExpensive = buildVehicule(2, 'Tesla S', 'ELECTRIC', 15000, 19.0, 400, 85000, 0)
    const petrol = buildVehicule(3, 'Clio', 'PETROL', 15000, 5.5, 300, 20000, 0)

    expect(svc.calculateBonusEcologique(electricCheap, 12000.0)).toBe(7000.0) // RFR <= 15 400
    expect(svc.calculateBonusEcologique(electricCheap, 25000.0)).toBe(4000.0) // RFR standard
    expect(svc.calculateBonusEcologique(electricExpensive, 12000.0)).toBe(0.0) // > 47 000 €
    expect(svc.calculateBonusEcologique(petrol, 12000.0)).toBe(0.0) // non electrique
  })

  it('calcule les emissions de CO2 en phase d usage', () => {
    const petrol = buildVehicule(1, 'Clio', 'PETROL', 10000, 6.0, 0, 0, 0)
    const electric = buildVehicule(2, 'Zoe', 'ELECTRIC', 10000, 15.0, 0, 0, 0)

    expect(svc.calculateCO2EmissionsGPerKm(petrol)).toBeCloseTo(138.0, 3) // 6.0 x 23.0
    expect(svc.calculateCO2EmissionsGPerKm(electric)).toBeCloseTo(7.5, 3) // 15.0 x 0.5
    expect(svc.calculateAnnualCO2Kg(petrol)).toBeCloseTo(1380.0, 3) // 138 g x 10 000 km / 1000
  })

  it('retombe sur le prix de l essence pour un hybride sans tarif propre', () => {
    const hybrid = buildVehicule(4, 'Prius', 'HYBRID', 12000, 4.2, 350, 20000, 0)

    expect(svc.resolveFuelPrice(hybrid, { PETROL: 1.9 })).toBeCloseTo(1.9, 3)
  })

  // ── Cas non couverts par le Java, ajoutes ici ────────────────────────

  it('leve quand aucun prix d energie n est fourni', () => {
    const petrol = buildVehicule(1, 'Clio', 'PETROL', 12000, 5.5, 300, 20000, 0)

    expect(() => svc.resolveFuelPrice(petrol, null)).toThrow("Les prix d'energie sont obligatoires.")
  })

  it('leve quand ni l energie du vehicule ni l essence ne sont tarifees', () => {
    const diesel = buildVehicule(1, 'Golf', 'DIESEL', 12000, 5.5, 300, 20000, 0)

    expect(() => svc.resolveFuelPrice(diesel, { ELECTRIC: 0.25 })).toThrow(
      "Prix d'energie manquant pour: DIESEL",
    )
  })

  it('applique le facteur CO2 specifique aux hybrides rechargeables', () => {
    const phev = buildVehicule(1, 'PHEV', 'PLUGIN_HYBRID', 10000, 6.0, 0, 0, 0)

    // 6.0 x 9.5 — facteur absent de docs/FUNCTIONAL.md mais present dans le code
    expect(svc.calculateCO2EmissionsGPerKm(phev)).toBeCloseTo(57.0, 3)
  })
})
