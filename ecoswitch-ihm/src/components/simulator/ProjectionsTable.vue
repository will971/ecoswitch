<script setup>
defineProps({
  yearlyForecast: {
    type: Array,
    required: true
  },
  isLeasing: {
    type: Boolean,
    default: false
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
          <tr v-if="isLeasing">
            <th>Durée</th>
            <th>
              Dépenses Véhicule Actuel
              <span class="th-sub">Carburant + Entretien</span>
            </th>
            <th>
              Dépenses Nouveau Véhicule
              <span class="th-sub">Loyers Leasing + Électricité</span>
            </th>
            <th>
              Économie Nette Réalisée
              <span class="th-sub">Gain en poche cumulé</span>
            </th>
            <th>Bilan Trésorerie</th>
          </tr>
          <tr v-else>
            <th>Année</th>
            <th>
              Coût Total Actuel
              <span class="th-sub">Carburant + Entretien</span>
            </th>
            <th>
              Coût Total Nouveau
              <span class="th-sub">Achat net + Énergie + Entretien</span>
            </th>
            <th>
              Économie Nette Cumulée
              <span class="th-sub">Gain net amorti</span>
            </th>
            <th>Statut Amortissement</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in yearlyForecast" :key="row.year" class="table-row">
            <td data-label="Période" class="font-semibold text-main">
              {{ isLeasing ? `Fin Année ${row.year} (${row.year * 12} mois)` : `Année ${row.year}` }}
            </td>
            <td data-label="Véhicule Actuel" class="font-mono text-muted">
              {{ formatCurrency(row.currentCost) }}
            </td>
            <td data-label="Nouveau Véhicule" class="font-mono text-muted">
              {{ formatCurrency(row.targetCost) }}
            </td>
            <td 
              data-label="Économie Nette" 
              class="font-mono font-bold"
              :class="row.isProfitable ? 'text-teal' : 'text-rose'"
            >
              {{ row.isProfitable ? `+${formatCurrency(Math.abs(row.difference))}` : `-${formatCurrency(Math.abs(row.difference))}` }}
            </td>
            <td data-label="Statut">
              <span class="badge badge-small" :class="row.isProfitable ? 'badge-teal' : (isLeasing ? 'badge-amber' : 'badge-amber')">
                <template v-if="isLeasing">
                  {{ row.isProfitable ? 'Gain Net Réalisé' : 'Effort Mensuel Absorbé' }}
                </template>
                <template v-else>
                  {{ row.isProfitable ? 'Investissement Amorti' : 'En cours d\'amortissement' }}
                </template>
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
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
}

.table-glass {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.78rem;
}

.table-glass th {
  background: transparent;
  color: var(--text-main);
  font-weight: 600;
  font-size: 0.72rem;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-glass);
  vertical-align: top;
}

.th-sub {
  display: block;
  font-size: 0.64rem;
  font-weight: 400;
  color: var(--text-dimmed);
  margin-top: 2px;
}

.table-glass td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-subtle);
  color: var(--text-main);
}

.table-glass tr:last-child td {
  border-bottom: none;
}

.table-row:hover td {
  background: var(--bg-card-hover);
}

@media (max-width: 600px) {
  .table-glass th, .table-glass td {
    padding: 8px 10px;
    font-size: 0.72rem;
  }
}
</style>
