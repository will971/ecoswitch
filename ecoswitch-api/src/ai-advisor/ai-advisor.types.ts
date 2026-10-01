/**
 * Corps envoye par MobilityInsightCard.vue (forme PLATE).
 *
 * Le Java attendait des objets `Vehicule` imbriques : l'endpoint repondait 400
 * a chaque appel et le front affichait toujours son texte de repli. On adopte
 * la forme que le front envoie reellement. Les champs optionnels du bas ne sont
 * pas envoyes aujourd'hui mais sont pris en compte s'ils arrivent.
 */
export interface AiAdvisorRequest {
  currentVehicleName?: string
  currentFuelType?: string
  currentConsumption?: number
  targetVehicleName?: string
  targetFuelType?: string
  targetConsumption?: number
  annualMileage?: number
  /** Economie annuelle totale (= result.annualSavings de /profitability/direct). */
  annualFuelSavings?: number
  annualCO2Savings?: number
  breakEvenYear?: number | null
  switchInvestment?: number
  homeChargingRatio?: number | null
  taxIncome?: number | null
  scrapVehicle?: boolean | null
  isLeasing?: boolean | null
  targetMonthlyPrice?: number

  totalSubsidies?: number
  monthlySavings?: number
  targetPurchasePrice?: number
  currentResaleValue?: number
}

export type AdviceStatus = 'POSITIVE' | 'MODERATE' | 'CAUTION'

export interface AiAdvisorResponse {
  verdict: string
  status: string
  financialAdvice: string
  chargingAdvice: string
  ecologicalImpact: string
  keyRecommendations: string[]
  confidenceScore: number
  aiEngine: string
}

/** Donnees normalisees consommees par le prompt et le moteur de regles. */
export interface AdviceInput {
  currentName: string
  currentFuelType: string
  currentConsumption: number
  currentResaleValue: number
  targetName: string
  targetFuelType: string
  targetConsumption: number
  targetPurchasePrice: number
  mileage: number
  subsidies: number
  annualSavings: number
  monthlySavings: number
  homeRatio: number | null
  breakEvenYear: number | null
  co2: number
  isLeasing: boolean
}
