<script setup>
import { DollarSign, TrendingUp, Sparkles } from '@lucide/vue'

defineProps({
  result: {
    type: Object,
    required: true
  }
})

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
}
</script>

<template>
  <div class="leasing-card p-4 border-glass rounded mb-4 glow-cyan bg-cyan-glass">
    <h4 class="text-md font-heading text-cyan mb-3 flex items-center gap-2">
      <TrendingUp size="20" class="text-cyan" />
      <span>Comparatif Mensuel : Budget d'Usage LOA/LLD</span>
    </h4>
    
    <p class="text-xs text-muted mb-4">
      En mode leasing, nous comparons le coût global mensuel de votre véhicule actuel (carburant + assurance + entretien) avec le loyer mensuel et les frais d'usage du nouveau véhicule.
    </p>

    <div class="grid-cols-3 gap-3">
      <!-- Budget Actuel -->
      <div class="p-3 border-glass rounded bg-deep-glass text-center">
        <div class="text-xxs text-dimmed uppercase font-semibold">Budget Actuel</div>
        <div class="font-heading text-lg mt-1 text-rose font-bold">
          {{ formatCurrency(result.currentMonthlyTotalCost) }} / mois
        </div>
        <div class="text-xxs text-muted mt-1">(Carburant + Assurance + Entretien)</div>
      </div>

      <!-- Budget Nouveau -->
      <div class="p-3 border-glass rounded bg-deep-glass text-center">
        <div class="text-xxs text-dimmed uppercase font-semibold">Budget Nouveau</div>
        <div class="font-heading text-lg mt-1 text-cyan font-bold">
          {{ formatCurrency(result.targetMonthlyTotalCost) }} / mois
        </div>
        <div class="text-xxs text-muted mt-1">(Loyer + Électricité + Ass. + Ent.)</div>
      </div>

      <!-- Économie Mensuelle -->
      <div class="p-3 border-glass rounded bg-deep-glass text-center">
        <div class="text-xxs text-dimmed uppercase font-semibold">Gain Mensuel</div>
        <div class="font-heading text-lg mt-1 font-bold" :class="result.monthlySavings > 0 ? 'text-teal' : 'text-rose'">
          {{ result.monthlySavings > 0 ? '+' : '' }}{{ formatCurrency(result.monthlySavings) }} / mois
        </div>
        <div class="text-xxs text-muted mt-1">(Bilan net en poche)</div>
      </div>
    </div>

    <!-- Verdict dynamique -->
    <div class="mt-4 p-2 bg-deep-glass rounded text-xxs text-muted border-glass">
      <strong class="text-gradient"><Sparkles size="12" class="inline text-cyan" /> Verdict Budget : </strong>
      <span v-if="result.monthlySavings > 0">
        Cette transition en leasing est **financièrement très avantageuse** pour votre trésorerie mensuelle. Vous gagnez **{{ formatCurrency(result.monthlySavings) }}** de reste à vivre chaque mois tout en roulant dans un véhicule plus moderne et écologique.
      </span>
      <span v-else>
        Le coût du leasing du nouveau véhicule engendre un surcoût mensuel net de **{{ formatCurrency(Math.abs(result.monthlySavings)) }}**. C'est un choix à arbitrer par rapport à l'agrément d'un véhicule neuf et votre réduction d'impact écologique.
      </span>
    </div>
  </div>
</template>

<style scoped>
.bg-cyan-glass {
  background: rgba(34, 211, 238, 0.05);
}
.border-glass {
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.glow-cyan {
  box-shadow: 0 0 15px -3px rgba(34, 211, 238, 0.1);
}
.text-cyan {
  color: #22d3ee;
}
.text-rose {
  color: #f43f5e;
}
.text-teal {
  color: #10b981;
}
.inline {
  display: inline-block;
  vertical-align: middle;
}
</style>
