import { describe, expect, it } from 'vitest'
import { deterministicAdvice } from './advice-rules'
import { normalize } from './ai-advisor.service'
import { grouped } from './format'

/** Payload tel que l'envoie reellement MobilityInsightCard.vue. */
const FRONT_PAYLOAD = {
  currentVehicleName: 'Peugeot 208',
  currentFuelType: 'PETROL',
  currentConsumption: 6.5,
  targetVehicleName: 'Renault Zoe',
  targetFuelType: 'ELECTRIC',
  targetConsumption: 17.2,
  annualMileage: 15000,
  annualFuelSavings: 1800,
  annualCO2Savings: 2100,
  breakEvenYear: 4,
  switchInvestment: 18000,
  homeChargingRatio: 0.8,
  taxIncome: 20000,
  scrapVehicle: true,
  isLeasing: false,
  targetMonthlyPrice: 299,
}

describe('conseiller IA — forme plate du front', () => {
  it('accepte le payload que le front envoie (le Java repondait 400)', () => {
    const r = deterministicAdvice(normalize(FRONT_PAYLOAD))
    expect(r.status).toBe('POSITIVE')
    expect(r.aiEngine).toBe('Moteur Expert Local')
    expect(r.verdict).toContain('rentabilisé en seulement 4 ans')
    expect(r.verdict).toContain('15 000 km')
  })

  it('refuse un payload sans carburant cible', () => {
    expect(() => normalize({ ...FRONT_PAYLOAD, targetFuelType: undefined })).toThrow(
      'Données de simulation incomplètes',
    )
  })

  it('deduit le loyer de l economie mensuelle en leasing, comme compareDirect', () => {
    const a = normalize({ ...FRONT_PAYLOAD, isLeasing: true, annualFuelSavings: 1200, targetMonthlyPrice: 300 })
    expect(a.monthlySavings).toBe(1200 / 12 - 300)
    expect(deterministicAdvice(a).status).toBe('CAUTION') // -200 €/mois
  })

  it('classe un leasing absorbe par les economies en POSITIVE', () => {
    const a = normalize({ ...FRONT_PAYLOAD, isLeasing: true, annualFuelSavings: 4800, targetMonthlyPrice: 300 })
    expect(deterministicAdvice(a).status).toBe('POSITIVE')
  })

  it('complete toujours jusqu a au moins une recommandation d essai', () => {
    const r = deterministicAdvice(normalize({ ...FRONT_PAYLOAD, targetFuelType: 'PETROL' }))
    expect(r.keyRecommendations.at(-1)).toContain('essai routier')
  })
})

describe('format des nombres', () => {
  it('groupe les milliers avec une espace, comme le Java', () => {
    expect(grouped(15000)).toBe('15 000')
    expect(grouped(1234567)).toBe('1 234 567')
    expect(grouped(999)).toBe('999')
    expect(grouped(-25000)).toBe('-25 000')
  })
})
