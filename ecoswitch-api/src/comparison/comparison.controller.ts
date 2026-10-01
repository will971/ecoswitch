import { Body, Controller, Get, Header, HttpCode, Post } from '@nestjs/common'
import { AiAdvisorService } from '../ai-advisor/ai-advisor.service'
import type { AiAdvisorRequest } from '../ai-advisor/ai-advisor.types'
import { Public } from '../auth/decorators'
import { DomainError } from '../common/domain-error'
import { FuelPricesService } from '../fuel-prices/fuel-prices.service'
import { ComparisonService } from './comparison.service'
import { optBool, optNum, toPrices, toVehiculeInput } from './vehicule-input'

/**
 * Calculs publics : le simulateur fonctionne sans compte. DirectSimulator.vue
 * appelle d'ailleurs /profitability/direct par un fetch brut, sans jeton.
 *
 * Les POST renvoient 200 (et non le 201 par defaut de Nest), comme Spring.
 */
@Public()
@Controller('api/v1/comparisons')
export class ComparisonController {
  constructor(
    private readonly comparison: ComparisonService,
    private readonly fuelPrices: FuelPricesService,
    private readonly advisor: AiAdvisorService,
  ) {}

  @Get('fuel-prices/live')
  @Header('Cache-Control', 'max-age=300, public')
  fuelPricesLive() {
    return this.fuelPrices.getLive()
  }

  @Post('ai-advisor')
  @HttpCode(200)
  aiAdvisor(@Body() body: AiAdvisorRequest) {
    return this.advisor.advise(body)
  }

  @Post('profitability/direct')
  @HttpCode(200)
  direct(@Body() body: Record<string, unknown>) {
    return this.comparison.compareDirect({
      currentVehicle: toVehiculeInput(body?.currentVehicle)!,
      targetVehicle: toVehiculeInput(body?.targetVehicle)!,
      fuelPricesByType: requirePrices(body?.fuelPricesByType),
      maxYears: optNum(body?.maxYears),
      immediateRepairCost: optNum(body?.immediateRepairCost),
      homeChargingRatio: optNum(body?.homeChargingRatio),
      taxIncome: optNum(body?.taxIncome),
      scrapVehicle: optBool(body?.scrapVehicle),
      isLeasing: optBool(body?.isLeasing),
      customLeasingMonthlyPrice: optNum(body?.customLeasingMonthlyPrice),
    })
  }

  @Post('profitability/custom')
  @HttpCode(200)
  custom(@Body() body: Record<string, unknown>) {
    const ids = Array.isArray(body?.targetVehicleIds)
      ? body.targetVehicleIds.map(Number).filter(Number.isFinite)
      : []
    return this.comparison.compareCustomProfitability({
      currentVehicle: toVehiculeInput(body?.currentVehicle)!,
      targetVehicleIds: ids,
      fuelPricesByType: requirePrices(body?.fuelPricesByType),
      maxYears: optNum(body?.maxYears),
      immediateRepairCost: optNum(body?.immediateRepairCost),
    })
  }
}

/** Message identique a CostCalculationService.resolveFuelPrice cote Java. */
function requirePrices(raw: unknown): Record<string, number> {
  const prices = toPrices(raw)
  if (prices === null) throw new DomainError("Les prix d'energie sont obligatoires.")
  return prices
}
