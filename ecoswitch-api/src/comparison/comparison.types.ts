import type { FuelPricesByType, FuelType, Vehicule } from './fuel-type'

/** Miroir de FinitionMotorisationDto.java — 24 champs, forme plate. */
export interface CatalogVariant {
  id: number
  finitionId: number
  finitionName: string
  finitionImageUrl: string | null
  motorisationId: number
  motorisationName: string
  fuelType: FuelType
  consumptionWltp: number
  powerHp: number | null
  batteryCapacityKwh: number | null
  autonomieWltpKm: number | null
  consoThermiquePhev: number | null
  modelId: number
  modelName: string
  modelImageUrl: string | null
  category: string | null
  brandId: number
  brandName: string
  brandLogoUrl: string | null
  purchasePrice: number
  monthlyLoa: number | null
  monthlyLld: number | null
  defaultMaintenanceCost: number | null
  estimatedResaleValue: number | null
}

export interface VehicleProfitability {
  vehicleId: number | null
  vehicleName: string
  switchInvestment: number
  currentAnnualCost: number
  targetAnnualCost: number
  annualSavings: number
  breakEvenYear: number | null
  totalCostDeltaAtHorizon: number
}

export interface DirectProfitabilityRequest {
  currentVehicle: Vehicule
  targetVehicle: Vehicule
  fuelPricesByType: FuelPricesByType
  maxYears?: number | null
  immediateRepairCost?: number | null
  homeChargingRatio?: number | null
  taxIncome?: number | null
  scrapVehicle?: boolean | null
  isLeasing?: boolean | null
  customLeasingMonthlyPrice?: number | null
}

/** 16 champs — forme consommee par SimulationResults.vue et persistee telle quelle. */
export interface DirectProfitabilityResponse {
  currentAnnualCost: number
  targetAnnualCost: number
  annualSavings: number
  switchInvestment: number
  breakEvenYear: number | null
  totalCostDeltaAtHorizon: number
  recommendations: VehicleProfitability[]
  bonusEcologique: number
  primeConversion: number
  totalSubsidies: number
  currentAnnualCO2: number
  targetAnnualCO2: number
  annualCO2Savings: number
  currentMonthlyTotalCost: number
  targetMonthlyTotalCost: number
  monthlySavings: number
}

export interface CustomProfitabilityRequest {
  currentVehicle: Vehicule
  targetVehicleIds: number[]
  fuelPricesByType: FuelPricesByType
  maxYears?: number | null
  immediateRepairCost?: number | null
}

export interface ProfitabilityComparisonResponse {
  currentVehicleId: number
  currentVehicleName: string
  maxYears: number
  alternatives: VehicleProfitability[]
}
