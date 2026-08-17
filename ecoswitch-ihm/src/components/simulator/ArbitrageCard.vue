<script setup>
import { Wrench, Sparkles, Scale } from '@lucide/vue'

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
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}
</script>

<template>
  <div class="card-glass p-4 mb-4">
    <div class="flex items-center gap-2 mb-3">
      <div class="wrench-icon-badge flex-center">
        <Wrench size="16" class="text-rose" />
      </div>
      <div>
        <h4 class="text-main font-heading text-sm font-bold m-0">Arbitrage Réparation vs Remplacement</h4>
        <p class="text-xxs text-muted m-0">Analyse de rentabilité suite à devis de garagiste</p>
      </div>
    </div>
    
    <div class="grid-2-fields gap-3 mb-3">
      <div class="arbitrage-box p-3 rounded-xl border-glass bg-card-subtle">
        <div class="text-xxs uppercase font-bold text-dimmed mb-1">Scénario 1 : Réparer</div>
        <div class="text-sm font-bold text-rose">{{ formatCurrency(immediateRepairCost) }}</div>
        <div class="text-xxs text-dimmed mt-1">Dépense immédiate à fonds perdus</div>
      </div>
      <div class="arbitrage-box p-3 rounded-xl border-glass bg-card-subtle">
        <div class="text-xxs uppercase font-bold text-dimmed mb-1">Scénario 2 : Remplacer (Net)</div>
        <div class="text-sm font-bold text-cyan">{{ formatCurrency(result.switchInvestment) }}</div>
        <div class="text-xxs text-dimmed mt-1">Investissement net après revente & aides</div>
      </div>
    </div>
    
    <div class="flex-between text-xs py-2 border-t border-glass mb-3">
      <span class="text-muted">Effort de trésorerie net initial supplémentaire :</span>
      <span class="font-bold text-xs" :class="result.switchInvestment - immediateRepairCost > 0 ? 'text-rose' : 'text-teal'">
        {{ formatCurrency(result.switchInvestment - immediateRepairCost) }}
      </span>
    </div>

    <!-- Verdict de l'Assistant -->
    <div class="verdict-bubble p-3 rounded-xl border-glass bg-card text-xs">
      <strong class="text-main font-bold flex items-center gap-1 mb-1">
        <Sparkles size="13" class="text-teal" />
        <span>Conseil de transition :</span>
      </strong>
      <p class="text-muted text-xxs m-0 leading-relaxed">
        <span v-if="result.breakEvenYear && result.breakEvenYear <= 3">
          Il est vivement conseillé de <strong>remplacer votre véhicule</strong>. Vos économies d'énergie amortissent l'effort financier net supplémentaire en seulement <strong>{{ result.breakEvenYear }} an{{ result.breakEvenYear > 1 ? 's' : '' }}</strong>.
        </span>
        <span v-else-if="result.breakEvenYear && result.breakEvenYear <= 7">
          Le <strong>remplacement est rentable sur le moyen terme</strong> ({{ result.breakEvenYear }} ans). Réparer un modèle ancien immobilise de la trésorerie sans réduire vos factures d'énergie.
        </span>
        <span v-else-if="result.breakEvenYear">
          Amortissement sur le long terme ({{ result.breakEvenYear }} ans). Si votre trésorerie est limitée à court terme, la <strong>réparation est envisageable</strong>.
        </span>
        <span v-else>
          Il est plus économique de <strong>faire réparer votre véhicule actuel</strong>, l'achat du nouveau modèle exigeant un effort trop élevé par rapport aux économies d'énergie.
        </span>
      </p>
    </div>
  </div>
</template>

<style scoped>
.wrench-icon-badge {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: hsla(var(--accent-rose) / 0.12);
  border: 1px solid hsla(var(--accent-rose) / 0.25);
}
</style>
