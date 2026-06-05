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
  <div class="projections-wrapper">
    <!-- Bouton Accordéon pour le Tableau des Projections (disclosure style Apple) -->
    <div class="accordion-header">
      <button type="button" class="apple-accordion-btn" @click="showProjectionsTable = !showProjectionsTable">
        <span>{{ showProjectionsTable ? 'Masquer les projections détaillées' : 'Afficher les projections année par année' }}</span>
        <component :is="showProjectionsTable ? ChevronUp : ChevronDown" size="14" class="chevron-icon" />
      </button>
    </div>

    <!-- Projections année par année -->
    <div v-if="showProjectionsTable" class="table-container mt-4">
      <table class="table-glass">
        <thead>
          <tr>
            <th>Année</th>
            <th>Coût Actuel (Réparé)</th>
            <th>Coût Cible</th>
            <th>Bilan cumulé</th>
            <th>État</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in yearlyForecast" :key="row.year" class="table-row">
            <td data-label="Année" class="font-semibold text-main">{{ row.year }}</td>
            <td data-label="Coût Actuel (Réparé)">{{ formatCurrency(row.currentCost) }}</td>
            <td data-label="Coût Cible">{{ formatCurrency(row.targetCost) }}</td>
            <td data-label="Bilan cumulé" :class="row.difference <= 0 ? 'text-teal' : 'text-rose'" class="font-bold">
              {{ row.difference <= 0 ? '-' : '+' }}{{ formatCurrency(Math.abs(row.difference)) }}
            </td>
            <td data-label="État">
              <span class="apple-badge" :class="row.isProfitable ? 'badge-success' : 'badge-danger'">
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
.projections-wrapper {
  width: 100%;
}

.accordion-header {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  border-top: 1px solid hsl(var(--border-glass));
  padding-top: 20px;
}

.apple-accordion-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: hsl(var(--bg-deep) / 0.25);
  border: 1px solid hsl(var(--border-glass));
  border-radius: 20px;
  padding: 8px 18px;
  font-family: var(--font-heading);
  font-size: 0.85rem;
  font-weight: 500;
  color: hsl(var(--text-main));
  cursor: pointer;
  transition: all 0.2s ease;
}
.apple-accordion-btn:hover {
  background: hsl(var(--bg-deep) / 0.6);
  border-color: hsl(var(--accent-teal) / 0.4);
}
.chevron-icon {
  color: hsl(var(--accent-teal));
}

/* ── Apple Style Table ── */
.table-container {
  width: 100%;
  overflow-x: auto;
  border-radius: 16px;
  border: 1px solid hsl(var(--border-glass));
  background: hsl(var(--bg-deep) / 0.1);
}

.table-glass {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.875rem;
}

.table-glass th {
  background: transparent;
  color: hsl(var(--text-dimmed));
  font-weight: 600;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 16px 20px;
  border-bottom: 1px solid hsl(var(--border-glass));
}

.table-glass td {
  padding: 16px 20px;
  border-bottom: 1px solid hsla(var(--border-glass) / 0.4);
  color: hsl(var(--text-main));
  font-family: var(--font-sans);
}

.table-glass th:first-child,
.table-glass td:first-child {
  padding-left: 24px;
}

.table-glass th:last-child,
.table-glass td:last-child {
  padding-right: 24px;
}

.table-glass tr:last-child td {
  border-bottom: none;
}

.table-row {
  transition: background-color 0.2s ease;
}
.table-row:hover td {
  background: hsla(var(--bg-hover) / 0.5);
}

/* Apple Badge Style */
.apple-badge {
  display: inline-flex;
  align-items: center;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
}
.badge-success {
  background: hsla(var(--accent-teal) / 0.12);
  color: hsl(var(--accent-teal));
  border: 1px solid hsla(var(--accent-teal) / 0.15);
}
.badge-danger {
  background: hsla(var(--accent-rose) / 0.12);
  color: hsl(var(--accent-rose));
  border: 1px solid hsla(var(--accent-rose) / 0.15);
}

.text-teal { color: hsl(var(--accent-teal)) !important; }
.text-rose { color: hsl(var(--accent-rose)) !important; }
.text-main { color: hsl(var(--text-main)); }

/* Responsive view */
@media (max-width: 768px) {
  .table-glass th {
    padding: 12px 14px;
  }
  .table-glass td {
    padding: 12px 14px;
  }
}

@media (max-width: 600px) {
  .table-glass, 
  .table-glass thead, 
  .table-glass tbody, 
  .table-glass th, 
  .table-glass td, 
  .table-glass tr { 
    display: block; 
  }
  
  .table-glass thead tr { 
    position: absolute;
    top: -9999px;
    left: -9999px;
  }
  
  .table-glass tr {
    margin-bottom: 12px;
    border-bottom: 1px solid hsl(var(--border-glass));
    padding: 8px 12px;
  }
  .table-glass tr:last-child {
    border-bottom: none;
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
  
  .table-glass td::before { 
    content: attr(data-label);
    position: absolute;
    left: 0;
    width: 45%; 
    padding-right: 10px; 
    white-space: nowrap;
    text-align: left;
    font-weight: 600;
    color: hsl(var(--text-dimmed));
    font-size: 0.8rem;
  }
}
</style>
