import { fixed, grouped } from './format'
import type { AdviceInput, AiAdvisorResponse } from './ai-advisor.types'

/**
 * Moteur de regles « Moteur Expert Local » — portage de
 * AiAdvisorService.generateDeterministicAdvice. Sert de repli quand Gemini
 * n'est pas configure ou ne repond pas.
 */
export function deterministicAdvice(a: AdviceInput): AiAdvisorResponse {
  const isElectric = a.targetFuelType === 'ELECTRIC'
  const isHybrid = a.targetFuelType === 'HYBRID' || a.targetFuelType === 'PLUGIN_HYBRID'
  const homeRatio = a.homeRatio ?? 0.85

  let verdict: string
  let status: string
  if (a.isLeasing) {
    if (a.monthlySavings >= 0) {
      status = 'POSITIVE'
      verdict = `Opération financièrement gagnante : vos économies de carburant absorbent entièrement votre loyer de leasing avec +${fixed(a.monthlySavings, 0)} €/mois de gain net.`
    } else if (a.monthlySavings > -150) {
      status = 'MODERATE'
      verdict = `Contrat LOA/LLD très compétitif : pour un effort net de seulement ${fixed(Math.abs(a.monthlySavings), 0)} €/mois, vous roulez dans un véhicule moderne et garanti.`
    } else {
      status = 'CAUTION'
      verdict = `Projet de leasing haut de gamme : le loyer implique un surcoût mensuel net de ${fixed(Math.abs(a.monthlySavings), 0)} € après déduction des économies d'énergie.`
    }
  } else if (a.breakEvenYear !== null && a.breakEvenYear <= 5) {
    status = 'POSITIVE'
    verdict = `Excellente opportunité : votre passage à la ${a.targetName} est rentabilisé en seulement ${a.breakEvenYear} ans grâce à vos ${grouped(a.mileage)} km annuels.`
  } else if (a.breakEvenYear !== null && a.breakEvenYear <= 8) {
    status = 'POSITIVE'
    verdict = `Projet équilibré : votre investissement sur la ${a.targetName} s'amortit en ${a.breakEvenYear} ans avec un gain net immédiat sur vos pleins d'énergie.`
  } else if (a.annualSavings > 600) {
    status = 'MODERATE'
    verdict = `Investissement axé sur le confort et les économies d'usage : vous gagnez ${fixed(a.annualSavings, 0)} € par an sur votre carburant.`
  } else {
    status = 'CAUTION'
    verdict = "Changement axé sur le renouvellement de véhicule : l'écart de prix initial nécessite un horizon plus long pour être amorti."
  }

  let financialAdvice: string
  if (a.isLeasing) {
    financialAdvice =
      a.monthlySavings >= 0
        ? `En leasing, vous réalisez une économie d'énergie de ${fixed(a.annualSavings, 0)} €/an, ce qui compense l'intégralité du loyer et dégage un surplus de trésorerie de ${fixed(a.monthlySavings, 0)} €/mois.`
        : `Sur vos ${grouped(a.mileage)} km/an, vous économisez ${fixed(a.annualSavings / 12, 0)} €/mois de carburant, ce qui allège considérablement la charge de votre loyer de leasing.`
  } else {
    financialAdvice =
      a.monthlySavings > 0
        ? `Sur vos ${grouped(a.mileage)} km/an, vous économisez environ ${fixed(a.monthlySavings, 0)} € chaque mois sur vos dépenses énergétiques (soit ${fixed(a.annualSavings, 0)} €/an).`
        : `Vos dépenses mensuelles d'énergie restent stables (écart de ${fixed(Math.abs(a.monthlySavings), 0)} €/mois). L'avantage principal réside dans la fiabilité et la valeur de revente.`
  }

  let chargingAdvice: string
  if (isElectric) {
    if (homeRatio >= 0.7) {
      const weeklyCost = (a.mileage / 52 / 100) * a.targetConsumption * 0.25
      chargingAdvice = `Avec ${fixed(homeRatio * 100, 0)}% de recharge à domicile, un plein complet nocturne en heures creuses vous revient à seulement ~${fixed(weeklyCost, 1)} € pour couvrir toute votre semaine (~${fixed(a.mileage / 52, 0)} km).`
    } else {
      chargingAdvice =
        'Pour vos recharges en extérieur, utilisez les bornes en voirie pendant vos courses ou activités pour bénéficier de tarifs préférentiels.'
    }
  } else if (isHybrid) {
    chargingAdvice =
      "Ce modèle hybride s'auto-recharge au freinage sans nécessiter de branchement : idéal pour réduire de 30% à 40% votre consommation en ville."
  } else {
    chargingAdvice = `Consommation modérée de ${fixed(a.targetConsumption, 1)} L/100km adaptée aux longs trajets réguliers.`
  }

  const ecologicalImpact =
    a.co2 > 500
      ? `Vous évitez le rejet de ${grouped(a.co2)} kg de CO₂ par an, ce qui équivaut à la captation de ${Math.round(a.co2 / 25)} arbres adultes ou ${Math.round(a.co2 / 200)} allers-retours Paris-Nice évités.`
      : 'Bilan carbone stable et aligné avec les standards d\'émissions récents.'

  const recs: string[] = []
  if (a.subsidies > 0) {
    recs.push(`Bénéficiez de ${fixed(a.subsidies, 0)} € d'aides gouvernementales (Bonus / Prime) déduites directement par le concessionnaire.`)
  }
  if (isElectric && homeRatio >= 0.7) {
    recs.push("Installez une prise renforcée 3.7 kW (Green'Up) pour ~350 € ou une Wallbox bénéficiant de 500 € de crédit d'impôt.")
    recs.push("Programmez vos sessions de charge entre 22h et 6h du matin pour profiter du tarif Heures Creuses d'EDF.")
  }
  if (a.currentResaleValue > 0) {
    recs.push(`Votre véhicule actuel (${a.currentName}) apporte un apport estimé à ${fixed(a.currentResaleValue, 0)} € pour réduire la mensualité.`)
  }
  if (recs.length < 3) {
    recs.push("Réservez un essai routier de 48h en concession pour tester l'autonomie sur vos trajets du quotidien.")
  }

  return {
    verdict,
    status,
    financialAdvice,
    chargingAdvice,
    ecologicalImpact,
    keyRecommendations: recs,
    confidenceScore: 94,
    aiEngine: 'Moteur Expert Local',
  }
}
