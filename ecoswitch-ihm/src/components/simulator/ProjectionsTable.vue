<script setup>
import { ref } from 'vue'
import { ChevronUp, ChevronDown } from '@lucide/vue'

defineProps({
  yearlyForecast: {
    type: Array,
    required: true
  }
})

const showProjectionsTable = ref(false)

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
}
</script>

<template>
  <div>
    <!-- Bouton Accordéon pour le Tableau des Projections -->
    <div class="mt-4 border-t border-glass pt-3 flex-center">
      <button type="button" class="btn btn-secondary w-100 flex-center gap-2 text-xxs font-semibold" @click="showProjectionsTable = !showProjectionsTable">
        <span>{{ showProjectionsTable ? 'Masquer le tableau des projections' : 'Afficher le tableau des projections détaillées' }}</span>
        <component :is="showProjectionsTable ? ChevronUp : ChevronDown" size="14" class="text-teal" />
      </button>
    </div>

    <!-- Projections année par année -->
    <div v-if="showProjectionsTable" class="table-container mt-3">
      <table class="table-glass">
        <thead>
          <tr>
            <th>Année</th>
            <th>Coût Actuel (Réparé)</th>
            <th>Coût Cible</th>
            <th>Bilan</th>
            <th>État</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in yearlyForecast" :key="row.year">
            <td data-label="Année">{{ row.year }}</td>
            <td data-label="Coût Actuel (Réparé)">{{ formatCurrency(row.currentCost) }}</td>
            <td data-label="Coût Cible">{{ formatCurrency(row.targetCost) }}</td>
            <td data-label="Bilan" :class="row.difference <= 0 ? 'text-teal' : 'text-rose'">
              {{ row.difference <= 0 ? '-' : '+' }}{{ formatCurrency(Math.abs(row.difference)) }}
            </td>
            <td data-label="État">
              <span class="badge" :class="row.isProfitable ? 'badge-teal' : 'badge-rose'">
                {{ row.isProfitable ? 'Rentable' : 'Déficit' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 600px) {
  /* Force table elements to display as block */
  .table-glass, 
  .table-glass thead, 
  .table-glass tbody, 
  .table-glass th, 
  .table-glass td, 
  .table-glass tr { 
    display: block; 
  }
  
  /* Hide the table headers off-screen for accessibility (screen readers can still access it) */
  .table-glass thead tr { 
    position: absolute;
    top: -9999px;
    left: -9999px;
  }
  
  .table-glass tr {
    margin-bottom: 12px;
    border: 1px solid hsl(var(--border-glass));
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.01);
    padding: 8px 12px;
  }
  
  .table-glass td { 
    border: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05); 
    position: relative;
    padding-left: 50% !important; 
    text-align: right !important;
    padding: 10px 0;
    min-height: 40px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
  
  .table-glass td:last-child {
    border-bottom: 0;
  }
  
  /* Add label on the left using the data-label attribute */
  .table-glass td::before { 
    content: attr(data-label);
    position: absolute;
    left: 0;
    width: 45%; 
    padding-right: 10px; 
    white-space: nowrap;
    text-align: left;
    font-weight: 600;
    color: hsl(var(--text-muted));
    font-size: 0.8rem;
  }
}
</style>

