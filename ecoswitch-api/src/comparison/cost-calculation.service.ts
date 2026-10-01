import { Injectable } from '@nestjs/common'
import { DomainError } from '../common/domain-error'
import type { FuelPricesByType, Vehicule } from './fuel-type'

/**
 * Portage fidele de CostCalculationService.java.
 *
 * Contrainte de migration : les resultats doivent etre bit-a-bit identiques a
 * l'implementation Java. Toute "amelioration" (arrondi, garde supplementaire,
 * refactorisation d'une formule) change des montants affiches a l'utilisateur.
 * Les ecarts apparents avec docs/FUNCTIONAL.md sont intentionnels : le code fait
 * foi, la doc est en retard (cf. PUBLIC_FAST_CHARGE_PRICE et le facteur PHEV).
 *
 * Verifie par test/golden/cost-calculation*.golden.json, extrait du Java.
 */
@Injectable()
export class CostCalculationService {
  /** Horizon d'analyse par defaut, en annees. */
  static readonly DEFAULT_MAX_YEARS = 15

  /** Tarif moyen d'une borne publique rapide. La doc annonce 0,65 — le code fait foi. */
  private static readonly PUBLIC_FAST_CHARGE_PRICE = 0.55

  /** Facteurs d'emission en phase d'usage, par unite de consommation (g/km). */
  private static readonly CO2_FACTORS: Record<string, number> = {
    PETROL: 23.0,
    DIESEL: 26.4,
    HYBRID: 20.0,
    PLUGIN_HYBRID: 9.5,
    ELECTRIC: 0.5,
  }

  // Hypotheses PHEV (CostCalculationService.java:35-63)
  private static readonly PHEV_WORKING_DAYS = 250.0
  private static readonly PHEV_DEFAULT_RANGE_KM = 55
  private static readonly PHEV_DEFAULT_CHARGE_RATE = 0.85
  private static readonly PHEV_MIN_CHARGE_RATE = 0.2
  private static readonly PHEV_ELECTRIC_CONSO = 18.0
  private static readonly PHEV_FALLBACK_THERMAL_CONSO = 6.5
  private static readonly PHEV_FALLBACK_ELECTRIC_PRICE = 0.2516
  private static readonly PHEV_FALLBACK_PETROL_PRICE = 2.04
  private static readonly PHEV_FALLBACK_MILEAGE = 15000

  // Baremes des aides (CostCalculationService.java:113-144)
  private static readonly BONUS_PRICE_CAP = 47000.0
  private static readonly INCOME_THRESHOLD = 15400.0

  // ── Cout d'energie ────────────────────────────────────────────────────

  calculateAnnualFuelCost(
    vehicle: Vehicule,
    fuelPrice: number,
    fuelPricesByType?: FuelPricesByType | null,
    homeChargingRatio?: number | null,
    autonomieWltpKm?: number | null,
  ): number {
    if (vehicle.fuelType === 'PLUGIN_HYBRID') {
      return this.calculatePhevAnnualCost(vehicle, fuelPricesByType, homeChargingRatio, autonomieWltpKm)
    }
    return (vehicle.annualMileage / 100.0) * vehicle.consumption * fuelPrice
  }

  /**
   * Hybride rechargeable : la part electrique depend du rapport entre l'autonomie
   * WLTP et le trajet quotidien moyen, module par le taux de recharge.
   */
  calculatePhevAnnualCost(
    vehicle: Vehicule,
    fuelPricesByType?: FuelPricesByType | null,
    homeChargingRatio?: number | null,
    autonomieWltpKm?: number | null,
  ): number {
    const annualMileage =
      vehicle.annualMileage > 0 ? vehicle.annualMileage : CostCalculationService.PHEV_FALLBACK_MILEAGE
    const dailyDistance = annualMileage / CostCalculationService.PHEV_WORKING_DAYS

    const electricRange =
      autonomieWltpKm != null && autonomieWltpKm > 0
        ? autonomieWltpKm
        : CostCalculationService.PHEV_DEFAULT_RANGE_KM
    const chargeRate =
      homeChargingRatio != null
        ? Math.max(CostCalculationService.PHEV_MIN_CHARGE_RATE, homeChargingRatio)
        : CostCalculationService.PHEV_DEFAULT_CHARGE_RATE

    const electricShare = Math.min(1.0, electricRange / Math.max(1.0, dailyDistance)) * chargeRate
    const thermalShare = Math.max(0.0, 1.0 - electricShare)

    const electricMileage = annualMileage * electricShare
    const thermalMileage = annualMileage * thermalShare

    const thermalConso =
      vehicle.consumption > 0 ? vehicle.consumption : CostCalculationService.PHEV_FALLBACK_THERMAL_CONSO

    const electricPrice =
      fuelPricesByType != null && Object.hasOwn(fuelPricesByType, 'ELECTRIC')
        ? fuelPricesByType['ELECTRIC']
        : CostCalculationService.PHEV_FALLBACK_ELECTRIC_PRICE
    const petrolPrice =
      fuelPricesByType != null && Object.hasOwn(fuelPricesByType, 'PETROL')
        ? fuelPricesByType['PETROL']
        : CostCalculationService.PHEV_FALLBACK_PETROL_PRICE

    const electricCost =
      (electricMileage / 100.0) * CostCalculationService.PHEV_ELECTRIC_CONSO * electricPrice
    const thermalCost = (thermalMileage / 100.0) * thermalConso * petrolPrice

    return electricCost + thermalCost
  }

  calculateAnnualCost(
    vehicle: Vehicule,
    fuelPrice: number,
    fuelPricesByType?: FuelPricesByType | null,
    homeChargingRatio?: number | null,
    autonomieWltpKm?: number | null,
  ): number {
    const fuelCost = this.calculateAnnualFuelCost(
      vehicle,
      fuelPrice,
      fuelPricesByType,
      homeChargingRatio,
      autonomieWltpKm,
    )
    return fuelCost + vehicle.maintenanceCost
  }

  calculateTotalCost(vehicle: Vehicule, years: number, fuelPrice: number): number {
    const annualCost = this.calculateAnnualCost(vehicle, fuelPrice)
    return vehicle.purchasePrice + annualCost * years
  }

  // ── Resolution des prix ───────────────────────────────────────────────

  /**
   * Les hybrides sans tarif propre retombent sur l'essence. Une energie inconnue
   * retombe aussi sur l'essence ; en dernier recours, on leve.
   */
  resolveFuelPrice(vehicle: Vehicule, fuelPricesByType?: FuelPricesByType | null): number {
    if (fuelPricesByType == null) {
      throw new DomainError("Les prix d'energie sont obligatoires.")
    }
    const fuelType = vehicle.fuelType
    if (
      (fuelType === 'HYBRID' || fuelType === 'PLUGIN_HYBRID') &&
      !Object.hasOwn(fuelPricesByType, fuelType)
    ) {
      const petrolPrice = fuelPricesByType['PETROL']
      if (petrolPrice == null) {
        throw new DomainError("Prix d'energie (Essence) manquant pour le calcul Hybride.")
      }
      return petrolPrice
    }
    const fuelPrice = fuelPricesByType[fuelType]
    if (fuelPrice == null) {
      const petrolPrice = fuelPricesByType['PETROL']
      if (petrolPrice != null) return petrolPrice
      throw new DomainError("Prix d'energie manquant pour: " + fuelType)
    }
    return fuelPrice
  }

  /** Pondere domicile / borne publique rapide, pour les electriques uniquement. */
  resolveWeightedFuelPrice(
    vehicle: Vehicule,
    baseFuelPrice: number,
    homeChargingRatio?: number | null,
  ): number {
    if (vehicle.fuelType === 'ELECTRIC' && homeChargingRatio != null) {
      const homeRatio = Math.max(0.0, Math.min(1.0, homeChargingRatio))
      const fastRatio = 1.0 - homeRatio
      return homeRatio * baseFuelPrice + fastRatio * CostCalculationService.PUBLIC_FAST_CHARGE_PRICE
    }
    return baseFuelPrice
  }

  // ── Aides de l'Etat ───────────────────────────────────────────────────

  calculateBonusEcologique(targetVehicle: Vehicule, taxIncome?: number | null): number {
    if (targetVehicle.fuelType !== 'ELECTRIC') return 0.0
    if (targetVehicle.purchasePrice > CostCalculationService.BONUS_PRICE_CAP) return 0.0
    if (taxIncome != null && taxIncome <= CostCalculationService.INCOME_THRESHOLD) return 7000.0
    return 4000.0
  }

  calculatePrimeConversion(
    currentVehicle: Vehicule,
    targetVehicle: Vehicule,
    scrapVehicle?: boolean | null,
    taxIncome?: number | null,
  ): number {
    if (scrapVehicle == null || !scrapVehicle) return 0.0
    if (
      targetVehicle.fuelType !== 'ELECTRIC' &&
      targetVehicle.fuelType !== 'HYBRID' &&
      targetVehicle.fuelType !== 'PLUGIN_HYBRID'
    ) {
      return 0.0
    }
    if (currentVehicle.fuelType !== 'PETROL' && currentVehicle.fuelType !== 'DIESEL') return 0.0
    if (taxIncome != null && taxIncome <= CostCalculationService.INCOME_THRESHOLD) return 3000.0
    return 1500.0
  }

  // ── Emissions ─────────────────────────────────────────────────────────

  calculateCO2EmissionsGPerKm(vehicle: Vehicule): number {
    if (vehicle.consumption <= 0) return 0.0
    const factor = CostCalculationService.CO2_FACTORS[vehicle.fuelType]
    return factor === undefined ? 0.0 : vehicle.consumption * factor
  }

  calculateAnnualCO2Kg(vehicle: Vehicule): number {
    const gPerKm = this.calculateCO2EmissionsGPerKm(vehicle)
    return (gPerKm * vehicle.annualMileage) / 1000.0
  }

  // ── Seuil de rentabilite ──────────────────────────────────────────────

  /**
   * Surcharge a prix explicites : l'appelant a deja resolu et pondere les prix,
   * et calcule l'investissement net des aides.
   *
   * Note de fidelite : utilise calculateAnnualCost a 2 arguments — donc SANS
   * traitement PHEV ni ponderation interne. C'est le comportement du Java.
   */
  calculateSwitchCostAtYear(
    currentVehicle: Vehicule,
    targetVehicle: Vehicule,
    year: number,
    currentFuelPrice: number,
    targetFuelPrice: number,
    switchInvestment: number,
    immediateRepairCost: number,
  ): number {
    const currentAnnualCost = this.calculateAnnualCost(currentVehicle, currentFuelPrice)
    const targetAnnualCost = this.calculateAnnualCost(targetVehicle, targetFuelPrice)
    const currentTotalFromNow = immediateRepairCost + currentAnnualCost * year
    const targetTotalFromNow = switchInvestment + targetAnnualCost * year
    return targetTotalFromNow - currentTotalFromNow
  }

  calculateBreakEvenYear(
    currentVehicle: Vehicule,
    targetVehicle: Vehicule,
    maxYears: number,
    currentFuelPrice: number,
    targetFuelPrice: number,
    switchInvestment: number,
    immediateRepairCost: number,
  ): number | null {
    for (let year = 1; year <= maxYears; year++) {
      const costDelta = this.calculateSwitchCostAtYear(
        currentVehicle,
        targetVehicle,
        year,
        currentFuelPrice,
        targetFuelPrice,
        switchInvestment,
        immediateRepairCost,
      )
      if (costDelta <= 0.0) return year
    }
    return null
  }

  /**
   * Surcharge a partir de la table de prix : resout les prix elle-meme et calcule
   * l'investissement SANS deduire les aides — contrairement a la surcharge
   * explicite, que ComparisonBusiness utilise pour le simulateur. Les deux
   * coexistent dans le Java ; ne pas les unifier.
   */
  calculateSwitchCostAtYearFromPrices(
    currentVehicle: Vehicule,
    targetVehicle: Vehicule,
    year: number,
    fuelPricesByType: FuelPricesByType,
    immediateRepairCost = 0.0,
  ): number {
    const currentAnnualCost = this.calculateAnnualCost(
      currentVehicle,
      this.resolveFuelPrice(currentVehicle, fuelPricesByType),
    )
    const targetAnnualCost = this.calculateAnnualCost(
      targetVehicle,
      this.resolveFuelPrice(targetVehicle, fuelPricesByType),
    )
    const switchInvestment = Math.max(0.0, targetVehicle.purchasePrice - currentVehicle.resaleValue)

    const currentTotalFromNow = immediateRepairCost + currentAnnualCost * year
    const targetTotalFromNow = switchInvestment + targetAnnualCost * year
    return targetTotalFromNow - currentTotalFromNow
  }

  calculateBreakEvenYearFromPrices(
    currentVehicle: Vehicule,
    targetVehicle: Vehicule,
    maxYears: number,
    fuelPricesByType: FuelPricesByType,
    immediateRepairCost = 0.0,
  ): number | null {
    for (let year = 1; year <= maxYears; year++) {
      const costDelta = this.calculateSwitchCostAtYearFromPrices(
        currentVehicle,
        targetVehicle,
        year,
        fuelPricesByType,
        immediateRepairCost,
      )
      if (costDelta <= 0.0) return year
    }
    return null
  }
}
