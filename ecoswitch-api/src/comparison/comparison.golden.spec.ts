import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { ComparisonService, CatalogVariantProvider } from './comparison.service'
import { CostCalculationService } from './cost-calculation.service'
import type { CatalogVariant } from './comparison.types'
import type { FuelPricesByType, FuelType, Vehicule } from './fuel-type'

/**
 * Parite stricte de ComparisonBusiness avec le Java.
 *
 * Le moteur de recommandation itere tout le catalogue : les vecteurs n'ont de
 * sens que contre le catalogue fige de catalog-fixture.json, produit par le
 * meme generateur Java que les attendus.
 *
 * Comme pour le moteur de calcul, on compare les NOMBRES et non les chaines :
 * Double.toString() et String(n) divergent en notation scientifique.
 */

const GOLDEN_DIR = join(__dirname, '..', '..', 'test', 'golden')

const PRICE_SETS: Record<string, FuelPricesByType> = {
  full: { PETROL: 1.9, DIESEL: 1.74, ELECTRIC: 0.23, HYBRID: 1.85, PLUGIN_HYBRID: 1.88 },
  noHybrid: { PETROL: 2.04, DIESEL: 1.8, ELECTRIC: 0.2516 },
}

/**
 * Le generateur Java serialise TOUS les doubles en chaines (pour preserver la
 * notation de Double.toString). Les champs numeriques du catalogue sont des
 * ENTREES : il faut les reconvertir en nombres, sans quoi `0 + "300.0"` donne
 * la chaine "0300.0" au lieu de 300.
 */
const CATALOG_NUMERIC_FIELDS = [
  'consumptionWltp', 'batteryCapacityKwh', 'consoThermiquePhev', 'purchasePrice',
  'monthlyLoa', 'monthlyLld', 'defaultMaintenanceCost', 'estimatedResaleValue',
] as const

const fixture = JSON.parse(readFileSync(join(GOLDEN_DIR, 'catalog-fixture.json'), 'utf8'))
const CATALOG: CatalogVariant[] = fixture.vectors.map((v: Record<string, unknown>) => {
  const out = { ...v }
  for (const f of CATALOG_NUMERIC_FIELDS) {
    if (out[f] !== null && out[f] !== undefined) out[f] = Number(out[f])
  }
  return out as unknown as CatalogVariant
})

class FixtureCatalog extends CatalogVariantProvider {
  async getVariants(): Promise<CatalogVariant[]> {
    return CATALOG
  }
}

function service(): ComparisonService {
  return new ComparisonService(new CostCalculationService(), new FixtureCatalog())
}

function toVehicule(g: Record<string, unknown>): Vehicule {
  return {
    name: g.name as string,
    fuelType: g.fuelType as FuelType,
    annualMileage: g.annualMileage as number,
    consumption: Number(g.consumption),
    maintenanceCost: Number(g.maintenanceCost),
    purchasePrice: Number(g.purchasePrice),
    resaleValue: Number(g.resaleValue),
  }
}

/** Egalite au bit pres, insensible a la notation Java vs JS. */
function numEq(actual: number, expected: unknown): boolean {
  if (expected === null || expected === undefined) return false
  return Object.is(actual, Number(expected))
}

/**
 * Seuls champs ou l'egalite stricte est relachee, et uniquement quand la
 * mensualite de leasing est ESTIMEE (formule par defaut) plutot que fournie.
 *
 * Raison : la formule passe par Math.pow(1 + t, -48). La spec Java autorise
 * Math.pow a devier d'1 ulp (seul StrictMath.pow est correctement arrondi), et
 * V8 a sa propre implementation. Le Java n'est donc pas lui-meme reproductible
 * au bit pres ici — il peut varier selon la JVM et l'architecture. Exiger mieux
 * que la source ne garantit n'aurait pas de sens.
 *
 * Ecart maximal mesure sur les 592 vecteurs : 3,4e-13 EUR/mois, soit une erreur
 * relative de 1,3e-15. La tolerance retenue (1e-9 EUR) reste 7 ordres de
 * grandeur sous le milliardieme d'euro.
 */
const LEASING_DERIVED_FIELDS = new Set(['targetMonthlyTotalCost', 'monthlySavings'])
const LEASING_TOLERANCE_EUR = 1e-9

function fieldMatches(
  field: string,
  actual: number,
  expected: unknown,
  usesEstimatedLeasing: boolean,
): boolean {
  if (numEq(actual, expected)) return true
  if (usesEstimatedLeasing && LEASING_DERIVED_FIELDS.has(field)) {
    return Math.abs(actual - Number(expected)) <= LEASING_TOLERANCE_EUR
  }
  return false
}

function checkReco(
  actual: Record<string, unknown>[],
  expected: Record<string, unknown>[],
  path: string,
  out: string[],
): void {
  if (actual.length !== expected.length) {
    out.push(`${path}: ${expected.length} attendue(s), ${actual.length} obtenue(s)`)
    return
  }
  for (let i = 0; i < expected.length; i++) {
    const a = actual[i]
    const e = expected[i]
    if (a.vehicleId !== e.vehicleId) out.push(`${path}[${i}].vehicleId: ${e.vehicleId} != ${a.vehicleId}`)
    if (a.vehicleName !== e.vehicleName) out.push(`${path}[${i}].vehicleName: ${e.vehicleName} != ${a.vehicleName}`)
    if (a.breakEvenYear !== e.breakEvenYear) {
      out.push(`${path}[${i}].breakEvenYear: ${e.breakEvenYear} != ${a.breakEvenYear}`)
    }
    for (const k of ['switchInvestment', 'currentAnnualCost', 'targetAnnualCost', 'annualSavings', 'totalCostDeltaAtHorizon']) {
      if (!numEq(a[k] as number, e[k])) out.push(`${path}[${i}].${k}: ${e[k]} != ${a[k]}`)
    }
  }
}

describe('ComparisonService.compareDirect — parite avec le Java', () => {
  const golden = JSON.parse(readFileSync(join(GOLDEN_DIR, 'comparison-direct.golden.json'), 'utf8'))

  it(`charge ${golden.count} vecteurs et 24 variantes de catalogue`, () => {
    expect(golden.vectors.length).toBe(golden.count)
    expect(CATALOG.length).toBe(24)
  })

  it('reproduit chaque vecteur a l identique', async () => {
    const svc = service()
    const mismatches: string[] = []

    for (const v of golden.vectors) {
      const req = {
        currentVehicle: toVehicule(v.current),
        targetVehicle: toVehicule(v.target),
        fuelPricesByType: PRICE_SETS[v.priceSet],
        maxYears: v.maxYears,
        immediateRepairCost: Number(v.immediateRepairCost),
        homeChargingRatio: v.homeChargingRatio == null ? null : Number(v.homeChargingRatio),
        taxIncome: v.taxIncome == null ? null : Number(v.taxIncome),
        scrapVehicle: v.scrapVehicle,
        isLeasing: v.isLeasing,
        customLeasingMonthlyPrice: v.customLeasingMonthlyPrice == null ? null : Number(v.customLeasingMonthlyPrice),
      }

      let actual: Record<string, unknown> | null = null
      let error: string | null = null
      try {
        actual = (await svc.compareDirect(req)) as unknown as Record<string, unknown>
      } catch (e) {
        error = e instanceof Error ? e.message : String(e)
      }

      if (v.error !== undefined) {
        if (error !== v.error) mismatches.push(`${v.id}: erreur attendue "${v.error}", obtenue "${error ?? 'aucune'}"`)
        continue
      }
      if (error !== null) {
        mismatches.push(`${v.id}: erreur inattendue "${error}"`)
        continue
      }

      const exp = v.response
      // La mensualite n'est estimee (donc dependante de Math.pow) que si le
      // leasing est actif ET qu'aucun loyer personnalise n'est fourni.
      const custom = v.customLeasingMonthlyPrice
      const usesEstimatedLeasing =
        v.isLeasing === true && (custom == null || Number(custom) <= 0)

      for (const k of [
        'currentAnnualCost', 'targetAnnualCost', 'annualSavings', 'switchInvestment',
        'totalCostDeltaAtHorizon', 'bonusEcologique', 'primeConversion', 'totalSubsidies',
        'currentAnnualCO2', 'targetAnnualCO2', 'annualCO2Savings',
        'currentMonthlyTotalCost', 'targetMonthlyTotalCost', 'monthlySavings',
      ]) {
        if (!fieldMatches(k, actual![k] as number, exp[k], usesEstimatedLeasing)) {
          mismatches.push(`${v.id}/${k}: attendu ${exp[k]}, obtenu ${actual![k]} [${v.current.fuelType}->${v.target.fuelType} leasing=${v.isLeasing}]`)
        }
      }
      if (actual!.breakEvenYear !== exp.breakEvenYear) {
        mismatches.push(`${v.id}/breakEvenYear: attendu ${exp.breakEvenYear}, obtenu ${actual!.breakEvenYear}`)
      }
      checkReco(
        actual!.recommendations as Record<string, unknown>[],
        exp.recommendations as Record<string, unknown>[],
        `${v.id}/recommendations`,
        mismatches,
      )
    }

    if (mismatches.length > 0) {
      throw new Error(`${mismatches.length} ecart(s) avec le Java :\n` + mismatches.slice(0, 25).join('\n'))
    }
  })
})

describe('ComparisonService.compareCustomProfitability — parite avec le Java', () => {
  const golden = JSON.parse(readFileSync(join(GOLDEN_DIR, 'comparison-custom.golden.json'), 'utf8'))

  it(`charge ${golden.count} vecteurs`, () => {
    expect(golden.vectors.length).toBe(golden.count)
  })

  it('reproduit chaque vecteur a l identique', async () => {
    const svc = service()
    const mismatches: string[] = []

    for (const v of golden.vectors) {
      const req = {
        currentVehicle: toVehicule(v.current),
        targetVehicleIds: v.targetVehicleIds,
        fuelPricesByType: PRICE_SETS[v.priceSet],
        maxYears: v.maxYears,
        immediateRepairCost: Number(v.immediateRepairCost),
      }

      let actual: Record<string, unknown> | null = null
      let error: string | null = null
      try {
        actual = (await svc.compareCustomProfitability(req)) as unknown as Record<string, unknown>
      } catch (e) {
        error = e instanceof Error ? e.message : String(e)
      }

      if (v.error !== undefined) {
        if (error !== v.error) mismatches.push(`${v.id}: erreur attendue "${v.error}", obtenue "${error ?? 'aucune'}"`)
        continue
      }
      if (error !== null) {
        mismatches.push(`${v.id}: erreur inattendue "${error}"`)
        continue
      }

      const exp = v.response
      if (actual!.maxYears !== exp.maxYears) {
        mismatches.push(`${v.id}/maxYears: attendu ${exp.maxYears}, obtenu ${actual!.maxYears}`)
      }
      if (actual!.currentVehicleName !== exp.currentVehicleName) {
        mismatches.push(`${v.id}/currentVehicleName: attendu ${exp.currentVehicleName}, obtenu ${actual!.currentVehicleName}`)
      }
      checkReco(
        actual!.alternatives as Record<string, unknown>[],
        exp.alternatives as Record<string, unknown>[],
        `${v.id}/alternatives`,
        mismatches,
      )
    }

    if (mismatches.length > 0) {
      throw new Error(`${mismatches.length} ecart(s) avec le Java :\n` + mismatches.slice(0, 25).join('\n'))
    }
  })
})
