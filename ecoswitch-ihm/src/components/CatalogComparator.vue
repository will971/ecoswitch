<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { Zap, HelpCircle, ArrowRight, ArrowLeft, DollarSign, TrendingUp, Sparkles, AlertCircle, RefreshCw, Wrench, ChevronDown, ChevronUp } from '@lucide/vue'
import vehicleEcoSavingsImg from '../assets/vehicle_eco_savings.png'

const props = defineProps({
  currentUser: Object,
  userProfiles: {
    type: Array,
    default: () => []
  },
  activeUserProfile: Object
})

const vehicles = ref([])
const loading = ref(false)
const calculating = ref(false)
const error = ref(null)

const selectedProfileId = ref(null)
const selectedTargetIds = ref([])
const maxYears = ref(15)
const immediateRepairCost = ref(0)
const isAdvanced = ref(false)
const activeMobileView = ref('form') // form or results

const manualVehicle = ref({
  name: 'Mon véhicule',
  fuelType: 'PETROL',
  consumption: 7.0,
  purchasePrice: 0,
  resaleValue: 5000,
  insuranceCost: 600,
  maintenanceCost: 400
})

const fuelPrices = ref({
  PETROL: 1.88,
  DIESEL: 1.74,
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

watch(() => props.activeUserProfile, (newProfile) => {
  if (newProfile) {
    selectedProfileId.value = newProfile.id
    if (newProfile.petrolPrice) fuelPrices.value.PETROL = newProfile.petrolPrice
    if (newProfile.dieselPrice) fuelPrices.value.DIESEL = newProfile.dieselPrice
    if (newProfile.electricPrice) fuelPrices.value.ELECTRIC = newProfile.electricPrice
  }
}, { immediate: true })

const toggleTargetSelection = (id) => {
  const index = selectedTargetIds.value.indexOf(id)
  if (index > -1) {
    selectedTargetIds.value.splice(index, 1)
  } else {
    selectedTargetIds.value.push(id)
  }
}

const hasProfiles = computed(() => props.userProfiles && props.userProfiles.length > 0)

const compare = async () => {
  if (selectedTargetIds.value.length === 0) {
    error.value = "Veuillez cocher au moins un véhicule cible à comparer."
    return
  }

  calculating.value = true
  error.value = null
  result.value = null

  let currentVehicleData = null
  if (hasProfiles.value) {
    if (!selectedProfileId.value) {
      error.value = "Veuillez sélectionner un de vos véhicules."
      calculating.value = false
      return
    }
    const profile = props.userProfiles.find(p => p.id === selectedProfileId.value)
    currentVehicleData = { ...profile, purchasePrice: 0 }
  } else {
    currentVehicleData = { ...manualVehicle.value }
  }

  try {
    const response = await fetch('/api/v1/comparisons/profitability/custom', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        currentVehicle: currentVehicleData,
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
    activeMobileView.value = 'results'
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
    <!-- Hero Banner Premium avec Image & Identité Visuelle -->
    <div class="hero-banner-card mb-5">
      <div class="hero-content text-left">
        <div class="badge badge-teal mb-3 flex items-center gap-1 w-max">
          <TrendingUp size="12" /> <span>Analyse Comparative de Flotte</span>
        </div>
        <h2 class="hero-title font-heading">Comparateur du Catalogue</h2>
        <p class="hero-description">
          Comparez votre véhicule actuel à une ou plusieurs alternatives cibles pour déterminer précisément le point de rentabilité et le retour sur investissement écologique.
        </p>
      </div>
      <div class="hero-image-wrapper hide-on-mobile">
        <img :src="vehicleEcoSavingsImg" class="hero-brand-image" alt="EcoSwitch Transition" />
      </div>
    </div>

    <!-- Layout Formulaire + Résultats -->
    <div class="grid-cols-2">
      <!-- Section Formulaire -->
      <section class="card-glass glow-teal" :class="{ 'mobile-hidden': activeMobileView === 'results' }">
        <!-- Bouton Aller aux résultats sur Mobile uniquement s'il y a un résultat déjà calculé -->
        <div v-if="result" class="mobile-next-btn-container hide-on-desktop mb-3">
          <button class="btn btn-secondary btn-small w-100 flex-center gap-1 border-teal" @click="activeMobileView = 'results'">
            <span>Voir les résultats de comparaison</span>
            <ArrowRight size="14" class="text-teal" />
          </button>
        </div>
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
          <div class="form-group mb-4 p-3 border-glass rounded bg-deep-glass">
            <label class="form-label text-cyan font-semibold">1. Votre véhicule actuel</label>
            
            <div v-if="hasProfiles">
              <p class="text-xs text-dimmed mb-2">Sélectionnez le véhicule à comparer depuis votre garage :</p>
              <select v-model="selectedProfileId" class="form-control form-select border-cyan text-cyan">
                <option v-for="p in userProfiles" :key="p.id" :value="p.id">
                  {{ p.name }} ({{ p.fuelType }})
                </option>
              </select>
            </div>
            
            <div v-else class="manual-vehicle-form">
              <p class="text-xs text-amber mb-3">
                Connectez-vous et créez un profil pour éviter la saisie manuelle.
              </p>
              <div class="form-group mb-2">
                <label class="form-label text-xs">Nom du véhicule</label>
                <input v-model="manualVehicle.name" type="text" class="form-control form-control-sm" />
              </div>
              <div class="flex gap-2 mb-2">
                <div class="form-group w-100">
                  <label class="form-label text-xs">Énergie</label>
                  <select v-model="manualVehicle.fuelType" class="form-control form-select form-control-sm">
                    <option value="PETROL">Essence</option>
                    <option value="DIESEL">Diesel</option>
                    <option value="ELECTRIC">Électrique</option>
                    <option value="HYBRID">Hybride</option>
                  </select>
                </div>
                <div class="form-group w-100">
                  <label class="form-label text-xs">Conso</label>
                  <input v-model.number="manualVehicle.consumption" type="number" step="0.1" class="form-control form-control-sm" />
                </div>
              </div>
              <div class="flex gap-2">
                <div class="form-group w-100">
                  <label class="form-label text-xs">Revente (€)</label>
                  <input v-model.number="manualVehicle.resaleValue" type="number" class="form-control form-control-sm" />
                </div>
                <div class="form-group w-100">
                  <label class="form-label text-xs">Assurance (€/an)</label>
                  <input v-model.number="manualVehicle.insuranceCost" type="number" class="form-control form-control-sm" />
                </div>
              </div>
            </div>

            <div class="form-group border-t border-glass pt-3 mt-3">
              <label class="form-label text-rose font-semibold flex-between">
                <span class="flex items-center gap-1"><Wrench size="14" /> Frais de réparations immédiats (€)</span>
                <span class="badge badge-rose badge-small">Optionnel</span>
              </label>
              <input v-model.number="immediateRepairCost" type="number" min="0" class="form-control border-rose-focus" placeholder="ex: 3000" />
              <p class="text-xxs text-dimmed mt-1">Coût des réparations imminentes si vous gardez votre voiture.</p>
            </div>
          </div>

          <!-- Sélection Véhicules Cibles -->
          <div class="form-group mb-4 p-3 border-glass rounded">
            <label class="form-label text-teal font-semibold mb-3">2. Sélectionnez les alternatives du catalogue</label>
            <div class="targets-checklist">
              <div v-for="v in vehicles" :key="v.id" 
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
            <div class="grid-3-fields">
              <div class="form-group">
                <label class="form-label text-xs">Essence</label>
                <input v-model.number="fuelPrices.PETROL" type="number" step="0.01" class="form-control form-control-sm" />
              </div>
              <div class="form-group">
                <label class="form-label text-xs">Diesel</label>
                <input v-model.number="fuelPrices.DIESEL" type="number" step="0.01" class="form-control form-control-sm" />
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
      <section class="card-glass flex flex-column justify-between" :class="{ 'mobile-hidden': activeMobileView === 'form' }">
        <!-- Bouton Retour sur Mobile uniquement -->
        <div class="mobile-back-btn-container hide-on-desktop mb-3">
          <button class="btn btn-secondary btn-small flex-center gap-1" @click="activeMobileView = 'form'">
            <ArrowLeft size="14" class="text-teal" />
            <span>Retour à la saisie</span>
          </button>
        </div>
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
                  <div class="text-xxs text-dimmed uppercase">Bilan à {{ result.maxYears }} ans</div>
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
.grid-3-fields {
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
.gap-2 { gap: 8px; }
.items-center { align-items: center; }
.bg-deep-glass { background: rgba(0, 0, 0, 0.2); }
.border-rose-focus:focus {
  border-color: hsl(var(--accent-rose)) !important;
  box-shadow: 0 0 0 3px rgba(225, 29, 72, 0.2) !important;
}
.text-amber { color: #fbbf24; }
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

/* Premium Hero Banner Styles */
.hero-banner-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  background: radial-gradient(circle at top right, #0c201d 0%, #080c14 100%);
  border: 1px solid rgba(20, 184, 166, 0.2);
  border-radius: 16px;
  padding: 2rem 2.5rem;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;
}
.hero-banner-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.08) 0%, transparent 70%);
  z-index: 1;
  pointer-events: none;
}
.hero-content {
  z-index: 2;
  flex: 1;
}
.hero-title {
  font-size: 1.6rem;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff 40%, #a5f3fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.75rem;
}
.hero-description {
  color: #94a3b8;
  font-size: 0.85rem;
  line-height: 1.6;
  max-width: 32rem;
  margin: 0;
}
.hero-image-wrapper {
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.hero-brand-image {
  max-height: 140px;
  width: auto;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.4s ease;
}
.hero-brand-image:hover {
  transform: scale(1.05) rotate(1deg);
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.7), 0 0 20px rgba(20, 184, 166, 0.25);
}
.w-max {
  width: max-content;
}
@media (max-width: 768px) {
  .hero-banner-card {
    flex-direction: column;
    text-align: center;
    padding: 1.5rem;
  }
  .hero-content {
    text-align: center !important;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .hero-description {
    max-width: 100%;
  }
  .hide-on-mobile {
    display: none;
  }
}
</style>
