<script setup>
import { ArrowRight } from '@lucide/vue'

defineProps({
  recommendations: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['load-alternative'])

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}
</script>

<template>
  <div class="recommendations-container mb-4">
    <div class="flex-between items-center mb-3">
      <div>
        <h4 class="text-xs font-semibold text-main m-0 uppercase tracking-wider">Alternatives recommandées</h4>
        <p class="text-xxs text-muted m-0">Véhicules du catalogue offrant un ratio coût/usage attractif :</p>
      </div>
      <span class="badge badge-teal badge-small">Catalogue</span>
    </div>

    <!-- Grille de cartes épurées sans débordement -->
    <div class="recs-grid">
      <div
        v-for="rec in recommendations"
        :key="rec.vehicleId"
        class="rec-card"
        @click="emit('load-alternative', rec)"
      >
        <div class="rec-top-row mb-2">
          <div class="flex-between items-center gap-2">
            <span class="rec-vehicle-name font-semibold text-xs text-main truncate">
              {{ rec.vehicleName }}
            </span>
            <span class="badge badge-small badge-teal font-mono shrink-0">
              +{{ formatCurrency(rec.annualSavings) }}/an
            </span>
          </div>
        </div>

        <div class="rec-bottom-row flex-between items-center text-xxs pt-2 border-t border-glass">
          <span class="text-dimmed">
            Prix net : <strong class="text-main font-mono">{{ formatCurrency(rec.switchInvestment) }}</strong>
          </span>
          <span class="rec-action-link text-teal flex items-center gap-1 font-semibold">
            Simuler <ArrowRight size="11" />
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recommendations-container {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-lg);
  padding: 18px 22px;
  box-shadow: var(--shadow-card);
  box-sizing: border-box;
  width: 100%;
}

.recs-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}
@media (max-width: 900px) {
  .recs-grid {
    grid-template-columns: 1fr;
  }
}

.rec-card {
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.rec-card:hover {
  border-color: var(--accent-teal);
  background: var(--bg-card-hover);
  transform: translateY(-1px);
}

.rec-vehicle-name {
  min-width: 0;
}

.rec-action-link {
  transition: transform 0.15s ease;
}
.rec-card:hover .rec-action-link {
  transform: translateX(2px);
}
</style>
