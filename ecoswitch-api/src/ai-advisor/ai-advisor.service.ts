import { Inject, Injectable } from '@nestjs/common'
import { CACHE_MANAGER } from '@nestjs/cache-manager'
import type { Cache } from 'cache-manager'
import { DomainError } from '../common/domain-error'
import { geminiGenerate } from '../common/gemini'
import { deterministicAdvice } from './advice-rules'
import { fixed } from './format'
import type { AdviceInput, AiAdvisorRequest, AiAdvisorResponse } from './ai-advisor.types'

const CACHE_TTL_MS = 60 * 60 * 1000
const FALLBACK_MODELS = ['gemini-flash-lite-latest', 'gemini-3.5-flash-lite', 'gemini-flash-latest']

@Injectable()
export class AiAdvisorService {
  constructor(@Inject(CACHE_MANAGER) private readonly cache: Cache) {}

  async advise(request: AiAdvisorRequest): Promise<AiAdvisorResponse> {
    const input = normalize(request)
    const key = `ai-advisor:${JSON.stringify(input)}`
    const cached = await this.cache.get<AiAdvisorResponse>(key)
    if (cached) return cached

    const result = (await this.fromGemini(input)) ?? deterministicAdvice(input)
    await this.cache.set(key, result, CACHE_TTL_MS)
    return result
  }

  private async fromGemini(a: AdviceInput): Promise<AiAdvisorResponse | null> {
    const ai = await geminiGenerate(buildPrompt(a), { models: FALLBACK_MODELS })
    if (ai === null) return null
    try {
      const text = ai.text.replace(/^```(?:json)?/, '').replace(/```$/, '').trim()
      const parsed = JSON.parse(text) as Record<string, unknown>
      const str = (k: string, d: string) => (typeof parsed[k] === 'string' ? (parsed[k] as string) : d)
      return {
        verdict: str('verdict', 'Transition avantageuse.'),
        status: str('status', 'RECOMMENDED'),
        financialAdvice: str('financialAdvice', ''),
        chargingAdvice: str('chargingAdvice', ''),
        ecologicalImpact: str('ecologicalImpact', ''),
        keyRecommendations: Array.isArray(parsed.keyRecommendations)
          ? parsed.keyRecommendations.map(String)
          : [],
        confidenceScore: Number.isInteger(parsed.confidenceScore) ? (parsed.confidenceScore as number) : 94,
        aiEngine: `Google Gemini AI (${ai.modelVersion})`,
      }
    } catch {
      // Reponse non JSON : on bascule sur le moteur de regles.
      return null
    }
  }
}

/** Convertit la forme plate du front en donnees de conseil. */
export function normalize(r: AiAdvisorRequest): AdviceInput {
  if (!r || !r.targetFuelType) {
    throw new DomainError('Données de simulation incomplètes pour générer le conseil IA.')
  }
  const n = (v: unknown, d: number) => (typeof v === 'number' && Number.isFinite(v) ? v : d)
  const annualSavings = n(r.annualFuelSavings, 0)
  const isLeasing = r.isLeasing === true

  // Meme definition que compareDirect : economie mensuelle nette, loyer deduit
  // en leasing. Le front ne l'envoie pas, on la reconstitue.
  const monthlySavings =
    typeof r.monthlySavings === 'number'
      ? r.monthlySavings
      : annualSavings / 12 - (isLeasing ? n(r.targetMonthlyPrice, 0) : 0)

  return {
    currentName: r.currentVehicleName || 'Votre véhicule',
    currentFuelType: r.currentFuelType || 'PETROL',
    currentConsumption: n(r.currentConsumption, 0),
    currentResaleValue: n(r.currentResaleValue, 0),
    targetName: r.targetVehicleName || 'véhicule cible',
    targetFuelType: r.targetFuelType,
    targetConsumption: n(r.targetConsumption, 0),
    targetPurchasePrice: n(r.targetPurchasePrice, 0),
    mileage: n(r.annualMileage, 15000),
    subsidies: n(r.totalSubsidies, 0),
    annualSavings,
    monthlySavings,
    homeRatio: typeof r.homeChargingRatio === 'number' ? r.homeChargingRatio : null,
    breakEvenYear: typeof r.breakEvenYear === 'number' ? r.breakEvenYear : null,
    co2: n(r.annualCO2Savings, 0),
    isLeasing,
  }
}

function buildPrompt(a: AdviceInput): string {
  const homePct = a.homeRatio !== null ? a.homeRatio * 100 : 85
  const price = a.targetPurchasePrice > 0 ? `${fixed(a.targetPurchasePrice, 0)} €` : 'non communiqué'
  return `Tu es l'expert automobile et conseiller en transition écologique d'EcoSwitch.
Analyse cette simulation réelle de changement de véhicule pour un particulier français et produis une synthèse percutante, personnalisée, ultra-claire et bienveillante en français.

DONNÉES DE LA SIMULATION :
- Type d'acquisition : ${a.isLeasing ? "Location avec Option d'Achat (LOA) / LLD (Contrat 3 à 5 ans)" : 'Achat Comptant / Crédit classique'}
- Véhicule actuel : ${a.currentName} (${a.currentFuelType}, consommation : ${fixed(a.currentConsumption, 1)} L/100km ou kWh/100km)
- Véhicule cible : ${a.targetName} (${a.targetFuelType}, consommation : ${fixed(a.targetConsumption, 1)} L/100km ou kWh/100km, prix : ${price})
- Kilométrage annuel : ${fixed(a.mileage, 0)} km/an (~${fixed(a.mileage / 365, 0)} km/jour)
- Profil de recharge domicile : ${fixed(homePct, 0)}% à domicile
- Aides de l'État déduites : ${fixed(a.subsidies, 0)} €
- Économie annuelle de carburant/énergie : ${fixed(a.annualSavings, 0)} €/an
- Bilan trésorerie mensuelle nette : ${fixed(a.monthlySavings, 0)} €/mois ${a.isLeasing ? '(Loyer déduit des économies)' : "(Gain net d'usage)"}
- Réduction de CO2 : ${fixed(a.co2, 0)} kg/an

CONSIGNE SPÉCIALE LEASING / LOA / LLD :
Si l'acquisition est en leasing (LOA/LLD), ne parle JAMAIS d'amortissement sur 10 ans ni du prix total d'achat, car le contrat dure entre 3 et 5 ans (36 à 60 mois). Focalise ton conseil financier sur l'effort de loyer mensuel et la manière dont les économies de carburant viennent compenser tout ou partie du loyer.

RÉPONDS UNIQUEMENT AU FORMAT JSON STRICT avec la structure suivante :
{
  "verdict": "Une phrase de verdict percutante et personnalisée qui résume la pertinence de ce choix pour son profil.",
  "status": "POSITIVE" (si gain net ou loyer bien absorbé) ou "MODERATE" (si équilibré) ou "CAUTION" (si surcoût mensuel élevé),
  "financialAdvice": "Explication claire du gain financier mensuel ou de l'effort de trésorerie net par mois.",
  "chargingAdvice": "Conseil pratique et chiffré sur sa routine de recharge et le coût du plein.",
  "ecologicalImpact": "Vulgarisation concrète de son impact environnemental (arbres ou trajets).",
  "keyRecommendations": [
    "Recommandation pratique 1",
    "Recommandation pratique 2",
    "Recommandation pratique 3"
  ],
  "confidenceScore": 95
}
`
}
