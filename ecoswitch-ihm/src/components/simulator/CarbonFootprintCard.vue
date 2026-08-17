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
  <div v-if="result.annualCO2Savings > 0" class="apple-carbon-card p-4 border-glass rounded mb-5">
    <div class="flex items-center gap-2 mb-2">
      <div class="carbon-icon-circle">
        <Leaf size="18" class="text-teal" />
      </div>
      <h4 class="card-title text-main font-bold m-0">Impact Écologique & Climat</h4>
    </div>
    
    <p class="card-subtitle text-xs text-muted mb-4">
      En changeant pour ce véhicule, vous réduisez concrètement votre empreinte carbone chaque année :
    </p>

    <!-- Grille des 3 métriques écologiques -->
    <div class="carbon-grid">
      <!-- CO2 Économisé -->
      <div class="carbon-item">
        <div class="carbon-item-icon mb-2">
          <Leaf class="text-teal" size="22" />
        </div>
        <div class="carbon-label">CO₂ non rejeté</div>
        <div class="carbon-value text-teal">
          -{{ formatNumber(result.annualCO2Savings) }} kg<span class="carbon-unit">/an</span>
        </div>
        <div class="carbon-subtext">de gaz à effet de serre en moins</div>
      </div>

      <!-- Équivalent en Arbres -->
      <div class="carbon-item">
        <div class="carbon-item-icon mb-2">
          <Trees class="text-teal" size="22" />
        </div>
        <div class="carbon-label">Équivalent Arbres</div>
        <div class="carbon-value text-teal">
          {{ formatNumber(result.annualCO2Savings / 25.0) }}<span class="carbon-unit"> arbres</span>
        </div>
        <div class="carbon-subtext">capacité d'absorption annuelle</div>
      </div>

      <!-- Équivalent en Vols -->
      <div class="carbon-item">
        <div class="carbon-item-icon mb-2">
          <Plane class="text-cyan" size="22" />
        </div>
        <div class="carbon-label">Vols Évités</div>
        <div class="carbon-value text-cyan">
          {{ formatNumber(result.annualCO2Savings / 200.0) }}<span class="carbon-unit"> vols</span>
        </div>
        <div class="carbon-subtext">trajets Paris - Nice équivalents</div>
      </div>
    </div>
  </div>
  
  <div v-else class="apple-carbon-card carbon-neutral p-4 border-glass rounded mb-5 flex items-center gap-3">
    <div class="negative-icon-circle">
      <AlertCircle size="24" class="text-amber" />
    </div>
    <div>
      <h5 class="card-title text-main font-bold m-0 text-sm">Bilan Carbone Neutre</h5>
      <p class="card-subtitle text-xs text-muted m-0 mt-1">
        Les émissions annuelles de ce modèle sont équivalentes ou proches de votre véhicule actuel.
      </p>
    </div>
  </div>
</template>

<style scoped>
.apple-carbon-card {
  background: hsl(var(--bg-glass));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 20px;
  box-shadow: var(--shadow-card);
}

.carbon-icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: hsla(var(--accent-teal) / 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-title {
  font-family: var(--font-sans);
  font-size: 1rem;
}

.card-subtitle {
  line-height: 1.5;
}

/* Carbon grid layout */
.carbon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
@media (max-width: 768px) {
  .carbon-grid {
    grid-template-columns: 1fr;
  }
}

.carbon-item {
  background: hsl(var(--bg-card));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 14px;
  padding: 16px 12px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.carbon-label {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: hsl(var(--text-muted));
  font-weight: 700;
  margin-bottom: 6px;
}

.carbon-value {
  font-family: var(--font-sans);
  font-size: 1.45rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.1;
  margin-bottom: 4px;
}

.carbon-unit {
  font-size: 0.8rem;
  font-weight: 600;
  color: hsl(var(--text-muted));
  margin-left: 2px;
}

.carbon-subtext {
  font-size: 0.75rem;
  color: hsl(var(--text-muted));
}

.negative-icon-circle {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: hsla(var(--accent-amber) / 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.text-teal { color: hsl(var(--accent-teal)) !important; }
.text-cyan { color: hsl(var(--accent-cyan)) !important; }
.text-amber { color: hsl(var(--accent-amber)) !important; }
</style>
