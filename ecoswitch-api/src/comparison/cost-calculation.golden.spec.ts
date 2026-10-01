import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { CostCalculationService } from './cost-calculation.service'
import type { FuelPricesByType, FuelType, Vehicule } from './fuel-type'

/**
 * Parite stricte avec l'implementation Java.
 *
 * Les attendus proviennent de test/golden/*.json, produits par
 * GoldenVectorGenerator.java execute contre le code Spring Boot en place.
 *
 * Les doubles y sont serialises via Double.toString(). ATTENTION : ce n'est PAS
 * la meme chaine que String(n) en JS — Java passe en notation scientifique sous
 * 1e-3 et au-dela de 1e7, JS seulement sous 1e-6 et au-dela de 1e21. Ainsi
 * Double.toString(0.0005) rend "5.0E-4" quand String(0.0005) rend "0.0005".
 *
 * On compare donc les NOMBRES, pas les chaines : les deux representations se
 * parsent vers le meme binary64 (Double.toString garantit l'aller-retour), et
 * Object.is reste une egalite au bit pres — aucune tolerance n'est introduite.
 */

const GOLDEN_DIR = join(__dirname, '..', '..', 'test', 'golden')

interface GoldenVehicle {
  name: string
  fuelType: string
  annualMileage: number
  consumption: string
  maintenanceCost: string
  purchasePrice: string
  resaleValue: string
}

type Expected = string | number | null | { error: string }

const PRICE_SETS: Record<string, FuelPricesByType> = {
  full: { PETROL: 1.9, DIESEL: 1.74, ELECTRIC: 0.23, HYBRID: 1.85, PLUGIN_HYBRID: 1.88 },
  noHybrid: { PETROL: 2.04, DIESEL: 1.8, ELECTRIC: 0.2516 },
  electricOnly: { ELECTRIC: 0.25 },
}

function toVehicule(g: GoldenVehicle): Vehicule {
  return {
    id: 1,
    name: g.name,
    fuelType: g.fuelType as FuelType,
    annualMileage: g.annualMileage,
    consumption: Number(g.consumption),
    maintenanceCost: Number(g.maintenanceCost),
    purchasePrice: Number(g.purchasePrice),
    resaleValue: Number(g.resaleValue),
  }
}

/** Valeur comparable : un nombre, un entier/null, ou une erreur. */
type Captured = { kind: 'num'; value: number } | { kind: 'int'; value: number | null } | { kind: 'err'; message: string }

function capture(fn: () => number): Captured {
  try {
    return { kind: 'num', value: fn() }
  } catch (e) {
    return { kind: 'err', message: e instanceof Error ? e.message : String(e) }
  }
}

function captureInt(fn: () => number | null): Captured {
  try {
    return { kind: 'int', value: fn() }
  } catch (e) {
    return { kind: 'err', message: e instanceof Error ? e.message : String(e) }
  }
}

/**
 * Compare a l'attendu Java. Les nombres sont compares au bit pres via Object.is
 * apres parsing — insensible a la notation, strict sur la valeur.
 */
function matches(actual: Captured, expected: Expected): boolean {
  if (expected !== null && typeof expected === 'object') {
    return actual.kind === 'err' && actual.message === expected.error
  }
  if (actual.kind === 'err') return false
  if (expected === null) return actual.value === null
  if (typeof expected === 'number') return Object.is(actual.value, expected)
  // Chaine : un double serialise par Java
  if (actual.value === null) return false
  return Object.is(actual.value, Number(expected))
}

function show(c: Captured): string {
  if (c.kind === 'err') return `erreur("${c.message}")`
  return String(c.value)
}

const svc = new CostCalculationService()

describe('CostCalculationService — parite avec le Java (vehicule seul)', () => {
  const golden = JSON.parse(readFileSync(join(GOLDEN_DIR, 'cost-calculation.golden.json'), 'utf8'))

  it(`charge ${golden.count} vecteurs`, () => {
    expect(golden.vectors.length).toBe(golden.count)
    expect(golden.count).toBeGreaterThan(1000)
  })

  it('reproduit chaque vecteur a l identique', () => {
    const mismatches: string[] = []

    for (const v of golden.vectors) {
      const vehicle = toVehicule(v.vehicle)
      const prices = PRICE_SETS[v.priceSet]
      const homeRatio = v.homeChargingRatio == null ? null : Number(v.homeChargingRatio)
      const taxIncome = v.taxIncome == null ? null : Number(v.taxIncome)
      const autonomie = v.autonomieWltpKm

      let base: number
      try {
        base = svc.resolveFuelPrice(vehicle, prices)
      } catch {
        base = 1.9
      }

      const actual: Record<string, Captured> = {
        resolveFuelPrice: capture(() => svc.resolveFuelPrice(vehicle, prices)),
        resolveWeightedFuelPrice: capture(() => svc.resolveWeightedFuelPrice(vehicle, base, homeRatio)),
        calculateAnnualFuelCost_simple: capture(() => svc.calculateAnnualFuelCost(vehicle, base)),
        calculateAnnualFuelCost_full: capture(() =>
          svc.calculateAnnualFuelCost(vehicle, base, prices, homeRatio, autonomie),
        ),
        calculatePhevAnnualCost: capture(() =>
          svc.calculatePhevAnnualCost(vehicle, prices, homeRatio, autonomie),
        ),
        calculateAnnualCost_simple: capture(() => svc.calculateAnnualCost(vehicle, base)),
        calculateAnnualCost_full: capture(() =>
          svc.calculateAnnualCost(vehicle, base, prices, homeRatio, autonomie),
        ),
        calculateTotalCost_5y: capture(() => svc.calculateTotalCost(vehicle, 5, base)),
        calculateCO2EmissionsGPerKm: capture(() => svc.calculateCO2EmissionsGPerKm(vehicle)),
        calculateAnnualCO2Kg: capture(() => svc.calculateAnnualCO2Kg(vehicle)),
        calculateBonusEcologique: capture(() => svc.calculateBonusEcologique(vehicle, taxIncome)),
      }

      for (const [method, expected] of Object.entries(v.results)) {
        if (!matches(actual[method], expected as Expected)) {
          mismatches.push(
            `${v.id}/${method}: attendu ${JSON.stringify(expected)}, obtenu ${show(actual[method])} ` +
              `[${v.vehicle.fuelType} km=${v.vehicle.annualMileage} conso=${v.vehicle.consumption} ` +
              `prix=${v.vehicle.purchasePrice} ratio=${v.homeChargingRatio} rfr=${v.taxIncome} set=${v.priceSet}]`,
          )
        }
      }
    }

    if (mismatches.length > 0) {
      throw new Error(
        `${mismatches.length} ecart(s) avec le Java :\n` + mismatches.slice(0, 25).join('\n'),
      )
    }
  })
})

describe('CostCalculationService — parite avec le Java (paires courant/cible)', () => {
  const golden = JSON.parse(
    readFileSync(join(GOLDEN_DIR, 'cost-calculation-pairs.golden.json'), 'utf8'),
  )

  it(`charge ${golden.count} vecteurs`, () => {
    expect(golden.vectors.length).toBe(golden.count)
    expect(golden.count).toBeGreaterThan(1000)
  })

  it('reproduit chaque vecteur a l identique', () => {
    const mismatches: string[] = []

    for (const v of golden.vectors) {
      const cur = toVehicule(v.current)
      const tgt = toVehicule(v.target)
      const prices = PRICE_SETS[v.priceSet]
      const taxIncome = v.taxIncome == null ? null : Number(v.taxIncome)
      const scrap = v.scrapVehicle
      const maxYears = v.maxYears
      const repair = Number(v.immediateRepairCost)
      const homeRatio = v.homeChargingRatio == null ? null : Number(v.homeChargingRatio)

      const actual: Record<string, Captured> = {
        calculatePrimeConversion: capture(() =>
          svc.calculatePrimeConversion(cur, tgt, scrap, taxIncome),
        ),
        calculateSwitchCostAtYear_map: capture(() =>
          svc.calculateSwitchCostAtYearFromPrices(cur, tgt, maxYears, prices, repair),
        ),
        calculateBreakEvenYear_map: captureInt(() =>
          svc.calculateBreakEvenYearFromPrices(cur, tgt, maxYears, prices, repair),
        ),
      }

      // Reproduit le calcul d'investissement de ComparisonBusiness.compareDirect
      let curP: number | null = null
      let tgtP: number | null = null
      try {
        curP = svc.resolveWeightedFuelPrice(cur, svc.resolveFuelPrice(cur, prices), homeRatio)
      } catch {
        curP = null
      }
      try {
        tgtP = svc.resolveWeightedFuelPrice(tgt, svc.resolveFuelPrice(tgt, prices), homeRatio)
      } catch {
        tgtP = null
      }

      if (curP !== null && tgtP !== null) {
        const bonus = svc.calculateBonusEcologique(tgt, taxIncome)
        const prime = svc.calculatePrimeConversion(cur, tgt, scrap, taxIncome)
        const rawInv = Math.max(0.0, tgt.purchasePrice - cur.resaleValue)
        const inv = Math.max(0.0, rawInv - (bonus + prime))
        actual.switchInvestment = { kind: 'num', value: inv }
        actual.calculateSwitchCostAtYear_explicit = capture(() =>
          svc.calculateSwitchCostAtYear(cur, tgt, maxYears, curP, tgtP, inv, repair),
        )
        actual.calculateBreakEvenYear_explicit = captureInt(() =>
          svc.calculateBreakEvenYear(cur, tgt, maxYears, curP, tgtP, inv, repair),
        )
      } else {
        actual.switchInvestment = { kind: 'int', value: null }
        actual.calculateSwitchCostAtYear_explicit = { kind: 'err', message: 'prix non resolus' }
        actual.calculateBreakEvenYear_explicit = { kind: 'err', message: 'prix non resolus' }
      }

      for (const [method, expected] of Object.entries(v.results)) {
        if (!matches(actual[method], expected as Expected)) {
          mismatches.push(
            `${v.id}/${method}: attendu ${JSON.stringify(expected)}, obtenu ${show(actual[method])} ` +
              `[${v.current.fuelType}->${v.target.fuelType} rfr=${v.taxIncome} scrap=${v.scrapVehicle} ` +
              `ans=${v.maxYears} rep=${v.immediateRepairCost} set=${v.priceSet}]`,
          )
        }
      }
    }

    if (mismatches.length > 0) {
      throw new Error(
        `${mismatches.length} ecart(s) avec le Java :\n` + mismatches.slice(0, 25).join('\n'),
      )
    }
  })
})
