<script setup>
import { Wrench } from '@lucide/vue'

const props = defineProps({
  result: {
    type: Object,
    required: true
  },
  immediateRepairCost: {
    type: Number,
    required: true
  },
  currentVehicle: {
    type: Object,
    required: true
  }
})

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
}
</script>

<template>
  <div class="arbitrage-card p-3 border-glass rounded mb-4 bg-card-glass">
    <h4 class="text-sm font-heading text-rose mb-3 flex items-center gap-1">
      <Wrench size="16" /> Arbitrage Financier : Réparer vs Remplacer
    </h4>
    <div class="grid-cols-2 text-xs gap-3">
      <div class="p-2 border-glass rounded bg-deep-glass">
        <div class="text-dimmed uppercase text-xxs">Scénario Réparer (Upfront)</div>
        <div class="font-heading text-md mt-1 text-rose font-bold">{{ formatCurrency(immediateRepairCost) }}</div>
      </div>
      <div class="p-2 border-glass rounded bg-deep-glass">
        <div class="text-dimmed uppercase text-xxs">Scénario Remplacer (Upfront Net)</div>
        <div class="font-heading text-md mt-1 text-cyan font-bold">{{ formatCurrency(result.switchInvestment) }}</div>
      </div>
    </div>
    <div class="mt-3 border-t border-glass pt-2 text-xxs text-dimmed flex-between">
      <span>Effort de trésorerie net initial pour le remplacement :</span>
      <span class="font-semibold" :class="result.switchInvestment - immediateRepairCost > 0 ? 'text-rose' : 'text-teal'">
        {{ formatCurrency(result.switchInvestment - immediateRepairCost) }}
      </span>
    </div>

    <!-- Verdict dynamique intelligent -->
    <div class="mt-3 p-2 bg-deep-glass rounded text-xxs text-muted border-glass">
      <strong class="text-gradient">Verdict de l'Assistant : </strong>
      <span v-if="result.breakEvenYear && result.breakEvenYear <= 3">
        Il est fortement recommandé de **remplacer le véhicule**. Bien que le remplacement exige un effort financier net de {{ formatCurrency(result.switchInvestment - immediateRepairCost) }}, les économies d'énergie substantielles amortiront ce surcoût en seulement **{{ result.breakEvenYear }} ans**. Réparer une carrosserie abîmée à hauteur de {{ formatCurrency(immediateRepairCost) }} sur un modèle ancien n'est pas viable.
      </span>
      <span v-else-if="result.breakEvenYear && result.breakEvenYear <= 7">
        Le **remplacement est pertinent sur le moyen terme** (seuil à **{{ result.breakEvenYear }} ans**). La facture de garage de {{ formatCurrency(immediateRepairCost) }} représente une trop grande partie de la valeur de revente de votre véhicule actuel ({{ formatCurrency(currentVehicle.resaleValue) }}). Il est conseillé de sauter le pas.
      </span>
      <span v-else-if="result.breakEvenYear">
        Le remplacement s'amortit lentement (seuil à **{{ result.breakEvenYear }} ans**). Si votre trésorerie est limitée, la **réparation à {{ formatCurrency(immediateRepairCost) }} est envisageable**, bien que le changement reste rentable sur le très long terme.
      </span>
      <span v-else>
        Il est plus raisonnable de **faire réparer votre véhicule actuel**. L'achat du nouveau modèle exige un effort financier trop important par rapport aux économies d'énergie réelles obtenues.
      </span>
    </div>
  </div>
</template>
