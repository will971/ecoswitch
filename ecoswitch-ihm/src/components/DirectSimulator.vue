<script setup>
import { ref, watch, onMounted } from 'vue'
import { Zap, ArrowRight, Sparkles, AlertCircle, Layers, SlidersHorizontal, CheckCircle2, RefreshCw } from '@lucide/vue'
import { apiGetCatalogVariants, apiGetLiveFuelPrices } from '../utils/api.js'

// Import des sous-composants
import StepByStepWizard from './simulator/StepByStepWizard.vue'
// ExpressWizard et VehicleFormBlock sont masqués
// import ExpressWizard from './simulator/ExpressWizard.vue'
// import VehicleFormBlock from './simulator/VehicleFormBlock.vue'
import SimulationResults from './simulator/SimulationResults.vue'

const simulationMode = ref('step') // 'step' | 'express' | 'expert'

const emit = defineEmits(['open-auth', 'open-garage'])

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
  },
  liveFuelPrices: {
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
  maintenanceCost: null,
  resaleValue: null
})

const targetVehicle = ref({
  name: '',
  purchasePrice: null,
  fuelType: '',
  consumption: null,
  annualMileage: null,
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

// Nouveaux états profil & financement
const hasCurrentVehicle = ref(true)
const currentVehicleFinanceType = ref('CASH')
const currentVehicleMonthlyCost = ref(0)
const currentLoaBuyoutPrice = ref(null)
const baselineMobilityCost = ref(86)

const catalogVehicles = ref([])

const fetchLivePrices = async () => {
  liveFuelLoading.value = true
  try {
    const data = await apiGetLiveFuelPrices()
    if (data && data.prices) {
      liveFuelData.value = data
      if (data.prices.PETROL) {
        fuelPrices.value.PETROL = data.prices.PETROL
        fuelPrices.value.HYBRID = data.prices.PETROL
      }
      if (data.prices.DIESEL) {
        fuelPrices.value.DIESEL = data.prices.DIESEL
      }
      if (data.prices.ELECTRIC) {
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
  currentVehicle.value.maintenanceCost = profile.maintenanceCost
  currentVehicle.value.resaleValue = profile.resaleValue

  // N'écraser les prix que si le profil définit expressément des prix personnalisés
  if (profile.hasCustomPrices) {
    if (profile.petrolPrice) fuelPrices.value.PETROL = profile.petrolPrice
    if (profile.dieselPrice) fuelPrices.value.DIESEL = profile.dieselPrice
    if (profile.electricPrice) fuelPrices.value.ELECTRIC = profile.electricPrice
    fuelPrices.value.HYBRID = profile.petrolPrice || fuelPrices.value.HYBRID
  }
}

watch(() => props.liveFuelPrices, (newPrices) => {
  if (newPrices) {
    if (newPrices.PETROL) {
      fuelPrices.value.PETROL = newPrices.PETROL
      fuelPrices.value.HYBRID = newPrices.PETROL
    }
    if (newPrices.DIESEL) fuelPrices.value.DIESEL = newPrices.DIESEL
    if (newPrices.ELECTRIC) fuelPrices.value.ELECTRIC = newPrices.ELECTRIC
  }
}, { deep: true, immediate: true })

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
    const effectiveCurrentVehicle = hasCurrentVehicle.value ? {
      ...currentVehicle.value,
      purchasePrice: 0,
      annualMileage: currentVehicle.value.annualMileage || targetVehicle.value.annualMileage || 15000,
      maintenanceCost: currentVehicle.value.maintenanceCost || (currentVehicle.value.fuelType === 'ELECTRIC' ? 250 : 450),
      resaleValue: currentVehicle.value.resaleValue || 0
    } : {
      name: 'Mobilité Sans Voiture',
      brand: 'Sans',
      model: 'Voiture',
      fuelType: 'PETROL',
      consumption: 0.001,
      purchasePrice: 0,
      annualMileage: targetVehicle.value.annualMileage || 12000,
      maintenanceCost: 0,
      resaleValue: 0
    }

    const payload = {
      currentVehicle: effectiveCurrentVehicle,
      targetVehicle: {
        ...targetVehicle.value,
        annualMileage: targetVehicle.value.annualMileage || currentVehicle.value.annualMileage || 15000,
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
      <!-- Parcours Guidé Pas-à-Pas (Vue unique active) -->
      <StepByStepWizard
        v-model:currentVehicle="currentVehicle"
        v-model:targetVehicle="targetVehicle"
        v-model:homeChargingRatio="homeChargingRatio"
        v-model:taxIncome="taxIncome"
        v-model:scrapVehicle="scrapVehicle"
        v-model:isLeasing="isLeasing"
        v-model:customLeasingMonthlyPrice="customLeasingMonthlyPrice"
        v-model:hasCurrentVehicle="hasCurrentVehicle"
        v-model:currentVehicleFinanceType="currentVehicleFinanceType"
        v-model:currentVehicleMonthlyCost="currentVehicleMonthlyCost"
        v-model:currentLoaBuyoutPrice="currentLoaBuyoutPrice"
        v-model:baselineMobilityCost="baselineMobilityCost"
        v-model:immediateRepairCost="immediateRepairCost"
        v-model:fuelPrices="fuelPrices"
        :catalogVehicles="catalogVehicles"
        :loading="loading"
        :currentUser="currentUser"
        @submit="calculate"
        @open-auth="emit('open-auth')"
      />

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
        :hasCurrentVehicle="hasCurrentVehicle"
        :currentVehicleFinanceType="currentVehicleFinanceType"
        :currentVehicleMonthlyCost="currentVehicleMonthlyCost"
        :currentLoaBuyoutPrice="currentLoaBuyoutPrice"
        :baselineMobilityCost="baselineMobilityCost"
        @back="showResults = false"
        @load-alternative="handleLoadAlternative"
        @open-auth="emit('open-auth')"
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
