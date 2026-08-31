<script setup>
import { Leaf, Trees, Plane, AlertCircle } from '@lucide/vue'

const props = defineProps({
  result: {
    type: Object,
    required: true
  }
})

const formatNumber = (val) => {
  return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(val || 0)
}
</script>

<template>
  <div v-if="result.annualCO2Savings > 0" class="carbon-card">
    <div class="flex items-center gap-2 mb-1.5">
      <Leaf size="16" class="text-teal" />
      <h4 class="card-title text-main font-semibold text-xs m-0">Impact Environnemental</h4>
    </div>
    
    <p class="text-xxs text-muted mb-3">
      Réduction d'émissions de CO₂ estimée par rapport à votre véhicule actuel :
    </p>

    <!-- Grille des 3 métriques écologiques -->
    <div class="carbon-grid">
      <!-- CO2 Économisé -->
      <div class="carbon-item">
        <div class="carbon-label">CO₂ Évité</div>
        <div class="carbon-value text-teal font-mono">
          -{{ formatNumber(result.annualCO2Savings) }} kg<span class="carbon-unit font-sans">/an</span>
        </div>
        <div class="carbon-subtext">Gaz à effet de serre</div>
      </div>

      <!-- Équivalent en Arbres -->
      <div class="carbon-item">
        <div class="carbon-label">Capacité Arbres</div>
        <div class="carbon-value text-teal font-mono">
          {{ formatNumber(result.annualCO2Savings / 25.0) }}<span class="carbon-unit font-sans"> arbres</span>
        </div>
        <div class="carbon-subtext">Absorption annuelle</div>
      </div>

      <!-- Équivalent en Vols -->
      <div class="carbon-item">
        <div class="carbon-label">Vols Équivalents</div>
        <div class="carbon-value text-teal font-mono">
          {{ formatNumber(result.annualCO2Savings / 200.0) }}<span class="carbon-unit font-sans"> vols</span>
        </div>
        <div class="carbon-subtext">Paris – Nice évités</div>
      </div>
    </div>
  </div>
  
  <div v-else class="carbon-card carbon-neutral flex items-center gap-3">
    <AlertCircle size="18" class="text-amber shrink-0" />
    <div>
      <h5 class="card-title text-main font-semibold m-0 text-xs">Bilan Carbone Équivalent</h5>
      <p class="text-xxs text-muted m-0 mt-0.5">
        Les émissions annuelles estimées de ce modèle sont similaires à votre véhicule actuel.
      </p>
    </div>
  </div>
</template>

<style scoped>
.carbon-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  box-shadow: var(--shadow-card);
}

.card-title {
  font-size: 0.85rem;
}

/* Carbon grid layout */
.carbon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
@media (max-width: 768px) {
  .carbon-grid {
    grid-template-columns: 1fr;
  }
}

.carbon-item {
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  text-align: left;
  display: flex;
  flex-direction: column;
}

.carbon-label {
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-dimmed);
  font-weight: 600;
  margin-bottom: 4px;
}

.carbon-value {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.1;
  margin-bottom: 2px;
}

.carbon-unit {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-dimmed);
  margin-left: 2px;
}

.carbon-subtext {
  font-size: 0.68rem;
  color: var(--text-muted);
}
</style>
