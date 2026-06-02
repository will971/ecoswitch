<script setup>
import { ref, watch, onMounted } from 'vue'
import { Zap, HelpCircle, ArrowRight, ArrowLeft, DollarSign, TrendingUp, Sparkles, CheckCircle2, AlertCircle, FileSpreadsheet, Save, X, Share2 } from '@lucide/vue'
import vehicleEcoSavingsImg from '../assets/vehicle_eco_savings.png'
import { apiSaveSimulation } from '../utils/api.js'

// Import des sous-composants
import VehicleFormBlock from './simulator/VehicleFormBlock.vue'
import ArbitrageCard from './simulator/ArbitrageCard.vue'
import AdvisorRecommendations from './simulator/AdvisorRecommendations.vue'
import ProjectionsTable from './simulator/ProjectionsTable.vue'
import CarbonFootprintCard from './simulator/CarbonFootprintCard.vue'
import LeasingCard from './simulator/LeasingCard.vue'
import ShareModal from './simulator/ShareModal.vue'

const props = defineProps({
  loadedSimulation: {
    type: Object,
    default: null
  },
  currentUser: {
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
const activeMobileView = ref('form') // form or results

// B2C States
const homeChargingRatio = ref(0.8)
const taxIncome = ref(20000)
const scrapVehicle = ref(false)
const isLeasing = ref(false)
const customLeasingMonthlyPrice = ref(null)
const showShareModal = ref(false)

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

onMounted(() => {
  fetchCatalog()
})

const onCurrentVehicleSelected = (v) => {
  targetVehicle.value.annualMileage = v.annualMileage
}

const onAnnualMileageChange = (val) => {
  targetVehicle.value.annualMileage = val
}

const showSaveModal = ref(false)
const saveNote = ref('')

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
    activeMobileView.value = 'results'
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const getYearlyForecast = () => {
  if (!result.value) return []
  
  const forecast = []
  let cumulativeCurrent = immediateRepairCost.value
  let cumulativeTarget = result.value.switchInvestment

  for (let year = 1; year <= maxYears.value; year++) {
    cumulativeCurrent += result.value.currentAnnualCost
    cumulativeTarget += result.value.targetAnnualCost
    const diff = cumulativeTarget - cumulativeCurrent
    const isProfitable = diff <= 0

    forecast.push({
      year,
      currentCost: cumulativeCurrent,
      targetCost: cumulativeTarget,
      difference: diff,
      isProfitable
    })
  }
  return forecast
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
}

const exportToCSV = () => {
  if (!result.value) return
  
  const yearly = getYearlyForecast()
  let csvContent = "data:text/csv;charset=utf-8,"
    + "Annee,Cout Actuel Cumule (avec reparations) (EUR),Cout Cible Cumule (EUR),Bilan Net Cumule (EUR),Etat Rentabilite\n"

  yearly.forEach(row => {
    csvContent += `${row.year},${row.currentCost.toFixed(2)},${row.targetCost.toFixed(2)},${row.difference.toFixed(2)},${row.isProfitable ? 'Rentable' : 'Deficit'}\n`
  })

  const encodedUri = encodeURI(csvContent)
  const link = document.createElement("a")
  link.setAttribute("href", encodedUri)
  const currentName = currentVehicle.value.name.replace(/\s+/g, '_')
  const targetName = targetVehicle.value.name.replace(/\s+/g, '_')
  link.setAttribute("download", `Simulation_Rentabilite_${currentName}_vs_${targetName}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const triggerSave = () => {
  if (!props.currentUser) {
    alert("Veuillez d'abord vous connecter pour sauvegarder vos simulations.")
    return
  }
  showSaveModal.value = true
}

const confirmSave = async () => {
  const simData = {
    currentVehicle:    { ...currentVehicle.value },
    targetVehicle:     { ...targetVehicle.value },
    fuelPricesByType:  { ...fuelPrices.value },
    maxYears:          maxYears.value,
    immediateRepairCost: immediateRepairCost.value,
    homeChargingRatio: homeChargingRatio.value,
    taxIncome:         taxIncome.value,
    scrapVehicle:      scrapVehicle.value,
    isLeasing:         isLeasing.value,
    customLeasingMonthlyPrice: customLeasingMonthlyPrice.value,
    result:            { ...result.value },
    note:              saveNote.value
  }
  const simName = `${currentVehicle.value.name} vs ${targetVehicle.value.name}`

  try {
    await apiSaveSimulation(simName, simData)
    showSaveModal.value = false
    saveNote.value = ''
    alert('Simulation enregistrée avec succès dans votre espace !')
  } catch (err) {
    if (err.message === 'SESSION_EXPIRED') {
      alert('Votre session a expiré. Veuillez vous reconnecter.')
      localStorage.removeItem('saas_user')
      localStorage.removeItem('saas_token')
      window.location.reload()
    } else {
      alert('Erreur lors de la sauvegarde : ' + err.message)
    }
  }
}

const loadAlternative = async (rec) => {
  loading.value = true
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

    <div class="grid-cols-2">
      <!-- Section Formulaire -->
      <section class="card-glass glow-teal" :class="{ 'mobile-hidden': activeMobileView === 'results' }">
        <!-- Bouton Aller aux résultats sur Mobile uniquement s'il y a un résultat déjà calculé -->
        <div v-if="result" class="mobile-next-btn-container hide-on-desktop mb-3">
          <button class="btn btn-secondary btn-small w-100 flex-center gap-1 border-teal" @click="activeMobileView = 'results'">
            <span>Voir les résultats calculés</span>
            <ArrowRight size="14" class="text-teal" />
          </button>
        </div>
        <h3 class="mb-3 text-gradient-teal flex-between">
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

        <!-- Formulaire Véhicule Actuel -->
        <VehicleFormBlock
          v-model:vehicle="currentVehicle"
          v-model:immediateRepairCost="immediateRepairCost"
          type="current"
          :isAdvanced="isAdvanced"
          :catalogVehicles="catalogVehicles"
          @annual-mileage-change="onAnnualMileageChange"
          @suggestion-selected="onCurrentVehicleSelected"
        />

        <!-- Formulaire Véhicule Cible -->
        <VehicleFormBlock
          v-model:vehicle="targetVehicle"
          type="target"
          :isAdvanced="isAdvanced"
          :catalogVehicles="catalogVehicles"
        />

        <!-- SECTION B2C : Options de Financement et Aides d'État -->
        <div class="general-params p-3 border-glass rounded mb-4 bg-b2c-glass">
          <h4 class="mb-3 text-gradient-teal text-sm font-semibold flex items-center gap-2">
            <Sparkles size="16" />
            <span>⚙️ Options Grand Public (B2C)</span>
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

        <button :disabled="loading" class="btn btn-primary w-100" @click="calculate">
          <span v-if="loading" class="spinner"><Zap size="18" /></span>
          <span v-else>Calculer la rentabilité</span>
          <ArrowRight size="18" />
        </button>

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
        <div v-if="!result && !loading" class="flex-center flex-column h-100 text-center text-dimmed py-5">
          <HelpCircle size="64" class="mb-3 text-cyan opacity-40" />
          <h4 class="mb-2 text-muted">Prêt pour la simulation</h4>
          <p class="max-w-sm">Remplissez les détails des véhicules à gauche et cliquez sur le bouton de calcul pour voir l'analyse financière complète.</p>
        </div>

        <div v-if="loading" class="flex-center flex-column h-100 py-5">
          <Zap size="64" class="spinner text-teal mb-3" />
          <h4 class="text-teal">Analyse en cours...</h4>
          <p class="text-muted">Nous calculons les coûts d'énergie, d'assurance et d'entretien sur les {{ maxYears }} prochaines années...</p>
        </div>

        <div v-if="result" class="results-layout">
          <div class="flex-between mb-4">
            <h3 class="text-gradient">Résultats de la Simulation</h3>
            <div class="actions flex gap-2">
              <button class="btn btn-secondary btn-small flex-center gap-1 glow-teal" @click="showShareModal = true" title="Partager le bilan">
                <Share2 size="14" class="text-teal" />
                <span>Partager</span>
              </button>
              <button class="btn btn-secondary btn-small flex-center gap-1 glow-teal" @click="exportToCSV" title="Exporter en CSV">
                <FileSpreadsheet size="14" class="text-teal" />
                <span>Exporter</span>
              </button>
              <button class="btn btn-secondary btn-small flex-center gap-1 glow-teal" @click="triggerSave" title="Enregistrer la simulation">
                <Save size="14" class="text-cyan" />
                <span>Enregistrer</span>
              </button>
            </div>
          </div>

          <!-- Bilan Écologique CO2 -->
          <CarbonFootprintCard :result="result" />

          <!-- Comparatif Leasing LOA/LLD (Si activé) -->
          <LeasingCard v-if="isLeasing" :result="result" />

          <!-- Arbitrage Financier (Si frais de réparation) -->
          <ArbitrageCard
            v-if="immediateRepairCost > 0"
            :result="result"
            :immediateRepairCost="immediateRepairCost"
            :currentVehicle="currentVehicle"
          />

          <!-- Badge Rentabilité Premium -->
          <div class="profitability-status-banner p-4 rounded mb-4 flex-center flex-column text-center"
               :class="result.breakEvenYear ? 'bg-success-glass border-teal' : 'bg-warning-glass border-amber'">
            <div v-if="result.breakEvenYear" class="flex-center flex-column">
              <CheckCircle2 size="40" class="text-teal mb-2" />
              <h4 class="text-teal font-heading text-lg">Changement Rentable !</h4>
              <p class="text-muted text-sm mt-1">Vous commencerez à économiser de l'argent après seulement</p>
              <div class="break-even-number font-heading text-3xl text-teal mt-2">
                {{ result.breakEvenYear }} {{ result.breakEvenYear > 1 ? 'ans' : 'an' }}
              </div>
            </div>
            <div v-else class="flex-center flex-column">
              <AlertCircle size="40" class="text-rose mb-2" />
              <h4 class="text-rose font-heading text-lg">Non rentable sur {{ maxYears }} ans</h4>
              <p class="text-muted text-sm mt-1">Le coût cumulé du nouveau véhicule reste supérieur à l'actuel (avec réparations) sur cette période.</p>
            </div>
          </div>

          <!-- Grid Statistiques Clés -->
          <div class="stats-grid mb-4">
            <div class="stat-card p-3 border-glass rounded bg-card-glass text-center">
              <DollarSign class="text-cyan mb-1" size="20" />
              <div class="text-xs text-dimmed uppercase">Économie Annuelle</div>
              <div class="font-heading text-xl mt-1" :class="result.annualSavings > 0 ? 'text-teal' : 'text-rose'">
                {{ formatCurrency(result.annualSavings) }} / an
              </div>
            </div>

            <div class="stat-card p-3 border-glass rounded bg-card-glass text-center">
              <DollarSign class="text-rose mb-1" size="20" />
              <div class="text-xs text-dimmed uppercase">Investissement Transition</div>
              <div class="font-heading text-xl mt-1 text-rose">
                {{ formatCurrency(result.switchInvestment) }}
              </div>
              <div v-if="result.totalSubsidies > 0" class="text-xxs text-teal mt-1">
                (Aides déduites : -{{ formatCurrency(result.totalSubsidies) }})
              </div>
            </div>

            <div class="stat-card p-3 border-glass rounded bg-card-glass text-center">
              <TrendingUp class="text-teal mb-1" size="20" />
              <div class="text-xs text-dimmed uppercase">Bilan à {{ maxYears }} ans</div>
              <div class="font-heading text-xl mt-1" :class="result.totalCostDeltaAtHorizon <= 0 ? 'text-teal' : 'text-rose'">
                {{ result.totalCostDeltaAtHorizon <= 0 ? 'Gain de ' : 'Perte de ' }}
                {{ formatCurrency(Math.abs(result.totalCostDeltaAtHorizon)) }}
              </div>
            </div>
          </div>

          <!-- Recommandations Intelligentes -->
          <AdvisorRecommendations
            v-if="result.recommendations && result.recommendations.length > 0"
            :recommendations="result.recommendations"
            @load-alternative="loadAlternative"
          />

          <!-- Tableau des Projections -->
          <ProjectionsTable :yearlyForecast="getYearlyForecast()" />
        </div>
      </section>
    </div>

    <!-- Modal d'enregistrement des simulations -->
    <div v-if="showSaveModal" class="auth-modal-overlay flex-center">
      <div class="card-glass glow-teal auth-modal-card p-4 relative max-w-md w-100">
        <button class="absolute top-4 right-4 text-dimmed hover-text-main" @click="showSaveModal = false">
          <X size="20" />
        </button>
        <h3 class="text-gradient mb-3">Enregistrer la Simulation</h3>
        <p class="text-xs text-muted mb-4">Ajoutez une note pour retrouver facilement cette simulation dans votre espace.</p>
        
        <div class="form-group mb-4">
          <label class="form-label">Note / Mémo descriptif</label>
          <input v-model="saveNote" type="text" class="form-control" placeholder="ex: Projet Tesla été 2026, km augmenté" required />
        </div>

        <div class="flex-between gap-3">
          <button class="btn btn-secondary w-50" @click="showSaveModal = false">Annuler</button>
          <button class="btn btn-primary w-50" @click="confirmSave">Enregistrer</button>
        </div>
      </div>
    </div>

    <!-- Modal de Partage Social -->
    <ShareModal
      :show="showShareModal"
      :currentVehicle="currentVehicle"
      :targetVehicle="targetVehicle"
      :result="result"
      @close="showShareModal = false"
    />
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
.stat-card {
  background: rgba(255, 255, 255, 0.03);
}
.bg-success-glass {
  background: rgba(16, 185, 129, 0.08);
}
.bg-warning-glass {
  background: rgba(244, 63, 94, 0.08);
}
.border-teal {
  border: 1px solid rgba(16, 185, 129, 0.3);
}
.border-amber {
  border: 1px solid rgba(244, 63, 94, 0.3);
}
.text-teal { color: #10b981; }
.text-rose { color: #f43f5e; }
.text-cyan { color: #22d3ee; }
.text-xxs { font-size: 0.65rem; }
.text-xs { font-size: 0.75rem; }
.text-sm { font-size: 0.875rem; }
.text-lg { font-size: 1.125rem; }
.text-xl { font-size: 1.25rem; }
.text-3xl { font-size: 1.875rem; }
.uppercase { text-transform: uppercase; }
.w-100 { width: 100%; }
.w-50 { width: 50%; }
.h-100 { height: 100%; }
.opacity-40 { opacity: 0.4; }
.max-w-sm { max-width: 24rem; }
.py-5 { padding-top: 3rem; padding-bottom: 3rem; }
.mr-2 { margin-right: 0.5rem; }
.text-center { text-align: center; }
.font-semibold { font-weight: 600; }
.border-t { border-top: 1px solid; }
.pt-3 { padding-top: 12px; }

/* Modal overlay styling */
.auth-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 1000;
}
.auth-modal-card {
  z-index: 1001;
  background: hsl(var(--bg-deep) / 0.9);
}
.hover-text-main:hover {
  color: hsl(var(--text-main));
}
.absolute { position: absolute; }
.top-4 { top: 1rem; }
.right-4 { right: 1rem; }
.max-w-md { max-width: 28rem; }
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
