<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { Zap, HelpCircle, ArrowRight, ArrowLeft, DollarSign, TrendingUp, Sparkles, AlertCircle, RefreshCw, Wrench, Check } from '@lucide/vue'
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

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

onMounted(() => {
  fetchVehicles()
})
</script>

<template>
  <div class="catalog-comparator-container animation-fadeIn">
    
    <!-- Hero Banner Épuré -->
    <div class="hero-banner-card mb-4">
      <div class="hero-content text-left">
        <div class="badge badge-teal mb-2.5 flex items-center gap-1.5 w-max">
          <TrendingUp size="13" /> <span>Analyse Multi-Véhicules</span>
        </div>
        <h2 class="hero-title text-main">Comparateur du Catalogue</h2>
        <p class="hero-description">
          Comparez votre véhicule de référence à plusieurs modèles simultanément. Classez les alternatives selon leur seuil de rentabilité (ROI) et leur impact sur votre trésorerie.
        </p>
      </div>
      <div class="hero-image-wrapper hide-on-mobile">
        <img :src="vehicleEcoSavingsImg" class="hero-brand-image" alt="EcoSwitch Transition" />
      </div>
    </div>

    <!-- Layout Formulaire + Résultats -->
    <div class="grid-cols-2">
      <!-- Section Formulaire -->
      <section class="card-glass" :class="{ 'mobile-hidden': activeMobileView === 'results' }">
        <!-- Bouton Aller aux résultats sur Mobile -->
        <div v-if="result" class="mobile-next-btn-container hide-on-desktop mb-3">
          <button class="btn btn-secondary btn-small w-100 flex-center gap-1.5" @click="activeMobileView = 'results'">
            <span>Voir les résultats comparatifs</span>
            <ArrowRight size="14" class="text-teal" />
          </button>
        </div>

        <div class="flex-between items-center mb-3">
          <h3 class="text-main font-heading text-md font-bold m-0">Configuration du comparatif</h3>
          <button class="btn btn-secondary btn-small flex-center" @click="fetchVehicles" title="Actualiser le catalogue">
            <RefreshCw size="14" :class="loading ? 'spinner' : ''" />
          </button>
        </div>

        <!-- Bascule Mode Simple / Avancé -->
        <div class="flex-between mb-4 p-2 bg-card-subtle rounded-xl border-glass text-xs">
          <span class="text-xxs text-dimmed font-bold uppercase">Niveau de précision</span>
          <div class="segmented-control">
            <button type="button" class="segmented-item" :class="{ active: !isAdvanced }" @click="isAdvanced = false">
              Standard
            </button>
            <button type="button" class="segmented-item" :class="{ active: isAdvanced }" @click="isAdvanced = true">
              Avancé
            </button>
          </div>
        </div>

        <div v-if="loading && vehicles.length === 0" class="flex-center py-4 text-xs text-muted">
          <Sparkles class="spinner text-teal mr-2" size="18" /> Chargement du catalogue...
        </div>

        <div v-else-if="vehicles.length === 0" class="text-center py-4 text-dimmed">
          <AlertCircle size="32" class="text-rose mb-2 mx-auto" />
          <p class="text-xs">Le catalogue est vide. Ajoutez des véhicules dans l'onglet Catalogue.</p>
        </div>

        <div v-else>
          <!-- 1. Votre véhicule actuel -->
          <div class="form-group mb-4 p-3.5 border-glass rounded-xl bg-card-subtle">
            <label class="form-label text-xs font-bold text-main mb-2">1. Votre véhicule de référence</label>
            
            <div v-if="hasProfiles">
              <p class="text-xxs text-muted mb-2">Véhicule sélectionné depuis votre Garage :</p>
              <select v-model="selectedProfileId" class="form-control form-select text-xs">
                <option v-for="p in userProfiles" :key="p.id" :value="p.id">
                  {{ p.name }} ({{ p.fuelType }})
                </option>
              </select>
            </div>
            
            <div v-else class="manual-vehicle-form">
              <p class="text-xxs text-amber mb-2">
                Saisie manuelle &middot; Créez un profil garage pour sauvegarder vos données.
              </p>
              <div class="form-group mb-2">
                <label class="form-label text-xxs">Nom du modèle</label>
                <input v-model="manualVehicle.name" type="text" class="form-control text-xs" />
              </div>
              <div class="grid-2-fields mb-2">
                <div class="form-group mb-0">
                  <label class="form-label text-xxs">Énergie</label>
                  <select v-model="manualVehicle.fuelType" class="form-control form-select text-xs">
                    <option value="PETROL">Essence</option>
                    <option value="DIESEL">Diesel</option>
                    <option value="ELECTRIC">Électrique</option>
                    <option value="HYBRID">Hybride</option>
                  </select>
                </div>
                <div class="form-group mb-0">
                  <label class="form-label text-xxs">Conso (L ou kWh/100km)</label>
                  <input v-model.number="manualVehicle.consumption" type="number" step="0.1" class="form-control text-xs" />
                </div>
              </div>
              <div class="grid-2-fields">
                <div class="form-group mb-0">
                  <label class="form-label text-xxs">Valeur de reprise (€)</label>
                  <input v-model.number="manualVehicle.resaleValue" type="number" class="form-control text-xs" />
                </div>
                <div class="form-group mb-0">
                  <label class="form-label text-xxs">Assurance (€/an)</label>
                  <input v-model.number="manualVehicle.insuranceCost" type="number" class="form-control text-xs" />
                </div>
              </div>
            </div>

            <div class="form-group border-t border-glass pt-2.5 mt-2.5 mb-0">
              <label class="form-label text-rose flex-between text-xs m-0">
                <span class="flex items-center gap-1"><Wrench size="13" /> Frais de réparations immédiats (€)</span>
                <span class="badge badge-rose badge-small">Optionnel</span>
              </label>
              <input v-model.number="immediateRepairCost" type="number" min="0" class="form-control text-xs mt-1" placeholder="ex: 2500" />
            </div>
          </div>

          <!-- 2. Sélection Véhicules Cibles -->
          <div class="form-group mb-4 p-3.5 border-glass rounded-xl bg-card-subtle">
            <div class="flex-between items-center mb-2.5">
              <label class="form-label text-xs font-bold text-main m-0">2. Cochez les modèles cibles à comparer</label>
              <span class="badge badge-teal badge-small">{{ selectedTargetIds.length }} sélectionné{{ selectedTargetIds.length > 1 ? 's' : '' }}</span>
            </div>

            <div class="targets-checklist flex flex-column gap-1.5">
              <div
                v-for="v in vehicles"
                :key="v.id" 
                class="target-item p-2.5 rounded-xl border-glass flex-between cursor-pointer"
                :class="{ selected: selectedTargetIds.includes(v.id) }"
                @click="toggleTargetSelection(v.id)"
              >
                <div class="flex items-center gap-2.5">
                  <div class="custom-checkbox flex-center" :class="{ checked: selectedTargetIds.includes(v.id) }">
                    <Check v-if="selectedTargetIds.includes(v.id)" size="12" />
                  </div>
                  <div>
                    <div class="text-xs font-bold text-main">{{ v.name }}</div>
                    <div class="text-xxs text-dimmed">{{ v.fuelType }} &middot; {{ v.consumption }} {{ v.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km</div>
                  </div>
                </div>
                <div class="text-xs font-bold text-main">{{ formatCurrency(v.purchasePrice) }}</div>
              </div>
            </div>
          </div>

          <!-- Paramètres généraux avancés -->
          <div v-if="isAdvanced" class="general-params p-3.5 border-glass rounded-xl mb-4 bg-card-subtle">
            <label class="form-label text-xxs uppercase text-dimmed mb-2 block">3. Paramètres de simulation</label>
            <div class="form-group mb-3">
              <label class="form-label text-xs">Horizon maximal (années)</label>
              <input v-model.number="maxYears" type="number" min="1" max="30" class="form-control text-xs" />
            </div>

            <label class="form-label text-xxs uppercase text-dimmed mb-2 block">Prix des énergies (€/L ou €/kWh)</label>
            <div class="grid-3-fields">
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Essence</label>
                <input v-model.number="fuelPrices.PETROL" type="number" step="0.01" class="form-control text-xs" />
              </div>
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Diesel</label>
                <input v-model.number="fuelPrices.DIESEL" type="number" step="0.01" class="form-control text-xs" />
              </div>
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Élec</label>
                <input v-model.number="fuelPrices.ELECTRIC" type="number" step="0.01" class="form-control text-xs" />
              </div>
            </div>
          </div>

          <button :disabled="calculating" class="btn btn-primary w-100 py-3 text-sm font-bold" @click="compare">
            <span v-if="calculating" class="spinner mr-2"><Zap size="16" /></span>
            <span v-else>Comparer les alternatives</span>
            <ArrowRight size="16" />
          </button>
        </div>

        <p v-if="error" class="error-msg flex-center mt-3 text-rose text-xs">
          <AlertCircle size="16" class="mr-1.5 shrink-0" /> {{ error }}
        </p>
      </section>

      <!-- Section Résultats -->
      <section class="card-glass flex flex-column" :class="{ 'mobile-hidden': activeMobileView === 'form' }">
        <div class="mobile-back-btn-container hide-on-desktop mb-3">
          <button class="btn btn-secondary btn-small flex items-center gap-1" @click="activeMobileView = 'form'">
            <ArrowLeft size="14" />
            <span>Modifier la sélection</span>
          </button>
        </div>

        <div v-if="!result && !calculating" class="flex-center flex-column h-100 text-center py-5">
          <HelpCircle size="48" class="mb-3 text-teal opacity-40" />
          <h4 class="text-main font-heading text-md font-bold mb-1">Prêt pour la comparaison</h4>
          <p class="text-xs text-muted max-w-sm m-0">Sélectionnez les modèles cibles à gauche et lancez le calcul pour découvrir les options les plus rentables.</p>
        </div>

        <div v-if="calculating" class="flex-center flex-column h-100 py-5">
          <Zap size="48" class="spinner text-teal mb-3" />
          <h4 class="text-main font-heading text-md font-bold mb-1">Calcul des alternatives...</h4>
          <p class="text-xs text-muted">Comparaison des coûts d'énergie, d'entretien et du retour sur investissement...</p>
        </div>

        <div v-if="result" class="results-layout flex-1 flex flex-column">
          <div class="mb-3">
            <h3 class="text-main font-heading text-md font-bold mb-1">Résultats du Comparatif</h3>
            <p class="text-xs text-muted m-0">
              Référence : <strong class="text-main">{{ result.currentVehicleName }}</strong> &middot; Classées par rentabilité
            </p>
          </div>

          <!-- Liste des alternatives classées -->
          <div class="alternatives-list flex-1 overflow-y-auto flex flex-column gap-3">
            <div
              v-for="(alt, idx) in result.alternatives"
              :key="alt.vehicleId" 
              class="alt-card p-3.5 rounded-xl border-glass"
              :class="alt.breakEvenYear && alt.breakEvenYear <= 3 ? 'border-teal' : alt.breakEvenYear ? 'border-cyan' : 'border-rose'"
            >
              <!-- En-tête avec Rang et Seuil -->
              <div class="flex-between items-center mb-2">
                <div class="flex items-center gap-2">
                  <span class="badge badge-teal badge-small">Rang #{{ idx + 1 }}</span>
                  <span class="text-xs font-bold text-main">{{ alt.vehicleName }}</span>
                </div>
                <span class="badge badge-small font-bold" :class="alt.breakEvenYear ? 'badge-teal' : 'badge-rose'">
                  {{ alt.breakEvenYear ? `Rentable en ${alt.breakEvenYear} ans` : 'Non rentable' }}
                </span>
              </div>

              <!-- Caractéristiques financières clés -->
              <div class="grid-3-fields gap-2 my-2.5">
                <div class="stat-box text-center p-2 rounded-lg border-glass bg-card-subtle">
                  <div class="text-xxs text-dimmed uppercase font-bold">Économie / an</div>
                  <div class="text-sm font-bold mt-0.5" :class="alt.annualSavings > 0 ? 'text-teal' : 'text-rose'">
                    {{ formatCurrency(alt.annualSavings) }}
                  </div>
                </div>
                <div class="stat-box text-center p-2 rounded-lg border-glass bg-card-subtle">
                  <div class="text-xxs text-dimmed uppercase font-bold">Investissement</div>
                  <div class="text-sm font-bold text-rose mt-0.5">
                    {{ formatCurrency(alt.switchInvestment) }}
                  </div>
                </div>
                <div class="stat-box text-center p-2 rounded-lg border-glass bg-card-subtle">
                  <div class="text-xxs text-dimmed uppercase font-bold">Bilan à {{ result.maxYears }} ans</div>
                  <div class="text-sm font-bold mt-0.5" :class="alt.totalCostDeltaAtHorizon <= 0 ? 'text-teal' : 'text-rose'">
                    {{ alt.totalCostDeltaAtHorizon <= 0 ? '+' : '' }}{{ formatCurrency(-alt.totalCostDeltaAtHorizon) }}
                  </div>
                </div>
              </div>

              <!-- Coût annuel comparé -->
              <div class="flex-between text-xxs text-muted pt-2 border-t border-glass">
                <span>Coût d'usage annuel (Énergie + Assur. + Entretien) :</span>
                <span class="font-bold text-teal">{{ formatCurrency(alt.targetAnnualCost) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

  </div>
</template>

<style scoped>
.hero-banner-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  background: hsl(var(--bg-card));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 20px;
  padding: 1.75rem 2.25rem;
  box-shadow: var(--shadow-card);
}

.hero-title {
  font-size: 1.65rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin-bottom: 0.5rem;
  line-height: 1.2;
}

.hero-description {
  color: hsl(var(--text-muted));
  font-size: 0.88rem;
  line-height: 1.6;
  max-width: 38rem;
  margin: 0;
}

.hero-brand-image {
  max-height: 120px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  border: 1px solid hsl(var(--border-glass));
}

.targets-checklist {
  max-height: 220px;
  overflow-y: auto;
  padding-right: 4px;
}

.target-item {
  background: hsl(var(--bg-card));
  transition: all 0.2s ease;
}
.target-item:hover {
  border-color: hsl(var(--accent-teal) / 0.4);
}
.target-item.selected {
  border-color: hsl(var(--accent-teal));
  background: hsla(var(--accent-teal) / 0.08);
}

.custom-checkbox {
  width: 18px;
  height: 18px;
  border-radius: 5px;
  border: 1.5px solid hsl(var(--border-glass));
  background: hsl(var(--bg-card-subtle));
  flex-shrink: 0;
}
.custom-checkbox.checked {
  background: hsl(var(--accent-teal));
  border-color: hsl(var(--accent-teal));
  color: #fff;
}

.alt-card {
  background: hsl(var(--bg-card-subtle));
  border: 1.5px solid hsl(var(--border-glass));
  transition: all 0.2s ease;
}
.alt-card.border-teal {
  border-left: 4px solid hsl(var(--accent-teal));
}
.alt-card.border-cyan {
  border-left: 4px solid hsl(var(--accent-cyan));
}
.alt-card.border-rose {
  border-left: 4px solid hsl(var(--accent-rose));
}

.w-max { width: max-content; }
.mr-2 { margin-right: 0.5rem; }
.mr-1\.5 { margin-right: 0.375rem; }
.max-w-sm { max-width: 20rem; }

@media (max-width: 768px) {
  .hero-banner-card {
    flex-direction: column;
    padding: 1.25rem;
    text-align: center;
  }
}
</style>
