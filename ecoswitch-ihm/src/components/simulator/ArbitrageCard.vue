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
  <div class="apple-arbitrage-card p-5">
    <span class="card-tag">Arbitrage Financier</span>
    <h4 class="card-title text-rose flex items-center gap-2 mb-3">
      <Wrench size="18" />
      <span>Réparer vs Remplacer</span>
    </h4>
    
    <div class="arbitrage-grid mb-4">
      <div class="arbitrage-item">
        <div class="item-label">Scénario Réparer (Upfront)</div>
        <div class="item-value text-rose">{{ formatCurrency(immediateRepairCost) }}</div>
      </div>
      <div class="arbitrage-item">
        <div class="item-label">Scénario Remplacer (Upfront Net)</div>
        <div class="item-value text-cyan">{{ formatCurrency(result.switchInvestment) }}</div>
      </div>
    </div>
    
    <div class="divider"></div>
    
    <div class="flex-between text-xs mb-4">
      <span class="text-muted">Effort de trésorerie net initial pour le remplacement :</span>
      <span class="font-bold text-sm" :class="result.switchInvestment - immediateRepairCost > 0 ? 'text-rose' : 'text-teal'">
        {{ formatCurrency(result.switchInvestment - immediateRepairCost) }}
      </span>
    </div>

    <!-- Verdict dynamique intelligent -->
    <div class="verdict-bubble">
      <div class="verdict-text">
        <strong class="text-gradient">Verdict de l'Assistant : </strong>
        <span v-if="result.breakEvenYear && result.breakEvenYear <= 3">
          Il est fortement conseillé de **remplacer votre véhicule**. Bien que le remplacement exige un effort financier net de {{ formatCurrency(result.switchInvestment - immediateRepairCost) }}, les économies d'énergie substantielles amortiront ce surcoût en seulement **{{ result.breakEvenYear }} ans**. Réparer une carrosserie abîmée à hauteur de {{ formatCurrency(immediateRepairCost) }} sur un modèle ancien n'est pas viable.
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
  </div>
</template>

<style scoped>
.apple-arbitrage-card {
  background: hsl(var(--bg-glass));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 22px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.02);
  transition: transform 0.3s ease, border-color 0.2s ease;
}
.apple-arbitrage-card:hover {
  border-color: hsl(var(--border-glass-focus));
}

.card-tag {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: hsl(var(--text-dimmed));
  margin-bottom: 6px;
  display: block;
}

.card-title {
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: 700;
  margin: 0;
}

.arbitrage-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 576px) {
  .arbitrage-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

.arbitrage-item {
  background: hsl(var(--bg-deep) / 0.2);
  border: 1px solid hsl(var(--border-glass));
  border-radius: 16px;
  padding: 16px;
}

.item-label {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: hsl(var(--text-dimmed));
  font-weight: 600;
  margin-bottom: 4px;
}

.item-value {
  font-family: var(--font-heading);
  font-size: 1.15rem;
  font-weight: 800;
}

.divider {
  height: 1px;
  background: hsl(var(--border-glass));
  margin: 16px 0;
}

.verdict-bubble {
  background: hsl(var(--bg-deep) / 0.3);
  border-radius: 12px;
  padding: 12px;
}

.verdict-text {
  font-size: 0.75rem;
  line-height: 1.4;
  color: hsl(var(--text-muted));
}

.text-teal { color: hsl(var(--accent-teal)) !important; }
.text-rose { color: hsl(var(--accent-rose)) !important; }
.text-cyan { color: hsl(var(--accent-cyan)) !important; }
</style>
