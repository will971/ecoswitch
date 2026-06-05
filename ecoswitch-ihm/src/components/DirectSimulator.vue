<script setup>
import { ref, watch, onMounted } from 'vue'
import { Zap, ArrowRight, Sparkles, AlertCircle } from '@lucide/vue'
import vehicleEcoSavingsImg from '../assets/vehicle_eco_savings.png'

// Import des sous-composants
import VehicleFormBlock from './simulator/VehicleFormBlock.vue'
import SimulationResults from './simulator/SimulationResults.vue'

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
  DIESEL: 1.74,
  ELECTRIC: 0.25
})

const maxYears = ref(10)
const immediateRepairCost = ref(0)
const loading = ref(false)
const error = ref(null)
const result = ref(null)
const isAdvanced = ref(false)
const showResults = ref(false)

// B2C States
const homeChargingRatio = ref(0.8)
const taxIncome = ref(20000)
const scrapVehicle = ref(false)
const isLeasing = ref(false)
const customLeasingMonthlyPrice = ref(null)

const catalogVehicles = ref([])

const fetchCatalog = async () => {
  try {
    const res = await fetch('/api/v1/vehicules')
    if (res.ok) {
      catalogVehicles.value = await res.json()
    }
  } catch (err) {
    console.error("Erreur de chargement du catalogue pour autocompletion", err)
  }
}

onMounted(async () => {
  await fetchCatalog()
  
  // Appliquer le profil s'il est déjà là au montage
  if (props.userProfile) {
    applyUserProfile(props.userProfile)
  }

  // Vérifier s'il y a un véhicule cible sélectionné depuis le catalogue
  const targetId = localStorage.getItem('eco_target_vehicle_id')
  if (targetId) {
    const v = catalogVehicles.value.find(c => c.id === parseInt(targetId))
    if (v) {
      targetVehicle.value = { ...v }
      localStorage.removeItem('eco_target_vehicle_id')
    } else {
      // S'il n'est pas dans le catalogue en mémoire (pagination), on peut le fetch
      fetch(`/api/v1/vehicules/${targetId}`)
        .then(res => res.json())
        .then(data => {
          targetVehicle.value = { ...data }
          localStorage.removeItem('eco_target_vehicle_id')
        })
        .catch(e => console.error(e))
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

  fuelPrices.value.PETROL = profile.petrolPrice
  fuelPrices.value.DIESEL = profile.dieselPrice
  fuelPrices.value.ELECTRIC = profile.electricPrice
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
    
    // Charger aussi les états B2C si disponibles
    if (newVal.homeChargingRatio !== undefined) homeChargingRatio.value = newVal.homeChargingRatio
    if (newVal.taxIncome !== undefined) taxIncome.value = newVal.taxIncome
    if (newVal.scrapVehicle !== undefined) scrapVehicle.value = newVal.scrapVehicle
    if (newVal.isLeasing !== undefined) isLeasing.value = newVal.isLeasing
    if (newVal.customLeasingMonthlyPrice !== undefined) customLeasingMonthlyPrice.value = newVal.customLeasingMonthlyPrice
    
    if (result.value) {
      showResults.value = true
    }
  }
}
watch(() => props.loadedSimulation, onLoadedSimulationChange, { immediate: true })

const getDefaultInsuranceCost = (fuelType) => {
  switch (fuelType) {
    case 'ELECTRIC': return 600
    case 'HYBRID': return 650
    case 'DIESEL': return 750
    default: return 700
  }
}

const getDefaultMaintenanceCost = (fuelType) => {
  switch (fuelType) {
    case 'ELECTRIC': return 250
    case 'HYBRID': return 350
    case 'DIESEL': return 500
    default: return 450
  }
}

const calculate = async () => {
  // Validation des champs principaux obligatoires
  if (!currentVehicle.value.name || !currentVehicle.value.name.trim()) {
    error.value = "Veuillez saisir le nom du modèle pour le véhicule actuel."
    return
  }
  if (!currentVehicle.value.fuelType) {
    error.value = "Veuillez sélectionner le type d'énergie pour le véhicule actuel."
    return
  }
  if (currentVehicle.value.consumption === null || currentVehicle.value.consumption === '' || currentVehicle.value.consumption <= 0) {
    error.value = "Veuillez saisir une consommation valide pour le véhicule actuel."
    return
  }
  if (currentVehicle.value.resaleValue === null || currentVehicle.value.resaleValue === '' || currentVehicle.value.resaleValue < 0) {
    error.value = "Veuillez saisir une valeur de reprise/revente valide pour le véhicule actuel."
    return
  }
  
  if (currentVehicle.value.annualMileage === null || currentVehicle.value.annualMileage === '' || currentVehicle.value.annualMileage <= 0) {
    error.value = "Veuillez saisir un kilométrage annuel valide."
    return
  }

  if (!targetVehicle.value.name || !targetVehicle.value.name.trim()) {
    error.value = "Veuillez saisir le nom du modèle pour le nouveau véhicule."
    return
  }
  if (!targetVehicle.value.fuelType) {
    error.value = "Veuillez sélectionner le type d'énergie pour le nouveau véhicule."
    return
  }
  if (targetVehicle.value.consumption === null || targetVehicle.value.consumption === '' || targetVehicle.value.consumption <= 0) {
    error.value = "Veuillez saisir une consommation valide pour le nouveau véhicule."
    return
  }
  if (targetVehicle.value.purchasePrice === null || targetVehicle.value.purchasePrice === '' || targetVehicle.value.purchasePrice <= 0) {
    error.value = "Veuillez saisir un prix d'achat valide pour le nouveau véhicule."
    return
  }

  // Fallbacks automatiques sur les coûts d'entretien/assurance si non renseignés (B2C)
  if (currentVehicle.value.insuranceCost === null || currentVehicle.value.insuranceCost === '') {
    currentVehicle.value.insuranceCost = getDefaultInsuranceCost(currentVehicle.value.fuelType)
  }
  if (currentVehicle.value.maintenanceCost === null || currentVehicle.value.maintenanceCost === '') {
    currentVehicle.value.maintenanceCost = getDefaultMaintenanceCost(currentVehicle.value.fuelType)
  }
  if (targetVehicle.value.insuranceCost === null || targetVehicle.value.insuranceCost === '') {
    targetVehicle.value.insuranceCost = getDefaultInsuranceCost(targetVehicle.value.fuelType)
  }
  if (targetVehicle.value.maintenanceCost === null || targetVehicle.value.maintenanceCost === '') {
    targetVehicle.value.maintenanceCost = getDefaultMaintenanceCost(targetVehicle.value.fuelType)
  }

  if (isAdvanced.value) {
    if (maxYears.value === null || maxYears.value === '' || maxYears.value <= 0) {
      error.value = "Veuillez saisir un horizon maximal de simulation valide."
      return
    }
  }

  loading.value = true
  error.value = null
  result.value = null
  showResults.value = true

  try {
    const response = await fetch('/api/v1/comparisons/profitability/direct', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        currentVehicle: currentVehicle.value,
        targetVehicle: targetVehicle.value,
        fuelPricesByType: fuelPrices.value,
        maxYears: maxYears.value,
        immediateRepairCost: immediateRepairCost.value,
        homeChargingRatio: homeChargingRatio.value,
        taxIncome: taxIncome.value,
        scrapVehicle: scrapVehicle.value,
        isLeasing: isLeasing.value,
        customLeasingMonthlyPrice: customLeasingMonthlyPrice.value
      })
    })

    if (!response.ok) {
      const errData = await response.json()
      throw new Error(errData.error || 'Erreur lors du calcul de rentabilité.')
    }

    result.value = await response.json()
  } catch (err) {
    error.value = err.message
    showResults.value = false // retourner à la saisie en cas d'erreur
  } finally {
    loading.value = false
  }
}

const loadAlternative = async (rec) => {
  loading.value = true
  showResults.value = true
  try {
    const response = await fetch(`/api/v1/vehicules/${rec.vehicleId}`)
    if (response.ok) {
      const data = await response.json()
      targetVehicle.value = { ...data }
      await calculate()
    }
  } catch (err) {
    console.error("Erreur de chargement du véhicule recommandé", err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="direct-simulator-container">
    <!-- Hero Banner Premium avec Image & Identité Visuelle -->
    <div class="hero-banner-card mb-5">
      <div class="hero-content text-left">
        <div class="badge badge-teal mb-3 flex items-center gap-1 w-max">
          <Zap size="12" /> <span>Transition Écologique & Économique</span>
        </div>
        <h2 class="hero-title font-heading">Simulateur de Rentabilité à la volée</h2>
        <p class="hero-description">
          Évaluez intelligemment l'impact financier et environnemental d'un changement de véhicule. Comparez le coût total de possession (TCO), estimez les subventions fiscales, et visualisez vos gains de CO₂ en temps réel.
        </p>
      </div>
      <div class="hero-image-wrapper hide-on-mobile">
        <img :src="vehicleEcoSavingsImg" class="hero-brand-image" alt="EcoSwitch Transition" />
      </div>
    </div>

    <!-- Mode saisie des formulaires -->
    <div v-if="!showResults" class="card-glass glow-teal p-5">
      <h3 class="mb-4 text-gradient-teal flex-between">
        <span>Saisie des véhicules</span>
        <Sparkles class="text-teal" size="18" />
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

      <!-- Grille des 2 formulaires côte à côte sur écran large -->
      <div class="forms-grid mb-4">
        <!-- Formulaire Véhicule Actuel -->
        <div class="form-column border-glass-right pr-4">
          <h4 class="text-sm font-semibold text-teal mb-3">Véhicule Actuel / Remplacé</h4>
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
        <div class="form-column pl-4">
          <h4 class="text-sm font-semibold text-cyan mb-3">Nouveau Véhicule Envisagé</h4>
          <VehicleFormBlock
            v-model:vehicle="targetVehicle"
            type="target"
            :isAdvanced="isAdvanced"
            :catalogVehicles="catalogVehicles"
          />
        </div>
      </div>

      <!-- SECTION B2C : Options de Financement et Aides d'État -->
      <div class="general-params p-3 border-glass rounded mb-4 bg-b2c-glass">
        <h4 class="mb-3 text-gradient-teal text-sm font-semibold flex items-center gap-2">
          <Sparkles size="16" />
          <span>⚙️ Options de simulation</span>
        </h4>

        <!-- Mode de financement (Achat comptant ou leasing) -->
        <div class="form-group mb-3 pb-3 border-b border-glass">
          <label class="form-label text-xxs text-dimmed uppercase">Mode de financement du nouveau véhicule</label>
          <div class="flex gap-2 mt-1">
            <button type="button" class="btn btn-secondary w-50 py-1.5 text-xs font-semibold" :class="!isLeasing ? 'active-mode' : ''" @click="isLeasing = false">
              Achat comptant / Crédit
            </button>
            <button type="button" class="btn btn-secondary w-50 py-1.5 text-xs font-semibold" :class="isLeasing ? 'active-mode' : ''" @click="isLeasing = true">
              Leasing (LOA / LLD)
            </button>
          </div>
          
          <div v-if="isLeasing" class="form-group mt-3">
            <label class="form-label text-xs">Loyer mensuel estimé (€/mois)</label>
            <input v-model.number="customLeasingMonthlyPrice" type="number" class="form-control" placeholder="ex: 290 (laisser vide pour estimation auto)" />
          </div>
        </div>

        <!-- Bonus et Conversion (Subventions) -->
        <div class="form-group mb-3 pb-3 border-b border-glass">
          <label class="form-label text-xxs text-dimmed uppercase">Éligibilité aux Subventions de l'État</label>
          
          <div class="form-group mt-2">
            <label class="form-label text-xs">Revenu Fiscal de Référence par part (RFR en €)</label>
            <input v-model.number="taxIncome" type="number" class="form-control" placeholder="ex: 15000" />
            <p class="text-xxs text-dimmed mt-1">Sert à estimer la majoration du bonus écologique (seuil à 15 400 €).</p>
          </div>

          <div class="checkbox-group flex items-center gap-2 mt-2">
            <input v-model="scrapVehicle" type="checkbox" id="scrapCheck" class="pointer" />
            <label for="scrapCheck" class="text-xs text-main pointer-events-none">Mettre à la casse un vieux véhicule thermique</label>
          </div>
        </div>

        <!-- Profil de recharge (Uniquement si véhicule électrique concerné) -->
        <div v-if="currentVehicle.fuelType === 'ELECTRIC' || targetVehicle.fuelType === 'ELECTRIC'" class="form-group p-3 rounded bg-deep-glass border-glass">
          <label class="form-label text-xxs text-dimmed uppercase flex-between">
            <span>🔌 Répartition des recharges électriques</span>
            <span class="font-bold text-teal">{{ (homeChargingRatio * 100).toFixed(0) }}% Domicile</span>
          </label>
          <input v-model.number="homeChargingRatio" type="range" min="0" max="1" step="0.05" class="w-100 accent-teal cursor-pointer" />
          <div class="flex-between text-xxs text-muted mt-1 font-semibold">
            <span>0% (Tout sur Borne Publique Rapide)</span>
            <span>100% (Tout à Domicile)</span>
          </div>
          <p class="text-xxs text-dimmed mt-2 mb-0">
            Ajustez la proportion de recharges effectuées chez vous (au tarif de base de {{ fuelPrices.ELECTRIC }} €/kWh) par rapport aux recharges d'appoint sur borne publique rapide d'autoroute (tarif majoré de 0.65 €/kWh).
          </p>
        </div>
      </div>

      <!-- Paramètres généraux de la simulation (Uniquement en Mode Avancé) -->
      <div v-if="isAdvanced" class="general-params p-3 border-glass rounded mb-4">
        <h4 class="mb-3 text-muted text-sm uppercase">Paramètres globaux</h4>
        <div class="grid-2-fields">
          <div class="form-group">
            <label class="form-label">Kilométrage annuel (km/an)</label>
            <input v-model.number="currentVehicle.annualMileage" type="number" class="form-control" @input="targetVehicle.annualMileage = currentVehicle.annualMileage" required />
          </div>
          <div class="form-group">
            <label class="form-label">Horizon max (ans)</label>
            <input v-model.number="maxYears" type="number" min="1" max="30" class="form-control" required />
          </div>
        </div>

        <h5 class="my-3 text-dimmed text-sm uppercase">Prix des énergies (€/L ou €/kWh)</h5>
        <div class="grid-3-fields">
          <div class="form-group">
            <label class="form-label text-xs">Essence</label>
            <input v-model.number="fuelPrices.PETROL" type="number" step="0.01" class="form-control" />
          </div>
          <div class="form-group">
            <label class="form-label text-xs">Diesel</label>
            <input v-model.number="fuelPrices.DIESEL" type="number" step="0.01" class="form-control" />
          </div>
          <div class="form-group">
            <label class="form-label text-xs">Élec</label>
            <input v-model.number="fuelPrices.ELECTRIC" type="number" step="0.01" class="form-control" />
          </div>
        </div>
      </div>

      <button :disabled="loading" class="btn btn-primary w-100" @click="calculate">
        <span v-if="loading" class="spinner mr-2"><Zap size="18" /></span>
        <span v-else>Calculer la rentabilité</span>
        <ArrowRight size="18" />
      </button>

      <p v-if="error" class="error-msg flex-center mt-3 text-rose">
        <AlertCircle size="18" class="mr-2" /> {{ error }}
      </p>
    </div>

    <!-- Mode résultats de la simulation -->
    <div v-else>
      <div v-if="loading" class="flex-center flex-column py-5 card-glass glow-teal">
        <Zap size="64" class="spinner text-teal mb-3" />
        <h4 class="text-teal font-heading">Analyse en cours...</h4>
        <p class="text-muted">Nous calculons les coûts d'énergie, d'assurance et d'entretien sur les {{ maxYears }} prochaines années...</p>
      </div>

      <SimulationResults
        v-else-if="result"
        :result="result"
        :currentVehicle="currentVehicle"
        :targetVehicle="targetVehicle"
        :fuelPrices="fuelPrices"
        :maxYears="maxYears"
        :immediateRepairCost="immediateRepairCost"
        :isLeasing="isLeasing"
        :currentUser="currentUser"
        @back="showResults = false"
        @load-alternative="loadAlternative"
      />
    </div>
  </div>
</template>

<style scoped>
.forms-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}
.border-glass-right {
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}
@media (max-width: 992px) {
  .forms-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  .border-glass-right {
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    padding-bottom: 24px;
    padding-right: 0 !important;
  }
  .form-column {
    padding-left: 0 !important;
  }
}
.text-teal { color: #10b981; }
.text-rose { color: #f43f5e; }
.text-cyan { color: #22d3ee; }
.text-xxs { font-size: 0.65rem; }
.text-xs { font-size: 0.75rem; }
.text-sm { font-size: 0.875rem; }
.uppercase { text-transform: uppercase; }
.w-100 { width: 100%; }
.w-50 { width: 50%; }
.mr-2 { margin-right: 0.5rem; }
.font-semibold { font-weight: 600; }

.active-mode {
  background: linear-gradient(135deg, hsl(var(--accent-teal) / 0.15) 0%, hsl(var(--accent-cyan) / 0.15) 100%) !important;
  border-color: hsl(var(--accent-cyan) / 0.7) !important;
  color: hsl(var(--accent-cyan)) !important;
  box-shadow: 0 0 10px 0 hsl(var(--accent-cyan) / 0.1);
}

.bg-b2c-glass {
  background: rgba(255, 255, 255, 0.01);
  border: 1px dashed rgba(255, 255, 255, 0.15);
}
.checkbox-group {
  margin-top: 8px;
}
.pointer {
  cursor: pointer;
}
.accent-teal {
  accent-color: #10b981;
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
