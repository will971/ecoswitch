<script setup>
import { ref } from 'vue'

defineProps({
  yearlyForecast: {
    type: Array,
    required: true
  }
})

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}
</script>

<template>
  <div class="projections-wrapper">
    <div class="table-container">
      <table class="table-glass">
        <thead>
          <tr>
            <th>Année</th>
            <th>Coût Actuel Cumulé</th>
            <th>Coût Nouveau Cumulé</th>
            <th>Bilan Net Cumulé</th>
            <th>Statut</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in yearlyForecast" :key="row.year" class="table-row">
            <td data-label="Année" class="font-bold text-main">Année {{ row.year }}</td>
            <td data-label="Coût Actuel Cumulé">{{ formatCurrency(row.currentCost) }}</td>
            <td data-label="Coût Nouveau Cumulé">{{ formatCurrency(row.targetCost) }}</td>
            <td data-label="Bilan Net Cumulé" :class="row.difference <= 0 ? 'text-teal' : 'text-rose'" class="font-bold">
              {{ row.difference <= 0 ? '+' : '-' }}{{ formatCurrency(Math.abs(row.difference)) }}
            </td>
            <td data-label="Statut">
              <span class="badge badge-small" :class="row.isProfitable ? 'badge-teal' : 'badge-amber'">
                {{ row.isProfitable ? 'Rentable' : 'Investissement' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.projections-wrapper {
  width: 100%;
}

.table-container {
  width: 100%;
  overflow-x: auto;
  border-radius: 14px;
  border: 1px solid hsl(var(--border-glass));
  background: hsl(var(--bg-card-subtle));
}

.table-glass {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.8rem;
}

.table-glass th {
  background: transparent;
  color: hsl(var(--text-dimmed));
  font-weight: 700;
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 12px 16px;
  border-bottom: 1px solid hsl(var(--border-glass));
}

.table-glass td {
  padding: 12px 16px;
  border-bottom: 1px solid hsl(var(--border-glass) / 0.5);
  color: hsl(var(--text-main));
}

.table-glass tr:last-child td {
  border-bottom: none;
}

.table-row:hover td {
  background: hsla(var(--accent-teal) / 0.04);
}

@media (max-width: 600px) {
  .table-glass th, .table-glass td {
    padding: 8px 10px;
    font-size: 0.72rem;
  }
}
</style>
