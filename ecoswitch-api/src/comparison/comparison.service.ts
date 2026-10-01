import { BadRequestException, Injectable, Logger } from '@nestjs/common'
import { CostCalculationService } from './cost-calculation.service'
import type { Vehicule } from './fuel-type'
import type {
  CatalogVariant,
  CustomProfitabilityRequest,
  DirectProfitabilityRequest,
  DirectProfitabilityResponse,
  ProfitabilityComparisonResponse,
  VehicleProfitability,
} from './comparison.types'

/** Source du catalogue, injectee pour rester testable sans base. */
export abstract class CatalogVariantProvider {
  abstract getVariants(): Promise<CatalogVariant[]>
}

/**
 * Portage fidele de ComparisonBusiness.java.
 *
 * Deux chemins de calcul volontairement DIFFERENTS coexistent, comme dans le
 * Java — ne pas les unifier :
 *   - compareDirect     : prix ponderes, cout annuel conscient du PHEV, aides
 *                         deduites de l'investissement, moteur de recommandation.
 *   - compareCustom     : prix bruts (aucune ponderation), cout annuel simple,
 *                         AUCUNE aide deduite.
 *
 * Verifie par comparison-*.golden.spec.ts contre les vecteurs extraits du Java.
 */
@Injectable()
export class ComparisonService {
  private static readonly DEFAULT_MAX_YEARS = 15

  // Hypotheses de leasing (ComparisonBusiness.java:~215)
  private static readonly LEASING_MONTHS = 48.0
  private static readonly LEASING_DOWN_PAYMENT_RATE = 0.1
  private static readonly LEASING_RESIDUAL_RATE = 0.45
  private static readonly LEASING_ANNUAL_RATE = 0.039

  // Filtre d'adequation autonomie / kilometrage (ComparisonBusiness.java:276-281)
  private static readonly DEFAULT_WLTP_WHEN_UNKNOWN = 350
  private static readonly HIGH_MILEAGE = 25000
  private static readonly HIGH_MILEAGE_MIN_WLTP = 400
  private static readonly MID_MILEAGE = 18000
  private static readonly MID_MILEAGE_MIN_WLTP = 300

  private static readonly FALLBACK_MAINTENANCE = 250.0
  private static readonly FALLBACK_RESALE = 0.0
  private static readonly FALLBACK_MILEAGE = 15000

  private readonly logger = new Logger(ComparisonService.name)

  constructor(
    private readonly cost: CostCalculationService,
    private readonly catalog: CatalogVariantProvider,
  ) {}

  // ── Simulateur direct ─────────────────────────────────────────────────

  async compareDirect(request: DirectProfitabilityRequest): Promise<DirectProfitabilityResponse> {
    this.validateDirectRequest(request)

    const current = request.currentVehicle
    const target = request.targetVehicle
    const maxYears = request.maxYears == null ? ComparisonService.DEFAULT_MAX_YEARS : request.maxYears
    const immediateRepairCost = request.immediateRepairCost == null ? 0.0 : request.immediateRepairCost
    const prices = request.fuelPricesByType
    const ratio = request.homeChargingRatio

    const currentPrice = this.cost.resolveWeightedFuelPrice(
      current,
      this.cost.resolveFuelPrice(current, prices),
      ratio,
    )
    const targetPrice = this.cost.resolveWeightedFuelPrice(
      target,
      this.cost.resolveFuelPrice(target, prices),
      ratio,
    )

    const currentAnnualCost = this.cost.calculateAnnualCost(current, currentPrice, prices, ratio, null)
    const targetAnnualCost = this.cost.calculateAnnualCost(target, targetPrice, prices, ratio, null)
    const annualSavings = currentAnnualCost - targetAnnualCost

    const bonusEcologique = this.cost.calculateBonusEcologique(target, request.taxIncome)
    const primeConversion = this.cost.calculatePrimeConversion(
      current,
      target,
      request.scrapVehicle,
      request.taxIncome,
    )
    const totalSubsidies = bonusEcologique + primeConversion

    const rawSwitchInvestment = Math.max(0.0, target.purchasePrice - current.resaleValue)
    const switchInvestment = Math.max(0.0, rawSwitchInvestment - totalSubsidies)

    const totalCostDeltaAtHorizon = this.cost.calculateSwitchCostAtYear(
      current, target, maxYears, currentPrice, targetPrice, switchInvestment, immediateRepairCost,
    )
    const breakEvenYear = this.cost.calculateBreakEvenYear(
      current, target, maxYears, currentPrice, targetPrice, switchInvestment, immediateRepairCost,
    )

    const currentAnnualCO2 = this.cost.calculateAnnualCO2Kg(current)
    const targetAnnualCO2 = this.cost.calculateAnnualCO2Kg(target)
    const annualCO2Savings = currentAnnualCO2 - targetAnnualCO2

    const leasingMonthlyPrice = this.resolveLeasingMonthlyPrice(request, target)
    const currentMonthlyTotalCost = currentAnnualCost / 12.0
    const targetMonthlyTotalCost = leasingMonthlyPrice + targetAnnualCost / 12.0
    const monthlySavings = currentMonthlyTotalCost - targetMonthlyTotalCost

    const recommendations = await this.buildRecommendations(
      request, current, target, currentAnnualCost, currentPrice, maxYears, immediateRepairCost,
    )

    return {
      currentAnnualCost,
      targetAnnualCost,
      annualSavings,
      switchInvestment,
      breakEvenYear,
      totalCostDeltaAtHorizon,
      recommendations,
      bonusEcologique,
      primeConversion,
      totalSubsidies,
      currentAnnualCO2,
      targetAnnualCO2,
      annualCO2Savings,
      currentMonthlyTotalCost,
      targetMonthlyTotalCost,
      monthlySavings,
    }
  }

  /**
   * Mensualite LOA/LLD. Si aucun loyer n'est fourni, estimation sur 48 mois :
   * apport 10 %, valeur residuelle 45 %, taux nominal 3,9 %/an.
   */
  private resolveLeasingMonthlyPrice(request: DirectProfitabilityRequest, target: Vehicule): number {
    if (request.isLeasing == null || !request.isLeasing) return 0.0

    const custom = request.customLeasingMonthlyPrice
    if (custom != null && custom > 0) return custom

    const prix = target.purchasePrice
    const apport = prix * ComparisonService.LEASING_DOWN_PAYMENT_RATE
    const residuel = prix * ComparisonService.LEASING_RESIDUAL_RATE
    const capitalFinance = prix - apport - residuel
    const tauxMensuel = ComparisonService.LEASING_ANNUAL_RATE / 12.0

    const loyerAmortissement =
      (capitalFinance * tauxMensuel) /
      (1.0 - Math.pow(1.0 + tauxMensuel, -ComparisonService.LEASING_MONTHS))
    const loyerResiduel = residuel * tauxMensuel

    return loyerAmortissement + loyerResiduel
  }

  /**
   * Parcourt tout le catalogue et renvoie les 3 meilleures alternatives.
   *
   * Un vehicule dont le calcul echoue est ignore sans faire echouer la requete —
   * comportement du Java (`catch (Exception ignored)`), mais on le loge ici.
   */
  private async buildRecommendations(
    request: DirectProfitabilityRequest,
    current: Vehicule,
    target: Vehicule,
    currentAnnualCost: number,
    currentPrice: number,
    maxYears: number,
    immediateRepairCost: number,
  ): Promise<VehicleProfitability[]> {
    const catalog = await this.getCatalogSafely()
    const recommendations: VehicleProfitability[] = []
    const userAnnualMileage =
      current.annualMileage > 0 ? current.annualMileage : ComparisonService.FALLBACK_MILEAGE
    const prices = request.fuelPricesByType
    const ratio = request.homeChargingRatio

    for (const item of catalog) {
      const candidate = this.toVehicule(item)

      if (
        candidate.name.toLowerCase() === current.name?.toLowerCase() ||
        candidate.name.toLowerCase() === target.name?.toLowerCase()
      ) {
        continue
      }

      // Un gros rouleur n'est pas servi par une citadine electrique a faible autonomie.
      if (candidate.fuelType === 'ELECTRIC') {
        const wltp = item.autonomieWltpKm ?? ComparisonService.DEFAULT_WLTP_WHEN_UNKNOWN
        if (
          userAnnualMileage >= ComparisonService.HIGH_MILEAGE &&
          wltp < ComparisonService.HIGH_MILEAGE_MIN_WLTP
        ) {
          continue
        }
        if (
          userAnnualMileage >= ComparisonService.MID_MILEAGE &&
          wltp < ComparisonService.MID_MILEAGE_MIN_WLTP
        ) {
          continue
        }
      }

      try {
        const catFuelPrice = this.cost.resolveFuelPrice(candidate, prices)
        const weightedCatPrice = this.cost.resolveWeightedFuelPrice(candidate, catFuelPrice, ratio)

        const catAnnualCost = this.cost.calculateAnnualCost(
          candidate, weightedCatPrice, prices, ratio, item.autonomieWltpKm,
        )
        const catSavings = currentAnnualCost - catAnnualCost

        const catBonus = this.cost.calculateBonusEcologique(candidate, request.taxIncome)
        const catPrime = this.cost.calculatePrimeConversion(
          current, candidate, request.scrapVehicle, request.taxIncome,
        )
        const catTotalSubsidies = catBonus + catPrime

        const catRawSwitch = Math.max(0.0, candidate.purchasePrice - current.resaleValue)
        const catSwitchInvestment = Math.max(0.0, catRawSwitch - catTotalSubsidies)

        const catTotalCostDelta = this.cost.calculateSwitchCostAtYear(
          current, candidate, maxYears, currentPrice, weightedCatPrice, catSwitchInvestment, immediateRepairCost,
        )
        const catBreakEvenYear = this.cost.calculateBreakEvenYear(
          current, candidate, maxYears, currentPrice, weightedCatPrice, catSwitchInvestment, immediateRepairCost,
        )

        if (catBreakEvenYear !== null || catSavings > 0) {
          recommendations.push({
            vehicleId: candidate.id ?? null,
            vehicleName: candidate.name,
            switchInvestment: catSwitchInvestment,
            currentAnnualCost,
            targetAnnualCost: catAnnualCost,
            annualSavings: catSavings,
            breakEvenYear: catBreakEvenYear,
            totalCostDeltaAtHorizon: catTotalCostDelta,
          })
        }
      } catch (e) {
        this.logger.debug(
          `Variante ${item.id} ecartee des recommandations : ${e instanceof Error ? e.message : e}`,
        )
      }
    }

    this.sortByProfitability(recommendations)
    return recommendations.slice(0, 3)
  }

  private async getCatalogSafely(): Promise<CatalogVariant[]> {
    try {
      return await this.catalog.getVariants()
    } catch (e) {
      this.logger.warn(`Catalogue indisponible, aucune recommandation : ${e instanceof Error ? e.message : e}`)
      return []
    }
  }

  // ── Comparateur du catalogue ──────────────────────────────────────────

  /**
   * Note de fidelite : n'applique NI ponderation de recharge NI aides. Le
   * calcul differe de compareDirect, c'est le comportement du Java.
   */
  async compareCustomProfitability(
    request: CustomProfitabilityRequest,
  ): Promise<ProfitabilityComparisonResponse> {
    if (request == null || request.currentVehicle == null) {
      throw new BadRequestException('Le véhicule actuel est obligatoire.')
    }
    if (request.targetVehicleIds == null || request.targetVehicleIds.length === 0) {
      throw new BadRequestException('Au moins un véhicule cible est obligatoire.')
    }

    const current = request.currentVehicle
    const maxYears = request.maxYears == null ? ComparisonService.DEFAULT_MAX_YEARS : request.maxYears
    const immediateRepairCost = request.immediateRepairCost == null ? 0.0 : request.immediateRepairCost
    const prices = request.fuelPricesByType

    const currentFuelPrice = this.cost.resolveFuelPrice(current, prices)
    const currentAnnualCost = this.cost.calculateAnnualCost(current, currentFuelPrice)

    const catalog = await this.getCatalogSafely()
    const byId = new Map(catalog.map((v) => [v.id, v]))
    const alternatives: VehicleProfitability[] = []

    for (const targetId of request.targetVehicleIds) {
      const variant = byId.get(targetId)
      if (variant === undefined) continue // id inconnu : ignore, comme le Java
      const targetVehicle = this.toVehicule(variant)

      const targetFuelPrice = this.cost.resolveFuelPrice(targetVehicle, prices)
      const targetAnnualCost = this.cost.calculateAnnualCost(targetVehicle, targetFuelPrice)
      const annualSavings = currentAnnualCost - targetAnnualCost

      const switchInvestment = Math.max(0.0, targetVehicle.purchasePrice - current.resaleValue)
      const totalCostDeltaAtHorizon = this.cost.calculateSwitchCostAtYear(
        current, targetVehicle, maxYears, currentFuelPrice, targetFuelPrice, switchInvestment, immediateRepairCost,
      )
      const breakEvenYear = this.cost.calculateBreakEvenYear(
        current, targetVehicle, maxYears, currentFuelPrice, targetFuelPrice, switchInvestment, immediateRepairCost,
      )

      alternatives.push({
        vehicleId: targetVehicle.id ?? null,
        vehicleName: targetVehicle.name,
        switchInvestment,
        currentAnnualCost,
        targetAnnualCost,
        annualSavings,
        breakEvenYear,
        totalCostDeltaAtHorizon,
      })
    }

    this.sortByProfitability(alternatives)

    return {
      currentVehicleId: current.id ?? -1,
      currentVehicleName: current.name,
      maxYears,
      alternatives,
    }
  }

  // ── Utilitaires ───────────────────────────────────────────────────────

  /**
   * Tri : d'abord ceux qui ont un seuil de rentabilite, puis par seuil croissant,
   * puis par delta a l'horizon. Array.sort est stable depuis ES2019, comme le
   * TimSort de Java — l'ordre des ex aequo est donc preserve a l'identique.
   */
  private sortByProfitability(list: VehicleProfitability[]): void {
    list.sort((a, b) => {
      const aNull = a.breakEvenYear === null
      const bNull = b.breakEvenYear === null
      if (aNull !== bNull) return aNull ? 1 : -1

      const aYear = a.breakEvenYear ?? Number.MAX_SAFE_INTEGER
      const bYear = b.breakEvenYear ?? Number.MAX_SAFE_INTEGER
      if (aYear !== bYear) return aYear - bYear

      if (a.totalCostDeltaAtHorizon !== b.totalCostDeltaAtHorizon) {
        return a.totalCostDeltaAtHorizon - b.totalCostDeltaAtHorizon
      }
      return 0
    })
  }

  /** Projette une variante du catalogue vers la forme plate `Vehicule`. */
  private toVehicule(dto: CatalogVariant): Vehicule {
    return {
      id: dto.id,
      name: `${dto.brandName} ${dto.modelName} ${dto.motorisationName} ${dto.finitionName}`,
      brand: dto.brandName,
      model: dto.modelName,
      version: `${dto.motorisationName} - ${dto.finitionName}`,
      fuelType: dto.fuelType,
      consumption: dto.consumptionWltp,
      purchasePrice: dto.purchasePrice,
      maintenanceCost: dto.defaultMaintenanceCost ?? ComparisonService.FALLBACK_MAINTENANCE,
      resaleValue: dto.estimatedResaleValue ?? ComparisonService.FALLBACK_RESALE,
      annualMileage: 0,
      url: dto.finitionImageUrl ?? dto.modelImageUrl,
    }
  }

  private validateDirectRequest(request: DirectProfitabilityRequest): void {
    if (request == null) throw new BadRequestException('La requete est obligatoire.')
    if (request.currentVehicle == null) throw new BadRequestException('Le vehicule actuel est obligatoire.')
    if (request.targetVehicle == null) throw new BadRequestException('Le vehicule cible est obligatoire.')
    if (request.currentVehicle.fuelType == null) {
      throw new BadRequestException('Le type de carburant du vehicule actuel est obligatoire.')
    }
    if (request.targetVehicle.fuelType == null) {
      throw new BadRequestException('Le type de carburant du vehicule cible est obligatoire.')
    }
    if (request.maxYears != null && request.maxYears <= 0) {
      throw new BadRequestException('maxYears doit etre superieur a 0.')
    }
  }
}
