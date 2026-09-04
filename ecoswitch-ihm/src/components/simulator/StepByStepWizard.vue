<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import {
  Car,
  Zap,
  Home,
  Building2,
  ParkingCircle,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  Search,
  Check,
  CreditCard,
  Layers,
  HelpCircle,
  Train,
  Bike,
  ShieldCheck,
  Lock,
  User,
  SlidersHorizontal,
  ChevronRight,
  Calculator,
  RefreshCw,
  Clock,
  Compass,
  Wallet,
  ExternalLink
} from '@lucide/vue'
import { apiGetCatalogVariants, apiGetLiveFuelPrices } from '../../utils/api.js'

const props = defineProps({
  currentVehicle: {
    type: Object,
    required: true
  },
  targetVehicle: {
    type: Object,
    required: true
  },
  fuelPrices: {
    type: Object,
    required: true
  },
  homeChargingRatio: {
    type: Number,
    default: 0.85
  },
  taxIncome: {
    type: Number,
    default: 20000
  },
  scrapVehicle: {
    type: Boolean,
    default: false
  },
  isLeasing: {
    type: Boolean,
    default: false
  },
  customLeasingMonthlyPrice: {
    type: Number,
    default: null
  },
  catalogVehicles: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentUser: {
    type: Object,
    default: null
  },
  hasCurrentVehicle: {
    type: Boolean,
    default: true
  },
  currentVehicleFinanceType: {
    type: String,
    default: 'CASH'
  },
  currentVehicleMonthlyCost: {
    type: Number,
    default: 0
  },
  currentLoaBuyoutPrice: {
    type: Number,
    default: null
  },
  baselineMobilityCost: {
    type: Number,
    default: 86
  },
  immediateRepairCost: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits([
  'update:currentVehicle',
  'update:targetVehicle',
  'update:homeChargingRatio',
  'update:taxIncome',
  'update:scrapVehicle',
  'update:isLeasing',
  'update:customLeasingMonthlyPrice',
  'update:hasCurrentVehicle',
  'update:currentVehicleFinanceType',
  'update:currentVehicleMonthlyCost',
  'update:currentLoaBuyoutPrice',
  'update:baselineMobilityCost',
  'update:immediateRepairCost',
  'update:fuelPrices',
  'submit',
  'switch-to-express',
  'switch-to-expert',
  'open-auth'
])

// ── ÉTAT DE LA NAVIGATION (1 QUESTION PAR ÉCRAN) ───────────────────────────
const currentScreen = ref('Q01_TRANSPORT')
const historyStack = ref([])

// Variantes dynamiques depuis le catalogue officiel
const catalogVariants = ref([])

// Modèles courants du parc français (Fallback & Quick-Pick)
const popularCurrentCars = [
  { name: 'Peugeot 208 II PureTech 100', brand: 'Peugeot', fuelType: 'PETROL', consumption: 5.3, maintenanceCost: 440, tag: 'Essence' },
  { name: 'Renault Clio V TCe 90', brand: 'Renault', fuelType: 'PETROL', consumption: 5.2, maintenanceCost: 420, tag: 'Essence' },
  { name: 'Citroën C3 III PureTech 110', brand: 'Citroën', fuelType: 'PETROL', consumption: 5.5, maintenanceCost: 430, tag: 'Essence' },
  { name: 'Dacia Sandero Stepway TCe', brand: 'Dacia', fuelType: 'PETROL', consumption: 5.8, maintenanceCost: 380, tag: 'Essence' },
  { name: 'Peugeot 308 III BlueHDi 130', brand: 'Peugeot', fuelType: 'DIESEL', consumption: 4.5, maintenanceCost: 580, tag: 'Diesel' },
  { name: 'Toyota Yaris IV Hybrid', brand: 'Toyota', fuelType: 'HYBRID', consumption: 3.8, maintenanceCost: 360, tag: 'Hybride' }
]

// Modèles cibles par gabarit
const popularTargetsByFormat = {
  CITY: [
    { name: 'Citroën ë-C3 You (2024)', brand: 'Citroën', fuelType: 'ELECTRIC', consumption: 14.8, purchasePrice: 23300, monthlyLoa: 99, maintenanceCost: 190, badge: 'Dès 99€/mois' },
    { name: 'Renault 5 E-Tech EV40', brand: 'Renault', fuelType: 'ELECTRIC', consumption: 14.9, purchasePrice: 25000, monthlyLoa: 150, maintenanceCost: 200, badge: 'Coup de Cœur' },
    { name: 'Peugeot e-208 Allure', brand: 'Peugeot', fuelType: 'ELECTRIC', consumption: 15.2, purchasePrice: 33500, monthlyLoa: 180, maintenanceCost: 220, badge: 'Best-Seller' }
  ],
  COMPACT: [
    { name: 'Renault Megane E-Tech EV60', brand: 'Renault', fuelType: 'ELECTRIC', consumption: 16.1, purchasePrice: 38000, monthlyLoa: 290, maintenanceCost: 240, badge: 'Polyvalente' },
    { name: 'Tesla Model 3 Propulsion', brand: 'Tesla', fuelType: 'ELECTRIC', consumption: 14.4, purchasePrice: 41490, monthlyLoa: 360, maintenanceCost: 250, badge: 'Grande Autonomie' },
    { name: 'MG 4 EV Standard', brand: 'MG', fuelType: 'ELECTRIC', consumption: 16.0, purchasePrice: 29990, monthlyLoa: 199, maintenanceCost: 220, badge: 'Rapport Q/P' }
  ],
  SUV: [
    { name: 'Tesla Model Y Propulsion', brand: 'Tesla', fuelType: 'ELECTRIC', consumption: 15.7, purchasePrice: 44990, monthlyLoa: 399, maintenanceCost: 260, badge: 'N°1 Mondial' },
    { name: 'Renault Scenic E-Tech 87kWh', brand: 'Renault', fuelType: 'ELECTRIC', consumption: 16.8, purchasePrice: 46990, monthlyLoa: 380, maintenanceCost: 250, badge: 'Élue Voiture de l’Année' },
    { name: 'Kia EV3 Long Range', brand: 'Kia', fuelType: 'ELECTRIC', consumption: 15.6, purchasePrice: 40990, monthlyLoa: 330, maintenanceCost: 240, badge: 'Familiale 600km' }
  ]
}

// État local réactif des champs de saisie
const searchQuery = ref('')
const selectedFormat = ref('CITY')
const targetMonthlyBudget = ref(250)
const dailyDistanceCommute = ref(20)
const hasImminentRepairs = ref(false)
const imminentRepairAmount = ref(1000)
const customIncomeExact = ref(false)

// Progression calculée
const totalEstimatedSteps = computed(() => (props.hasCurrentVehicle ? 10 : 9))

const currentStepNumber = computed(() => {
  switch (currentScreen.value) {
    case 'Q01_TRANSPORT': return 1
    // Branche A
    case 'A02_STATUS': return 2
    case 'A03_MONTHLY': return 3
    case 'A04_FUEL': return props.currentVehicleFinanceType === 'CASH' ? 3 : 4
    case 'A05_CONSUMPTION':
    case 'A05_MODEL': return props.currentVehicleFinanceType === 'CASH' ? 4 : 5
    case 'A06_MILEAGE': return props.currentVehicleFinanceType === 'CASH' ? 5 : 6
    case 'A08_MAINTENANCE': return props.currentVehicleFinanceType === 'CASH' ? 6 : 7
    case 'A09_DEPARTURE': return props.currentVehicleFinanceType === 'CASH' ? 7 : 8
    case 'A09_RESALE_ESTIMATE': return props.currentVehicleFinanceType === 'CASH' ? 8 : 9
    case 'A09_LOA_BUY': return props.currentVehicleFinanceType === 'CASH' ? 8 : 9
    // Branche B
    case 'B02_BUDGET': return 2
    case 'B03_USAGE': return 3
    case 'B04_MILEAGE': return 4
    case 'B05_FINANCING': return 5
    // Tronc Commun
    case 'C01_CHARGING': return props.hasCurrentVehicle ? 8 : 6
    case 'C02_INCOME': return props.hasCurrentVehicle ? 9 : 7
    case 'C03_FORMAT': return props.hasCurrentVehicle ? 10 : 8
    case 'C04_BUDGET': return props.hasCurrentVehicle ? 10 : 9
    case 'FINAL_AUTH': return props.hasCurrentVehicle ? 10 : 9
    default: return 1
  }
})

const progressPercent = computed(() => {
  return Math.min(100, Math.round((currentStepNumber.value / totalEstimatedSteps.value) * 100))
})

// Navigation interne
const navigateTo = (screen) => {
  historyStack.value.push(currentScreen.value)
  currentScreen.value = screen
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goBack = () => {
  if (historyStack.value.length > 0) {
    currentScreen.value = historyStack.value.pop()
  }
}

// ── LOGIQUE DES ÉCRANS ──────────────────────────────────────────────────────

// 1. Écran 01 : Modes de transport actuels
const selectTransportMode = (mode) => {
  if (mode === 'CAR') {
    emit('update:hasCurrentVehicle', true)
    navigateTo('A02_STATUS')
  } else {
    emit('update:hasCurrentVehicle', false)
    if (mode === 'TRANSIT') emit('update:baselineMobilityCost', 86)
    else if (mode === 'TRAIN') emit('update:baselineMobilityCost', 180)
    else if (mode === 'SOFT') emit('update:baselineMobilityCost', 15)
    else if (mode === 'CAR_SHARE') emit('update:baselineMobilityCost', 140)
    
    // Initialise un véhicule actuel neutre/standard pour que l'API puisse tourner
    if (!props.currentVehicle.fuelType) {
      props.currentVehicle.fuelType = 'PETROL'
      props.currentVehicle.name = 'Profil Mobilité Alternative'
      props.currentVehicle.consumption = 6.0
      props.currentVehicle.resaleValue = 0
      props.currentVehicle.purchasePrice = 0
    }
    navigateTo('B02_BUDGET')
  }
}

// 2. Écran A02 : Statut de détention
const selectVehicleStatus = (status) => {
  emit('update:currentVehicleFinanceType', status)
  if (status === 'CASH') {
    emit('update:currentVehicleMonthlyCost', 0)
    navigateTo('A04_FUEL')
  } else {
    if (!props.currentVehicleMonthlyCost) {
      emit('update:currentVehicleMonthlyCost', 280)
    }
    navigateTo('A03_MONTHLY')
  }
}

// 3. Écran A03 : Mensualité actuelle
const confirmMonthlyCost = () => {
  navigateTo('A04_FUEL')
}

// 4. Écran A04 : Carburant
const selectFuelType = (fuel) => {
  props.currentVehicle.fuelType = fuel
  if (fuel === 'ELECTRIC') {
    props.currentVehicle.consumption = 16.5
    props.currentVehicle.maintenanceCost = 250
    props.currentVehicle.name = props.currentVehicle.name || 'Véhicule Électrique'
  } else if (fuel === 'DIESEL') {
    props.currentVehicle.consumption = 5.2
    props.currentVehicle.maintenanceCost = 500
    props.currentVehicle.name = props.currentVehicle.name || 'Véhicule Diesel'
  } else if (fuel === 'HYBRID') {
    props.currentVehicle.consumption = 4.5
    props.currentVehicle.maintenanceCost = 420
    props.currentVehicle.name = props.currentVehicle.name || 'Véhicule Hybride'
  } else {
    props.currentVehicle.consumption = 6.5
    props.currentVehicle.maintenanceCost = 450
    props.currentVehicle.name = props.currentVehicle.name || 'Véhicule Essence'
  }
  navigateTo('A05_CONSUMPTION')
}

// 5. Écran A05 : Consommation moyenne réelle (Slider)
const setConsumptionPreset = (liters, label) => {
  props.currentVehicle.consumption = liters
  if (!props.currentVehicle.name || props.currentVehicle.name.startsWith('Véhicule')) {
    props.currentVehicle.name = label
  }
}

// Recherche catalogue autocomplétion
const filteredCatalogVariants = computed(() => {
  if (!searchQuery.value || searchQuery.value.trim().length < 2) return []
  const q = searchQuery.value.toLowerCase()
  return catalogVariants.value.filter(v => 
    v.brandName?.toLowerCase().includes(q) || 
    v.modelName?.toLowerCase().includes(q)
  ).slice(0, 6)
})

// 6. Écran A06 : Kilométrage
const setMileage = (km) => {
  props.currentVehicle.annualMileage = km
  props.targetVehicle.annualMileage = km
}

const applyCommuteDistance = () => {
  // 220 jours ouvrés x Aller-Retour + 2500 km loisirs
  const estimated = Math.round(dailyDistanceCommute.value * 2 * 220 + 2500)
  setMileage(estimated)
  navigateTo('A08_MAINTENANCE')
}

// 7. Écran A08 : Entretien
const confirmMaintenance = () => {
  if (!props.currentVehicle.maintenanceCost) {
    props.currentVehicle.maintenanceCost = 450
  }
  if (hasImminentRepairs.value) {
    emit('update:immediateRepairCost', imminentRepairAmount.value || 0)
  } else {
    emit('update:immediateRepairCost', 0)
  }
  navigateTo('A09_DEPARTURE')
}

// 9. Écran A09 : Devenir du véhicule
const selectDepartureOption = (opt) => {
  if (opt === 'SCRAP') {
    emit('update:scrapVehicle', true)
    props.currentVehicle.resaleValue = 0
    navigateTo('C01_CHARGING')
  } else if (opt === 'RESALE') {
    emit('update:scrapVehicle', false)
    navigateTo('A09_RESALE_ESTIMATE')
  } else if (opt === 'KEEP') {
    emit('update:scrapVehicle', false)
    props.currentVehicle.resaleValue = 0
    navigateTo('C01_CHARGING')
  } else if (opt === 'LLD_RETURN') {
    props.currentVehicle.resaleValue = 0
    navigateTo('C01_CHARGING')
  } else if (opt === 'LOA_RETURN') {
    props.currentVehicle.resaleValue = 0
    navigateTo('C01_CHARGING')
  } else if (opt === 'LOA_BUYOUT') {
    navigateTo('A09_LOA_BUY')
  } else if (opt === 'LOA_TRADE') {
    navigateTo('C01_CHARGING')
  }
}

// 10. Écran B03 : Usage non véhiculé
const selectFutureUsage = (usage) => {
  navigateTo('B04_MILEAGE')
}

// 11. Écran B05 : Financement non véhiculé
const selectFinancing = (isLease) => {
  emit('update:isLeasing', isLease)
  navigateTo('C01_CHARGING')
}

// 12. Écran C01 : Stationnement & Recharge
const selectChargingProfile = (ratio) => {
  emit('update:homeChargingRatio', ratio)
  navigateTo('C02_INCOME')
}

// 13. Écran C02 : Revenu fiscal
const selectIncomeTier = (tier) => {
  if (tier === 'MODEST') {
    emit('update:taxIncome', 14000)
  } else {
    emit('update:taxIncome', 25000)
  }
  navigateTo('C03_FORMAT')
}

// 14. Écran C03 : Format cible & Véhicule
const selectTargetFormat = (format) => {
  selectedFormat.value = format
  const candidates = popularTargetsByFormat[format] || popularTargetsByFormat.CITY
  const defaultPick = candidates[0]
  
  props.targetVehicle.name = defaultPick.name
  props.targetVehicle.brand = defaultPick.brand
  props.targetVehicle.fuelType = defaultPick.fuelType
  props.targetVehicle.consumption = defaultPick.consumption
  props.targetVehicle.purchasePrice = defaultPick.purchasePrice
  props.targetVehicle.maintenanceCost = defaultPick.maintenanceCost
  props.targetVehicle.annualMileage = props.currentVehicle.annualMileage || 15000
  props.targetVehicle.resaleValue = 0

  if (props.isLeasing && defaultPick.monthlyLoa) {
    emit('update:customLeasingMonthlyPrice', defaultPick.monthlyLoa)
  }

  navigateTo('C04_BUDGET')
}

// 15. Écran C04 : Budget cible
const confirmTargetBudget = () => {
  if (props.isLeasing) {
    emit('update:customLeasingMonthlyPrice', targetMonthlyBudget.value)
  }
  // Si l'utilisateur n'est pas authentifié, proposer l'écran d'enregistrement optionnel
  if (!props.currentUser) {
    navigateTo('FINAL_AUTH')
  } else {
    emit('submit')
  }
}

// 16. Écran FINAL_AUTH
const triggerAuthAndSubmit = () => {
  // Sauvegarde provisoire dans le localStorage pour récupération automatique après login
  try {
    const profileToSave = {
      hasCurrentVehicle: props.hasCurrentVehicle,
      currentVehicle: props.currentVehicle,
      currentVehicleFinanceType: props.currentVehicleFinanceType,
      currentVehicleMonthlyCost: props.currentVehicleMonthlyCost,
      baselineMobilityCost: props.baselineMobilityCost,
      homeChargingRatio: props.homeChargingRatio,
      taxIncome: props.taxIncome
    }
    localStorage.setItem('eco_pending_profile', JSON.stringify(profileToSave))
  } catch (e) {}

  emit('open-auth')
  emit('submit')
}

const proceedWithoutAuth = () => {
  emit('submit')
}

const formatFuelPrice = (price, fallback = 0, decimals = 2) => {
  const val = price ?? fallback
  const num = Number(val)
  if (isNaN(num)) return Number(fallback).toFixed(decimals).replace('.', ',')
  return num.toFixed(decimals).replace('.', ',')
}

onMounted(async () => {
  try {
    const data = await apiGetCatalogVariants()
    if (data && data.length > 0) {
      catalogVariants.value = data
    }
  } catch (e) {
    console.warn("Impossible de charger les variantes du catalogue", e)
  }

  // Synchronisation proactive des prix des carburants en direct
  try {
    const liveData = await apiGetLiveFuelPrices()
    if (liveData && liveData.prices) {
      emit('update:fuelPrices', {
        ...props.fuelPrices,
        PETROL: liveData.prices.PETROL || props.fuelPrices.PETROL,
        DIESEL: liveData.prices.DIESEL || props.fuelPrices.DIESEL,
        ELECTRIC: liveData.prices.ELECTRIC || props.fuelPrices.ELECTRIC,
        HYBRID: liveData.prices.PETROL || liveData.prices.HYBRID || props.fuelPrices.PETROL
      })
    }
  } catch (e) {
    console.warn("Prix live dans StepByStepWizard : conservation des valeurs existantes", e)
  }

  if (!props.currentVehicle.annualMileage) {
    props.currentVehicle.annualMileage = 15000
    props.targetVehicle.annualMileage = 15000
  }
  if (!props.currentVehicle.maintenanceCost) {
    props.currentVehicle.maintenanceCost = 440
  }
})
</script>

<template>
  <div class="step-wizard-container animation-fadeIn">
    
    <!-- HEADER SUPÉRIEUR ÉPURÉ (APPLE / LINEAR STYLE) -->
    <div class="wizard-nav-header">
      <div class="flex-between items-center mb-2">
        <button
          v-if="historyStack.length > 0"
          type="button"
          class="btn-back-touch"
          @click="goBack"
          aria-label="Question précédente"
        >
          <ArrowLeft size="18" />
          <span class="btn-back-text">Précédent</span>
        </button>
        <div v-else class="flex items-center gap-2">
          <span class="badge badge-teal badge-small">Simulateur Guidé</span>
        </div>

        <div class="flex items-center gap-3">
          <span class="step-counter-text font-mono">
            Question {{ currentStepNumber }} / {{ totalEstimatedSteps }}
          </span>
        </div>
      </div>

      <!-- Barre de Progression Fluide -->
      <div class="progress-bar-track">
        <div
          class="progress-bar-fill"
          :style="{ width: `${progressPercent}%` }"
        ></div>
      </div>
    </div>

    <!-- CORPS DU WIZARD : UNE QUESTION UNIQUE PAR ÉCRAN -->
    <div class="wizard-card-viewport">

      <!-- ================================================================= -->
      <!-- ÉCRAN 01 : HABITUDES DE MOBILITÉ ACTUELLES (POINT DE DÉPART)     -->
      <!-- ================================================================= -->
      <div v-if="currentScreen === 'Q01_TRANSPORT'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Point de départ</span>
          <h2 class="question-title">Comment vous déplacez-vous principalement aujourd'hui ?</h2>
          <p class="question-desc">
            Pour évaluer précisément vos dépenses réelles et comparer avec votre future solution.
          </p>
        </div>

        <div class="options-grid">
          <button type="button" class="option-card-touch featured" @click="selectTransportMode('CAR')">
            <div class="card-icon-bubble teal">
              <Car size="24" />
            </div>
            <div class="card-content">
              <div class="card-label">Ma voiture personnelle</div>
              <div class="card-sub">Thermique, hybride ou électrique (ou 2-roues)</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectTransportMode('TRANSIT')">
            <div class="card-icon-bubble blue">
              <Train size="24" />
            </div>
            <div class="card-content">
              <div class="card-label">Transports en commun</div>
              <div class="card-sub">Métro, bus, tram, RER (~86 €/mois)</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectTransportMode('TRAIN')">
            <div class="card-icon-bubble cyan">
              <Compass size="24" />
            </div>
            <div class="card-content">
              <div class="card-label">Train / TER régulier</div>
              <div class="card-sub">Abonnements régionaux, TGV, navettes</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectTransportMode('SOFT')">
            <div class="card-icon-bubble green">
              <Bike size="24" />
            </div>
            <div class="card-content">
              <div class="card-label">Vélo, Trottinette ou Marche</div>
              <div class="card-sub">Mobilité active, télétravail ou courtes distances</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectTransportMode('CAR_SHARE')">
            <div class="card-icon-bubble amber">
              <Wallet size="24" />
            </div>
            <div class="card-content">
              <div class="card-label">Autopartage, Location ou VTC</div>
              <div class="card-sub">Sans voiture au quotidien (Getaround, Sixt, Uber)</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-02 : STATUT DU VÉHICULE ACTUEL               -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A02_STATUS'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Véhicule Actuel</span>
          <h2 class="question-title">Quel est le statut de votre véhicule actuel ?</h2>
          <p class="question-desc">
            Cela nous permet d'intégrer vos éventuelles charges financières chaque mois.
          </p>
        </div>

        <div class="options-grid">
          <button type="button" class="option-card-touch" @click="selectVehicleStatus('CASH')">
            <div class="card-icon-bubble teal">
              <Check size="22" />
            </div>
            <div class="card-content">
              <div class="card-label">Propriétaire (payé comptant)</div>
              <div class="card-sub">Crédit terminé ou acheté sans financement (0 €/mois)</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectVehicleStatus('CREDIT')">
            <div class="card-icon-bubble blue">
              <CreditCard size="22" />
            </div>
            <div class="card-content">
              <div class="card-label">Propriétaire avec crédit en cours</div>
              <div class="card-sub">Vous remboursez un prêt bancaire chaque mois</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectVehicleStatus('LOA')">
            <div class="card-icon-bubble amber">
              <Clock size="22" />
            </div>
            <div class="card-content">
              <div class="card-label">En LOA (Location avec Option d'Achat)</div>
              <div class="card-sub">Loyer mensuel avec possibilité de rachat final</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectVehicleStatus('LLD')">
            <div class="card-icon-bubble cyan">
              <RefreshCw size="22" />
            </div>
            <div class="card-content">
              <div class="card-label">En LLD (Location Longue Durée)</div>
              <div class="card-sub">Loyer mensuel pur avec restitution obligatoire</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectVehicleStatus('COMPANY')">
            <div class="card-icon-bubble gray">
              <Building2 size="22" />
            </div>
            <div class="card-content">
              <div class="card-label">Véhicule de fonction ou société</div>
              <div class="card-sub">Véhicule fourni avec éventuelle part salariale</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-03 : MENSUALITÉ CRÉDIT / LOYER               -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A03_MONTHLY'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Charges mensuelles</span>
          <h2 class="question-title">Combien payez-vous chaque mois pour ce véhicule ?</h2>
          <p class="question-desc">
            Votre mensualité de crédit ou loyer de location (hors carburant).
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ currentVehicleMonthlyCost || 280 }}</span>
            <span class="big-currency">€ / mois</span>
          </div>

          <input
            type="range"
            min="50"
            max="900"
            step="10"
            :value="currentVehicleMonthlyCost || 280"
            @input="emit('update:currentVehicleMonthlyCost', Number($event.target.value))"
            class="slider-touch accent-teal w-100 mb-4"
          />

          <div class="pills-row mb-4">
            <button
              v-for="p in [150, 250, 320, 450, 600]"
              :key="p"
              type="button"
              class="pill-touch"
              :class="{ active: currentVehicleMonthlyCost === p }"
              @click="emit('update:currentVehicleMonthlyCost', p)"
            >
              {{ p }} €
            </button>
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch"
            @click="confirmMonthlyCost"
          >
            <span>Continuer</span>
            <ArrowRight size="18" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-04 : CARBURANT ACTUEL                        -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A04_FUEL'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Énergie</span>
          <h2 class="question-title">Quel carburant utilise votre véhicule actuel ?</h2>
          <p class="question-desc">
            Nous appliquons les prix moyens officiels des carburants relevés en direct.
          </p>
        </div>

        <div class="options-grid">
          <button type="button" class="option-card-touch" @click="selectFuelType('PETROL')">
            <div class="card-icon-bubble amber">⛽</div>
            <div class="card-content">
              <div class="card-label">Essence (SP95 / E10)</div>
              <div class="card-sub">{{ formatFuelPrice(fuelPrices.PETROL, 1.88) }} € / L (Cours direct)</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectFuelType('DIESEL')">
            <div class="card-icon-bubble blue">🛢️</div>
            <div class="card-content">
              <div class="card-label">Diesel (Gazole)</div>
              <div class="card-sub">{{ formatFuelPrice(fuelPrices.DIESEL, 1.76) }} € / L (Cours direct)</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectFuelType('HYBRID')">
            <div class="card-icon-bubble green">🔋</div>
            <div class="card-content">
              <div class="card-label">Hybride ou Hybride Rechargeable</div>
              <div class="card-sub">{{ formatFuelPrice(fuelPrices.PETROL, 1.88) }} € / L (SP95) + Électrique</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectFuelType('ELECTRIC')">
            <div class="card-icon-bubble teal">⚡</div>
            <div class="card-content">
              <div class="card-label">100% Électrique</div>
              <div class="card-sub">~ {{ formatFuelPrice(fuelPrices.ELECTRIC, 0.25) }} € / kWh à domicile</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-05 : CONSOMMATION MOYENNE DU VÉHICULE        -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A05_CONSUMPTION' || currentScreen === 'A05_MODEL'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Consommation</span>
          <h2 class="question-title">Combien consomme votre véhicule actuel ?</h2>
          <p class="question-desc">
            {{ currentVehicle.fuelType === 'ELECTRIC' 
              ? 'Indiquez sa consommation moyenne en kWh / 100 km (affichée sur votre tableau de bord).' 
              : 'Indiquez sa consommation moyenne réelle en L / 100 km (en usage mixte au quotidien).' }}
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ Number(currentVehicle.consumption || 6.0).toFixed(1) }}</span>
            <span class="big-currency">{{ currentVehicle.fuelType === 'ELECTRIC' ? 'kWh / 100 km' : 'L / 100 km' }}</span>
          </div>

          <!-- Curseur interactif tactile -->
          <input
            v-if="currentVehicle.fuelType === 'ELECTRIC'"
            type="range"
            min="11.0"
            max="28.0"
            step="0.5"
            :value="currentVehicle.consumption || 16.5"
            @input="currentVehicle.consumption = Number($event.target.value)"
            class="slider-touch accent-teal w-100 mb-3"
          />
          <input
            v-else
            type="range"
            min="3.2"
            max="13.5"
            step="0.1"
            :value="currentVehicle.consumption || (currentVehicle.fuelType === 'DIESEL' ? 5.2 : 6.5)"
            @input="currentVehicle.consumption = Number($event.target.value)"
            class="slider-touch accent-teal w-100 mb-3"
          />

          <!-- Paliers rapides de repères selon la motorisation choisie -->
          <div class="pills-row mb-4">
            <template v-if="currentVehicle.fuelType === 'DIESEL'">
              <button
                v-for="p in [
                  { l: '4.5 L · Citadine', v: 4.5, n: 'Citadine Diesel' },
                  { l: '5.2 L · Berline', v: 5.2, n: 'Berline Diesel' },
                  { l: '6.5 L · SUV / Break', v: 6.5, n: 'SUV Diesel' },
                  { l: '8.0 L · Grand SUV', v: 8.0, n: 'Grand SUV Diesel' }
                ]"
                :key="p.v"
                type="button"
                class="pill-touch"
                :class="{ active: Math.abs(currentVehicle.consumption - p.v) < 0.15 }"
                @click="setConsumptionPreset(p.v, p.n)"
              >
                {{ p.l }}
              </button>
            </template>

            <template v-else-if="currentVehicle.fuelType === 'HYBRID'">
              <button
                v-for="p in [
                  { l: '3.8 L · Citadine', v: 3.8, n: 'Citadine Hybride' },
                  { l: '4.8 L · Berline', v: 4.8, n: 'Berline Hybride' },
                  { l: '6.0 L · SUV', v: 6.0, n: 'SUV Hybride' }
                ]"
                :key="p.v"
                type="button"
                class="pill-touch"
                :class="{ active: Math.abs(currentVehicle.consumption - p.v) < 0.15 }"
                @click="setConsumptionPreset(p.v, p.n)"
              >
                {{ p.l }}
              </button>
            </template>

            <template v-else-if="currentVehicle.fuelType === 'ELECTRIC'">
              <button
                v-for="p in [
                  { l: '14.5 kWh · Citadine', v: 14.5, n: 'Citadine Électrique' },
                  { l: '17.0 kWh · Berline', v: 17.0, n: 'Berline Électrique' },
                  { l: '21.0 kWh · SUV', v: 21.0, n: 'SUV Électrique' }
                ]"
                :key="p.v"
                type="button"
                class="pill-touch"
                :class="{ active: Math.abs(currentVehicle.consumption - p.v) < 0.3 }"
                @click="setConsumptionPreset(p.v, p.n)"
              >
                {{ p.l }}
              </button>
            </template>

            <template v-else>
              <!-- Essence -->
              <button
                v-for="p in [
                  { l: '5.5 L · Citadine', v: 5.5, n: 'Citadine Essence' },
                  { l: '6.5 L · Berline', v: 6.5, n: 'Berline Essence' },
                  { l: '7.8 L · SUV', v: 7.8, n: 'SUV Essence' },
                  { l: '9.2 L · Ancien / Sport', v: 9.2, n: 'Véhicule Essence' }
                ]"
                :key="p.v"
                type="button"
                class="pill-touch"
                :class="{ active: Math.abs(currentVehicle.consumption - p.v) < 0.15 }"
                @click="setConsumptionPreset(p.v, p.n)"
              >
                {{ p.l }}
              </button>
            </template>
          </div>

          <!-- Champ optionnel pour désigner le véhicule -->
          <div class="mb-4" style="max-width: 280px; margin-left: auto; margin-right: auto;">
            <label class="text-xxs font-semibold text-dimmed uppercase mb-1 block text-center">
              Modèle ou nom du véhicule (optionnel) :
            </label>
            <input
              v-model="currentVehicle.name"
              type="text"
              class="form-control text-sm text-center"
              placeholder="ex: Mégane 2013, Golf 7, Clio..."
            />
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch"
            @click="navigateTo('A06_MILEAGE')"
          >
            <span>Continuer ({{ Number(currentVehicle.consumption || 6.0).toFixed(1) }} {{ currentVehicle.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100)</span>
            <ArrowRight size="18" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-06 : KILOMÉTRAGE ANNUEL                      -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A06_MILEAGE'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Distance</span>
          <h2 class="question-title">Combien de kilomètres parcourez-vous par an ?</h2>
          <p class="question-desc">
            Le kilométrage est le levier n°1 : plus vous roulez, plus l'électrique est rentable.
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ (currentVehicle.annualMileage || 15000).toLocaleString('fr-FR') }}</span>
            <span class="big-currency">km / an</span>
          </div>

          <input
            type="range"
            min="3000"
            max="45000"
            step="1000"
            :value="currentVehicle.annualMileage || 15000"
            @input="setMileage(Number($event.target.value))"
            class="slider-touch accent-teal w-100 mb-4"
          />

          <div class="pills-row mb-4">
            <button
              v-for="km in [8000, 12000, 15000, 22000, 30000]"
              :key="km"
              type="button"
              class="pill-touch"
              :class="{ active: currentVehicle.annualMileage === km }"
              @click="setMileage(km)"
            >
              {{ (km / 1000) }}k km
            </button>
          </div>

          <!-- Mini calculateur domicile-travail -->
          <div class="commute-helper-box p-3 rounded-lg border-glass bg-card-subtle mb-4 text-xs">
            <div class="flex-between items-center mb-1">
              <span class="font-semibold text-main flex items-center gap-1.5">
                <Calculator size="14" class="text-teal" /> Calculer via mon trajet travail
              </span>
              <span class="text-teal font-bold font-mono">{{ dailyDistanceCommute }} km aller</span>
            </div>
            <input
              v-model.number="dailyDistanceCommute"
              type="range"
              min="5"
              max="100"
              step="5"
              class="slider-touch w-100 my-2"
            />
            <button type="button" class="btn btn-secondary btn-small w-100" @click="applyCommuteDistance">
              Appliquer (~{{ Math.round(dailyDistanceCommute * 2 * 220 + 2500).toLocaleString('fr-FR') }} km/an)
            </button>
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch"
            @click="navigateTo('A08_MAINTENANCE')"
          >
            <span>Continuer</span>
            <ArrowRight size="18" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-08 : ENTRETIEN & RÉPARATIONS PRÉVUES         -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A08_MAINTENANCE'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Entretien</span>
          <h2 class="question-title">Combien vous coûte l'entretien par an ?</h2>
          <p class="question-desc">
            Révisions annuelles, vidanges, pneumatiques, freins et pièces d'usure courantes.
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ (currentVehicle.maintenanceCost || 450).toLocaleString('fr-FR') }}</span>
            <span class="big-currency">€ / an</span>
          </div>

          <!-- Curseur interactif tactile -->
          <input
            type="range"
            min="100"
            max="1800"
            step="50"
            :value="currentVehicle.maintenanceCost || 450"
            @input="currentVehicle.maintenanceCost = Number($event.target.value)"
            class="slider-touch accent-teal w-100 mb-3"
          />

          <!-- Paliers rapides de repères -->
          <div class="pills-row mb-4">
            <button
              v-for="p in [
                { l: '250 € · Économe / Récent', v: 250 },
                { l: '450 € · Moyenne courante', v: 450 },
                { l: '700 € · Plus de 8 ans', v: 700 },
                { l: '1 000 € · Fort usage / Ancien', v: 1000 }
              ]"
              :key="p.v"
              type="button"
              class="pill-touch"
              :class="{ active: currentVehicle.maintenanceCost === p.v }"
              @click="currentVehicle.maintenanceCost = p.v"
            >
              {{ p.l }}
            </button>
          </div>

          <!-- Checkbox grosse réparation imminente -->
          <div class="imminent-repairs-card p-3 border-glass rounded-lg bg-card-subtle text-left mb-4">
            <label class="flex items-center gap-2 cursor-pointer mb-1">
              <input type="checkbox" v-model="hasImminentRepairs" class="accent-teal cursor-pointer" />
              <span class="text-xs font-semibold text-main">
                Avez-vous une grosse réparation prévue prochainement ?
              </span>
            </label>
            <div v-if="hasImminentRepairs" class="mt-2 animation-fadeIn">
              <div class="text-xxs text-muted mb-1">Montant estimé du devis (courroie, injecteurs, embrayage...) :</div>
              <div class="flex gap-2">
                <input
                  v-model.number="imminentRepairAmount"
                  type="number"
                  class="form-control text-xs"
                  placeholder="ex: 1200"
                />
                <span class="badge badge-amber badge-small shrink-0">À déduire</span>
              </div>
            </div>
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch"
            @click="confirmMaintenance"
          >
            <span>Valider mon budget entretien ({{ (currentVehicle.maintenanceCost || 450).toLocaleString('fr-FR') }} €/an)</span>
            <ArrowRight size="18" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-09 : DEVENIR DU VÉHICULE ACTUEL              -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A09_DEPARTURE'" class="screen-box animation-fadeIn">
        
        <!-- CAS 1 : PROPRIÉTAIRE (COMPTANT OU CRÉDIT) -->
        <div v-if="currentVehicleFinanceType === 'CASH' || currentVehicleFinanceType === 'CREDIT'">
          <div class="question-header">
            <span class="question-badge">Devenir du véhicule</span>
            <h2 class="question-title">Que comptez-vous faire de votre véhicule actuel ?</h2>
            <p class="question-desc">
              Cela détermine votre apport financier et votre éligibilité à la Prime à la conversion.
            </p>
          </div>

          <div class="options-grid">
            <button type="button" class="option-card-touch featured" @click="selectDepartureOption('SCRAP')">
              <div class="card-icon-bubble green">♻️</div>
              <div class="card-content">
                <div class="card-label">Mise à la casse (Véhicule ancien)</div>
                <div class="card-sub">Diesel avant 2011 ou Essence avant 2006 · <strong>Prime jusqu'à 3 000 €</strong></div>
              </div>
              <ChevronRight size="18" class="chevron-hint" />
            </button>

            <button type="button" class="option-card-touch" @click="selectDepartureOption('RESALE')">
              <div class="card-icon-bubble teal">💶</div>
              <div class="card-content">
                <div class="card-label">Revente d'occasion ou reprise garage</div>
                <div class="card-sub">
                  <span v-if="currentVehicle.resaleValue && currentVehicle.resaleValue > 0">
                    Apport personnel prévu : ~{{ currentVehicle.resaleValue.toLocaleString('fr-FR') }} € déduit du projet
                  </span>
                  <span v-else>
                    Définir librement votre valeur de reprise (apport déduit de l'achat)
                  </span>
                </div>
              </div>
              <ChevronRight size="18" class="chevron-hint" />
            </button>

            <button type="button" class="option-card-touch" @click="selectDepartureOption('KEEP')">
              <div class="card-icon-bubble gray">🔑</div>
              <div class="card-content">
                <div class="card-label">Je le conserve (2nd véhicule du foyer)</div>
                <div class="card-sub">Aucun apport issu de la revente</div>
              </div>
              <ChevronRight size="18" class="chevron-hint" />
            </button>
          </div>
        </div>

        <!-- CAS 2 : LOCATION LONGUE DURÉE (LLD) -->
        <div v-else-if="currentVehicleFinanceType === 'LLD'">
          <div class="question-header">
            <span class="question-badge">Fin de contrat LLD</span>
            <h2 class="question-title">Que prévoyez-vous pour votre contrat LLD actuel ?</h2>
            <p class="question-desc">
              En LLD, le véhicule est obligatoirement restitué au loueur à échéance.
            </p>
          </div>

          <div class="options-grid">
            <button type="button" class="option-card-touch featured" @click="selectDepartureOption('LLD_RETURN')">
              <div class="card-icon-bubble teal">🔄</div>
              <div class="card-content">
                <div class="card-label">Restitution au loueur en fin de contrat</div>
                <div class="card-sub">Vos loyers actuels s'arrêtent, libérant 100% de votre budget</div>
              </div>
              <ChevronRight size="18" class="chevron-hint" />
            </button>

            <button type="button" class="option-card-touch" @click="selectDepartureOption('LLD_RETURN')">
              <div class="card-icon-bubble blue">⏳</div>
              <div class="card-content">
                <div class="card-label">Contrat en cours (anticipation)</div>
                <div class="card-sub">Planifier la transition pour la fin d'engagement</div>
              </div>
              <ChevronRight size="18" class="chevron-hint" />
            </button>
          </div>
        </div>

        <!-- CAS 3 : LOCATION AVEC OPTION D'ACHAT (LOA) -->
        <div v-else>
          <div class="question-header">
            <span class="question-badge">Option d'Achat LOA</span>
            <h2 class="question-title">Que comptez-vous faire en fin de votre LOA actuelle ?</h2>
            <p class="question-desc">
              Vous pouvez restituer le véhicule, le racheter ou le faire reprendre par un concessionnaire.
            </p>
          </div>

          <div class="options-grid">
            <button type="button" class="option-card-touch" @click="selectDepartureOption('LOA_RETURN')">
              <div class="card-icon-bubble teal">🔄</div>
              <div class="card-content">
                <div class="card-label">Restitution simple au bailleur</div>
                <div class="card-sub">Fin des loyers sans lever l'option d'achat</div>
              </div>
              <ChevronRight size="18" class="chevron-hint" />
            </button>

            <button type="button" class="option-card-touch featured" @click="selectDepartureOption('LOA_BUYOUT')">
              <div class="card-icon-bubble amber">💰</div>
              <div class="card-content">
                <div class="card-label">Racheter le véhicule (Lever l'option)</div>
                <div class="card-sub">Payer la valeur résiduelle pour en devenir propriétaire</div>
              </div>
              <ChevronRight size="18" class="chevron-hint" />
            </button>

            <button type="button" class="option-card-touch" @click="selectDepartureOption('LOA_TRADE')">
              <div class="card-icon-bubble cyan">🤝</div>
              <div class="card-content">
                <div class="card-label">Reprise concessionnaire avec solde de LOA</div>
                <div class="card-sub">Le garage solde votre dossier et reverse la plus-value éventuelle</div>
              </div>
              <ChevronRight size="18" class="chevron-hint" />
            </button>
          </div>
        </div>

      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-09 bis : PRIX DU RACHAT EN FIN DE LOA        -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A09_LOA_BUY'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Rachat LOA</span>
          <h2 class="question-title">Quel est le montant de l'option d'achat finale ?</h2>
          <p class="question-desc">
            Indiqué sur votre contrat de LOA (valeur résiduelle pour devenir propriétaire).
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ currentLoaBuyoutPrice || 9500 }}</span>
            <span class="big-currency">€</span>
          </div>

          <input
            type="range"
            min="3000"
            max="25000"
            step="500"
            :value="currentLoaBuyoutPrice || 9500"
            @input="emit('update:currentLoaBuyoutPrice', Number($event.target.value))"
            class="slider-touch accent-teal w-100 mb-4"
          />

          <div class="pills-row mb-4">
            <button
              v-for="val in [6000, 8500, 11000, 14000]"
              :key="val"
              type="button"
              class="pill-touch"
              :class="{ active: currentLoaBuyoutPrice === val }"
              @click="emit('update:currentLoaBuyoutPrice', val)"
            >
              {{ val }} €
            </button>
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch"
            @click="navigateTo('C01_CHARGING')"
          >
            <span>Continuer vers le nouveau véhicule</span>
            <ArrowRight size="18" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE A : ÉCRAN A-09 ter : VALEUR DE REVENTE / REPRISE DU VÉHICULE -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'A09_RESALE_ESTIMATE'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Apport &amp; Revente</span>
          <h2 class="question-title">À combien estimez-vous la valeur de reprise ou revente de votre véhicule ?</h2>
          <p class="question-desc">
            Ce montant servira d'apport personnel et viendra directement diminuer le coût d'achat de votre futur véhicule.
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ (currentVehicle.resaleValue !== null && currentVehicle.resaleValue !== undefined) ? currentVehicle.resaleValue.toLocaleString('fr-FR') : '0' }}</span>
            <span class="big-currency">€ d'apport</span>
          </div>

          <!-- Saisie directe manuelle -->
          <div class="resale-input-container mb-3">
            <label class="text-xs font-semibold text-dimmed mb-1 block">Saisissez votre montant exact :</label>
            <div class="input-currency-wrapper">
              <input
                v-model.number="currentVehicle.resaleValue"
                type="number"
                min="0"
                max="100000"
                step="250"
                class="form-control text-center font-mono font-bold text-lg"
                placeholder="ex: 7 500"
              />
            </div>
          </div>

          <input
            type="range"
            min="0"
            max="35000"
            step="500"
            :value="currentVehicle.resaleValue || 0"
            @input="currentVehicle.resaleValue = Number($event.target.value)"
            class="slider-touch accent-teal w-100 mb-3"
          />

          <!-- Paliers rapides de repères -->
          <div class="pills-row mb-4">
            <button
              v-for="val in [0, 3000, 6000, 9000, 12000, 16000]"
              :key="val"
              type="button"
              class="pill-touch"
              :class="{ active: currentVehicle.resaleValue === val }"
              @click="currentVehicle.resaleValue = val"
            >
              {{ val === 0 ? '0 € (Aucun apport)' : val.toLocaleString('fr-FR') + ' €' }}
            </button>
          </div>

          <!-- Encadré explicatif sur la réalité des cotes automobiles & lien La Centrale -->
          <div class="info-callout mb-4">
            <div class="callout-icon">💡</div>
            <div class="callout-text text-xs w-100">
              <div class="mb-2">
                <strong>Chaque véhicule est unique :</strong> Entre deux véhicules d'une même année, la valeur réelle varie considérablement selon la <strong>marque</strong> (généraliste vs premium), la finition, le <strong>kilométrage réel au compteur</strong> et l'état général.
              </div>
              <div class="lacentrale-helper-box">
                <span class="text-xxs text-dimmed">Un doute sur la valeur de votre voiture ?</span>
                <a
                  href="https://www.lacentrale.fr/lacote_origine.php"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="lacentrale-btn"
                >
                  <span>Estimer gratuitement sur La Centrale</span>
                  <ExternalLink size="13" />
                </a>
              </div>
            </div>
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch mb-2"
            @click="navigateTo('C01_CHARGING')"
          >
            <span>Valider cet apport ({{ (currentVehicle.resaleValue || 0).toLocaleString('fr-FR') }} €)</span>
            <ArrowRight size="18" />
          </button>

          <button
            type="button"
            class="btn btn-link text-xs text-muted w-100 py-1"
            @click="() => { currentVehicle.resaleValue = 0; navigateTo('C01_CHARGING'); }"
          >
            Ne prévoir aucun apport issu de la revente (0 €)
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE B : ÉCRAN B-02 : BUDGET MOBILITÉ ACTUEL                  -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'B02_BUDGET'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Budget Actuel</span>
          <h2 class="question-title">À combien s'élèvent vos frais de mobilité chaque mois ?</h2>
          <p class="question-desc">
            Abonnements de transport, billets de train, locations de week-ends ou VTC.
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ baselineMobilityCost || 86 }}</span>
            <span class="big-currency">€ / mois</span>
          </div>

          <input
            type="range"
            min="10"
            max="500"
            step="5"
            :value="baselineMobilityCost || 86"
            @input="emit('update:baselineMobilityCost', Number($event.target.value))"
            class="slider-touch accent-teal w-100 mb-4"
          />

          <div class="pills-row mb-4">
            <button
              v-for="b in [15, 86, 150, 220, 350]"
              :key="b"
              type="button"
              class="pill-touch"
              :class="{ active: baselineMobilityCost === b }"
              @click="emit('update:baselineMobilityCost', b)"
            >
              {{ b }} €
            </button>
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch"
            @click="navigateTo('B03_USAGE')"
          >
            <span>Continuer</span>
            <ArrowRight size="18" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE B : ÉCRAN B-03 : USAGE DU FUTUR VÉHICULE                 -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'B03_USAGE'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Besoins</span>
          <h2 class="question-title">Quel sera l'usage principal de votre futur véhicule ?</h2>
          <p class="question-desc">
            Pour dimensionner l'autonomie et sélectionner la catégorie de véhicule parfaite.
          </p>
        </div>

        <div class="options-grid">
          <button type="button" class="option-card-touch" @click="selectFutureUsage('COMMUTE')">
            <div class="card-icon-bubble teal">💼</div>
            <div class="card-content">
              <div class="card-label">Trajets quotidiens domicile-travail</div>
              <div class="card-sub">Besoin d'économie maximale et de fiabilité</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectFutureUsage('FAMILY')">
            <div class="card-icon-bubble blue">👶</div>
            <div class="card-content">
              <div class="card-label">Famille & Vie quotidienne</div>
              <div class="card-sub">Courses, enfants, activités, espace intérieur</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectFutureUsage('LEISURE')">
            <div class="card-icon-bubble green">🌲</div>
            <div class="card-content">
              <div class="card-label">Loisirs, week-ends & Vacances</div>
              <div class="card-sub">Évasion, liberté et trajets régionaux</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectFutureUsage('REPLACE')">
            <div class="card-icon-bubble amber">⏱️</div>
            <div class="card-content">
              <div class="card-label">Remplacement des transports en commun</div>
              <div class="card-sub">Gain de temps, confort et indépendance</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE B : ÉCRAN B-04 : KILOMÉTRAGE PRÉVISIONNEL                -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'B04_MILEAGE'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Distance</span>
          <h2 class="question-title">Combien de kilomètres pensez-vous rouler par an ?</h2>
          <p class="question-desc">
            Vous n'êtes pas sûr ? Choisissez un profil standard ou faites une estimation.
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ (targetVehicle.annualMileage || 12000).toLocaleString('fr-FR') }}</span>
            <span class="big-currency">km / an</span>
          </div>

          <input
            type="range"
            min="3000"
            max="35000"
            step="1000"
            :value="targetVehicle.annualMileage || 12000"
            @input="setMileage(Number($event.target.value))"
            class="slider-touch accent-teal w-100 mb-4"
          />

          <div class="pills-row mb-4">
            <button
              v-for="km in [8000, 12000, 16000, 22000]"
              :key="km"
              type="button"
              class="pill-touch"
              :class="{ active: targetVehicle.annualMileage === km }"
              @click="setMileage(km)"
            >
              {{ (km / 1000) }}k km
            </button>
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch"
            @click="navigateTo('B05_FINANCING')"
          >
            <span>Continuer</span>
            <ArrowRight size="18" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- BRANCHE B : ÉCRAN B-05 : FINANCEMENT NON VÉHICULÉ                -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'B05_FINANCING'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Financement</span>
          <h2 class="question-title">Comment envisagez-vous d'acquérir ce véhicule ?</h2>
          <p class="question-desc">
            80% des conducteurs électriques choisissent la location pour sa mensualité fixe tout compris.
          </p>
        </div>

        <div class="options-grid">
          <button type="button" class="option-card-touch featured" @click="selectFinancing(true)">
            <div class="card-icon-bubble teal">🔄</div>
            <div class="card-content">
              <div class="card-label">En location tout compris (LOA / LLD)</div>
              <div class="card-sub">Mensualité fixe maîtrisée, véhicule garanti, sans aléas de revente</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectFinancing(false)">
            <div class="card-icon-bubble blue">💰</div>
            <div class="card-content">
              <div class="card-label">Achat comptant ou crédit bancaire</div>
              <div class="card-sub">Être propriétaire du véhicule à 100% dès le départ</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- TRONC COMMUN : ÉCRAN C-01 : STATIONNEMENT & RECHARGE             -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'C01_CHARGING'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Recharge</span>
          <h2 class="question-title">Où votre véhicule sera-t-il stationné la nuit ?</h2>
          <p class="question-desc">
            L'électricité à domicile coûte 3 à 4 fois moins cher que le carburant ou les bornes rapides !
          </p>
        </div>

        <div class="options-grid">
          <button type="button" class="option-card-touch featured" @click="selectChargingProfile(0.85)">
            <div class="card-icon-bubble green">
              <Home size="22" />
            </div>
            <div class="card-content">
              <div class="card-label">Maison individuelle (Garage / Prise)</div>
              <div class="card-sub">85% de recharge à domicile · <strong>Seulement ~3,70 € les 100 km</strong></div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectChargingProfile(0.60)">
            <div class="card-icon-bubble blue">
              <Building2 size="22" />
            </div>
            <div class="card-content">
              <div class="card-label">Copropriété / Parking partagé</div>
              <div class="card-sub">Recharge mixte · Droit à la prise & crédit d'impôt borne 500 €</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectChargingProfile(0.15)">
            <div class="card-icon-bubble amber">
              <ParkingCircle size="22" />
            </div>
            <div class="card-content">
              <div class="card-label">Stationnement en voirie</div>
              <div class="card-sub">Recharge sur bornes publiques de ville et réseau rapide</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- TRONC COMMUN : ÉCRAN C-02 : REVENUS FISCAUX & AIDES D'ÉTAT       -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'C02_INCOME'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Aides de l'État</span>
          <h2 class="question-title">Quel est votre revenu fiscal de référence par part ?</h2>
          <p class="question-desc">
            Information 100% anonyme servant à déduire le <strong>Bonus de 7 000 €</strong> ou le <strong>Leasing Social</strong>.
          </p>
        </div>

        <div class="options-grid">
          <button type="button" class="option-card-touch featured" @click="selectIncomeTier('MODEST')">
            <div class="card-icon-bubble green">🎁</div>
            <div class="card-content">
              <div class="card-label">Moins de 15 400 € / part (Foyer modeste)</div>
              <div class="card-sub"><strong>Bonus Majoré 7 000 €</strong> · Éligible <strong>Leasing Social 100€/mois</strong></div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectIncomeTier('STANDARD')">
            <div class="card-icon-bubble teal">⚡</div>
            <div class="card-content">
              <div class="card-label">Plus de 15 400 € / part (Foyer standard)</div>
              <div class="card-sub"><strong>Bonus Écologique Standard 4 000 €</strong> déduit de l'achat</div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>
        </div>

        <!-- Toggle montant personnalisé -->
        <div class="custom-income-accordion mt-3">
          <button
            type="button"
            class="btn-text-subtle text-xs text-muted flex items-center gap-1.5"
            @click="customIncomeExact = !customIncomeExact"
          >
            <HelpCircle size="14" />
            <span>Entrer mon montant exact (Avis d'imposition ligne 25)</span>
          </button>

          <div v-if="customIncomeExact" class="mt-2 animation-fadeIn flex gap-2">
            <input
              :value="taxIncome"
              @input="emit('update:taxIncome', Number($event.target.value))"
              type="number"
              class="form-control text-xs"
              placeholder="ex: 18500"
            />
            <button type="button" class="btn btn-secondary btn-small" @click="navigateTo('C03_FORMAT')">
              Valider
            </button>
          </div>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- TRONC COMMUN : ÉCRAN C-03 : GABARIT RECHERCHÉ                     -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'C03_FORMAT'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Préférences</span>
          <h2 class="question-title">Quel format de véhicule préférez-vous ?</h2>
          <p class="question-desc">
            Nous sélectionnerons les modèles les plus fiables et économiques du marché.
          </p>
        </div>

        <div class="options-grid">
          <button type="button" class="option-card-touch featured" @click="selectTargetFormat('CITY')">
            <div class="card-icon-bubble teal">🚗</div>
            <div class="card-content">
              <div class="card-label">Citadine agile & économique</div>
              <div class="card-sub">ex: <strong>Citroën ë-C3, Renault 5 E-Tech, Peugeot e-208</strong></div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectTargetFormat('COMPACT')">
            <div class="card-icon-bubble blue">🚙</div>
            <div class="card-content">
              <div class="card-label">Berline & Compacte polyvalente</div>
              <div class="card-sub">ex: <strong>Tesla Model 3, Renault Megane E-Tech, MG4</strong></div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>

          <button type="button" class="option-card-touch" @click="selectTargetFormat('SUV')">
            <div class="card-icon-bubble cyan">🚐</div>
            <div class="card-content">
              <div class="card-label">SUV & Familiale spacieuse</div>
              <div class="card-sub">ex: <strong>Tesla Model Y, Renault Scenic E-Tech, Kia EV3</strong></div>
            </div>
            <ChevronRight size="18" class="chevron-hint" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- TRONC COMMUN : ÉCRAN C-04 : BUDGET MENSUEL MAXIMUM                -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'C04_BUDGET'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Budget Cible</span>
          <h2 class="question-title">Quel budget mensuel maximum visez-vous ?</h2>
          <p class="question-desc">
            Nous calculons le coût complet tout compris (véhicule + énergie + entretien).
          </p>
        </div>

        <div class="interactive-input-card">
          <div class="big-amount-display">
            <span class="big-number font-mono">{{ targetMonthlyBudget }}</span>
            <span class="big-currency">€ / mois</span>
          </div>

          <input
            v-model.number="targetMonthlyBudget"
            type="range"
            min="99"
            max="650"
            step="10"
            class="slider-touch accent-teal w-100 mb-4"
          />

          <div class="pills-row mb-4">
            <button
              v-for="b in [99, 180, 250, 350, 480]"
              :key="b"
              type="button"
              class="pill-touch"
              :class="{ active: targetMonthlyBudget === b }"
              @click="targetMonthlyBudget = b"
            >
              {{ b }} €
            </button>
          </div>

          <!-- Indication en temps réel -->
          <div class="budget-hint-box p-3 rounded-lg border-glass bg-card-subtle mb-4 text-xs">
            <span v-if="targetMonthlyBudget <= 140" class="text-teal font-semibold">
              🎉 À ce tarif, vous êtes parfaitement dans la tranche du <strong>Leasing Social à 99€/mois</strong> !
            </span>
            <span v-else-if="targetMonthlyBudget <= 260" class="text-teal font-semibold">
              ✨ Parfait pour rouler en <strong>Citroën ë-C3</strong> ou <strong>Renault 5 E-Tech neuve</strong> !
            </span>
            <span v-else class="text-teal font-semibold">
              🚀 Accès direct aux <strong>Tesla Model 3 / Y</strong> ou <strong>Renault Scenic E-Tech</strong> !
            </span>
          </div>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch"
            @click="confirmTargetBudget"
          >
            <span>Calculer mes économies</span>
            <Sparkles size="18" />
          </button>
        </div>
      </div>

      <!-- ================================================================= -->
      <!-- ÉCRAN FINAL : AUTHENTIFICATION OPTIONNELLE AVANT RÉSULTATS        -->
      <!-- ================================================================= -->
      <div v-else-if="currentScreen === 'FINAL_AUTH'" class="screen-box animation-fadeIn">
        <div class="question-header">
          <span class="question-badge">Dernière étape</span>
          <h2 class="question-title">Votre simulation est prête !</h2>
          <p class="question-desc">
            Souhaitez-vous enregistrer vos réponses pour ne plus avoir à les ressaisir ?
          </p>
        </div>

        <div class="auth-pitch-card p-4 rounded-xl border-glass bg-card mb-4">
          <div class="card-icon-bubble teal mb-3">
            <Lock size="24" />
          </div>
          <h3 class="text-sm font-bold text-main mb-1">Sauvegarder mon profil de mobilité</h3>
          <p class="text-xs text-muted mb-4">
            Connectez-vous ou créez votre compte gratuit pour conserver vos véhicules dans votre garage virtuel et retrouver vos simulations sur tous vos écrans.
          </p>

          <button
            type="button"
            class="btn btn-primary w-100 btn-large-touch mb-2"
            @click="triggerAuthAndSubmit"
          >
            <User size="16" />
            <span>Enregistrer mon profil & Voir mes résultats</span>
          </button>

          <button
            type="button"
            class="btn btn-secondary w-100 text-xs py-2.5"
            @click="proceedWithoutAuth"
          >
            Découvrir mes résultats sans compte →
          </button>
        </div>
      </div>

    </div>

    <!-- FOOTER MOBILE TOUCH CONTROL : RESTE EN BAS DE L'ÉCRAN -->
    <div class="wizard-mobile-footer">
      <div class="flex-between items-center w-100">
        <button
          v-if="historyStack.length > 0"
          type="button"
          class="btn-footer-back"
          @click="goBack"
        >
          <ArrowLeft size="16" />
          <span>Précédent</span>
        </button>
        <span v-else class="text-xxs text-dimmed">EcoSwitch Studio v2.4</span>
      </div>
    </div>

  </div>
</template>

<style scoped>
.step-wizard-container {
  max-width: 640px;
  margin: 0 auto;
  min-height: 520px;
  display: flex;
  flex-direction: column;
}

/* ── HEADER NAVIGATION ─────────────────────────────────────────────────── */
.wizard-nav-header {
  margin-bottom: 20px;
  padding: 0 4px;
}

.btn-back-touch {
  background: transparent;
  border: none;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  transition: all 0.15s ease;
}
.btn-back-touch:hover {
  background: var(--bg-card-subtle);
  color: var(--text-main);
}

.step-counter-text {
  font-size: 0.75rem;
  color: var(--text-dimmed);
}

.btn-switch-expert-subtle {
  background: transparent;
  border: 1px solid var(--border-glass);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: 0.72rem;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-switch-expert-subtle:hover {
  border-color: var(--border-hover);
  color: var(--text-main);
}

.progress-bar-track {
  width: 100%;
  height: 4px;
  background: var(--bg-card-subtle);
  border-radius: 999px;
  overflow: hidden;
}
.progress-bar-fill {
  height: 100%;
  background: var(--accent-teal);
  border-radius: 999px;
  transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

/* ── VIEWPORT DES QUESTIONS ────────────────────────────────────────────── */
.wizard-card-viewport {
  flex: 1;
}

.screen-box {
  animation: fadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.question-header {
  margin-bottom: 22px;
  text-align: left;
}

.question-badge {
  display: inline-block;
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--accent-teal);
  margin-bottom: 6px;
}

.question-title {
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.3;
  margin-bottom: 6px;
}

.question-desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.45;
  margin: 0;
}

/* ── CARTES DE CHOIX TACTILES (MOBILE FIRST) ───────────────────────────── */
.options-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-card-touch {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: var(--shadow-sm);
  min-height: 64px;
  touch-action: manipulation;
}

.option-card-touch:hover {
  border-color: var(--border-hover);
  background: var(--bg-card-hover);
  transform: translateY(-1px);
}

.option-card-touch:active {
  transform: scale(0.985);
}

.option-card-touch.featured {
  border-color: rgba(5, 150, 105, 0.25);
  background: linear-gradient(to right, var(--bg-card), var(--accent-teal-soft));
}

.card-icon-bubble {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 1.2rem;
}
.card-icon-bubble.teal { background: var(--accent-teal-soft); color: var(--accent-teal); }
.card-icon-bubble.blue { background: var(--accent-blue-soft); color: var(--accent-blue); }
.card-icon-bubble.cyan { background: var(--accent-cyan-soft); color: var(--accent-cyan); }
.card-icon-bubble.green { background: rgba(16, 185, 129, 0.12); color: #10B981; }
.card-icon-bubble.amber { background: var(--accent-amber-soft); color: var(--accent-amber); }
.card-icon-bubble.gray { background: var(--bg-card-subtle); color: var(--text-muted); }

.card-content {
  flex: 1;
  min-width: 0;
}

.card-label {
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 2px;
}

.card-sub {
  font-size: 0.75rem;
  color: var(--text-muted);
  line-height: 1.35;
}

.chevron-hint {
  color: var(--text-dimmed);
  flex-shrink: 0;
}

/* ── INTERACTIVE CARD (SLIDERS & PILLS) ────────────────────────────────── */
.interactive-input-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-xl);
  padding: 24px 20px;
  box-shadow: var(--shadow-card);
  text-align: center;
}

.big-amount-display {
  margin-bottom: 20px;
}

.big-number {
  font-size: 2.8rem;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.03em;
}

.big-currency {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-muted);
  margin-left: 6px;
}

.slider-touch {
  height: 8px;
  border-radius: 999px;
  cursor: pointer;
}

.pills-row {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pill-touch {
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  padding: 6px 14px;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
}
.pill-touch:hover {
  color: var(--text-main);
  border-color: var(--border-hover);
}
.pill-touch.active {
  background: var(--accent-teal);
  color: #FFFFFF;
  border-color: var(--accent-teal);
}

.btn-large-touch {
  padding: 13px 20px;
  font-size: 0.95rem;
  border-radius: var(--radius-lg);
  font-weight: 600;
}

/* ── RECHERCHE & QUICK PICK CATALOGUE ──────────────────────────────────── */
.search-box-container {
  position: relative;
}
.search-icon-fixed {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}
.form-control-search {
  padding-left: 36px;
  height: 42px;
}

.search-results-list {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.search-result-item {
  width: 100%;
  padding: 10px 14px;
  border: none;
  background: transparent;
  text-align: left;
  border-bottom: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: background 0.15s ease;
}
.search-result-item:last-child {
  border-bottom: none;
}
.search-result-item:hover {
  background: var(--bg-card-subtle);
}

.quick-cars-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
@media (min-width: 480px) {
  .quick-cars-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.quick-car-btn {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 10px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
}
.quick-car-btn:hover {
  border-color: var(--accent-teal);
  background: var(--accent-teal-soft);
}
.quick-car-badge {
  font-size: 0.65rem;
  color: var(--text-dimmed);
  margin-top: 3px;
}

/* ── FOOTER SUBTILE ────────────────────────────────────────────────────── */
.wizard-mobile-footer {
  margin-top: 24px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}

.btn-footer-back, .btn-footer-mode {
  background: transparent;
  border: none;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--text-muted);
  cursor: pointer;
}
.btn-footer-back:hover, .btn-footer-mode:hover {
  color: var(--text-main);
}

.info-callout {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  text-align: left;
}

.callout-icon {
  font-size: 1.25rem;
  line-height: 1.2;
  flex-shrink: 0;
}

.callout-text {
  color: var(--text-muted);
  line-height: 1.5;
}

.resale-input-container {
  max-width: 260px;
  margin-left: auto;
  margin-right: auto;
}

.lacentrale-helper-box {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.lacentrale-btn {
  color: var(--accent-teal);
  font-weight: 600;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.76rem;
}
.lacentrale-btn:hover {
  text-decoration: underline;
}

/* ── RESPONSIVE & MOBILE POLISH ────────────────────────────────────────── */
@media (max-width: 480px) {
  .desktop-only {
    display: none;
  }
  .question-title {
    font-size: 1.18rem;
  }
  .big-number {
    font-size: 2.2rem;
  }
  .option-card-touch {
    padding: 12px 14px;
  }
  .card-icon-bubble {
    width: 38px;
    height: 38px;
  }
}
</style>
