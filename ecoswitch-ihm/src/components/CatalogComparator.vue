<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import {
  Zap,
  HelpCircle,
  ArrowRight,
  ArrowLeft,
  TrendingUp,
  Sparkles,
  AlertCircle,
  RefreshCw,
  Wrench,
  Check,
  Search,
  CheckCircle2,
  Car,
  Fuel,
  Gauge,
  ShieldCheck,
  DollarSign,
  Award,
  Layers,
  ChevronRight,
  Sliders,
  BarChart3,
  ExternalLink,
  Flame,
  Leaf,
  Filter
} from '@lucide/vue'
import vehicleEcoSavingsImg from '../assets/vehicle_eco_savings.png'
import { apiGetCatalogVariants, apiCompareCustomProfitability } from '../utils/api.js'

const props = defineProps({
  currentUser: Object,
  userProfiles: {
    type: Array,
    default: () => []
  },
  activeUserProfile: Object
})

const emit = defineEmits(['open-simulator'])

// ── State ──────────────────────────────────────────────────────────────────

const vehicles = ref([])
const loading = ref(false)
const calculating = ref(false)
const error = ref(null)

// Filtres du showroom
const searchQuery = ref('')
const selectedEnergyFilter = ref('ALL') // 'ALL', 'PROFITABLE', 'ELECTRIC', 'HYBRID', 'THERMAL'
const onlyProfitableFilter = ref(false)
const selectedBrandFilter = ref('ALL')

// Configuration du comparateur
const referenceMode = ref('CUSTOM') // 'GARAGE' | 'CUSTOM'
const selectedProfileId = ref(null)
const selectedTargetIds = ref([])
const maxYears = ref(10)
const immediateRepairCost = ref(0)
const activeTabResults = ref('cards') // 'cards' | 'matrix'
const activeMobileView = ref('config') // 'config' | 'showroom' | 'results'

const manualVehicle = ref({
  name: 'Mon véhicule actuel',
  brand: 'Renault',
  model: 'Clio IV',
  fuelType: 'PETROL',
  consumption: 6.8,
  annualMileage: 15000,
  purchasePrice: 0,
  resaleValue: 6000,
  insuranceCost: 650,
  maintenanceCost: 450
})

const fuelPrices = ref({
  PETROL: 1.88,
  DIESEL: 1.74,
  ELECTRIC: 0.25,
  HYBRID: 1.88
})

const result = ref(null)

// ── Chargement des données ─────────────────────────────────────────────────

const fetchVehicles = async () => {
  loading.value = true
  error.value = null
  try {
    const variants = await apiGetCatalogVariants()
    if (variants && variants.length > 0) {
      vehicles.value = variants.map(v => ({
        id: v.id,
        name: `${v.brandName} ${v.modelName}`,
        finitionName: v.finitionName,
        motorisationName: v.motorisationName,
        fullName: `${v.brandName} ${v.modelName} ${v.motorisationName} (${v.finitionName})`,
        brand: v.brandName,
        model: v.modelName,
        version: `${v.motorisationName} - ${v.finitionName}`,
        fuelType: v.fuelType,
        consumption: v.consumptionWltp,
        powerHp: v.powerHp,
        batteryCapacityKwh: v.batteryCapacityKwh,
        purchasePrice: v.purchasePrice,
        monthlyLoa: v.monthlyLoa,
        monthlyLld: v.monthlyLld,
        insuranceCost: v.defaultInsuranceCost || 650,
        maintenanceCost: v.defaultMaintenanceCost || 250,
        resaleValue: v.estimatedResaleValue || 0,
        imageUrl: v.finitionImageUrl || v.modelImageUrl,
        brandLogoUrl: v.brandLogoUrl,
        category: v.category || 'Véhicule'
      }))
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

// ── Computed Properties & Rentabilité Dynamique ─────────────────────────────

const hasProfiles = computed(() => props.userProfiles && props.userProfiles.length > 0)

const activeReferenceVehicle = computed(() => {
  if (referenceMode.value === 'GARAGE' && selectedProfileId.value) {
    const p = props.userProfiles.find(x => x.id === selectedProfileId.value)
    if (p) {
      return {
        name: p.name,
        fuelType: p.fuelType,
        consumption: p.consumption || 6.5,
        annualMileage: p.annualMileage || 15000,
        resaleValue: p.resaleValue || 5000,
        insuranceCost: p.insuranceCost || 600,
        maintenanceCost: p.maintenanceCost || 400
      }
    }
  }
  return manualVehicle.value
})

const currentVehicleAnnualFuelCost = computed(() => {
  const v = activeReferenceVehicle.value
  const p = fuelPrices.value[v.fuelType] || 1.80
  const km = v.annualMileage || 15000
  const c = v.consumption || 6.5
  return (c / 100) * km * p
})

const currentVehicleTotalAnnualCost = computed(() => {
  const v = activeReferenceVehicle.value
  const fuel = currentVehicleAnnualFuelCost.value
  const ins = v.insuranceCost || 600
  const maint = v.maintenanceCost || 400
  return fuel + ins + maint
})

// Calculateur de rentabilité pour un véhicule cible donné
const getVehicleMetrics = (v) => {
  const ref = activeReferenceVehicle.value
  const km = ref.annualMileage || 15000
  const fuelP = fuelPrices.value[v.fuelType] || 1.80
  const annualFuel = (v.consumption / 100) * km * fuelP
  const annualTotal = annualFuel + (v.insuranceCost || 650) + (v.maintenanceCost || 250)
  const annualSavings = currentVehicleTotalAnnualCost.value - annualTotal
  const switchInvestment = Math.max(0, (v.purchasePrice || 0) - (ref.resaleValue || 0)) + (immediateRepairCost.value || 0)
  
  let breakEvenYears = null
  let isProfitable = false
  if (annualSavings > 0) {
    breakEvenYears = switchInvestment <= 0 ? 0 : switchInvestment / annualSavings
    isProfitable = breakEvenYears <= maxYears.value
  }
  const netGainAtHorizon = (annualSavings * maxYears.value) - switchInvestment

  return {
    annualFuel,
    annualTotal,
    annualSavings,
    switchInvestment,
    breakEvenYears,
    isProfitable,
    netGainAtHorizon
  }
}

// Liste de tous les véhicules rentables selon l'horizon temporel choisi
const profitableVehicles = computed(() => {
  return vehicles.value.filter(v => {
    const metrics = getVehicleMetrics(v)
    return metrics.isProfitable
  })
})

const profitableCount = computed(() => profitableVehicles.value.length)

const availableBrands = computed(() => {
  const brandMap = new Map()
  for (const v of vehicles.value) {
    if (v.brand && !brandMap.has(v.brand)) {
      brandMap.set(v.brand, {
        name: v.brand,
        logoUrl: v.brandLogoUrl,
        count: 1
      })
    } else if (v.brand) {
      brandMap.get(v.brand).count++
    }
  }
  return Array.from(brandMap.values()).sort((a, b) => a.name.localeCompare(b.name))
})

const filteredVehicles = computed(() => {
  return vehicles.value.filter(v => {
    const metrics = getVehicleMetrics(v)

    // Filtre Rentabilité Exclusive
    if (onlyProfitableFilter.value && !metrics.isProfitable) {
      return false
    }

    // Filtre Énergie
    if (selectedEnergyFilter.value === 'PROFITABLE' && !metrics.isProfitable) {
      return false
    }
    if (selectedEnergyFilter.value !== 'ALL' && selectedEnergyFilter.value !== 'PROFITABLE') {
      if (selectedEnergyFilter.value === 'ELECTRIC' && v.fuelType !== 'ELECTRIC') return false
      if (selectedEnergyFilter.value === 'HYBRID' && v.fuelType !== 'HYBRID') return false
      if (selectedEnergyFilter.value === 'THERMAL' && v.fuelType !== 'PETROL' && v.fuelType !== 'DIESEL') return false
    }

    // Filtre Marque
    if (selectedBrandFilter.value !== 'ALL' && v.brand !== selectedBrandFilter.value) {
      return false
    }

    // Filtre Recherche
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      const matchName = v.name && v.name.toLowerCase().includes(q)
      const matchBrand = v.brand && v.brand.toLowerCase().includes(q)
      const matchModel = v.model && v.model.toLowerCase().includes(q)
      const matchFinition = v.finitionName && v.finitionName.toLowerCase().includes(q)
      const matchMotor = v.motorisationName && v.motorisationName.toLowerCase().includes(q)
      if (!matchName && !matchBrand && !matchModel && !matchFinition && !matchMotor) return false
    }
    return true
  }).sort((a, b) => {
    if (onlyProfitableFilter.value || selectedEnergyFilter.value === 'PROFITABLE') {
      const mA = getVehicleMetrics(a)
      const mB = getVehicleMetrics(b)
      return (mA.breakEvenYears || 99) - (mB.breakEvenYears || 99)
    }
    return 0
  })
})

// Podium / Insights
const bestRoiAlternative = computed(() => {
  if (!result.value || !result.value.alternatives || result.value.alternatives.length === 0) return null
  const viable = result.value.alternatives.filter(a => a.breakEvenYear !== null)
  return viable.length > 0 ? viable[0] : null
})

const maxSavingsAlternative = computed(() => {
  if (!result.value || !result.value.alternatives || result.value.alternatives.length === 0) return null
  const sorted = [...result.value.alternatives].sort((a, b) => b.annualSavings - a.annualSavings)
  return sorted[0]
})

// ── Méthodes & Actions ─────────────────────────────────────────────────────

watch(() => props.activeUserProfile, (newProfile) => {
  if (newProfile) {
    selectedProfileId.value = newProfile.id
    referenceMode.value = 'GARAGE'
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

const selectAllFiltered = () => {
  const ids = filteredVehicles.value.map(v => v.id)
  const allIn = ids.every(id => selectedTargetIds.value.includes(id))
  if (allIn) {
    selectedTargetIds.value = selectedTargetIds.value.filter(id => !ids.includes(id))
  } else {
    selectedTargetIds.value = Array.from(new Set([...selectedTargetIds.value, ...ids]))
  }
}

const switchMobileTab = (tab) => {
  activeMobileView.value = tab
  if (typeof window !== 'undefined') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const selectAllProfitable = () => {
  onlyProfitableFilter.value = true
  const ids = profitableVehicles.value.map(v => v.id)
  selectedTargetIds.value = ids
}

const quickCompareProfitable = async () => {
  onlyProfitableFilter.value = true
  selectedTargetIds.value = profitableVehicles.value.map(v => v.id)
  await compare()
}

const clearAllSelection = () => {
  selectedTargetIds.value = []
}

const compare = async () => {
  if (selectedTargetIds.value.length === 0) return
  calculating.value = true
  error.value = null

  let currentVehicleData
  if (referenceMode.value === 'GARAGE' && selectedProfileId.value) {
    const profile = props.userProfiles?.find(p => p.id === selectedProfileId.value)
    if (profile) {
      currentVehicleData = {
        name: profile.name,
        fuelType: profile.fuelType,
        consumption: profile.consumption || 6.5,
        annualMileage: profile.annualMileage || 15000,
        purchasePrice: 0,
        resaleValue: profile.resaleValue || 5000,
        insuranceCost: profile.insuranceCost || 600,
        maintenanceCost: profile.maintenanceCost || 400
      }
    }
  }

  if (!currentVehicleData) {
    currentVehicleData = { ...manualVehicle.value }
  }

  try {
    result.value = await apiCompareCustomProfitability({
      currentVehicle: currentVehicleData,
      targetVehicleIds: selectedTargetIds.value,
      fuelPricesByType: fuelPrices.value,
      maxYears: maxYears.value,
      immediateRepairCost: immediateRepairCost.value
    })
    activeMobileView.value = 'results'
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  } catch (err) {
    error.value = err.message
  } finally {
    calculating.value = false
  }
}

const openInSimulator = (targetVariantId) => {
  const found = vehicles.value.find(v => v.id === targetVariantId)
  if (found) {
    const simVehicle = {
      name: `${found.brand} ${found.model}`,
      brand: found.brand,
      model: found.model,
      version: found.version,
      fuelType: found.fuelType,
      consumption: found.consumption,
      purchasePrice: found.purchasePrice,
      monthlyLoa: found.monthlyLoa,
      monthlyLld: found.monthlyLld,
      insuranceCost: found.insuranceCost,
      maintenanceCost: found.maintenanceCost,
      resaleValue: found.resaleValue,
      annualMileage: activeReferenceVehicle.value.annualMileage || 15000
    }
    localStorage.setItem('eco_custom_target_vehicle', JSON.stringify(simVehicle))
    emit('open-simulator')
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
  <div class="luxury-comparator-root animation-fadeIn">
    
    <!-- ── 1. LUXURY EDITORIAL HERO ─────────────────────────────────────────── -->
    <header class="luxury-hero-banner hide-on-mobile">
      <div class="hero-glow-backdrop"></div>
      <div class="hero-inner">
        <div class="hero-left">
          <div class="luxury-pill-badge">
            <Sparkles size="12" class="text-emerald" />
            <span>ÉTUDE STRATÉGIQUE & ARBITRAGE TCO</span>
          </div>
          <h1 class="hero-luxury-title">Comparateur de Flotte & Rentabilité</h1>
          <p class="hero-luxury-subtitle">
            Filtrez instantanément les modèles <strong>économiquement rentables</strong> selon votre horizon d'amortissement ({{ maxYears }} ans) et comparez leurs gains nets.
          </p>
          <div class="hero-specs-row">
            <div class="spec-tag">
              <Car size="13" class="text-emerald" />
              <span><strong>138</strong> Variantes Certifiées</span>
            </div>
            <div class="spec-tag">
              <Award size="13" class="text-gold" />
              <span><strong>{{ profitableCount }}</strong> Modèles Rentables (≤ {{ maxYears }} ans)</span>
            </div>
            <div class="spec-tag">
              <TrendingUp size="13" class="text-emerald" />
              <span>Calcul d'Amortissement Dynamique</span>
            </div>
          </div>
        </div>

        <div class="hero-right hide-on-mobile">
          <div class="hero-visual-card">
            <img :src="vehicleEcoSavingsImg" class="hero-showcase-img" alt="EcoSwitch Luxury Analytics" />
          </div>
        </div>
      </div>
    </header>

    <!-- ── 2. WORKFLOW STEPPER SEGMENTÉ (CONTENEUR STICKY PARFAITEMENT POSITIONNÉ) ── -->
    <div class="stepper-sticky-container">
      <nav class="workflow-stepper-track" aria-label="Étapes du comparateur">
        <button 
          type="button"
          class="stepper-tab" 
          :class="{ active: activeMobileView === 'config' }"
          @click="switchMobileTab('config')"
        >
          <span class="stepper-pill-badge">1</span>
          <span class="stepper-tab-text">Référence</span>
        </button>

        <button 
          type="button"
          class="stepper-tab" 
          :class="{ active: activeMobileView === 'showroom' }"
          @click="switchMobileTab('showroom')"
        >
          <span class="stepper-pill-badge">2</span>
          <span class="stepper-tab-text">Showroom</span>
          <span v-if="selectedTargetIds.length > 0" class="stepper-counter-dot">{{ selectedTargetIds.length }}</span>
        </button>

        <button 
          type="button"
          class="stepper-tab" 
          :class="{ active: activeMobileView === 'results', disabled: !result }"
          @click="result && switchMobileTab('results')"
        >
          <span class="stepper-pill-badge">3</span>
          <span class="stepper-tab-text">Résultats</span>
        </button>
      </nav>
    </div>

    <!-- ── 3. MAIN DUAL / TRIPLE PANE CONTENT ──────────────────────────────── -->
    <div class="luxury-main-grid">
      
      <!-- ── SECTION A : VÉHICULE SOURCE (GAUCHE) ────────────────────────── -->
      <section class="luxury-panel config-panel" :class="{ 'mobile-active': activeMobileView === 'config' }">
        <div class="panel-header">
          <div class="panel-title-wrapper">
            <span class="panel-icon-badge">1</span>
            <div>
              <h2 class="panel-title">Véhicule de Référence</h2>
              <p class="panel-subtitle">Point de repère financier pour évaluer les gains</p>
            </div>
          </div>
        </div>

        <!-- Mode Source Switcher -->
        <div class="source-mode-segmented">
          <button 
            type="button" 
            class="seg-item" 
            :class="{ active: referenceMode === 'GARAGE' && hasProfiles }"
            :disabled="!hasProfiles"
            @click="referenceMode = 'GARAGE'"
          >
            <ShieldCheck size="14" />
            <span>Mon Garage</span>
            <span v-if="hasProfiles" class="badge-count">{{ userProfiles.length }}</span>
          </button>
          <button 
            type="button" 
            class="seg-item" 
            :class="{ active: referenceMode === 'CUSTOM' || !hasProfiles }"
            @click="referenceMode = 'CUSTOM'"
          >
            <Sliders size="14" />
            <span>Personnalisé</span>
          </button>
        </div>

        <!-- Sélection Garage -->
        <div v-if="referenceMode === 'GARAGE' && hasProfiles" class="garage-selector-box">
          <label class="luxury-label">Sélectionnez le véhicule à remplacer :</label>
          <div class="garage-cards-list">
            <div 
              v-for="p in userProfiles" 
              :key="p.id"
              class="garage-card-choice"
              :class="{ selected: selectedProfileId === p.id }"
              @click="selectedProfileId = p.id"
            >
              <div class="gcc-header">
                <span class="gcc-name">{{ p.name }}</span>
                <span class="gcc-energy-badge" :class="p.fuelType">{{ p.fuelType }}</span>
              </div>
              <div class="gcc-specs">
                <span>{{ p.consumption || 6.5 }} L/100km</span>
                <span>&middot;</span>
                <span>{{ (p.annualMileage || 15000).toLocaleString('fr-FR') }} km/an</span>
                <span>&middot;</span>
                <span>Reprise : {{ formatCurrency(p.resaleValue || 5000) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Saisie Personnalisée Haute Précision -->
        <div v-else class="custom-vehicle-form">
          <div class="form-row">
            <div class="form-group flex-1">
              <label class="luxury-label">Dénomination du modèle</label>
              <input v-model="manualVehicle.name" type="text" class="luxury-input" placeholder="ex: Peugeot 308 PureTech" />
            </div>
          </div>

          <!-- Choix Énergie en Chips Tactiles -->
          <div class="form-group">
            <label class="luxury-label">Motorisation & Énergie</label>
            <div class="energy-chips-grid">
              <button 
                type="button" 
                class="energy-chip" 
                :class="{ active: manualVehicle.fuelType === 'PETROL' }"
                @click="manualVehicle.fuelType = 'PETROL'"
              >
                <Flame size="14" /> Essence
              </button>
              <button 
                type="button" 
                class="energy-chip" 
                :class="{ active: manualVehicle.fuelType === 'DIESEL' }"
                @click="manualVehicle.fuelType = 'DIESEL'"
              >
                <Fuel size="14" /> Diesel
              </button>
              <button 
                type="button" 
                class="energy-chip" 
                :class="{ active: manualVehicle.fuelType === 'HYBRID' }"
                @click="manualVehicle.fuelType = 'HYBRID'"
              >
                <Leaf size="14" /> Hybride
              </button>
              <button 
                type="button" 
                class="energy-chip" 
                :class="{ active: manualVehicle.fuelType === 'ELECTRIC' }"
                @click="manualVehicle.fuelType = 'ELECTRIC'"
              >
                <Zap size="14" /> Électrique
              </button>
            </div>
          </div>

          <!-- Sliders de Conso & Kilométrage -->
          <div class="luxury-slider-block">
            <div class="slider-header">
              <span class="slider-title">Consommation moyenne</span>
              <span class="slider-badge">{{ manualVehicle.consumption }} {{ manualVehicle.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km</span>
            </div>
            <input 
              v-model.number="manualVehicle.consumption" 
              type="range" 
              min="2" 
              max="16" 
              step="0.1" 
              class="luxury-range"
            />
          </div>

          <div class="luxury-slider-block">
            <div class="slider-header">
              <span class="slider-title">Kilométrage annuel</span>
              <span class="slider-badge">{{ manualVehicle.annualMileage.toLocaleString('fr-FR') }} km / an</span>
            </div>
            <input 
              v-model.number="manualVehicle.annualMileage" 
              type="range" 
              min="5000" 
              max="50000" 
              step="1000" 
              class="luxury-range"
            />
          </div>

          <!-- Grille Coûts & Valeur de reprise -->
          <div class="luxury-grid-2">
            <div class="form-group">
              <label class="luxury-label">Valeur de reprise (€)</label>
              <input v-model.number="manualVehicle.resaleValue" type="number" step="500" class="luxury-input" />
            </div>
            <div class="form-group">
              <label class="luxury-label">Assurance annuelle (€)</label>
              <input v-model.number="manualVehicle.insuranceCost" type="number" step="50" class="luxury-input" />
            </div>
          </div>

          <div class="luxury-grid-2">
            <div class="form-group">
              <label class="luxury-label">Entretien annuel (€)</label>
              <input v-model.number="manualVehicle.maintenanceCost" type="number" step="50" class="luxury-input" />
            </div>
            <div class="form-group">
              <label class="luxury-label text-rose">Frais réparations immédiats (€)</label>
              <input v-model.number="immediateRepairCost" type="number" step="100" class="luxury-input" placeholder="0" />
            </div>
          </div>
        </div>

        <!-- Synthèse du budget de fonctionnement actuel -->
        <div class="current-tco-summary-card">
          <div class="tco-row">
            <span class="tco-title">Budget d'usage annuel estimé</span>
            <span class="tco-amount">{{ formatCurrency(currentVehicleTotalAnnualCost) }} / an</span>
          </div>
          <div class="tco-breakdown">
            <span>Carburant : {{ formatCurrency(currentVehicleAnnualFuelCost) }}</span>
            <span>&middot;</span>
            <span>Assurance : {{ formatCurrency(activeReferenceVehicle.insuranceCost || 600) }}</span>
            <span>&middot;</span>
            <span>Entretien : {{ formatCurrency(activeReferenceVehicle.maintenanceCost || 400) }}</span>
          </div>
        </div>

        <!-- Paramètres Horizon Temporel -->
        <div class="horizon-block">
          <div class="slider-header">
            <div class="flex items-center gap-1.5">
              <TrendingUp size="14" class="text-gold" />
              <span class="slider-title font-bold text-main">Horizon d'Amortissement Cible</span>
            </div>
            <span class="slider-badge-gold">{{ maxYears }} ans</span>
          </div>
          <p class="text-xxs text-dimmed mt-1 mb-2">
            Ajustez la durée pour recalculer immédiatement la liste des modèles rentables dans le showroom.
          </p>
          <input 
            v-model.number="maxYears" 
            type="range" 
            min="2" 
            max="15" 
            step="1" 
            class="luxury-range"
          />
        </div>

        <!-- CTA Mobile pour aller au showroom -->
        <div class="mobile-only mt-4">
          <button class="luxury-cta-btn w-100" @click="switchMobileTab('showroom')">
            <span>Explorer le Showroom ({{ selectedTargetIds.length }} choisis)</span>
            <ChevronRight size="16" />
          </button>
        </div>
      </section>

      <!-- ── SECTION B : SHOWROOM ET SÉLECTION DES ALTERNATIVES ────────────── -->
      <section class="luxury-panel showroom-panel" :class="{ 'mobile-active': activeMobileView === 'showroom' }">
        
        <!-- En-tête épuré du Showroom -->
        <div class="panel-header">
          <div class="panel-title-wrapper">
            <span class="panel-icon-badge">2</span>
            <div>
              <h2 class="panel-title">Showroom des Véhicules Cibles</h2>
              <p class="panel-subtitle">
                <span v-if="profitableCount > 0" class="text-emerald font-bold">✦ {{ profitableCount }} modèle{{ profitableCount > 1 ? 's rentables' : ' rentable' }}</span>
                <span v-else class="text-muted">Aucun modèle rentable sous {{ maxYears }} ans</span>
                <span class="mx-1 opacity-40">·</span>
                <span>{{ filteredVehicles.length }} disponible{{ filteredVehicles.length > 1 ? 's' : '' }}</span>
              </p>
            </div>
          </div>
          
          <div class="header-actions">
            <button 
              v-if="profitableCount > 0"
              type="button" 
              class="luxury-profitable-quick-btn" 
              @click="selectAllProfitable"
              title="Cocher automatiquement tous les modèles rentables"
            >
              <Sparkles size="13" class="text-gold" />
              <span>Cocher les {{ profitableCount }} rentables</span>
            </button>

            <button 
              type="button" 
              class="luxury-ghost-btn" 
              @click="selectAllFiltered"
              title="Cocher tous les modèles filtrés"
            >
              <Check size="13" />
              <span>Tout ({{ filteredVehicles.length }})</span>
            </button>

            <button 
              v-if="selectedTargetIds.length > 0"
              type="button" 
              class="luxury-ghost-btn text-rose" 
              @click="clearAllSelection"
            >
              Effacer ({{ selectedTargetIds.length }})
            </button>
          </div>
        </div>

        <!-- Filtres unifiés et compacts -->
        <div class="showroom-filters-wrapper">
          <!-- Barre de recherche -->
          <div class="search-luxury-bar">
            <Search size="14" class="search-luxury-icon" />
            <input 
              v-model="searchQuery" 
              type="text" 
              class="search-luxury-input" 
              placeholder="Rechercher une marque, un modèle..."
            />
            <button v-if="searchQuery" class="clear-search-btn" @click="searchQuery = ''">✕</button>
          </div>

          <!-- Ligne de filtres rapides (Segmented Pills) -->
          <div class="filter-chips-row">
            <button 
              class="filter-pill pill-profitable" 
              :class="{ active: onlyProfitableFilter }"
              @click="onlyProfitableFilter = !onlyProfitableFilter"
            >
              <Sparkles size="12" />
              <span>Rentables ({{ profitableCount }})</span>
            </button>

            <button 
              class="filter-pill" 
              :class="{ active: selectedEnergyFilter === 'ALL' && !onlyProfitableFilter }"
              @click="selectedEnergyFilter = 'ALL'; onlyProfitableFilter = false"
            >
              Tous ({{ vehicles.length }})
            </button>
            <button 
              class="filter-pill" 
              :class="{ active: selectedEnergyFilter === 'ELECTRIC' }"
              @click="selectedEnergyFilter = 'ELECTRIC'"
            >
              ⚡ 100% Élec
            </button>
            <button 
              class="filter-pill" 
              :class="{ active: selectedEnergyFilter === 'HYBRID' }"
              @click="selectedEnergyFilter = 'HYBRID'"
            >
              🍃 Hybride
            </button>
            <button 
              class="filter-pill" 
              :class="{ active: selectedEnergyFilter === 'THERMAL' }"
              @click="selectedEnergyFilter = 'THERMAL'"
            >
              ⛽ Thermique
            </button>
          </div>

          <!-- Filtres Marques (Logos épurés) -->
          <div class="brands-pills-carousel">
            <button 
              class="brand-pill-item" 
              :class="{ active: selectedBrandFilter === 'ALL' }"
              @click="selectedBrandFilter = 'ALL'"
            >
              Toutes marques
            </button>
            <button 
              v-for="b in availableBrands" 
              :key="b.name"
              class="brand-pill-item" 
              :class="{ active: selectedBrandFilter === b.name }"
              @click="selectedBrandFilter = (selectedBrandFilter === b.name ? 'ALL' : b.name)"
            >
              <img v-if="b.logoUrl" :src="b.logoUrl" :alt="b.name" class="brand-pill-logo" />
              <span>{{ b.name }}</span>
              <span class="brand-pill-count">{{ b.count }}</span>
            </button>
          </div>
        </div>

        <!-- Grille des Cartes Véhicules Showroom -->
        <div v-if="loading" class="showroom-loading-state">
          <Sparkles size="24" class="spinner text-emerald" />
          <p>Chargement des modèles officiels...</p>
        </div>

        <div v-else-if="filteredVehicles.length === 0" class="showroom-empty-state">
          <AlertCircle size="32" class="text-dimmed mb-2" />
          <p v-if="onlyProfitableFilter">
            Aucun modèle n'est rentabilisé en moins de <strong>{{ maxYears }} ans</strong> avec vos paramètres actuels.
            <br />
            <span class="text-xxs text-muted mt-1 block">Essayez d'augmenter l'horizon de comparaison ou vos kilomètres annuels.</span>
          </p>
          <p v-else>Aucun modèle ne correspond à votre recherche.</p>
        </div>

        <div v-else class="showroom-vehicles-grid">
          <div 
            v-for="v in filteredVehicles" 
            :key="v.id"
            class="showroom-vehicle-card"
            :class="{ 
              selected: selectedTargetIds.includes(v.id),
              'is-profitable-border': getVehicleMetrics(v).isProfitable
            }"
            @click="toggleTargetSelection(v.id)"
          >
            <!-- Bloc 1 : Vignette Visuelle Silhouette + Logo Marque -->
            <div class="svc-thumb-box">
              <div class="svc-image-container">
                <img 
                  v-if="v.imageUrl" 
                  :src="v.imageUrl" 
                  :alt="v.name" 
                  class="svc-vehicle-img" 
                  @error="(e) => e.target.style.opacity = '0.3'"
                />
                <Car v-else size="26" class="text-dimmed opacity-40" />
              </div>
              <img v-if="v.brandLogoUrl" :src="v.brandLogoUrl" :alt="v.brand" class="svc-brand-mini-badge" />
            </div>

            <!-- Bloc 2 : Détails Véhicule (Hiérarchie typographique luxe) -->
            <div class="svc-info-box">
              <!-- Ligne 1 : Nom du modèle en évidence (Sans troncature agressive) -->
              <div class="svc-title-row">
                <h3 class="svc-name">{{ v.brand }} {{ v.model }}</h3>
              </div>

              <!-- Ligne 2 : Finition & Motorisation -->
              <div class="svc-subtitle-row">
                <span v-if="v.finitionName" class="svc-finition">{{ v.finitionName }}</span>
                <span v-if="v.finitionName" class="svc-dot-sep">·</span>
                <span class="svc-powertrain">{{ v.motorisationName || (v.fuelType === 'ELECTRIC' ? '100% Électrique' : 'Hybride') }}</span>
              </div>

              <!-- Ligne 3 : Badge ROI / Rentabilité -->
              <div class="svc-roi-indicator">
                <span 
                  v-if="getVehicleMetrics(v).isProfitable" 
                  class="roi-chip-tag tag-rentable"
                >
                  <Sparkles size="10" />
                  Rentable en {{ getVehicleMetrics(v).breakEvenYears.toFixed(1) }} ans (+{{ formatCurrency(getVehicleMetrics(v).annualSavings) }}/an)
                </span>
                <span 
                  v-else-if="getVehicleMetrics(v).annualSavings > 0" 
                  class="roi-chip-tag tag-long"
                >
                  Amorti en {{ getVehicleMetrics(v).breakEvenYears.toFixed(1) }} ans
                </span>
                <span 
                  v-else 
                  class="roi-chip-tag tag-neutral"
                >
                  Surcoût à {{ maxYears }} ans
                </span>
              </div>
            </div>

            <!-- Bloc 3 : Tarifs & Checkbox tactile -->
            <div class="svc-action-box">
              <div class="svc-pricing">
                <div class="svc-price-main">{{ formatCurrency(v.purchasePrice) }}</div>
                <div v-if="v.monthlyLoa" class="svc-price-sub">dès {{ formatCurrency(v.monthlyLoa) }}/m</div>
              </div>
              
              <div class="svc-check-circle" :class="{ checked: selectedTargetIds.includes(v.id) }">
                <Check v-if="selectedTargetIds.includes(v.id)" size="14" />
              </div>
            </div>
          </div>
        </div>

        <!-- Barre d'Action Desktop classique -->
        <div class="showroom-bottom-bar hide-on-mobile">
          <div class="selection-counter">
            <span class="count-badge">{{ selectedTargetIds.length }}</span>
            <span class="count-text">modèle{{ selectedTargetIds.length > 1 ? 's' : '' }} sélectionné{{ selectedTargetIds.length > 1 ? 's' : '' }}</span>
          </div>

          <button 
            :disabled="calculating || selectedTargetIds.length === 0" 
            class="luxury-cta-btn" 
            @click="compare"
          >
            <span v-if="calculating" class="spinner"><Zap size="16" /></span>
            <span v-else>Lancer l'Arbitrage Stratégique</span>
            <ArrowRight size="16" />
          </button>
        </div>
      </section>

      <!-- ── SECTION C : RÉSULTATS COMPARATIFS (DROITE / PLEINE LARGEUR) ───── -->
      <section v-if="result" class="luxury-panel results-panel" :class="{ 'mobile-active': activeMobileView === 'results' }">
        <div class="panel-header">
          <div class="panel-title-wrapper">
            <span class="panel-icon-badge">3</span>
            <div>
              <h2 class="panel-title">Tableau de Bord & Arbitrage ROI</h2>
              <p class="panel-subtitle">
                Référence : <strong>{{ result.currentVehicleName }}</strong> &middot; Horizon : {{ result.maxYears }} ans
              </p>
            </div>
          </div>

          <!-- Bascule Vue Cartes / Matrice -->
          <div class="results-view-segmented">
            <button 
              class="r-seg-btn" 
              :class="{ active: activeTabResults === 'cards' }"
              @click="activeTabResults = 'cards'"
            >
              <Layers size="13" /> Cartes Détaillées
            </button>
            <button 
              class="r-seg-btn" 
              :class="{ active: activeTabResults === 'matrix' }"
              @click="activeTabResults = 'matrix'"
            >
              <BarChart3 size="13" /> Matrice TCO
            </button>
          </div>
        </div>

        <!-- Bouton Retour Showroom sur Mobile -->
        <div class="mobile-return-banner hide-on-desktop">
          <button type="button" class="luxury-ghost-btn w-100 flex-center gap-1.5" @click="switchMobileTab('showroom')">
            <ArrowLeft size="14" />
            <span>Modifier la sélection des modèles</span>
          </button>
        </div>

        <!-- ── PODIUM DES GAGNANTS (TOP INSIGHTS) ────────────────────────── -->
        <div class="luxury-podium-row">
          <!-- Carte 1: Meilleur ROI -->
          <div v-if="bestRoiAlternative" class="podium-card gold-border">
            <div class="podium-header">
              <Award size="16" class="text-gold" />
              <span>MEILLEUR AMORTISSEMENT (ROI)</span>
            </div>
            <div class="podium-model-name">{{ bestRoiAlternative.vehicleName }}</div>
            <div class="podium-highlight-val text-emerald">
              Amorti en {{ bestRoiAlternative.breakEvenYear }} an{{ bestRoiAlternative.breakEvenYear > 1 ? 's' : '' }}
            </div>
            <div class="podium-sub">
              Économie annuelle nette : <strong>+{{ formatCurrency(bestRoiAlternative.annualSavings) }}/an</strong>
            </div>
          </div>

          <!-- Carte 2: Plus grande économie annuelle -->
          <div v-if="maxSavingsAlternative" class="podium-card emerald-border">
            <div class="podium-header">
              <TrendingUp size="16" class="text-emerald" />
              <span>ÉCONOMIE D'USAGE MAXIMALE</span>
            </div>
            <div class="podium-model-name">{{ maxSavingsAlternative.vehicleName }}</div>
            <div class="podium-highlight-val text-emerald">
              +{{ formatCurrency(maxSavingsAlternative.annualSavings) }} / an
            </div>
            <div class="podium-sub">
              Coût annuel : {{ formatCurrency(maxSavingsAlternative.targetAnnualCost) }} (vs {{ formatCurrency(maxSavingsAlternative.currentAnnualCost) }})
            </div>
          </div>

          <!-- Carte 3: Bilan Horizon -->
          <div class="podium-card cyan-border">
            <div class="podium-header">
              <ShieldCheck size="16" class="text-cyan" />
              <span>SYNTHÈSE À {{ result.maxYears }} ANS</span>
            </div>
            <div class="podium-model-name">{{ result.alternatives.length }} alternatives analysées</div>
            <div class="podium-highlight-val text-cyan">
              {{ result.alternatives.filter(a => a.totalCostDeltaAtHorizon < 0).length }} Modèles Rentables
            </div>
            <div class="podium-sub">
              Optimisation de trésorerie nette sur {{ result.maxYears }} ans
            </div>
          </div>
        </div>

        <!-- ── VUE 1 : CARTES STRATÉGIQUES DÉTAILLÉES ─────────────────────── -->
        <div v-if="activeTabResults === 'cards'" class="results-cards-flow">
          <div 
            v-for="(alt, idx) in result.alternatives" 
            :key="alt.vehicleId"
            class="luxury-alt-card"
            :class="alt.breakEvenYear && alt.breakEvenYear <= 3 ? 'best-roi-card' : alt.breakEvenYear ? 'good-roi-card' : 'no-roi-card'"
          >
            <!-- Ligne supérieure avec Rang et Badges -->
            <div class="alt-card-header">
              <div class="alt-rank-block">
                <span class="alt-rank-number">#{{ idx + 1 }}</span>
                <div class="alt-names">
                  <h3 class="alt-vehicle-title">{{ alt.vehicleName }}</h3>
                </div>
              </div>

              <div class="alt-badges-block">
                <span 
                  v-if="alt.breakEvenYear" 
                  class="roi-status-pill roi-success"
                >
                  <TrendingUp size="12" /> Rentable en {{ alt.breakEvenYear }} an{{ alt.breakEvenYear > 1 ? 's' : '' }}
                </span>
                <span v-else class="roi-status-pill roi-warning">
                  Non rentabilisé à {{ result.maxYears }} ans
                </span>
              </div>
            </div>

            <!-- Grille des 4 Piliers Financiers -->
            <div class="alt-metrics-luxury-grid">
              <div class="metric-luxury-card">
                <span class="m-lux-label">Investissement net</span>
                <span class="m-lux-val">{{ formatCurrency(alt.switchInvestment) }}</span>
                <span class="m-lux-sub">Prix cible - Reprise</span>
              </div>

              <div class="metric-luxury-card">
                <span class="m-lux-label">Économie annuelle</span>
                <span class="m-lux-val text-emerald">+{{ formatCurrency(alt.annualSavings) }}/an</span>
                <span class="m-lux-sub">Énergie & Entretien</span>
              </div>

              <div class="metric-luxury-card">
                <span class="m-lux-label">Bilan net à 5 ans</span>
                <span 
                  class="m-lux-val" 
                  :class="(alt.annualSavings * 5 - alt.switchInvestment) > 0 ? 'text-emerald' : 'text-rose'"
                >
                  {{ (alt.annualSavings * 5 - alt.switchInvestment) > 0 ? '+' : '' }}{{ formatCurrency(alt.annualSavings * 5 - alt.switchInvestment) }}
                </span>
                <span class="m-lux-sub">Gains cumulés</span>
              </div>

              <div class="metric-luxury-card">
                <span class="m-lux-label">Bilan à {{ result.maxYears }} ans</span>
                <span 
                  class="m-lux-val font-bold" 
                  :class="alt.totalCostDeltaAtHorizon < 0 ? 'text-emerald' : 'text-rose'"
                >
                  {{ alt.totalCostDeltaAtHorizon < 0 ? 'Gain +' : 'Surcoût ' }}{{ formatCurrency(Math.abs(alt.totalCostDeltaAtHorizon)) }}
                </span>
                <span class="m-lux-sub">TCO Global Net</span>
              </div>
            </div>

            <!-- Jauge Comparative des Coûts Annuels -->
            <div class="alt-gauge-card">
              <div class="gauge-labels-row">
                <span class="gl-dimmed">Coût actuel de référence : <strong>{{ formatCurrency(alt.currentAnnualCost) }} / an</strong></span>
                <span class="gl-emerald">Nouveau coût cible : <strong>{{ formatCurrency(alt.targetAnnualCost) }} / an</strong></span>
              </div>
              <div class="gauge-track">
                <div 
                  class="gauge-fill-emerald"
                  :style="{ width: Math.min(100, Math.max(15, (alt.targetAnnualCost / (alt.currentAnnualCost || 1)) * 100)) + '%' }"
                ></div>
              </div>
            </div>

            <!-- Footer avec bouton Simulation Directe -->
            <div class="alt-card-footer">
              <button 
                type="button" 
                class="luxury-simulator-btn" 
                @click="openInSimulator(alt.vehicleId)"
              >
                <span>+ Ouvrir et personnaliser dans le Simulateur Pro</span>
                <ExternalLink size="13" />
              </button>
            </div>
          </div>
        </div>

        <!-- ── VUE 2 : MATRICE COMPARATIVE TCO COMPLÈTE ───────────────────── -->
        <div v-else-if="activeTabResults === 'matrix'" class="matrix-table-wrapper">
          <table class="luxury-matrix-table">
            <thead>
              <tr>
                <th>Rang</th>
                <th>Modèle & Version</th>
                <th>Investissement</th>
                <th>Coût Annuel Cible</th>
                <th>Économie / an</th>
                <th>Seuil ROI</th>
                <th>Gain Net à {{ result.maxYears }} ans</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(alt, idx) in result.alternatives" :key="alt.vehicleId">
                <td class="cell-rank">#{{ idx + 1 }}</td>
                <td class="cell-vehicle font-bold">{{ alt.vehicleName }}</td>
                <td class="cell-num">{{ formatCurrency(alt.switchInvestment) }}</td>
                <td class="cell-num text-emerald font-semibold">{{ formatCurrency(alt.targetAnnualCost) }}</td>
                <td class="cell-num text-emerald font-bold">+{{ formatCurrency(alt.annualSavings) }}</td>
                <td class="cell-roi">
                  <span v-if="alt.breakEvenYear" class="badge-roi-small">{{ alt.breakEvenYear }} ans</span>
                  <span v-else class="badge-roi-none">Non amorti</span>
                </td>
                <td class="cell-num" :class="alt.totalCostDeltaAtHorizon < 0 ? 'text-emerald font-bold' : 'text-rose'">
                  {{ alt.totalCostDeltaAtHorizon < 0 ? '+' : '-' }}{{ formatCurrency(Math.abs(alt.totalCostDeltaAtHorizon)) }}
                </td>
                <td class="cell-action">
                  <button class="btn-table-sim" @click="openInSimulator(alt.vehicleId)">
                    Simuler
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <!-- ── 4. BARRE D'ACTION FLOTTANTE FIXE MOBILE (TÉLÉPORTÉE POUR VISIBILITÉ 100% GARANTIE) ── -->
    <Teleport to="body">
      <div 
        v-if="activeMobileView === 'showroom'" 
        class="showroom-mobile-floating-bar"
      >
        <div class="smfb-content">
          <div class="smfb-left">
            <span class="smfb-badge">{{ selectedTargetIds.length }}</span>
            <span class="smfb-text">sélectionné{{ selectedTargetIds.length > 1 ? 's' : '' }}</span>
          </div>

          <div class="smfb-actions">
            <button 
              v-if="selectedTargetIds.length === 0 && profitableCount > 0"
              type="button" 
              class="smfb-quick-btn" 
              @click="quickCompareProfitable"
            >
              <Sparkles size="14" />
              <span>Top {{ profitableCount }} rentables</span>
            </button>

            <button 
              :disabled="calculating || selectedTargetIds.length === 0" 
              class="smfb-cta-btn" 
              @click="compare"
            >
              <span v-if="calculating" class="spinner"><Zap size="15" /></span>
              <span v-else class="flex items-center gap-1.5">
                <Zap size="15" />
                <span>Comparer ({{ selectedTargetIds.length }})</span>
                <ArrowRight size="15" />
              </span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* ==========================================================================
   LUXURY COMPARATOR DESIGN SYSTEM - APPLE / PORSCHE TIER DIGITAL UI
   ========================================================================== */

.luxury-comparator-root {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

/* ── 1. HERO EDITORIAL ─────────────────────────────────────────────────── */
.luxury-hero-banner {
  position: relative;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(245, 245, 247, 0.9));
  border: 1px solid var(--border-glass);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  padding: 28px 32px;
}
[data-theme="dark"] .luxury-hero-banner {
  background: linear-gradient(135deg, rgba(22, 22, 24, 0.95), rgba(16, 16, 18, 0.9));
  border-color: rgba(255, 255, 255, 0.08);
}
.hero-glow-backdrop {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(16, 124, 65, 0.15) 0%, rgba(0,0,0,0) 70%);
  pointer-events: none;
}
.hero-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}
.hero-luxury-title {
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--text-main);
  letter-spacing: -0.02em;
  margin: 6px 0;
}
.hero-luxury-subtitle {
  font-size: 0.85rem;
  color: var(--text-muted);
  max-width: 620px;
  line-height: 1.5;
  margin: 0 0 14px 0;
}
.luxury-pill-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 9999px;
  background: rgba(16, 124, 65, 0.1);
  color: var(--accent-teal);
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  border: 1px solid rgba(16, 124, 65, 0.2);
}
.hero-specs-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.spec-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--text-dimmed);
  background: var(--bg-card-subtle);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-glass);
}
.hero-showcase-img {
  width: 120px;
  height: auto;
  object-fit: contain;
  filter: drop-shadow(0 10px 20px rgba(0,0,0,0.15));
}

/* ── 2. WORKFLOW STEPPER SEGMENTED TRACK ─────────────────────────────── */
.stepper-sticky-container {
  display: none;
}

@media (max-width: 1024px) {
  .stepper-sticky-container {
    display: block;
    width: 100%;
    margin-bottom: 12px;
  }

  .workflow-stepper-track {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 4px;
    padding: 4px;
    background: var(--bg-card-subtle);
    border: 1px solid var(--border-glass);
    border-radius: 12px;
    width: 100%;
    box-sizing: border-box;
  }
}

.stepper-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 6px;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  min-width: 0;
  overflow: hidden;
}
.stepper-tab:hover {
  color: var(--text-main);
}
.stepper-tab.active {
  background: var(--bg-card);
  color: var(--accent-teal);
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
}
.stepper-tab.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.stepper-pill-badge {
  width: 18px;
  height: 18px;
  min-width: 18px;
  border-radius: 9999px;
  background: rgba(0, 0, 0, 0.06);
  color: var(--text-dimmed);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  font-weight: 800;
}
[data-theme="dark"] .stepper-pill-badge {
  background: rgba(255, 255, 255, 0.08);
}
.stepper-tab.active .stepper-pill-badge {
  background: var(--accent-teal);
  color: #ffffff;
}

.stepper-tab-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stepper-counter-dot {
  padding: 1px 5px;
  border-radius: 9999px;
  background: var(--accent-teal);
  color: #ffffff;
  font-size: 0.62rem;
  font-weight: 800;
  line-height: 1;
}
.workflow-step-btn.active .step-num {
  background: var(--accent-teal);
  color: #fff;
}

/* ── 3. MAIN PANE LAYOUT ──────────────────────────────────────────────── */
.luxury-main-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  align-items: start;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  min-width: 0;
}
@media (max-width: 1024px) {
  .luxury-main-grid {
    grid-template-columns: 1fr;
  }
  .luxury-panel {
    display: none;
  }
  .luxury-panel.mobile-active {
    display: flex !important;
  }
}

.luxury-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-glass);
  padding-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
  box-sizing: border-box;
}
.panel-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}
.panel-icon-badge {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
  font-weight: 800;
  font-size: 0.8rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(16, 124, 65, 0.2);
}
.panel-title {
  font-size: 1rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
}
.panel-subtitle {
  font-size: 0.7rem;
  color: var(--text-dimmed);
  margin: 0;
}

/* ── SECTION A : CONFIG SOURCE ────────────────────────────────────────── */
.source-mode-segmented {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  padding: 3px;
  border-radius: var(--radius-md);
  gap: 4px;
}
.seg-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-dimmed);
  cursor: pointer;
  transition: all 0.15s ease;
}
.seg-item.active {
  background: var(--bg-card);
  color: var(--text-main);
  box-shadow: var(--shadow-sm);
  font-weight: 700;
}
.badge-count {
  font-size: 0.65rem;
  padding: 1px 5px;
  border-radius: 9999px;
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
}

.garage-cards-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}
.garage-card-choice {
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  cursor: pointer;
  transition: all 0.15s ease;
}
.garage-card-choice:hover {
  background: var(--bg-card-hover);
}
.garage-card-choice.selected {
  border-color: var(--accent-teal);
  background: var(--accent-teal-soft);
}
.gcc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.gcc-name {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--text-main);
}
.gcc-energy-badge {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
}
.gcc-specs {
  font-size: 0.7rem;
  color: var(--text-dimmed);
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.luxury-label {
  display: block;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-dimmed);
  margin-bottom: 6px;
}
.luxury-input {
  width: 100%;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-glass);
  background: var(--bg-input);
  color: var(--text-main);
  font-size: 0.8rem;
  outline: none;
  transition: border-color 0.15s ease;
}
.luxury-input:focus {
  border-color: var(--accent-teal);
}

.energy-chips-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}
.energy-chip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 7px 10px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.energy-chip:hover {
  background: var(--bg-card-hover);
}
.energy-chip.active {
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
  border-color: var(--accent-teal);
  font-weight: 700;
}

.luxury-slider-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--bg-card-subtle);
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
}
.slider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.slider-title {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-dimmed);
}
.slider-badge {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--accent-teal);
}
.slider-badge-gold {
  font-size: 0.78rem;
  font-weight: 800;
  color: #D97706;
  background: rgba(245, 158, 11, 0.15);
  padding: 2px 8px;
  border-radius: 9999px;
  border: 1px solid rgba(245, 158, 11, 0.3);
}
.luxury-range {
  width: 100%;
  accent-color: var(--accent-teal);
  cursor: pointer;
}

.luxury-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.current-tco-summary-card {
  background: linear-gradient(135deg, rgba(16, 124, 65, 0.08), rgba(2, 132, 199, 0.05));
  border: 1px solid rgba(16, 124, 65, 0.2);
  border-radius: var(--radius-md);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tco-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tco-title {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--text-muted);
}
.tco-amount {
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--accent-teal);
}
.tco-breakdown {
  font-size: 0.68rem;
  color: var(--text-dimmed);
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.horizon-block {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.06), var(--bg-card-subtle));
  border: 1px solid rgba(245, 158, 11, 0.25);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}

/* ── SECTION B : SHOWROOM & SELECTION ─────────────────────────────────── */
.showroom-panel {
  min-height: 600px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.luxury-profitable-quick-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #059669 0%, #10b981 100%);
  color: #ffffff !important;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-sm);
  padding: 8px 14px;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 3px 10px rgba(16, 185, 129, 0.35);
  transition: all 0.2s ease;
}
.luxury-profitable-quick-btn:hover {
  background: linear-gradient(135deg, #047857 0%, #059669 100%);
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(16, 185, 129, 0.45);
}
.luxury-profitable-quick-btn:active {
  transform: scale(0.98);
}

.luxury-cta-btn-header {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--text-main);
  color: var(--text-inverse);
  border: none;
  border-radius: var(--radius-sm);
  padding: 8px 14px;
  font-size: 0.76rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: var(--shadow-sm);
}
.luxury-cta-btn-header:hover {
  background: var(--accent-teal);
  color: #ffffff;
  transform: translateY(-1px);
}

.luxury-ghost-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-sm);
  padding: 7px 12px;
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.luxury-ghost-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-main);
}

.showroom-filters-wrapper {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  min-width: 0;
  overflow: hidden;
}
.search-luxury-bar {
  position: relative;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}
.search-luxury-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-dimmed);
}
.search-luxury-input {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 32px 8px 34px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  color: var(--text-main);
  font-size: 0.78rem;
  outline: none;
}
.search-luxury-input:focus {
  border-color: var(--accent-teal);
  background: var(--bg-card);
}
.clear-search-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: var(--text-dimmed);
  cursor: pointer;
}

.filter-chips-row {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 2px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}
.filter-chips-row::-webkit-scrollbar {
  display: none;
}
.filter-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 9999px;
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  color: var(--text-dimmed);
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.filter-pill.active {
  background: var(--text-main);
  color: var(--text-inverse);
  border-color: var(--text-main);
}
.filter-pill.pill-profitable {
  border-color: rgba(245, 158, 11, 0.4);
  color: #B45309;
  background: rgba(245, 158, 11, 0.08);
}
.filter-pill.pill-profitable.active {
  background: linear-gradient(135deg, #D97706, #107C41);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(217, 119, 6, 0.3);
}
.profitable-pill-badge {
  background: rgba(245, 158, 11, 0.2);
  color: inherit;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 1px 6px;
  border-radius: 9999px;
}
.filter-pill.pill-profitable.active .profitable-pill-badge {
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.brands-pills-carousel {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 4px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}
.brands-pills-carousel::-webkit-scrollbar {
  display: none;
}
.brand-pill-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.brand-pill-item.active {
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
  border-color: var(--accent-teal);
  font-weight: 700;
}
.brand-pill-logo {
  width: 14px;
  height: 14px;
  object-fit: contain;
}
.brand-pill-count {
  font-size: 0.65rem;
  opacity: 0.6;
}

.showroom-vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  max-height: 480px;
  overflow-y: auto;
  padding-right: 4px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  min-width: 0;
}

.showroom-vehicle-card {
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-lg);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}
.showroom-vehicle-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
  border-color: rgba(16, 124, 65, 0.3);
}
.showroom-vehicle-card.is-profitable-border {
  border-color: rgba(16, 124, 65, 0.35);
}
.showroom-vehicle-card.selected {
  border-color: var(--accent-teal);
  background: linear-gradient(180deg, var(--accent-teal-soft), var(--bg-card));
  box-shadow: 0 0 0 1px var(--accent-teal);
}

.svc-thumb-box {
  position: relative;
  width: 100%;
}
.svc-image-container {
  height: 75px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border-glass);
}
.svc-vehicle-img {
  width: 90%;
  height: 90%;
  object-fit: contain;
  transition: transform 0.2s ease;
}
.showroom-vehicle-card:hover .svc-vehicle-img {
  transform: scale(1.04);
}
.svc-brand-mini-badge {
  position: absolute;
  top: 5px;
  left: 5px;
  width: 18px;
  height: 18px;
  object-fit: contain;
  background: var(--bg-card);
  border-radius: 4px;
  padding: 2px;
  border: 1px solid var(--border-glass);
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.svc-info-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}
.svc-title-row {
  display: flex;
  align-items: baseline;
  min-width: 0;
}
.svc-name {
  font-size: 0.88rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.svc-subtitle-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.68rem;
  color: var(--text-dimmed);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.svc-finition {
  font-weight: 600;
}
.svc-dot-sep {
  opacity: 0.4;
}
.svc-powertrain {
  color: var(--text-muted);
}

.svc-roi-indicator {
  margin: 1px 0;
  min-width: 0;
}
.roi-chip-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.65rem;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 4px;
  width: auto;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tag-rentable {
  background: rgba(16, 124, 65, 0.15);
  color: var(--accent-teal);
  border: 1px solid rgba(16, 124, 65, 0.25);
}
.tag-long {
  background: rgba(245, 158, 11, 0.1);
  color: #B45309;
  border: 1px solid rgba(245, 158, 11, 0.2);
}
.tag-neutral {
  background: var(--bg-card);
  color: var(--text-dimmed);
  border: 1px solid var(--border-glass);
}

.svc-action-box {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  border-top: 1px solid var(--border-glass);
  padding-top: 8px;
  margin-top: auto;
}
.svc-pricing {
  display: flex;
  flex-direction: column;
}
.svc-price-main {
  font-size: 0.88rem;
  font-weight: 800;
  color: var(--text-main);
}
.svc-price-sub {
  font-size: 0.65rem;
  color: var(--accent-cyan);
  font-weight: 600;
}
.svc-check-circle {
  width: 22px;
  height: 22px;
  border-radius: 9999px;
  border: 1.5px solid var(--border-glass);
  background: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}
.svc-check-circle.checked {
  background: var(--accent-teal);
  border-color: var(--accent-teal);
  color: #fff;
}

.showroom-bottom-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid var(--border-glass);
  margin-top: auto;
}
.selection-counter {
  display: flex;
  align-items: center;
  gap: 8px;
}
.count-badge {
  width: 24px;
  height: 24px;
  border-radius: 9999px;
  background: var(--accent-teal);
  color: #fff;
  font-weight: 800;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.count-text {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-main);
}

.luxury-cta-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: var(--radius-full);
  background: var(--text-main);
  color: var(--text-inverse);
  font-size: 0.82rem;
  font-weight: 800;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(0,0,0,0.15);
  transition: all 0.2s ease;
}
.luxury-cta-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.25);
}
.luxury-cta-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── SECTION C : RESULTATS COMPARATIFS ────────────────────────────────── */
.results-panel {
  grid-column: 1 / -1;
  background: var(--bg-card);
}
.results-view-segmented {
  display: flex;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  padding: 2px;
  border-radius: var(--radius-md);
  gap: 4px;
}
.r-seg-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: none;
  color: var(--text-dimmed);
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
}
.r-seg-btn.active {
  background: var(--bg-card);
  color: var(--text-main);
  box-shadow: var(--shadow-sm);
}

/* Podium */
.luxury-podium-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
@media (max-width: 900px) {
  .luxury-podium-row {
    grid-template-columns: 1fr;
  }
}
.podium-card {
  padding: 16px;
  border-radius: var(--radius-lg);
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.podium-card.gold-border {
  border-color: rgba(245, 158, 11, 0.4);
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08), var(--bg-card-subtle));
}
.podium-card.emerald-border {
  border-color: rgba(16, 124, 65, 0.4);
  background: linear-gradient(135deg, rgba(16, 124, 65, 0.08), var(--bg-card-subtle));
}
.podium-card.cyan-border {
  border-color: rgba(2, 132, 199, 0.4);
  background: linear-gradient(135deg, rgba(2, 132, 199, 0.08), var(--bg-card-subtle));
}
.podium-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: var(--text-dimmed);
}
.podium-model-name {
  font-size: 0.85rem;
  font-weight: 800;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.podium-highlight-val {
  font-size: 1.15rem;
  font-weight: 900;
}
.podium-sub {
  font-size: 0.7rem;
  color: var(--text-muted);
}

/* Cartes détaillées */
.results-cards-flow {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.luxury-alt-card {
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-xl);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: all 0.2s ease;
}
.luxury-alt-card.best-roi-card {
  border-left: 4px solid var(--accent-teal);
}
.luxury-alt-card.good-roi-card {
  border-left: 4px solid var(--accent-cyan);
}
.luxury-alt-card.no-roi-card {
  border-left: 4px solid var(--accent-rose);
}

.alt-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.alt-rank-block {
  display: flex;
  align-items: center;
  gap: 10px;
}
.alt-rank-number {
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--text-dimmed);
}
.alt-vehicle-title {
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
}
.roi-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: 0.72rem;
  font-weight: 800;
}
.roi-success {
  background: rgba(16, 124, 65, 0.15);
  color: var(--accent-teal);
  border: 1px solid rgba(16, 124, 65, 0.3);
}
.roi-warning {
  background: rgba(225, 29, 72, 0.1);
  color: var(--accent-rose);
  border: 1px solid rgba(225, 29, 72, 0.2);
}

.alt-metrics-luxury-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
@media (max-width: 768px) {
  .alt-metrics-luxury-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
.metric-luxury-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.m-lux-label {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-dimmed);
}
.m-lux-val {
  font-size: 1rem;
  font-weight: 800;
  color: var(--text-main);
}
.m-lux-sub {
  font-size: 0.62rem;
  color: var(--text-dimmed);
}

.alt-gauge-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 10px 14px;
}
.gauge-labels-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  margin-bottom: 6px;
}
.gl-dimmed {
  color: var(--text-muted);
}
.gl-emerald {
  color: var(--accent-teal);
  font-weight: 700;
}
.gauge-track {
  height: 6px;
  background: rgba(0,0,0,0.06);
  border-radius: 9999px;
  overflow: hidden;
}
[data-theme="dark"] .gauge-track {
  background: rgba(255,255,255,0.08);
}
.gauge-fill-emerald {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-teal), #34C759);
  border-radius: 9999px;
}

.alt-card-footer {
  display: flex;
  justify-content: flex-end;
}
.luxury-simulator-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  color: var(--accent-teal);
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.luxury-simulator-btn:hover {
  background: var(--accent-teal-soft);
  border-color: var(--accent-teal);
}

/* ── VUE MATRICE COMPARATIVE ──────────────────────────────────────────── */
.matrix-table-wrapper {
  overflow-x: auto;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
}
.luxury-matrix-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
  text-align: left;
}
.luxury-matrix-table th {
  background: var(--bg-card-subtle);
  padding: 10px 14px;
  font-weight: 700;
  color: var(--text-dimmed);
  border-bottom: 1px solid var(--border-glass);
}
.luxury-matrix-table td {
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-glass);
  background: var(--bg-card);
}
.cell-rank {
  font-weight: 900;
  color: var(--text-dimmed);
}
.badge-roi-small {
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
  font-size: 0.68rem;
}
.badge-roi-none {
  background: rgba(225, 29, 72, 0.1);
  color: var(--accent-rose);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.68rem;
}
.btn-table-sim {
  padding: 4px 8px;
  border-radius: 4px;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  font-size: 0.68rem;
  font-weight: 700;
  color: var(--text-main);
  cursor: pointer;
}
.btn-table-sim:hover {
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
}

/* Helpers */
.text-emerald { color: var(--accent-teal); }
.text-gold { color: #D97706; }
.text-cyan { color: var(--accent-cyan); }
.text-rose { color: var(--accent-rose); }
.spinner { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* ── RESPONSIVE MOBILE & TABLET OPTIMIZATIONS (< 1024px) ───────────────── */

@media (max-width: 1024px) {
  .luxury-comparator-root {
    gap: 8px;
    margin-top: 0;
    padding-top: 0;
  }

  /* Masquer le hero éditorial lourd sur mobile/tablette pour libérer 100% de l'espace */
  .luxury-hero-banner,
  .hide-on-mobile {
    display: none !important;
  }

  .luxury-main-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .luxury-panel {
    display: none;
  }

  .luxury-panel.mobile-active {
    display: flex !important;
  }

  /* Conteneur Sticky Opaque et Flou collé directement sous le Header Mobile (top: 0 dans app-main) */
  .stepper-sticky-container {
    display: block;
    position: sticky;
    top: 0;
    z-index: 85;
    background: var(--bg-app);
    background: linear-gradient(180deg, var(--bg-app) 0%, rgba(var(--bg-app-rgb, 245, 245, 247), 0.98) 100%);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    padding: 8px 0 10px 0;
    margin: 0 0 10px 0;
    width: 100%;
    box-sizing: border-box;
  }
  [data-theme="dark"] .stepper-sticky-container {
    background: var(--bg-app, #0f0f11);
    background: linear-gradient(180deg, #0f0f11 0%, rgba(15, 15, 17, 0.98) 100%);
  }

  .workflow-stepper-track {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 4px;
    padding: 4px;
    background: var(--bg-card);
    border: 1px solid var(--border-glass);
    border-radius: 12px;
    width: 100%;
    box-sizing: border-box;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
    user-select: none;
    -webkit-user-select: none;
  }

  .stepper-tab {
    padding: 7px 4px;
    font-size: 0.74rem;
    gap: 5px;
    border-radius: 8px;
    user-select: none;
    -webkit-user-select: none;
    -webkit-tap-highlight-color: transparent;
  }

  .stepper-tab-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    user-select: none;
    -webkit-user-select: none;
  }

  /* Panels Mobile */
  .luxury-panel {
    padding: 16px 12px;
    border-radius: var(--radius-lg);
    gap: 12px;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    overflow: hidden;
  }

  .showroom-panel {
    min-height: auto;
    padding-bottom: 130px; /* Espace impératif pour la barre flottante fixe */
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    overflow: hidden;
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    width: 100%;
    box-sizing: border-box;
    margin-bottom: 4px;
  }

  .header-actions {
    width: 100%;
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .luxury-profitable-quick-btn {
    width: 100%;
    justify-content: center;
    padding: 9px 12px;
    font-size: 0.78rem;
  }

  .luxury-cta-btn-header {
    width: 100%;
    justify-content: center;
    padding: 9px 12px;
    font-size: 0.76rem;
  }

  /* Showroom Grid : Format Liste Horizontale Spacieuse et Aérée sur Mobile */
  .showroom-vehicles-grid {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: none !important;
    overflow-y: visible !important;
    padding-right: 0;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
  }

  .showroom-vehicle-card {
    display: flex;
    flex-direction: row;
    align-items: center;
    padding: 10px 10px;
    gap: 8px;
    min-height: 72px;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    overflow: hidden;
    background: var(--bg-card);
    border: 1.5px solid var(--border-glass);
    border-radius: var(--radius-md);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    cursor: pointer;
    transition: all 0.12s ease;
    -webkit-tap-highlight-color: transparent;
  }
  .showroom-vehicle-card:active {
    transform: scale(0.985);
    background: var(--bg-card-hover);
  }
  .showroom-vehicle-card.selected {
    border-color: var(--accent-teal);
    background: linear-gradient(90deg, rgba(16, 124, 65, 0.09) 0%, var(--bg-card) 100%);
    box-shadow: 0 0 0 1px var(--accent-teal);
  }

  .svc-thumb-box {
    width: 56px;
    min-width: 56px;
    max-width: 56px;
    height: 48px;
    flex-shrink: 0;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-card-subtle);
    border: 1px solid var(--border-glass);
    border-radius: 8px;
    overflow: hidden;
  }
  .svc-image-container {
    width: 100%;
    height: 100%;
    border: none;
    background: transparent;
    border-radius: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .svc-vehicle-img {
    width: 92%;
    height: 92%;
    object-fit: contain;
  }
  .svc-brand-mini-badge {
    position: absolute;
    bottom: 2px;
    left: 2px;
    top: auto;
    width: 13px;
    height: 13px;
    padding: 1px;
    border-radius: 3px;
    background: var(--bg-card);
    border: 1px solid var(--border-glass);
  }

  .svc-info-box {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .svc-title-row {
    display: flex;
    align-items: baseline;
    min-width: 0;
    overflow: hidden;
  }
  .svc-name {
    font-size: 0.88rem;
    font-weight: 800;
    color: var(--text-main);
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    letter-spacing: -0.01em;
  }
  .svc-subtitle-row {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 0.64rem;
    color: var(--text-dimmed);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .svc-finition {
    font-weight: 600;
  }
  .svc-dot-sep {
    opacity: 0.4;
  }
  .svc-powertrain {
    color: var(--text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .svc-roi-indicator {
    margin: 2px 0 0 0;
    min-width: 0;
    overflow: hidden;
  }
  .roi-chip-tag {
    font-size: 0.62rem;
    font-weight: 800;
    padding: 2px 6px;
    border-radius: 4px;
    width: auto;
    max-width: 100%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: inline-flex;
    align-items: center;
    gap: 3px;
  }

  .svc-action-box {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    justify-content: center;
    min-width: 66px;
    gap: 3px;
    border-top: none;
    padding-top: 0;
    margin-top: 0;
  }
  .svc-pricing {
    text-align: right;
  }
  .svc-price-main {
    font-size: 0.86rem;
    font-weight: 800;
    color: var(--text-main);
    line-height: 1.1;
    white-space: nowrap;
  }
  .svc-price-sub {
    font-size: 0.56rem;
    color: var(--accent-cyan);
    font-weight: 600;
    line-height: 1.1;
    white-space: nowrap;
  }

  .svc-check-circle {
    width: 24px;
    height: 24px;
    min-width: 24px;
    border-radius: 9999px;
    border: 1.5px solid var(--border-glass);
    background: var(--bg-card-subtle);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s ease;
  }
  .svc-check-circle.checked {
    background: var(--accent-teal);
    border-color: var(--accent-teal);
    color: #fff;
  }

  /* ── BARRE D'ACTION FLOTTANTE FIXE MOBILE TÉLÉPORTÉE ── */
  .showroom-mobile-floating-bar {
    display: block;
    position: fixed;
    bottom: 66px; /* Ancrée au-dessus de la nav bar mobile (60px + 6px float) */
    left: 12px;
    right: 12px;
    z-index: 99999;
    padding: 8px 12px calc(8px + env(safe-area-inset-bottom)) 12px;
    background: rgba(255, 255, 255, 0.96);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
    border: 1px solid var(--border-glass);
    border-radius: var(--radius-full, 9999px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.18);
  }
  [data-theme="dark"] .showroom-mobile-floating-bar {
    background: rgba(22, 22, 26, 0.96);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.55);
  }

  .smfb-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    max-width: 500px;
    margin: 0 auto;
  }

  .smfb-left {
    display: flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
    padding-left: 4px;
  }

  .smfb-badge {
    min-width: 22px;
    height: 22px;
    border-radius: 9999px;
    background: var(--accent-teal);
    color: #ffffff;
    font-weight: 800;
    font-size: 0.75rem;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 5px;
  }

  .smfb-text {
    font-size: 0.72rem;
    font-weight: 700;
    color: var(--text-main);
  }

  .smfb-actions {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    justify-content: flex-end;
  }

  .smfb-quick-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 7px 10px;
    border-radius: var(--radius-full, 9999px);
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(16, 124, 65, 0.15));
    border: 1px solid rgba(245, 158, 11, 0.4);
    color: var(--text-main);
    font-size: 0.7rem;
    font-weight: 800;
    cursor: pointer;
    white-space: nowrap;
  }

  .smfb-cta-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 8px 16px;
    border-radius: var(--radius-full, 9999px);
    background: linear-gradient(135deg, #059669 0%, #10b981 100%);
    color: #ffffff;
    border: none;
    font-size: 0.78rem;
    font-weight: 800;
    cursor: pointer;
    box-shadow: 0 3px 10px rgba(16, 185, 129, 0.35);
    white-space: nowrap;
    transition: all 0.15s ease;
  }
  .smfb-cta-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    background: var(--bg-card-subtle);
    color: var(--text-dimmed);
    box-shadow: none;
  }
  .smfb-cta-btn:not(:disabled):active {
    transform: scale(0.98);
  }

  /* Podium 1 colonne */
  .luxury-podium-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  /* Cartes alternatives */
  .luxury-alt-card {
    padding: 14px;
    gap: 12px;
  }

  .alt-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .alt-badges-block {
    width: 100%;
  }

  .roi-status-pill {
    width: 100%;
    justify-content: center;
  }

  .alt-metrics-luxury-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .luxury-simulator-btn {
    width: 100%;
    justify-content: center;
    padding: 10px 14px;
    font-size: 0.78rem;
  }

  /* Matrice TCO Mobile : Colonne Véhicule Sticky */
  .matrix-table-wrapper {
    -webkit-overflow-scrolling: touch;
  }

  .luxury-matrix-table th:first-child,
  .luxury-matrix-table td:first-child {
    position: sticky;
    left: 0;
    z-index: 2;
    background: var(--bg-card);
    box-shadow: 2px 0 4px rgba(0, 0, 0, 0.05);
  }
}

@media (max-width: 640px) {
  /* Stepper ultra-compact */
  .workflow-tabs-nav {
    padding: 6px 6px;
    margin: -6px -10px 8px -10px;
    gap: 4px;
  }

  .workflow-step-btn {
    padding: 6px 6px;
    min-width: 85px;
    font-size: 0.65rem;
    gap: 4px;
  }

  .step-label {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  /* Formulaire Config */
  .energy-chips-grid {
    grid-template-columns: 1fr 1fr;
    gap: 6px;
  }

  .energy-chip {
    padding: 8px;
    font-size: 0.72rem;
  }

  .luxury-grid-2 {
    grid-template-columns: 1fr 1fr;
    gap: 6px;
  }

  .luxury-input {
    padding: 7px 10px;
    font-size: 0.78rem;
  }

  /* Résultats */
  .alt-metrics-luxury-grid {
    grid-template-columns: 1fr 1fr;
    gap: 6px;
  }

  .metric-luxury-card {
    padding: 8px 10px;
  }

  .m-lux-label {
    font-size: 0.6rem;
  }

  .m-lux-val {
    font-size: 0.88rem;
  }

  .m-lux-sub {
    font-size: 0.58rem;
  }

  .podium-card {
    padding: 12px;
  }

  .podium-highlight-val {
    font-size: 1.05rem;
  }

  .mobile-return-banner {
    margin-bottom: 8px;
  }
}
</style>
