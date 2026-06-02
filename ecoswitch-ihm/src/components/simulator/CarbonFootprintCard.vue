<script setup>
import { Leaf, Trees, Plane } from '@lucide/vue'

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
  <div v-if="result.annualCO2Savings > 0" class="carbon-footprint-card p-4 border-glass rounded mb-4 glow-green bg-green-glass">
    <h4 class="text-md font-heading text-green mb-3 flex items-center gap-2">
      <Leaf size="20" class="text-green animate-pulse" />
      <span>Bilan Écologique : Votre Impact Planétaire</span>
    </h4>
    
    <p class="text-xs text-muted mb-4">
      En changeant de véhicule, vous réduisez considérablement vos rejets de CO₂ dans l'atmosphère. Voici ce que cela représente concrètement par an :
    </p>

    <div class="grid-cols-3 gap-3">
      <!-- CO2 Économisé -->
      <div class="p-3 border-glass rounded bg-deep-glass text-center">
        <Leaf class="text-green mb-2 mx-auto" size="24" />
        <div class="text-xxs text-dimmed uppercase font-semibold">CO₂ Économisé</div>
        <div class="font-heading text-lg mt-1 text-green font-bold">
          -{{ formatNumber(result.annualCO2Savings) }} kg / an
        </div>
      </div>

      <!-- Équivalent en Arbres -->
      <div class="p-3 border-glass rounded bg-deep-glass text-center">
        <Trees class="text-emerald mb-2 mx-auto" size="24" />
        <div class="text-xxs text-dimmed uppercase font-semibold">Équivalent Arbres</div>
        <div class="font-heading text-lg mt-1 text-emerald font-bold">
          +{{ formatNumber(result.annualCO2Savings / 25.0) }} arbres
        </div>
        <div class="text-xxs text-muted mt-1">(absorbé en 1 an)</div>
      </div>

      <!-- Équivalent en Vols -->
      <div class="p-3 border-glass rounded bg-deep-glass text-center">
        <Plane class="text-cyan mb-2 mx-auto" size="24" />
        <div class="text-xxs text-dimmed uppercase font-semibold">Vols évités</div>
        <div class="font-heading text-lg mt-1 text-cyan font-bold">
          {{ formatNumber(result.annualCO2Savings / 200.0) }} vols
        </div>
        <div class="text-xxs text-muted mt-1">Paris - Nice</div>
      </div>
    </div>
  </div>
  <div v-else class="carbon-footprint-card p-4 border-glass rounded mb-4 bg-card-glass text-center">
    <Leaf size="32" class="text-rose mb-2 mx-auto" />
    <h4 class="text-sm font-heading text-rose mb-1">Pas d'économie carbone</h4>
    <p class="text-xs text-muted">
      Le véhicule cible émet autant ou plus de gaz à effet de serre que le véhicule actuel sur votre kilométrage annuel.
    </p>
  </div>
</template>

<style scoped>
.bg-green-glass {
  background: rgba(16, 185, 129, 0.05);
}
.border-glass {
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.glow-green {
  box-shadow: 0 0 15px -3px rgba(16, 185, 129, 0.1);
}
.text-green {
  color: #10b981;
}
.text-emerald {
  color: #34d399;
}
.text-cyan {
  color: #22d3ee;
}
.text-rose {
  color: #f43f5e;
}
.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .5; }
}
</style>
