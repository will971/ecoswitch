<script setup>
import { ref, watch, onMounted } from 'vue'
import { Zap, ArrowRight, Sparkles, AlertCircle, Layers, SlidersHorizontal, CheckCircle2, RefreshCw } from '@lucide/vue'
import { apiGetCatalogVariants, apiGetLiveFuelPrices } from '../utils/api.js'

// Import des sous-composants
import ExpressWizard from './simulator/ExpressWizard.vue'
import VehicleFormBlock from './simulator/VehicleFormBlock.vue'
import SimulationResults from './simulator/SimulationResults.vue'

const simulationMode = ref('express') // 'express' | 'expert'

const props = defineProps({
  loadedSimulation: {
    type: Object,
    default: null
  },
  currentUser: {
    type: Object,
    default: null
  },
  userProfile: {
    type: Object,
    default: null
  }
})

const currentVehicle = ref({
  name: '',
  purchasePrice: 0,
  fuelType: '',
  consumption: null,
  annualMileage: null,
  insuranceCost: null,
  maintenanceCost: null,
  resaleValue: null
})

const targetVehicle = ref({
  name: '',
  purchasePrice: null,
  fuelType: '',
  consumption: null,
  annualMileage: null,
  insuranceCost: null,
  maintenanceCost: null,
  resaleValue: 0
})

const fuelPrices = ref({
  PETROL: 1.88,
  DIESEL: 1.76,
  ELECTRIC: 0.25,
  HYBRID: 1.88
})

const liveFuelData = ref(null)
const liveFuelLoading = ref(false)

const maxYears = ref(10)
const immediateRepairCost = ref(0)
const loading = ref(false)
const error = ref(null)
const result = ref(null)
const isAdvanced = ref(false)
const showResults = ref(false)

// B2C States
const homeChargingRatio = ref(0.85)
const taxIncome = ref(20000)
const scrapVehicle = ref(false)
const isLeasing = ref(false)
const customLeasingMonthlyPrice = ref(null)

const catalogVehicles = ref([])

const fetchLivePrices = async () => {
  liveFuelLoading.value = true
  try {
    const data = await apiGetLiveFuelPrices()
    if (data && data.prices) {
      liveFuelData.value = data
      // N'écraser les prix par défaut que si l'utilisateur n'a pas de profil avec des prix customisés
      if (!props.userProfile?.petrolPrice && data.prices.PETROL) {
        fuelPrices.value.PETROL = data.prices.PETROL
        fuelPrices.value.HYBRID = data.prices.HYBRID || data.prices.PETROL
      }
      if (!props.userProfile?.dieselPrice && data.prices.DIESEL) {
        fuelPrices.value.DIESEL = data.prices.DIESEL
      }
      if (!props.userProfile?.electricPrice && data.prices.ELECTRIC) {
        fuelPrices.value.ELECTRIC = data.prices.ELECTRIC
      }
    }
  } catch (err) {
    console.warn("Impossible de récupérer les prix des carburants en direct", err)
  } finally {
    liveFuelLoading.value = false
  }
}

const fetchCatalog = async () => {
  try {
    const data = await apiGetCatalogVariants()
    if (data && data.length > 0) {
      catalogVehicles.value = data.map(v => ({
        id: v.id,
        name: `${v.brandName} ${v.modelName} ${v.motorisationName || ''} (${v.finitionName || ''})`.trim(),
        brand: v.brandName,
        model: v.modelName,
        version: `${v.motorisationName || ''} - ${v.finitionName || ''}`,
        fuelType: v.fuelType,
        consumption: v.consumptionWltp || 15.0,
        purchasePrice: v.purchasePrice || 35000,
        monthlyLoa: v.monthlyLoa,
        monthlyLld: v.monthlyLld,
        insuranceCost: v.defaultInsuranceCost || 650,
        maintenanceCost: v.defaultMaintenanceCost || 250,
        resaleValue: v.estimatedResaleValue || 0,
        imageUrl: v.finitionImageUrl || v.imageUrl || v.modelImageUrl,
        brandLogoUrl: v.brandLogoUrl
      }))
    }
  } catch (err) {
    console.error("Erreur de chargement du catalogue pour autocompletion", err)
  }
}

onMounted(async () => {
  fetchLivePrices()
  await fetchCatalog()
  
  // Appliquer le profil s'il est déjà là au montage
  if (props.userProfile) {
    applyUserProfile(props.userProfile)
  }

  // Vérifier s'il y a un véhicule personnalisé sélectionné depuis le catalogue
  const customTarget = localStorage.getItem('eco_custom_target_vehicle')
  if (customTarget) {
    try {
      const v = JSON.parse(customTarget)
      targetVehicle.value = {
        name: v.name || `${v.brand || ''} ${v.model || ''}`.trim(),
        brand: v.brand || '',
        model: v.model || '',
        fuelType: v.fuelType || 'ELECTRIC',
        consumption: v.consumption || 15.0,
        purchasePrice: v.purchasePrice || 35000,
        insuranceCost: v.insuranceCost || 650,
        maintenanceCost: v.maintenanceCost || 250,
        resaleValue: v.resaleValue || 0,
        annualMileage: v.annualMileage || currentVehicle.value.annualMileage || 15000,
        imageUrl: v.imageUrl || null
      }
      if (v.monthlyLoa) {
        isLeasing.value = true
        customLeasingMonthlyPrice.value = v.monthlyLoa
      } else if (v.monthlyLld) {
        isLeasing.value = true
        customLeasingMonthlyPrice.value = v.monthlyLld
      }
      localStorage.removeItem('eco_custom_target_vehicle')
    } catch (e) {
      console.error("Erreur parsing eco_custom_target_vehicle", e)
    }
  }

  // Vérifier s'il y a un ID de véhicule cible (legacy)
  const targetId = localStorage.getItem('eco_target_vehicle_id')
  if (targetId) {
    const v = catalogVehicles.value.find(c => c.id === parseInt(targetId))
    if (v) {
      targetVehicle.value = { ...v }
      if (v.monthlyLoa) {
        isLeasing.value = true
        customLeasingMonthlyPrice.value = v.monthlyLoa
      }
      localStorage.removeItem('eco_target_vehicle_id')
    }
  }
})

const applyUserProfile = (profile) => {
  currentVehicle.value.name = profile.name
  currentVehicle.value.fuelType = profile.fuelType
  currentVehicle.value.consumption = profile.consumption
  currentVehicle.value.annualMileage = profile.annualMileage
  currentVehicle.value.insuranceCost = profile.insuranceCost
  currentVehicle.value.maintenanceCost = profile.maintenanceCost
  currentVehicle.value.resaleValue = profile.resaleValue

  if (profile.petrolPrice) fuelPrices.value.PETROL = profile.petrolPrice
  if (profile.dieselPrice) fuelPrices.value.DIESEL = profile.dieselPrice
  if (profile.electricPrice) fuelPrices.value.ELECTRIC = profile.electricPrice
}

watch(() => props.userProfile, (newVal) => {
  if (newVal) {
    applyUserProfile(newVal)
  }
}, { immediate: true })

const onCurrentVehicleSelected = (v) => {
  targetVehicle.value.annualMileage = v.annualMileage
}

const onAnnualMileageChange = (val) => {
  targetVehicle.value.annualMileage = val
}

const onLoadedSimulationChange = (newVal) => {
  if (newVal) {
    currentVehicle.value = { ...newVal.currentVehicle }
    targetVehicle.value = { ...newVal.targetVehicle }
    fuelPrices.value = { ...newVal.fuelPricesByType }
    maxYears.value = newVal.maxYears
    immediateRepairCost.value = newVal.immediateRepairCost || 0
    result.value = { ...newVal.result }
    
    if (newVal.homeChargingRatio !== undefined) homeChargingRatio.value = newVal.homeChargingRatio
    if (newVal.taxIncome !== undefined) taxIncome.value = newVal.taxIncome
    if (newVal.scrapVehicle !== undefined) scrapVehicle.value = newVal.scrapVehicle
    if (newVal.isLeasing !== undefined) isLeasing.value = newVal.isLeasing
    if (newVal.customLeasingMonthlyPrice !== undefined) customLeasingMonthlyPrice.value = newVal.customLeasingMonthlyPrice
    
    showResults.value = true
  }
}

watch(() => props.loadedSimulation, onLoadedSimulationChange, { immediate: true, deep: true })

const calculate = async () => {
  loading.value = true
  error.value = null

  try {
    const payload = {
      currentVehicle: {
        ...currentVehicle.value,
        purchasePrice: 0,
        annualMileage: currentVehicle.value.annualMileage || 15000,
        insuranceCost: currentVehicle.value.insuranceCost || (currentVehicle.value.fuelType === 'ELECTRIC' ? 600 : 700),
        maintenanceCost: currentVehicle.value.maintenanceCost || (currentVehicle.value.fuelType === 'ELECTRIC' ? 250 : 450),
        resaleValue: currentVehicle.value.resaleValue || 0
      },
      targetVehicle: {
        ...targetVehicle.value,
        annualMileage: currentVehicle.value.annualMileage || 15000,
        insuranceCost: targetVehicle.value.insuranceCost || (targetVehicle.value.fuelType === 'ELECTRIC' ? 600 : 700),
        maintenanceCost: targetVehicle.value.maintenanceCost || (targetVehicle.value.fuelType === 'ELECTRIC' ? 250 : 450),
        resaleValue: 0
      },
      fuelPricesByType: fuelPrices.value,
      maxYears: maxYears.value,
      immediateRepairCost: immediateRepairCost.value || 0,
      homeChargingRatio: homeChargingRatio.value,
      taxIncome: taxIncome.value,
      scrapVehicle: scrapVehicle.value,
      isLeasing: isLeasing.value,
      customLeasingMonthlyPrice: customLeasingMonthlyPrice.value
    }

    const response = await fetch('/api/v1/comparisons/profitability/direct', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    if (!response.ok) {
      const errData = await response.json()
      throw new Error(errData.error || 'Erreur lors du calcul')
    }

    result.value = await response.json()
    showResults.value = true
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const handleLoadAlternative = async (rec) => {
  loading.value = true
  try {
    const response = await fetch(`/api/v1/catalog/variants/${rec.vehicleId}`)
    if (response.ok) {
      const data = await response.json()
      const fm = data.finition || {}
      const mot = data.motorisation || {}
      const mdl = fm.model || mot.model || {}
      const brd = mdl.brand || {}

      targetVehicle.value = {
        name: `${brd.name || ''} ${mdl.name || ''} ${mot.name || ''} (${fm.name || ''})`.trim(),
        purchasePrice: data.purchasePrice || 0,
        fuelType: mot.fuelType || 'ELECTRIC',
        consumption: mot.consumptionWltp || 15.0,
        annualMileage: currentVehicle.value.annualMileage || 15000,
        monthlyLoa: data.monthlyLoa,
        monthlyLld: data.monthlyLld,
        insuranceCost: data.defaultInsuranceCost || 650,
        maintenanceCost: data.defaultMaintenanceCost || 250,
        resaleValue: data.estimatedResaleValue || 0,
        imageUrl: fm.imageUrl || mdl.imageUrl
      }
      await calculate()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  } catch (err) {
    console.error("Erreur de chargement de l'alternative recommandée :", err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="direct-simulator-container">
    
    <!-- Hero Banner Épuré (Affiché UNIQUEMENT en mode formulaire de saisie) -->
    <div v-if="!showResults" class="hero-banner-card mb-4 animation-fadeIn">
      <div class="hero-content text-left">
        <div class="badge badge-teal mb-2.5 flex items-center gap-1.5 w-max">
          <Zap size="13" /> <span>Calculateur de Transition Énergétique</span>
        </div>
        <h2 class="hero-title text-main">Simulateur de Rentabilité Automobile</h2>
        <p class="hero-description">
          Évaluez en temps réel le coût total de possession (TCO), l'amortissement net, les subventions d'État (bonus & prime) et vos économies réelles de carburant et d'entretien.
        </p>
      </div>
    </div>

    <!-- Mode Saisie -->
    <div v-if="!showResults">
      <!-- Sélecteur de Mode Segmented Control -->
      <div class="flex-between items-center mb-4 p-2 rounded-xl border-glass bg-card">
        <div class="flex items-center gap-2 pl-2">
          <span class="text-xxs uppercase tracking-wider font-bold text-dimmed">Mode de saisie :</span>
          <span class="text-xs font-semibold text-main">
            {{ simulationMode === 'express' ? 'Parcours Express Guidé (3 étapes)' : 'Formulaire Expert Avancé' }}
          </span>
        </div>
        <div class="segmented-control">
          <button
            type="button"
            class="segmented-item"
            :class="{ active: simulationMode === 'express' }"
            @click="simulationMode = 'express'"
          >
            <Sparkles size="13" />
            <span>Mode Express</span>
          </button>
          <button
            type="button"
            class="segmented-item"
            :class="{ active: simulationMode === 'expert' }"
            @click="simulationMode = 'expert'"
          >
            <SlidersHorizontal size="13" />
            <span>Mode Expert</span>
          </button>
        </div>
      </div>

      <!-- Option 1 : PARCOURS EXPRESS GUIDÉ -->
      <ExpressWizard
        v-if="simulationMode === 'express'"
        v-model:currentVehicle="currentVehicle"
        v-model:targetVehicle="targetVehicle"
        v-model:homeChargingRatio="homeChargingRatio"
        v-model:taxIncome="taxIncome"
        v-model:scrapVehicle="scrapVehicle"
        v-model:isLeasing="isLeasing"
        v-model:customLeasingMonthlyPrice="customLeasingMonthlyPrice"
        :fuelPrices="fuelPrices"
        :catalogVehicles="catalogVehicles"
        :loading="loading"
        @submit="calculate"
        @switch-to-expert="simulationMode = 'expert'"
      />

      <!-- Option 2 : FORMULAIRE EXPERT DÉTAILLÉ -->
      <div v-else class="card-glass p-5">
        <div class="flex-between items-center mb-4">
          <h3 class="text-main m-0 flex items-center gap-2 text-md font-bold">
            <SlidersHorizontal class="text-teal" size="18" />
            <span>Saisie des véhicules — Mode Expert</span>
          </h3>
          <button type="button" class="btn btn-secondary btn-small py-1 px-3 text-xs" @click="simulationMode = 'express'">
            ← Retour au Mode Express
          </button>
        </div>

        <!-- Bascule Mode Simple / Avancé interne au mode Expert -->
        <div class="flex-between mb-4 p-2 bg-card-subtle rounded-xl border-glass text-xs">
          <span class="text-xxs text-dimmed font-bold uppercase">Niveau de précision</span>
          <div class="segmented-control">
            <button type="button" class="segmented-item" :class="{ active: !isAdvanced }" @click="isAdvanced = false">
              Standard
            </button>
            <button type="button" class="segmented-item" :class="{ active: isAdvanced }" @click="isAdvanced = true">
              Détaillé (Assurances & Entretien)
            </button>
          </div>
        </div>

        <!-- Grille des 2 formulaires côte à côte -->
        <div class="forms-grid mb-4">
          <!-- Formulaire Véhicule Actuel -->
          <div class="form-column">
            <div class="flex items-center gap-2 mb-2">
              <span class="badge badge-amber badge-small">Véhicule Actuel</span>
              <h4 class="text-xs uppercase font-bold text-dimmed m-0">Modèle à remplacer</h4>
            </div>
            <VehicleFormBlock
              v-model:vehicle="currentVehicle"
              v-model:immediateRepairCost="immediateRepairCost"
              type="current"
              :isAdvanced="isAdvanced"
              :catalogVehicles="catalogVehicles"
              @annual-mileage-change="onAnnualMileageChange"
              @suggestion-selected="onCurrentVehicleSelected"
            />
          </div>

          <!-- Formulaire Véhicule Cible -->
          <div class="form-column">
            <div class="flex items-center gap-2 mb-2">
              <span class="badge badge-teal badge-small">Nouveau Véhicule</span>
              <h4 class="text-xs uppercase font-bold text-dimmed m-0">Modèle Cible</h4>
            </div>
            <VehicleFormBlock
              v-model:vehicle="targetVehicle"
              type="target"
              :isAdvanced="isAdvanced"
              :catalogVehicles="catalogVehicles"
            />
          </div>
        </div>

        <!-- Options de Financement et Aides d'État -->
        <div class="general-params p-4 border-glass rounded-xl mb-4 bg-card-subtle">
          <h4 class="mb-3 text-main text-sm font-bold flex items-center gap-2">
            <Sparkles size="16" class="text-teal" />
            <span>Options de Financement & Subventions</span>
          </h4>

          <!-- Mode de financement -->
          <div class="form-group mb-3 pb-3 border-b border-glass">
            <label class="form-label text-xxs text-dimmed uppercase">Mode d'acquisition</label>
            <div class="segmented-control w-100 mt-1">
              <button type="button" class="segmented-item flex-1" :class="{ active: !isLeasing }" @click="isLeasing = false">
                Achat comptant / Crédit classique
              </button>
              <button type="button" class="segmented-item flex-1" :class="{ active: isLeasing }" @click="isLeasing = true">
                Leasing (LOA / LLD)
              </button>
            </div>
            
            <div v-if="isLeasing" class="form-group mt-3 mb-0">
              <label class="form-label text-xs">Loyer mensuel indicatif (€/mois)</label>
              <input v-model.number="customLeasingMonthlyPrice" type="number" class="form-control text-xs" placeholder="ex: 290 (laisser vide pour calcul automatique)" />
            </div>
          </div>

          <!-- Bonus et Conversion (Subventions) -->
          <div class="form-group mb-3 pb-3 border-b border-glass">
            <label class="form-label text-xxs text-dimmed uppercase">Éligibilité aux Subventions de l'État</label>
            
            <div class="form-group mt-2">
              <label class="form-label text-xs">Revenu Fiscal de Référence par part (RFR en €)</label>
              <div class="flex gap-2 items-center">
                <input v-model.number="taxIncome" type="number" class="form-control text-xs" placeholder="ex: 20000" />
                <span class="badge badge-small shrink-0" :class="taxIncome <= 15400 ? 'badge-cyan' : 'badge-teal'">
                  {{ taxIncome <= 15400 ? 'Bonus Majoré 7 000 €' : 'Bonus Standard 4 000 €' }}
                </span>
              </div>
            </div>

            <div class="flex items-center gap-2 mt-2">
              <input type="checkbox" id="scrapCheck" v-model="scrapVehicle" class="cursor-pointer" />
              <label for="scrapCheck" class="text-xs text-main cursor-pointer m-0">
                Mise à la casse d'un vieux véhicule (Prime à la conversion de +1 500 € à +3 000 €)
              </label>
            </div>
          </div>

          <!-- Paramètres généraux -->
          <div class="grid-2-fields mb-3">
            <div class="form-group mb-0">
              <label class="form-label text-xxs">Horizon d'analyse (années)</label>
              <input v-model.number="maxYears" type="number" min="1" max="30" class="form-control text-xs" required />
            </div>
            <div class="form-group mb-0">
              <label class="form-label text-xxs">% Recharge à domicile (vs Borne publique)</label>
              <div class="flex items-center gap-2">
                <input v-model.number="homeChargingRatio" type="range" min="0" max="1" step="0.05" class="w-100 accent-teal" />
                <span class="text-xs font-bold text-teal shrink-0">{{ Math.round(homeChargingRatio * 100) }}%</span>
              </div>
            </div>
          </div>

          <div class="form-group mb-0">
            <label class="form-label text-xxs uppercase text-dimmed mb-2 block">Prix des énergies (€/L ou €/kWh)</label>
            <div class="grid-3-fields">
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Essence</label>
                <input v-model.number="fuelPrices.PETROL" type="number" step="0.01" class="form-control text-xs" required />
              </div>
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Diesel</label>
                <input v-model.number="fuelPrices.DIESEL" type="number" step="0.01" class="form-control text-xs" required />
              </div>
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Élec. (Maison)</label>
                <input v-model.number="fuelPrices.ELECTRIC" type="number" step="0.01" class="form-control text-xs" required />
              </div>
            </div>
          </div>
        </div>

        <button :disabled="loading" class="btn btn-primary w-100 py-3 text-sm font-bold" @click="calculate">
          <span v-if="loading" class="spinner mr-2"><Zap size="16" /></span>
          <span v-else>Calculer la Rentabilité</span>
          <ArrowRight size="16" />
        </button>
      </div>

      <p v-if="error" class="error-msg flex-center mt-3 text-rose text-xs">
        <AlertCircle size="16" class="mr-1.5 shrink-0" /> {{ error }}
      </p>
    </div>

    <!-- Mode Résultats (100% de l'écran dédié aux résultats) -->
    <div v-else class="results-container animation-fadeIn">
      <SimulationResults
        :result="result"
        :currentVehicle="currentVehicle"
        :targetVehicle="targetVehicle"
        :fuelPrices="fuelPrices"
        :maxYears="maxYears"
        :immediateRepairCost="immediateRepairCost"
        :isLeasing="isLeasing"
        :homeChargingRatio="homeChargingRatio"
        :taxIncome="taxIncome"
        :scrapVehicle="scrapVehicle"
        :currentUser="currentUser"
        @back="showResults = false"
        @load-alternative="handleLoadAlternative"
      />
    </div>

  </div>
</template>

<style scoped>
.hero-banner-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-xl);
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
  color: var(--text-muted);
  font-size: 0.88rem;
  line-height: 1.6;
  max-width: 38rem;
  margin: 0;
}

.hero-brand-image {
  max-height: 120px;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-glass);
}

.forms-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 768px) {
  .forms-grid {
    grid-template-columns: 1fr;
  }
}

.w-max { width: max-content; }
.mr-2 { margin-right: 0.5rem; }
.mr-1\.5 { margin-right: 0.375rem; }

@media (max-width: 768px) {
  .hero-banner-card {
    flex-direction: column;
    padding: 1.25rem;
    text-align: center;
  }
  .hide-on-mobile {
    display: none;
  }
}
</style>
