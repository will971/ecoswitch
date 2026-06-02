<script setup>
import { ref, onMounted } from 'vue'
import { Zap, HelpCircle, ArrowRight, DollarSign, TrendingUp, Sparkles, AlertCircle, RefreshCw, Wrench, ChevronDown, ChevronUp } from '@lucide/vue'

const vehicles = ref([])
const loading = ref(false)
const calculating = ref(false)
const error = ref(null)

const currentVehicleId = ref(null)
const selectedTargetIds = ref([])
const maxYears = ref(15)
const immediateRepairCost = ref(0)
const isAdvanced = ref(false)

const fuelPrices = ref({
  PETROL: 1.88,
  DIESEL: 1.74,
  HYBRID: 1.82,
  ELECTRIC: 0.25
})

const result = ref(null)

const fetchVehicles = async () => {
  loading.value = true
  error.value = null
  try {
    const response = await fetch('/api/v1/vehicules')
    if (!response.ok) throw new Error('Impossible de charger le catalogue de véhicules.')
    vehicles.value = await response.json()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const toggleTargetSelection = (id) => {
  const index = selectedTargetIds.value.indexOf(id)
  if (index > -1) {
    selectedTargetIds.value.splice(index, 1)
  } else {
    selectedTargetIds.value.push(id)
  }
}

const compare = async () => {
  if (!currentVehicleId.value) {
    error.value = "Veuillez sélectionner votre véhicule actuel."
    return
  }
  if (selectedTargetIds.value.length === 0) {
    error.value = "Veuillez cocher au moins un véhicule cible à comparer."
    return
  }

  calculating.value = true
  error.value = null
  result.value = null

  try {
    const response = await fetch('/api/v1/comparisons/profitability', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        currentVehicleId: currentVehicleId.value,
        targetVehicleIds: selectedTargetIds.value,
        fuelPricesByType: fuelPrices.value,
        maxYears: maxYears.value,
        immediateRepairCost: immediateRepairCost.value
      })
    })

    if (!response.ok) {
      const errData = await response.json()
      throw new Error(errData.error || 'Erreur lors du calcul de rentabilité.')
    }

    result.value = await response.json()
  } catch (err) {
    error.value = err.message
  } finally {
    calculating.value = false
  }
}

const getAlternativeClass = (alternative) => {
  if (alternative.breakEvenYear === null) return 'border-rose'
  if (alternative.breakEvenYear <= 3) return 'border-teal'
  return 'border-cyan'
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
}

onMounted(() => {
  fetchVehicles()
})
</script>

<template>
  <div class="catalog-comparator-container">
    <div class="header-section text-center mb-5">
      <h2 class="text-gradient mb-2">Comparateur du Catalogue</h2>
      <p class="text-muted">Comparez un véhicule actuel du catalogue à une ou plusieurs alternatives pour identifier le meilleur choix de transition.</p>
    </div>

    <!-- Layout Formulaire + Résultats -->
    <div class="grid-cols-2">
      <!-- Section Formulaire -->
      <section class="card-glass glow-teal">
        <h3 class="mb-3 text-gradient-teal flex-between">
          <span>Configuration de la comparaison</span>
          <button class="btn btn-secondary btn-small flex-center" @click="fetchVehicles" title="Actualiser le catalogue">
            <RefreshCw size="14" :class="loading ? 'spinner' : ''" />
          </button>
        </h3>

        <!-- Bascule Mode Simple / Avancé -->
        <div class="flex-between mb-4 p-2 bg-deep-glass rounded border-glass text-xs">
          <span class="text-xxs text-dimmed font-semibold uppercase">⚙️ Options de saisie</span>
          <div class="flex gap-2">
            <button type="button" class="btn btn-secondary btn-small py-1 px-3 text-xxs font-semibold" :class="!isAdvanced ? 'active-mode' : ''" @click="isAdvanced = false">
              Mode Simplifié
            </button>
            <button type="button" class="btn btn-secondary btn-small py-1 px-3 text-xxs font-semibold" :class="isAdvanced ? 'active-mode' : ''" @click="isAdvanced = true">
              Mode Avancé
            </button>
          </div>
        </div>

        <div v-if="loading && vehicles.length === 0" class="flex-center py-4">
          <Sparkles class="spinner text-teal mr-2" size="20" /> Chargement du catalogue...
        </div>

        <div v-else-if="vehicles.length === 0" class="text-center py-4 text-dimmed">
          <AlertCircle size="32" class="text-rose mb-2" />
          <p>Le catalogue est vide. Créez d'abord des véhicules dans l'onglet "Gestion du Catalogue".</p>
        </div>

        <div v-else>
          <!-- Sélection Véhicule Actuel -->
          <div class="form-group mb-4 p-3 border-glass rounded">
            <label class="form-label text-cyan font-semibold">1. Votre véhicule actuel</label>
            <select v-model="currentVehicleId" class="form-control form-select" @change="selectedTargetIds = selectedTargetIds.filter(id => id !== currentVehicleId); immediateRepairCost = 0">
              <option :value="null" disabled>-- Sélectionner le véhicule --</option>
              <option v-for="v in vehicles" :key="v.id" :value="v.id">
                {{ v.name }} ({{ v.fuelType }})
              </option>
            </select>

            <div v-if="currentVehicleId" class="form-group border-t border-glass pt-3 mt-3">
              <label class="form-label text-rose font-semibold flex-between">
                <span class="flex items-center gap-1"><Wrench size="14" /> Frais de réparations immédiats (€)</span>
                <span class="badge badge-rose badge-small">Frais de garage</span>
              </label>
              <input v-model.number="immediateRepairCost" type="number" min="0" class="form-control border-rose-focus" placeholder="ex: 3000" />
              <p class="text-xxs text-dimmed mt-1">Saisissez le coût des réparations requises si vous décidez de conserver votre voiture actuelle.</p>
            </div>
          </div>

          <!-- Sélection Véhicules Cibles -->
          <div class="form-group mb-4 p-3 border-glass rounded">
            <label class="form-label text-teal font-semibold mb-3">2. Sélectionnez les véhicules cibles à comparer</label>
            <div class="targets-checklist">
              <div v-for="v in vehicles.filter(v => v.id !== currentVehicleId)" :key="v.id" 
                   class="target-checkbox-item flex-between p-2 rounded mb-2 border-glass"
                   :class="selectedTargetIds.includes(v.id) ? 'bg-card-selected' : ''"
                   @click="toggleTargetSelection(v.id)">
                <div class="flex-center">
                  <input type="checkbox" :checked="selectedTargetIds.includes(v.id)" class="mr-3 pointer-events-none" />
                  <div>
                    <div class="text-sm font-semibold">{{ v.name }}</div>
                    <div class="text-xs text-dimmed">{{ v.fuelType }} &middot; Consommation: {{ v.consumption }} {{ v.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km</div>
                  </div>
                </div>
                <div class="font-semibold text-sm">{{ formatCurrency(v.purchasePrice) }}</div>
              </div>
              <div v-if="vehicles.filter(v => v.id !== currentVehicleId).length === 0" class="text-center text-dimmed py-3 text-xs">
                Aucune alternative disponible en base de données.
              </div>
            </div>
          </div>

          <!-- Paramètres généraux (Uniquement en Mode Avancé) -->
          <div v-if="isAdvanced" class="general-params p-3 border-glass rounded mb-4">
            <h4 class="mb-3 text-muted text-sm uppercase">3. Paramètres de simulation</h4>
            <div class="form-group mb-3">
              <label class="form-label text-xs">Horizon maximal de calcul (ans)</label>
              <input v-model.number="maxYears" type="number" min="1" max="30" class="form-control" />
            </div>

            <h5 class="my-3 text-dimmed text-xs uppercase">Prix des énergies (€/L ou €/kWh)</h5>
            <div class="grid-4-fields">
              <div class="form-group">
                <label class="form-label text-xs">Essence</label>
                <input v-model.number="fuelPrices.PETROL" type="number" step="0.01" class="form-control form-control-sm" />
              </div>
              <div class="form-group">
                <label class="form-label text-xs">Diesel</label>
                <input v-model.number="fuelPrices.DIESEL" type="number" step="0.01" class="form-control form-control-sm" />
              </div>
              <div class="form-group">
                <label class="form-label text-xs">Hybride</label>
                <input v-model.number="fuelPrices.HYBRID" type="number" step="0.01" class="form-control form-control-sm" />
              </div>
              <div class="form-group">
                <label class="form-label text-xs">Élec</label>
                <input v-model.number="fuelPrices.ELECTRIC" type="number" step="0.01" class="form-control form-control-sm" />
              </div>
            </div>
          </div>

          <button :disabled="calculating" class="btn btn-primary w-100" @click="compare">
            <span v-if="calculating" class="spinner"><Zap size="18" /></span>
            <span v-else>Comparer les alternatives</span>
            <ArrowRight size="18" />
          </button>
        </div>

        <p v-if="error" class="error-msg flex-center mt-3 text-rose">
          <AlertCircle size="18" class="mr-2" /> {{ error }}
        </p>
      </section>

      <!-- Section Résultats -->
      <section class="card-glass flex flex-column justify-between">
        <div v-if="!result && !calculating" class="flex-center flex-column h-100 text-center text-dimmed py-5">
          <HelpCircle size="64" class="mb-3 text-teal opacity-40" />
          <h4 class="mb-2 text-muted">Prêt pour la comparaison</h4>
          <p class="max-w-sm">Sélectionnez les véhicules à comparer dans la base de données à gauche et lancez l'analyse pour trier les options par rentabilité financière.</p>
        </div>

        <div v-if="calculating" class="flex-center flex-column h-100 py-5">
          <Zap size="64" class="spinner text-teal mb-3" />
          <h4 class="text-teal">Calculs comparatifs en cours...</h4>
          <p class="text-muted">Nous comparons les budgets énergies, assurances et maintenances pour chaque modèle...</p>
        </div>

        <div v-if="result" class="results-layout h-100 flex flex-column">
          <h3 class="mb-4 text-gradient">Résultats comparatifs</h3>
          <p class="text-muted text-xs mb-4">
            Comparatif par rapport à votre véhicule actuel : <strong>{{ result.currentVehicleName }}</strong>.
            Les alternatives sont triées par rentabilité décroissante.
          </p>

          <!-- Liste des alternatives classées -->
          <div class="alternatives-list flex-1 overflow-y-auto">
            <div v-for="(alt, idx) in result.alternatives" :key="alt.vehicleId" 
                 class="alt-card-item card-glass p-3 mb-3 border-l-4" 
                 :class="getAlternativeClass(alt)">
              
              <!-- En-tête avec Rang et Seuil -->
              <div class="flex-between mb-2">
                <h4 class="font-heading text-md flex-center">
                  <span class="badge badge-teal mr-2">Option N°{{ idx + 1 }}</span>
                  <span class="text-gradient">{{ alt.vehicleName }}</span>
                </h4>
                <span class="badge font-bold" :class="alt.breakEvenYear ? 'badge-teal' : 'badge-rose'">
                  {{ alt.breakEvenYear ? 'Rentable en ' + alt.breakEvenYear + ' ans' : 'Non Rentable' }}
                </span>
              </div>

              <!-- Caractéristiques financières clés -->
              <div class="stats-grid-small mt-3">
                <div class="stat-mini text-center p-2 rounded border-glass">
                  <div class="text-xxs text-dimmed uppercase">Économies / an</div>
                  <div class="font-heading text-sm mt-1" :class="alt.annualSavings > 0 ? 'text-teal' : 'text-rose'">
                    {{ formatCurrency(alt.annualSavings) }}
                  </div>
                </div>
                <div class="stat-mini text-center p-2 rounded border-glass">
                  <div class="text-xxs text-dimmed uppercase">Investissement</div>
                  <div class="font-heading text-sm mt-1 text-rose">
                    {{ formatCurrency(alt.switchInvestment) }}
                  </div>
                </div>
                <div class="stat-mini text-center p-2 rounded border-glass">
                  <div class="text-xxs text-dimmed uppercase">Bilan à {{ maxYears }} ans</div>
                  <div class="font-heading text-sm mt-1" :class="alt.totalCostDeltaAtHorizon <= 0 ? 'text-teal' : 'text-rose'">
                    {{ alt.totalCostDeltaAtHorizon <= 0 ? '-' : '+' }}{{ formatCurrency(Math.abs(alt.totalCostDeltaAtHorizon)) }}
                  </div>
                </div>
              </div>

              <!-- Mini comparateur de coût annuel -->
              <div class="cost-summary mt-2 text-xxs text-dimmed flex-between">
                <span>Coût annuel (carburant + maint. + assu) :</span>
                <span>
                  Actuel: {{ formatCurrency(alt.currentAnnualCost) }} &middot; 
                  Cible: <strong class="text-teal">{{ formatCurrency(alt.targetAnnualCost) }}</strong>
                </span>
              </div>

              <!-- Mini-Arbitrage s'il y a des réparations immédiates -->
              <div v-if="immediateRepairCost > 0" class="mt-2 p-2 bg-deep-glass rounded text-xxs text-dimmed border-glass">
                <span class="flex items-center gap-1 text-rose font-semibold mb-1">
                  <Wrench size="12" /> Arbitrage Réparation
                </span>
                <span>Effort de trésorerie net initial : <strong>{{ formatCurrency(alt.switchInvestment - immediateRepairCost) }}</strong></span>
                <div class="mt-1 font-semibold text-xxs text-muted">
                  <span v-if="alt.breakEvenYear && alt.breakEvenYear <= 3" class="text-teal">
                    Recommandation : Remplacer (rentable sous {{ alt.breakEvenYear }} ans).
                  </span>
                  <span v-else-if="alt.breakEvenYear && alt.breakEvenYear <= 7" class="text-cyan">
                    Recommandation : Remplacer pertinent à moyen terme ({{ alt.breakEvenYear }} ans).
                  </span>
                  <span v-else-if="alt.breakEvenYear" class="text-amber">
                    Recommandation : Réparation envisageable, amortissement long ({{ alt.breakEvenYear }} ans).
                  </span>
                  <span v-else class="text-rose">
                    Recommandation : Faire réparer votre véhicule actuel.
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.grid-4-fields {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
@media (max-width: 576px) {
  .grid-4-fields {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
.targets-checklist {
  max-height: 250px;
  overflow-y: auto;
  padding-right: 4px;
}
.target-checkbox-item {
  cursor: pointer;
  background: rgba(255, 255, 255, 0.01);
  transition: all 0.2s ease;
}
.target-checkbox-item:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: hsl(var(--accent-cyan) / 0.3);
}
.bg-card-selected {
  background: rgba(8, 145, 178, 0.08) !important;
  border-color: hsl(var(--accent-cyan) / 0.5) !important;
}
.stats-grid-small {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.stat-mini {
  background: rgba(255, 255, 255, 0.02);
}
.border-l-4 {
  border-left-width: 4px !important;
}
.border-teal { border-left-color: hsl(var(--accent-teal)) !important; }
.border-cyan { border-left-color: hsl(var(--accent-cyan)) !important; }
.border-rose { border-left-color: hsl(var(--accent-rose)) !important; }

.border-t { border-top: 1px solid rgba(255, 255, 255, 0.1); }
.pt-3 { padding-top: 12px; }
.mt-3 { margin-top: 12px; }
.gap-1 { gap: 4px; }
.items-center { align-items: center; }
.bg-deep-glass { background: rgba(0, 0, 0, 0.2); }
.border-rose-focus:focus {
  border-color: hsl(var(--accent-rose)) !important;
  box-shadow: 0 0 0 3px rgba(225, 29, 72, 0.2) !important;
}
.text-teal { color: hsl(var(--accent-teal)); }
.text-rose { color: hsl(var(--accent-rose)); }
.text-cyan { color: hsl(var(--accent-cyan)); }
.text-xs { font-size: 0.75rem; }
.text-xxs { font-size: 0.65rem; }
.text-sm { font-size: 0.875rem; }
.text-md { font-size: 1rem; }
.font-semibold { font-weight: 600; }
.mr-2 { margin-right: 0.5rem; }
.mr-3 { margin-right: 0.75rem; }
.pointer-events-none { pointer-events: none; }
.w-100 { width: 100%; }
.h-100 { height: 100%; }
.opacity-40 { opacity: 0.4; }
.max-w-sm { max-width: 20rem; }
.py-4 { padding-top: 2rem; padding-bottom: 2rem; }
.py-5 { padding-top: 3rem; padding-bottom: 3rem; }
.py-3 { padding-top: 1.5rem; padding-bottom: 1.5rem; }
.flex-1 { flex: 1; }
.overflow-y-auto { overflow-y: auto; }
.active-mode {
  background: linear-gradient(135deg, hsl(var(--accent-teal) / 0.15) 0%, hsl(var(--accent-cyan) / 0.15) 100%) !important;
  border-color: hsl(var(--accent-cyan) / 0.7) !important;
  color: hsl(var(--accent-cyan)) !important;
  box-shadow: 0 0 10px 0 hsl(var(--accent-cyan) / 0.1);
}
</style>
