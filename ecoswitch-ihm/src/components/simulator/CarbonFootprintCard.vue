<script setup>
import { Leaf, Trees, Plane, AlertCircle } from '@lucide/vue'

defineProps({
  result: {
    type: Object,
    required: true
  }
})

const formatNumber = (val) => {
  return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 }).format(val)
}
</script>

<template>
  <div v-if="result.annualCO2Savings > 0" class="apple-carbon-card p-5">
    <span class="card-tag">Bilan Environnemental</span>
    <h4 class="card-title text-teal flex items-center gap-2">
      <Leaf size="20" class="animate-pulse" />
      <span>Votre Impact Planétaire</span>
    </h4>
    
    <p class="card-subtitle mt-2 mb-4">
      En changeant de véhicule, vous réduisez considérablement vos rejets de CO₂ dans l'atmosphère. Voici ce que cela représente concrètement par an :
    </p>

    <!-- Bento style items inside the card -->
    <div class="carbon-grid">
      <!-- CO2 Économisé -->
      <div class="carbon-item">
        <Leaf class="text-teal mb-2 mx-auto" size="24" />
        <div class="carbon-label">CO₂ Économisé</div>
        <div class="carbon-value text-teal">
          -{{ formatNumber(result.annualCO2Savings) }} kg<span class="carbon-unit">/an</span>
        </div>
      </div>

      <!-- Équivalent en Arbres -->
      <div class="carbon-item">
        <Trees class="text-teal mb-2 mx-auto" size="24" />
        <div class="carbon-label">Équivalent Arbres</div>
        <div class="carbon-value text-teal">
          +{{ formatNumber(result.annualCO2Savings / 25.0) }}<span class="carbon-unit"> arbres</span>
        </div>
        <div class="carbon-subtext">absorbé en 1 an</div>
      </div>

      <!-- Équivalent en Vols -->
      <div class="carbon-item">
        <Plane class="text-cyan mb-2 mx-auto" size="24" />
        <div class="carbon-label">Vols évités</div>
        <div class="carbon-value text-cyan">
          {{ formatNumber(result.annualCO2Savings / 200.0) }}<span class="carbon-unit"> vols</span>
        </div>
        <div class="carbon-subtext">Paris - Nice</div>
      </div>
    </div>
  </div>
  
  <div v-else class="apple-carbon-card carbon-negative p-5 flex items-center gap-4">
    <div class="negative-icon-circle">
      <AlertCircle size="28" class="text-rose" />
    </div>
    <div class="negative-content">
      <span class="card-tag">Bilan Environnemental</span>
      <h4 class="card-title text-rose m-0">Aucun gain carbone identifié</h4>
      <p class="card-subtitle mt-1 mb-0">
        Le véhicule cible émet autant ou plus de gaz à effet de serre que le véhicule actuel sur votre kilométrage annuel.
      </p>
    </div>
  </div>
</template>

<style scoped>
.apple-carbon-card {
  background: hsl(var(--bg-glass));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 22px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.02);
  transition: transform 0.3s ease, border-color 0.2s ease;
}
.apple-carbon-card:hover {
  border-color: hsl(var(--border-glass-focus));
}

.carbon-negative {
  background: hsla(var(--accent-rose) / 0.04);
  border-color: hsla(var(--accent-rose) / 0.15);
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

.card-subtitle {
  font-size: 0.8rem;
  line-height: 1.5;
  color: hsl(var(--text-muted));
}

/* Carbon grid layout */
.carbon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 768px) {
  .carbon-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

.carbon-item {
  background: hsl(var(--bg-deep) / 0.2);
  border: 1px solid hsl(var(--border-glass));
  border-radius: 16px;
  padding: 16px;
  text-align: center;
}

.carbon-label {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: hsl(var(--text-dimmed));
  font-weight: 600;
  margin-bottom: 4px;
}

.carbon-value {
  font-family: var(--font-heading);
  font-size: 1.15rem;
  font-weight: 800;
}

.carbon-unit {
  font-size: 0.75rem;
  font-weight: 400;
  color: hsl(var(--text-muted));
  margin-left: 2px;
}

.carbon-subtext {
  font-size: 0.65rem;
  color: hsl(var(--text-dimmed));
  margin-top: 4px;
}

.negative-icon-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: hsla(var(--accent-rose) / 0.1);
  flex-shrink: 0;
}

.negative-content {
  flex: 1;
}

.text-teal { color: hsl(var(--accent-teal)) !important; }
.text-rose { color: hsl(var(--accent-rose)) !important; }
.text-cyan { color: hsl(var(--accent-cyan)) !important; }

.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .5; }
}
</style>
