<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Zap,
  Home,
  Building2,
  ParkingCircle,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  Search,
  Check,
  Layers,
  HelpCircle,
  ChevronDown,
  ChevronUp,
  Calculator,
  Car,
  Fuel,
  CreditCard,
  ShieldCheck
} from '@lucide/vue'
import { apiGetCatalogVariants } from '../../utils/api.js'

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
  'submit',
  'switch-to-expert'
])

// Étape active (1: Actuel, 2: Logement & Aides, 3: Cible & Budget)
const currentStep = ref(1)

// Variantes dynamiques chargées depuis le catalogue officiel
const catalogVariants = ref([])

// Modèles courants du parc français (Fallback)
const popularCurrentCars = [
  {
    name: 'Peugeot 208 II PureTech 100',
    brand: 'Peugeot',
    fuelType: 'PETROL',
    consumption: 5.3,
    insuranceCost: 580,
    maintenanceCost: 440,
    resaleValue: 9800,
    tag: 'Essence'
  },
  {
    name: 'Renault Clio V TCe 90',
    brand: 'Renault',
    fuelType: 'PETROL',
    consumption: 5.2,
    insuranceCost: 560,
    maintenanceCost: 420,
    resaleValue: 9500,
    tag: 'Essence'
  },
  {
    name: 'Citroën C3 III PureTech 110',
    brand: 'Citroën',
    fuelType: 'PETROL',
    consumption: 5.5,
    insuranceCost: 550,
    maintenanceCost: 430,
    resaleValue: 9000,
    tag: 'Essence'
  },
  {
    name: 'Dacia Sandero Stepway TCe',
    brand: 'Dacia',
    fuelType: 'PETROL',
    consumption: 5.8,
    insuranceCost: 490,
    maintenanceCost: 380,
    resaleValue: 8200,
    tag: 'Essence'
  },
  {
    name: 'Peugeot 308 III BlueHDi 130',
    brand: 'Peugeot',
    fuelType: 'DIESEL',
    consumption: 4.5,
    insuranceCost: 740,
    maintenanceCost: 580,
    resaleValue: 14000,
    tag: 'Diesel'
  },
  {
    name: 'Volkswagen Golf VIII 2.0 TDI',
    brand: 'Volkswagen',
    fuelType: 'DIESEL',
    consumption: 4.1,
    insuranceCost: 780,
    maintenanceCost: 620,
    resaleValue: 15000,
    tag: 'Diesel'
  },
  {
    name: 'Toyota Yaris IV Hybrid',
    brand: 'Toyota',
    fuelType: 'HYBRID',
    consumption: 3.8,
    insuranceCost: 560,
    maintenanceCost: 360,
    resaleValue: 12500,
    tag: 'Hybride'
  },
  {
    name: 'Peugeot 206 / 207 HDi (Ancien)',
    brand: 'Peugeot',
    fuelType: 'DIESEL',
    consumption: 4.8,
    insuranceCost: 450,
    maintenanceCost: 500,
    resaleValue: 2000,
    tag: 'Vieux Diesel'
  }
]

// Modèles cibles recommandés pour l'étape 3 (Fallback)
const popularTargetCars = [
  {
    name: 'Tesla Model 3 Highland (2024)',
    brand: 'Tesla',
    fuelType: 'ELECTRIC',
    consumption: 14.4,
    purchasePrice: 41490,
    insuranceCost: 900,
    maintenanceCost: 250,
    badge: 'Best-Seller Élec'
  },
  {
    name: 'Renault Megane E-Tech EV60',
    brand: 'Renault',
    fuelType: 'ELECTRIC',
    consumption: 16.1,
    purchasePrice: 38000,
    insuranceCost: 780,
    maintenanceCost: 260,
    badge: 'Made in France'
  },
  {
    name: 'Peugeot e-208 GT',
    brand: 'Peugeot',
    fuelType: 'ELECTRIC',
    consumption: 15.4,
    purchasePrice: 34800,
    insuranceCost: 720,
    maintenanceCost: 240,
    badge: 'Citadine Élec'
  },
  {
    name: 'MG 4 EV Luxury',
    brand: 'MG',
    fuelType: 'ELECTRIC',
    consumption: 16.0,
    purchasePrice: 32990,
    insuranceCost: 760,
    maintenanceCost: 240,
    badge: 'Rapport Qualité/Prix'
  },
  {
    name: 'Dacia Spring Extreme',
    brand: 'Dacia',
    fuelType: 'ELECTRIC',
    consumption: 13.9,
    purchasePrice: 18900,
    insuranceCost: 450,
    maintenanceCost: 180,
    badge: 'Ultra Abordable'
  },
  {
    name: 'Toyota Yaris IV Hybrid (2020)',
    brand: 'Toyota',
    fuelType: 'HYBRID',
    consumption: 3.8,
    purchasePrice: 23950,
    insuranceCost: 560,
    maintenanceCost: 360,
    badge: 'Hybride sans prise'
  },
  {
    name: 'Renault Clio V E-Tech Hybrid',
    brand: 'Renault',
    fuelType: 'HYBRID',
    consumption: 4.2,
    purchasePrice: 22400,
    insuranceCost: 590,
    maintenanceCost: 380,
    badge: 'Hybride Polyvalente'
  },
  {
    name: 'Tesla Model Y RWD (2023)',
    brand: 'Tesla',
    fuelType: 'ELECTRIC',
    consumption: 15.7,
    purchasePrice: 44990,
    insuranceCost: 950,
    maintenanceCost: 280,
    badge: 'SUV Familial'
  }
]

// Construction dynamique des véhicules actuels issus du catalogue avec photos
const dynamicCurrentCars = computed(() => {
  if (!catalogVariants.value || catalogVariants.value.length === 0) {
    return popularCurrentCars
  }

  const desiredCurrent = [
    { brand: 'Peugeot', model: '208', fuel: 'HYBRID', tag: 'Hybride', resale: 12800 },
    { brand: 'Renault', model: 'Clio', fuel: 'HYBRID', tag: 'Hybride', resale: 12500 },
    { brand: 'Citroën', model: 'C3', tag: 'Électrique / Éco', resale: 11000 },
    { brand: 'Dacia', model: 'Duster', tag: 'Hybride / SUV', resale: 14200 },
    { brand: 'Volkswagen', model: 'Golf', tag: 'Hybride', resale: 16500 },
    { brand: 'Toyota', model: 'Yaris', tag: 'Hybride', resale: 13000 },
    { brand: 'Fiat', model: 'Panda', tag: 'Micro-Hybride', resale: 8500 },
    { brand: 'Peugeot', model: '2008', tag: 'SUV Hybride', resale: 15000 }
  ]

  const results = []
  const usedIds = new Set()

  for (const item of desiredCurrent) {
    const v = catalogVariants.value.find(c => 
      c.brandName?.toLowerCase().includes(item.brand.toLowerCase()) &&
      c.modelName?.toLowerCase().includes(item.model.toLowerCase()) &&
      (item.fuel ? c.fuelType === item.fuel : true) &&
      !usedIds.has(c.id)
    )

    if (v) {
      usedIds.add(v.id)
      results.push({
        id: v.id,
        name: `${v.brandName} ${v.modelName} ${v.motorisationName || ''}`.trim(),
        brand: v.brandName,
        model: v.modelName,
        fuelType: v.fuelType,
        consumption: v.consumptionWltp || 5.5,
        insuranceCost: v.defaultInsuranceCost || 580,
        maintenanceCost: v.defaultMaintenanceCost || 440,
        resaleValue: v.estimatedResaleValue || item.resale,
        imageUrl: v.finitionImageUrl || v.imageUrl || v.modelImageUrl,
        brandLogoUrl: v.brandLogoUrl,
        tag: item.tag || (v.fuelType === 'DIESEL' ? 'Diesel' : (v.fuelType === 'HYBRID' ? 'Hybride' : 'Essence'))
      })
    }
  }

  return results.length >= 4 ? results : popularCurrentCars
})

// Construction dynamique des véhicules cibles issus du catalogue avec photos
const dynamicTargetCars = computed(() => {
  if (!catalogVariants.value || catalogVariants.value.length === 0) {
    return popularTargetCars
  }

  const desiredTargets = [
    { brand: 'Tesla', model: 'Model 3', badge: 'Best-Seller Élec' },
    { brand: 'Renault', model: 'Megane', badge: 'Made in France' },
    { brand: 'Peugeot', model: 'e-208', fuel: 'ELECTRIC', badge: 'Citadine Élec' },
    { brand: 'MG', model: 'MG4', badge: 'Rapport Q/P' },
    { brand: 'Tesla', model: 'Model Y', badge: 'SUV Familial' },
    { brand: 'Dacia', model: 'Spring', badge: 'Ultra Éco' },
    { brand: 'BYD', model: 'Atto 3', badge: 'SUV Électrique' },
    { brand: 'Fiat', model: '500e', fuel: 'ELECTRIC', badge: 'Citadine Éco' }
  ]

  const results = []
  const usedIds = new Set()

  for (const item of desiredTargets) {
    const v = catalogVariants.value.find(c => 
      c.brandName?.toLowerCase().includes(item.brand.toLowerCase()) &&
      c.modelName?.toLowerCase().includes(item.model.toLowerCase()) &&
      (item.fuel ? c.fuelType === item.fuel : (c.fuelType === 'ELECTRIC' || c.fuelType === 'HYBRID')) &&
      !usedIds.has(c.id)
    )

    if (v) {
      usedIds.add(v.id)
      results.push({
        id: v.id,
        name: `${v.brandName} ${v.modelName} ${v.motorisationName || ''}`.trim(),
        brand: v.brandName,
        model: v.modelName,
        fuelType: v.fuelType,
        consumption: v.consumptionWltp || 15.0,
        purchasePrice: v.purchasePrice || 35000,
        monthlyLoa: v.monthlyLoa,
        monthlyLld: v.monthlyLld,
        insuranceCost: v.defaultInsuranceCost || 780,
        maintenanceCost: v.defaultMaintenanceCost || 240,
        imageUrl: v.finitionImageUrl || v.imageUrl || v.modelImageUrl,
        brandLogoUrl: v.brandLogoUrl,
        badge: item.badge || (v.fuelType === 'ELECTRIC' ? '100% Élec' : 'Hybride')
      })
    }
  }

  // Si un véhicule cible est déjà sélectionné mais n'est pas dans la liste par défaut, l'ajouter en première position
  if (props.targetVehicle && props.targetVehicle.name && props.targetVehicle.purchasePrice > 0) {
    const isAlreadyIn = results.some(r => r.name.toLowerCase() === props.targetVehicle.name.toLowerCase())
    if (!isAlreadyIn) {
      results.unshift({
        id: 'selected-target',
        name: props.targetVehicle.name,
        brand: props.targetVehicle.brand || '',
        model: props.targetVehicle.model || '',
        fuelType: props.targetVehicle.fuelType || 'ELECTRIC',
        consumption: props.targetVehicle.consumption || 15.0,
        purchasePrice: props.targetVehicle.purchasePrice || 35000,
        monthlyLoa: props.targetVehicle.monthlyLoa || props.customLeasingMonthlyPrice,
        monthlyLld: props.targetVehicle.monthlyLld,
        insuranceCost: props.targetVehicle.insuranceCost || 780,
        maintenanceCost: props.targetVehicle.maintenanceCost || 240,
        imageUrl: props.targetVehicle.imageUrl || null,
        brandLogoUrl: props.targetVehicle.brandLogoUrl || null,
        badge: 'Sélectionné'
      })
    }
  }

  return results.length >= 4 ? results : popularTargetCars
})

// Profils de recharge pour l'étape 2
const chargingProfiles = [
  {
    id: 'house',
    title: 'Maison individuelle (Prise / Borne)',
    desc: 'Recharge économique la nuit à domicile (~85%)',
    ratio: 0.85,
    icon: Home
  },
  {
    id: 'mixed',
    title: 'Copropriété / Parking partagé',
    desc: 'Recharge mixte (domicile + travail / ville ~60%)',
    ratio: 0.6,
    icon: Building2
  },
  {
    id: 'public',
    title: 'Stationnement en voirie',
    desc: 'Recharge sur bornes publiques et réseau rapide d’autoroute',
    ratio: 0.15,
    icon: ParkingCircle
  }
]

// Recherche dynamique catalogue
const searchCurrentQuery = ref('')
const searchTargetQuery = ref('')
const showCurrentCustom = ref(false)
const showTargetCustom = ref(false)

const selectedChargingProfile = ref('house')
const selectedIncomeTier = ref('standard')
const showCustomIncome = ref(false)
const showTaxHelp = ref(false)

// Options statiques pour l'étape 1
const fuelOptions = [
  { value: 'PETROL', label: 'Essence', icon: '⛽' },
  { value: 'DIESEL', label: 'Diesel', icon: '🛢️' },
  { value: 'HYBRID', label: 'Hybride', icon: '🔋' },
  { value: 'ELECTRIC', label: 'Électrique', icon: '⚡' }
]

const consumptionHints = computed(() => {
  if (props.currentVehicle.fuelType === 'ELECTRIC') {
    return [
      { label: '12 kWh', value: 12 },
      { label: '15 kWh', value: 15 },
      { label: '18 kWh', value: 18 },
      { label: '22 kWh', value: 22 }
    ]
  }
  if (props.currentVehicle.fuelType === 'HYBRID') {
    return [
      { label: '3.5 L', value: 3.5 },
      { label: '4.5 L', value: 4.5 },
      { label: '5.5 L', value: 5.5 }
    ]
  }
  if (props.currentVehicle.fuelType === 'DIESEL') {
    return [
      { label: '4 L', value: 4 },
      { label: '5 L', value: 5 },
      { label: '6 L', value: 6 },
      { label: '7 L', value: 7 }
    ]
  }
  return [
    { label: '5 L', value: 5 },
    { label: '6.5 L', value: 6.5 },
    { label: '8 L', value: 8 },
    { label: '10 L', value: 10 }
  ]
})

const resaleHints = [
  { l: '2 000 €', v: 2000 },
  { l: '5 000 €', v: 5000 },
  { l: '8 000 €', v: 8000 },
  { l: '12 000 €', v: 12000 },
  { l: '18 000 €', v: 18000 }
]

onMounted(async () => {
  try {
    const data = await apiGetCatalogVariants()
    if (data && data.length > 0) {
      catalogVariants.value = data
    }
  } catch (e) {
    console.warn('Impossible de charger les variantes du catalogue pour le wizard', e)
  }

  if (!props.currentVehicle.annualMileage) {
    props.currentVehicle.annualMileage = 15000
    props.targetVehicle.annualMileage = 15000
  }
  if (props.currentVehicle.resaleValue === null || props.currentVehicle.resaleValue === undefined) {
    props.currentVehicle.resaleValue = 8000
  }
  if (props.taxIncome && props.taxIncome <= 15400) {
    selectedIncomeTier.value = 'modest'
  }

  // Si un véhicule cible a été pré-sélectionné depuis le catalogue, l'afficher directement
  if (props.targetVehicle && props.targetVehicle.name && props.targetVehicle.purchasePrice > 0) {
    if (props.targetVehicle.monthlyLoa) {
      emit('update:isLeasing', true)
      emit('update:customLeasingMonthlyPrice', props.targetVehicle.monthlyLoa)
    }
  }
})

const selectCurrentCar = (car) => {
  props.currentVehicle.name = car.name
  props.currentVehicle.brand = car.brand || ''
  props.currentVehicle.model = car.model || ''
  props.currentVehicle.fuelType = car.fuelType
  props.currentVehicle.consumption = car.consumption
  props.currentVehicle.insuranceCost = car.insuranceCost || 580
  props.currentVehicle.maintenanceCost = car.maintenanceCost || 440
  props.currentVehicle.resaleValue = car.resaleValue || 8000
  props.currentVehicle.imageUrl = car.imageUrl || null
  showCurrentCustom.value = false
}

const selectTargetCar = (car) => {
  props.targetVehicle.name = car.name
  props.targetVehicle.brand = car.brand || ''
  props.targetVehicle.model = car.model || ''
  props.targetVehicle.fuelType = car.fuelType
  props.targetVehicle.consumption = car.consumption
  props.targetVehicle.purchasePrice = car.purchasePrice || 35000
  props.targetVehicle.insuranceCost = car.insuranceCost || 780
  props.targetVehicle.maintenanceCost = car.maintenanceCost || 240
  props.targetVehicle.imageUrl = car.imageUrl || null
  props.targetVehicle.annualMileage = props.currentVehicle.annualMileage || 15000

  // Pré-renseigner le tarif LOA si disponible
  if (car.monthlyLoa) {
    emit('update:isLeasing', true)
    emit('update:customLeasingMonthlyPrice', car.monthlyLoa)
  } else if (car.monthlyLld) {
    emit('update:isLeasing', true)
    emit('update:customLeasingMonthlyPrice', car.monthlyLld)
  }

  showTargetCustom.value = false
}

const currentSearchResults = computed(() => {
  if (!searchCurrentQuery.value || searchCurrentQuery.value.trim().length < 2) return []
  const q = searchCurrentQuery.value.toLowerCase()
  const source = (catalogVariants.value && catalogVariants.value.length > 0)
    ? catalogVariants.value.map(v => ({
        id: v.id,
        name: `${v.brandName} ${v.modelName} ${v.motorisationName || ''} (${v.finitionName || ''})`.trim(),
        brand: v.brandName,
        model: v.modelName,
        fuelType: v.fuelType,
        consumption: v.consumptionWltp || 5.5,
        insuranceCost: v.defaultInsuranceCost || 580,
        maintenanceCost: v.defaultMaintenanceCost || 440,
        resaleValue: v.estimatedResaleValue || 8000,
        imageUrl: v.finitionImageUrl || v.imageUrl || v.modelImageUrl
      }))
    : props.catalogVehicles
  return source.filter(v => v.name && v.name.toLowerCase().includes(q)).slice(0, 10)
})

const targetSearchResults = computed(() => {
  if (!searchTargetQuery.value || searchTargetQuery.value.trim().length < 2) return []
  const q = searchTargetQuery.value.toLowerCase()
  const source = (catalogVariants.value && catalogVariants.value.length > 0)
    ? catalogVariants.value.map(v => ({
        id: v.id,
        name: `${v.brandName} ${v.modelName} ${v.motorisationName || ''} (${v.finitionName || ''})`.trim(),
        brand: v.brandName,
        model: v.modelName,
        fuelType: v.fuelType,
        consumption: v.consumptionWltp || 15.0,
        purchasePrice: v.purchasePrice || 35000,
        monthlyLoa: v.monthlyLoa,
        monthlyLld: v.monthlyLld,
        insuranceCost: v.defaultInsuranceCost || 780,
        maintenanceCost: v.defaultMaintenanceCost || 240,
        imageUrl: v.finitionImageUrl || v.imageUrl || v.modelImageUrl
      }))
    : props.catalogVehicles
  return source.filter(v => v.name && v.name.toLowerCase().includes(q)).slice(0, 10)
})

const applyChargingProfile = (prof) => {
  selectedChargingProfile.value = prof.id
  emit('update:homeChargingRatio', prof.ratio)
}

const applyIncomeTier = (tier) => {
  selectedIncomeTier.value = tier
  if (tier === 'modest') {
    emit('update:taxIncome', 14000)
  } else {
    emit('update:taxIncome', 25000)
  }
}

const onCustomIncomeInput = (val) => {
  const num = Number(val)
  emit('update:taxIncome', num)
  if (num > 0 && num <= 15400) {
    selectedIncomeTier.value = 'modest'
  } else {
    selectedIncomeTier.value = 'standard'
  }
}

const setMileagePreset = (km) => {
  props.currentVehicle.annualMileage = km
  props.targetVehicle.annualMileage = km
}

const canGoToStep2 = computed(() => {
  return !!(props.currentVehicle.fuelType &&
    props.currentVehicle.consumption > 0 &&
    props.currentVehicle.annualMileage > 0)
})

const canSubmit = computed(() => {
  return canGoToStep2.value &&
    !!(props.targetVehicle.name &&
    props.targetVehicle.fuelType &&
    props.targetVehicle.consumption > 0 &&
    props.targetVehicle.purchasePrice > 0)
})

const nextStep = () => {
  if (currentStep.value === 1 && canGoToStep2.value) {
    currentStep.value = 2
  } else if (currentStep.value === 2) {
    currentStep.value = 3
  }
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

const formatFuelBadge = (fuelType) => {
  switch (fuelType) {
    case 'ELECTRIC': return 'Électrique'
    case 'HYBRID': return 'Hybride'
    case 'DIESEL': return 'Diesel'
    default: return 'Essence'
  }
}
</script>

<template>
  <div class="express-wizard card-glass p-5">
    
    <!-- Stepper Supérieur Apple Style -->
    <div class="wizard-header mb-5">
      <div class="flex-between items-center mb-4">
        <div class="flex items-center gap-2.5">
          <div class="wizard-icon-badge">
            <Zap size="18" class="text-teal" />
          </div>
          <div>
            <h3 class="wizard-title text-main m-0">Diagnostic Mobilité Express</h3>
            <p class="text-xs text-muted m-0">3 étapes simples pour évaluer votre rentabilité</p>
          </div>
        </div>

        <button type="button" class="btn-text-expert" @click="emit('switch-to-expert')">
          <Layers size="14" />
          <span>Passer en Mode Expert</span>
        </button>
      </div>

      <!-- Stepper Track -->
      <div class="stepper-track">
        <div
          class="stepper-step"
          :class="{ active: currentStep === 1, done: currentStep > 1 }"
          @click="currentStep = 1"
        >
          <div class="step-num">{{ currentStep > 1 ? '✓' : '1' }}</div>
          <span class="step-text">Véhicule Actuel</span>
        </div>
        <div class="stepper-line" :class="{ filled: currentStep > 1 }"></div>
        <div
          class="stepper-step"
          :class="{ active: currentStep === 2, done: currentStep > 2, disabled: !canGoToStep2 }"
          @click="canGoToStep2 && (currentStep = 2)"
        >
          <div class="step-num">{{ currentStep > 2 ? '✓' : '2' }}</div>
          <span class="step-text">Logement & Aides</span>
        </div>
        <div class="stepper-line" :class="{ filled: currentStep > 2 }"></div>
        <div
          class="stepper-step"
          :class="{ active: currentStep === 3, disabled: !canGoToStep2 }"
          @click="canGoToStep2 && (currentStep = 3)"
        >
          <div class="step-num">3</div>
          <span class="step-text">Nouveau Véhicule</span>
        </div>
      </div>
    </div>

    <!-- ÉTAPE 1 : VÉHICULE ACTUEL — Saisie directe -->
    <div v-if="currentStep === 1" class="wizard-step-body animation-fadeIn">
      <div class="step-intro mb-4">
        <h4 class="step-title font-heading text-main">
          1. Votre véhicule actuel
        </h4>
        <p class="step-subtitle text-muted text-xs">
          Renseignez les caractéristiques de votre véhicule pour calculer son coût annuel réel.
        </p>
      </div>

      <!-- Bloc principal : carburant + consommation -->
      <div class="current-form-card mb-3">
        <div class="current-form-row">
          <!-- Type de carburant -->
          <div class="form-group mb-0 flex-1">
            <label class="form-label text-xs font-bold">⛽ Type de carburant</label>
            <div class="fuel-pills-row">
              <button
                v-for="fuel in fuelOptions"
                :key="fuel.value"
                type="button"
                class="fuel-pill"
                :class="{ active: currentVehicle.fuelType === fuel.value }"
                @click="currentVehicle.fuelType = fuel.value"
              >
                <span class="fuel-pill-icon">{{ fuel.icon }}</span>
                <span class="fuel-pill-label">{{ fuel.label }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Consommation -->
        <div class="form-group mt-3 mb-0">
          <label class="form-label text-xs font-bold">
            📊 Consommation
            <span class="text-dimmed font-normal">
              ({{ currentVehicle.fuelType === 'ELECTRIC' ? 'kWh/100km' : 'L/100km' }})
            </span>
          </label>
          <div class="consumption-input-row">
            <input
              v-model.number="currentVehicle.consumption"
              type="number"
              step="0.1"
              min="0"
              class="form-control text-xs consumption-input"
              :placeholder="currentVehicle.fuelType === 'ELECTRIC' ? 'ex: 15.0' : 'ex: 6.0'"
            />
            <div class="consumption-hints">
              <button
                v-for="hint in consumptionHints"
                :key="hint.value"
                type="button"
                class="hint-chip"
                :class="{ active: currentVehicle.consumption === hint.value }"
                @click="currentVehicle.consumption = hint.value"
              >{{ hint.label }}</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Kilométrage annuel -->
      <div class="current-form-card mb-3">
        <label class="form-label text-xs font-bold mb-2 block">🗓️ Kilométrage annuel</label>
        <div class="mileage-presets flex gap-2 mb-3">
          <button type="button" class="btn-preset flex-1" :class="{ active: currentVehicle.annualMileage === 8000 }" @click="setMileagePreset(8000)">
            <span class="preset-km">8 000 km</span><span class="preset-desc">Urbain</span>
          </button>
          <button type="button" class="btn-preset flex-1" :class="{ active: currentVehicle.annualMileage === 15000 }" @click="setMileagePreset(15000)">
            <span class="preset-km">15 000 km</span><span class="preset-desc">Moyenne FR</span>
          </button>
          <button type="button" class="btn-preset flex-1" :class="{ active: currentVehicle.annualMileage === 25000 }" @click="setMileagePreset(25000)">
            <span class="preset-km">25 000 km</span><span class="preset-desc">Grand rouleur</span>
          </button>
        </div>
        <div class="flex-between items-center mb-1">
          <span class="text-xxs text-dimmed">3 000 km</span>
          <span class="badge badge-teal font-bold">{{ Number(currentVehicle.annualMileage || 0).toLocaleString('fr-FR') }} km / an</span>
          <span class="text-xxs text-dimmed">40 000 km</span>
        </div>
        <input
          v-model.number="currentVehicle.annualMileage"
          type="range" min="3000" max="40000" step="500"
          class="w-100 accent-teal cursor-pointer"
          @input="targetVehicle.annualMileage = currentVehicle.annualMileage"
        />
      </div>

      <!-- Valeur de reprise -->
      <div class="current-form-card mb-4">
        <label class="form-label text-xs font-bold mb-1 block">💰 Valeur de reprise estimée de votre véhicule</label>
        <p class="text-xxs text-dimmed mb-2">Sera déduite du coût net d'acquisition du nouveau véhicule.</p>
        <div class="resale-input-row">
          <input
            v-model.number="currentVehicle.resaleValue"
            type="number"
            class="form-control text-xs"
            placeholder="ex: 8 000"
          />
          <span class="resale-unit text-xs text-dimmed">€</span>
        </div>
        <div class="resale-hints flex gap-2 mt-2">
          <button v-for="h in resaleHints" :key="h.v" type="button" class="hint-chip" :class="{ active: currentVehicle.resaleValue === h.v }" @click="currentVehicle.resaleValue = h.v">{{ h.l }}</button>
        </div>
      </div>

      <!-- Bouton Suivant -->
      <div class="wizard-actions flex justify-end">
        <button
          type="button"
          class="btn btn-primary px-5 py-2.5 font-bold flex items-center gap-2"
          :disabled="!canGoToStep2"
          @click="nextStep"
        >
          <span>Continuer vers le logement &amp; les aides</span>
          <ArrowRight size="16" />
        </button>
      </div>
    </div>

    <!-- ÉTAPE 2 : LOGEMENT & AIDES D'ÉTAT -->

    <div v-else-if="currentStep === 2" class="wizard-step-body animation-fadeIn">
      <div class="step-intro mb-3.5">
        <h4 class="step-title font-heading text-main">
          2. Votre logement et vos aides de l'État
        </h4>
        <p class="step-subtitle text-muted text-xs">
          Ces critères définissent le coût réel de recharge et le montant des subventions publiques applicables.
        </p>
      </div>

      <!-- Cartes de profil de recharge -->
      <div class="form-group mb-4">
        <label class="form-label text-xs font-bold uppercase text-dimmed mb-2.5 block">
          🔌 Où stationnez-vous votre véhicule la nuit ?
        </label>
        <div class="charging-profiles-grid">
          <div
            v-for="prof in chargingProfiles"
            :key="prof.id"
            class="charging-card"
            :class="{ selected: selectedChargingProfile === prof.id }"
            @click="applyChargingProfile(prof)"
          >
            <div class="charging-icon-wrapper">
              <component :is="prof.icon" size="22" class="text-teal" />
            </div>
            <div class="charging-card-content">
              <h5 class="charging-title text-main font-bold text-xs">{{ prof.title }}</h5>
              <p class="charging-desc text-xxs text-muted m-0">{{ prof.desc }}</p>
            </div>
            <div class="charging-check flex-center" v-if="selectedChargingProfile === prof.id">
              <Check size="14" />
            </div>
          </div>
        </div>
      </div>

      <!-- Tranches de revenus et subventions -->
      <div class="subsidies-section p-4 rounded-xl border-glass mb-4 bg-card-subtle">
        <label class="form-label text-xs font-bold uppercase text-dimmed mb-2.5 block">
          🏛️ Éligibilité au Bonus Écologique de l'État
        </label>

        <div class="grid-2-fields gap-3 mb-3">
          <div
            class="income-tier-card p-3 rounded-xl border-glass cursor-pointer"
            :class="{ selected: selectedIncomeTier === 'standard' && !showCustomIncome }"
            @click="applyIncomeTier('standard'); showCustomIncome = false"
          >
            <div class="flex-between items-center mb-1">
              <span class="font-bold text-xs text-main">Revenu fiscal standard</span>
              <span class="badge badge-teal badge-small">Bonus 4 000 €</span>
            </div>
            <p class="text-xxs text-muted m-0">RFR supérieur à 15 400 € par part fiscale</p>
          </div>

          <div
            class="income-tier-card p-3 rounded-xl border-glass cursor-pointer"
            :class="{ selected: selectedIncomeTier === 'modest' && !showCustomIncome }"
            @click="applyIncomeTier('modest'); showCustomIncome = false"
          >
            <div class="flex-between items-center mb-1">
              <span class="font-bold text-xs text-main">Revenu fiscal modeste</span>
              <span class="badge badge-cyan badge-small">Bonus Majoré 7 000 €</span>
            </div>
            <p class="text-xxs text-muted m-0">RFR ≤ 15 400 € par part fiscale</p>
          </div>
        </div>

        <!-- Options complémentaires : Saisie exacte et Guide des parts -->
        <div class="flex-between items-center flex-wrap gap-2 mb-3 pt-2 border-t border-glass text-xs">
          <button
            type="button"
            class="btn-text-link flex items-center gap-1.5 text-xxs font-bold text-teal cursor-pointer"
            @click="showCustomIncome = !showCustomIncome"
          >
            <Calculator size="13" />
            <span>{{ showCustomIncome ? 'Masquer la saisie exacte' : 'Saisir mon RFR exact (€)' }}</span>
            <component :is="showCustomIncome ? ChevronUp : ChevronDown" size="12" />
          </button>

          <button
            type="button"
            class="btn-text-link flex items-center gap-1.5 text-xxs font-bold text-cyan cursor-pointer"
            @click="showTaxHelp = !showTaxHelp"
          >
            <HelpCircle size="13" />
            <span>{{ showTaxHelp ? 'Masquer le guide des parts' : 'Comment calculer mes parts fiscales ?' }}</span>
            <component :is="showTaxHelp ? ChevronUp : ChevronDown" size="12" />
          </button>
        </div>

        <!-- Formulaire de saisie du montant exact de RFR -->
        <div v-if="showCustomIncome" class="custom-income-input p-3 rounded-xl bg-card border-glass mb-3 animation-fadeIn">
          <label class="form-label text-xxs text-dimmed uppercase">Votre Revenu Fiscal de Référence par part (RFR en €)</label>
          <div class="flex gap-2 items-center mt-1">
            <input
              :value="taxIncome"
              type="number"
              class="form-control text-xs"
              placeholder="ex: 14200"
              @input="onCustomIncomeInput($event.target.value)"
            />
            <span class="badge badge-small shrink-0" :class="taxIncome <= 15400 ? 'badge-cyan' : 'badge-teal'">
              {{ taxIncome <= 15400 ? 'Bonus 7 000 €' : 'Bonus 4 000 €' }}
            </span>
          </div>
          <p class="text-xxs text-muted mt-1.5 m-0">
            Ligne "Revenu fiscal de référence" sur votre avis d'impôt sur les revenus.
          </p>
        </div>

        <!-- Guide interactif du calcul des parts fiscales -->
        <div v-if="showTaxHelp" class="tax-help-guide p-3 rounded-xl bg-card border-glass mb-3 animation-fadeIn">
          <h6 class="text-xs font-bold text-cyan mb-2">
            Repères selon la composition de votre foyer (Plafond 15 400 € / part) :
          </h6>
          <div class="tax-examples-grid mb-2">
            <div class="tax-example-pill">
              <span class="example-role">👤 Célibataire (1 part)</span>
              <span class="example-val">Revenu total &le; <strong>15 400 €</strong></span>
            </div>
            <div class="tax-example-pill">
              <span class="example-role">👫 Couple (2 parts)</span>
              <span class="example-val">Revenu total &le; <strong>30 800 €</strong></span>
            </div>
            <div class="tax-example-pill">
              <span class="example-role">👨‍👩‍👧 Couple + 1 enfant (2.5 parts)</span>
              <span class="example-val">Revenu total &le; <strong>38 500 €</strong></span>
            </div>
            <div class="tax-example-pill">
              <span class="example-role">👨‍👩‍👧‍👦 Couple + 2 enfants (3 parts)</span>
              <span class="example-val">Revenu total &le; <strong>46 200 €</strong></span>
            </div>
          </div>
        </div>

        <!-- Case prime à la conversion -->
        <div class="scrap-box p-3 rounded-xl border-glass flex items-center gap-3 bg-card">
          <input
            :checked="scrapVehicle"
            type="checkbox"
            id="scrapCheckExpress"
            class="cursor-pointer"
            @change="emit('update:scrapVehicle', $event.target.checked)"
          />
          <label for="scrapCheckExpress" class="cursor-pointer text-xs text-main m-0">
            <strong>Mise à la casse d'un vieux véhicule thermique</strong> (Prime à la conversion de +1 500 € à +3 000 €)
          </label>
        </div>
      </div>

      <!-- Boutons Navigation Étape 2 -->
      <div class="wizard-actions flex-between">
        <button type="button" class="btn btn-secondary px-4 py-2 text-xs flex items-center gap-2" @click="prevStep">
          <ArrowLeft size="14" />
          <span>Retour</span>
        </button>
        <button type="button" class="btn btn-primary px-5 py-2.5 font-bold flex items-center gap-2" @click="nextStep">
          <span>Choisir mon nouveau véhicule</span>
          <ArrowRight size="16" />
        </button>
      </div>
    </div>

    <!-- ÉTAPE 3 : NOUVEAU VÉHICULE & FINANCEMENT -->
    <div v-if="currentStep === 3" class="wizard-step-body animation-fadeIn">
      <div class="step-intro mb-3.5">
        <h4 class="step-title font-heading text-main">
          3. Quel nouveau véhicule souhaitez-vous comparer ?
        </h4>
        <p class="step-subtitle text-muted text-xs">
          Choisissez un modèle populaire ou recherchez un modèle précis dans le catalogue.
        </p>
      </div>

      <!-- Grille des voitures cibles populaires issues du catalogue -->
      <div class="cars-grid mb-4">
        <div
          v-for="car in dynamicTargetCars"
          :key="car.id || car.name"
          class="wizard-car-card"
          :class="{ selected: targetVehicle.name === car.name }"
          @click="selectTargetCar(car)"
        >
          <!-- Vignette miniature du modèle (gauche) -->
          <div class="car-thumbnail-box">
            <img
              v-if="car.imageUrl"
              :src="car.imageUrl"
              :alt="car.name"
              class="car-thumbnail-img"
              @error="(e) => e.target.style.display = 'none'"
            />
            <span v-else class="car-thumbnail-fallback">⚡</span>
          </div>

          <!-- Contenu texte (droite) -->
          <div class="car-content-box">
            <div class="car-header-row flex-between">
              <div class="flex items-center gap-1.5">
                <img v-if="car.brandLogoUrl" :src="car.brandLogoUrl" class="brand-mini-logo" />
                <span class="badge badge-small" :class="car.fuelType === 'ELECTRIC' ? 'badge-teal' : 'badge-cyan'">
                  {{ car.badge }}
                </span>
              </div>
              <div v-if="targetVehicle.name === car.name" class="check-circle flex-center">
                <Check size="11" />
              </div>
            </div>
            <div class="car-card-name text-main">{{ car.name }}</div>
            <div class="car-card-meta text-xxs text-dimmed">
              <strong class="text-teal">{{ formatCurrency(car.purchasePrice) }}</strong> &middot; {{ car.consumption }} {{ car.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km
            </div>
          </div>
        </div>
      </div>

      <!-- Option de recherche personnalisée cible -->
      <div class="custom-search-container p-3 rounded-xl border-glass mb-4 bg-card-subtle">
        <button
          type="button"
          class="btn-toggle-custom flex-between w-100"
          @click="showTargetCustom = !showTargetCustom"
        >
          <span class="flex items-center gap-2 text-xs font-semibold text-main">
            <Search size="14" class="text-teal" />
            <span>Rechercher un autre véhicule dans le catalogue complet</span>
          </span>
          <span class="text-xs text-teal font-semibold">{{ showTargetCustom ? 'Masquer ▲' : 'Rechercher ▼' }}</span>
        </button>

        <div v-if="showTargetCustom" class="custom-search-inputs mt-3 pt-3 border-t border-glass">
          <div class="form-group relative mb-3">
            <label class="form-label text-xxs">Nom du modèle</label>
            <input
              v-model="searchTargetQuery"
              type="text"
              class="form-control text-xs"
              placeholder="ex: e-2008, Ioniq 5, Fiat 500e..."
            />
            <div v-if="targetSearchResults.length > 0" class="search-dropdown card-glass">
              <div
                v-for="v in targetSearchResults"
                :key="v.id"
                class="dropdown-item"
                @click="selectTargetCar(v)"
              >
                <span class="font-bold text-xs text-main">{{ v.name }}</span>
                <span class="text-xxs text-muted">
                  {{ formatCurrency(v.purchasePrice) }} &middot; {{ v.consumption }} {{ v.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km
                </span>
              </div>
            </div>
          </div>

          <div class="grid-3-fields">
            <div class="form-group mb-0">
              <label class="form-label text-xxs">Nom complet</label>
              <input v-model="targetVehicle.name" type="text" class="form-control text-xs" />
            </div>
            <div class="form-group mb-0">
              <label class="form-label text-xxs">Prix d'achat (€)</label>
              <input v-model.number="targetVehicle.purchasePrice" type="number" class="form-control text-xs" />
            </div>
            <div class="form-group mb-0">
              <label class="form-label text-xxs">Consommation</label>
              <input v-model.number="targetVehicle.consumption" type="number" step="0.1" class="form-control text-xs" />
            </div>
          </div>
        </div>
      </div>

      <!-- Mode de financement -->
      <div class="financing-box p-4 rounded-xl border-glass mb-4 bg-card-subtle">
        <label class="form-label text-xs font-bold uppercase text-dimmed mb-2.5 block">
          💳 Mode d'acquisition du véhicule cible
        </label>
        
        <div class="segmented-control w-100 mb-3">
          <button
            type="button"
            class="segmented-item flex-1"
            :class="{ active: !isLeasing }"
            @click="emit('update:isLeasing', false)"
          >
            Achat Comptant / Crédit classique
          </button>
          <button
            type="button"
            class="segmented-item flex-1"
            :class="{ active: isLeasing }"
            @click="emit('update:isLeasing', true)"
          >
            Leasing (LOA / LLD)
          </button>
        </div>

        <div v-if="isLeasing" class="form-group mb-0">
          <label class="form-label text-xs">Loyer mensuel cible souhaité (€/mois)</label>
          <input
            :value="customLeasingMonthlyPrice"
            type="number"
            class="form-control text-xs"
            placeholder="ex: 290 (laisser vide pour calcul automatique selon le prix)"
            @input="emit('update:customLeasingMonthlyPrice', $event.target.value ? Number($event.target.value) : null)"
          />
        </div>
      </div>

      <!-- Boutons Finaux -->
      <div class="wizard-actions flex-between items-center">
        <button type="button" class="btn btn-secondary px-4 py-2 text-xs flex items-center gap-2" @click="prevStep">
          <ArrowLeft size="14" />
          <span>Retour</span>
        </button>
        <button
          type="button"
          class="btn btn-primary px-6 py-3 font-bold text-sm flex items-center gap-2"
          :disabled="!canSubmit || loading"
          @click="emit('submit')"
        >
          <span v-if="loading" class="spinner mr-1"></span>
          <Sparkles v-else size="17" />
          <span>Calculer mon Bilan Mobilité</span>
          <ArrowRight size="16" />
        </button>
      </div>
    </div>

  </div>
</template>

<style scoped>
.wizard-title {
  font-size: 1.15rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.wizard-icon-badge {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--accent-teal-soft);
  border: 1px solid rgba(16, 124, 65, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-text-expert {
  background: transparent;
  border: 1px solid var(--border-glass);
  color: var(--text-muted);
  font-size: 0.76rem;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s ease;
}
.btn-text-expert:hover {
  border-color: var(--border-hover);
  color: var(--text-main);
  background: var(--bg-card-subtle);
}

/* Stepper Track */
.stepper-track {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  margin-top: 1rem;
  padding: 0 0.5rem;
}

.stepper-step {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  z-index: 2;
}
.stepper-step.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.step-num {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--bg-card-subtle);
  border: 1.5px solid var(--border-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--text-dimmed);
  transition: all 0.15s ease;
}
.stepper-step.active .step-num {
  background: var(--accent-teal);
  border-color: var(--accent-teal);
  color: #fff;
}
.stepper-step.done .step-num {
  background: var(--accent-teal-soft);
  border-color: var(--accent-teal);
  color: var(--accent-teal);
}

.step-text {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--text-dimmed);
}
.stepper-step.active .step-text {
  color: var(--text-main);
}

.stepper-line {
  flex: 1;
  height: 2px;
  background: var(--border-glass);
  margin: 0 10px;
}
.stepper-line.filled {
  background: var(--accent-teal);
}

/* Cars Grid with Thumbnails */
.cars-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
@media (max-width: 640px) {
  .cars-grid {
    grid-template-columns: 1fr;
  }
}

.wizard-car-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 8px 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}
.wizard-car-card:hover {
  border-color: var(--border-hover);
  transform: translateY(-1px);
}
.wizard-car-card.selected {
  border-color: var(--accent-teal);
  background: var(--accent-teal-soft);
  box-shadow: 0 0 0 1px var(--accent-teal);
}

.car-thumbnail-box {
  width: 64px;
  height: 44px;
  border-radius: 6px;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}
.car-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.car-thumbnail-fallback {
  font-size: 1.3rem;
}

.car-content-box {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.car-header-row {
  margin-bottom: 1px;
}
.brand-mini-logo {
  width: 14px;
  height: 14px;
  object-fit: contain;
}
.car-card-name {
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.car-card-meta {
  font-size: 0.68rem;
  color: var(--text-dimmed);
}

.check-circle {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--accent-teal);
  color: #fff;
}

/* Charging Profiles */
.charging-profiles-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
@media (max-width: 768px) {
  .charging-profiles-grid { grid-template-columns: 1fr; }
}

.charging-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.charging-card:hover {
  border-color: var(--border-hover);
}
.charging-card.selected {
  border-color: var(--accent-teal);
  background: var(--accent-teal-soft);
}

.charging-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--accent-teal-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.charging-check {
  margin-left: auto;
  color: var(--accent-teal);
}

/* Income cards */
.income-tier-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  transition: all 0.15s ease;
}
.income-tier-card:hover {
  border-color: var(--border-hover);
}
.income-tier-card.selected {
  border-color: var(--accent-teal);
  background: var(--accent-teal-soft);
}

.tax-examples-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
@media (max-width: 600px) {
  .tax-examples-grid { grid-template-columns: 1fr; }
}
.tax-example-pill {
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  background: var(--bg-card-subtle);
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid var(--border-glass);
}

/* Mileage Presets */
.btn-preset {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-preset:hover {
  color: var(--text-main);
  border-color: var(--border-hover);
}
.btn-preset.active {
  background: var(--accent-teal-soft);
  border-color: var(--accent-teal);
  color: var(--accent-teal);
}

.btn-toggle-custom {
  background: transparent;
  border: none;
  cursor: pointer;
}

.btn-text-link {
  background: transparent;
  border: none;
  text-decoration: underline;
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 50;
  max-height: 180px;
  overflow-y: auto;
  border-radius: 12px;
  margin-top: 4px;
}
.dropdown-item {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}
.dropdown-item:hover {
  background: var(--accent-teal-soft);
}

@media (max-width: 640px) {
  .express-wizard {
    padding: 14px 10px !important;
  }
  .wizard-header .flex-between {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  .wizard-title {
    font-size: 1.05rem;
  }
  .btn-text-expert {
    width: 100%;
    justify-content: center;
    padding: 6px 10px;
    background: var(--bg-card-subtle);
    border-radius: 8px;
    border: 1px solid var(--border-glass);
  }
  .step-text {
    font-size: 0.7rem;
  }
  .step-num {
    width: 22px;
    height: 22px;
    font-size: 0.68rem;
  }
  .stepper-track {
    padding: 0;
  }
}

/* ── Step 1 Direct Form ─────────────────────────────────────── */
.current-form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 14px 16px;
}

/* Fuel pills */
.fuel-pills-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.fuel-pill {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 8px 12px;
  border-radius: 10px;
  border: 1.5px solid var(--border-glass);
  background: var(--bg-card-subtle);
  cursor: pointer;
  transition: all 0.15s ease;
  flex: 1;
  min-width: 64px;
}
.fuel-pill:hover {
  border-color: var(--border-hover);
}
.fuel-pill.active {
  border-color: var(--accent-teal);
  background: var(--accent-teal-soft);
  box-shadow: 0 0 0 1px var(--accent-teal);
}
.fuel-pill-icon {
  font-size: 1.25rem;
  line-height: 1;
}
.fuel-pill-label {
  font-size: 0.68rem;
  font-weight: 700;
  color: var(--text-muted);
  white-space: nowrap;
}
.fuel-pill.active .fuel-pill-label {
  color: var(--accent-teal);
}

/* Consumption row */
.consumption-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.consumption-input {
  width: 100px;
  flex-shrink: 0;
}
.consumption-hints {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* Generic hint chip */
.hint-chip {
  padding: 4px 10px;
  border-radius: 9999px;
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.12s ease;
}
.hint-chip:hover {
  border-color: var(--border-hover);
  color: var(--text-main);
}
.hint-chip.active {
  border-color: var(--accent-teal);
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
}

/* Mileage preset km label */
.preset-km {
  display: block;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-main);
}
.btn-preset .preset-desc {
  display: block;
  font-size: 0.62rem;
  color: var(--text-dimmed);
  font-weight: 500;
}
.btn-preset.flex-1 {
  text-align: center;
  padding: 6px 8px;
}

/* Resale row */
.resale-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.resale-unit {
  font-weight: 700;
}
.resale-hints {
  flex-wrap: wrap;
}
</style>
