<script setup>
import { ref, watch, onMounted } from 'vue'
import { Zap, HelpCircle, ArrowRight, DollarSign, TrendingUp, Sparkles, CheckCircle2, AlertCircle, FileSpreadsheet, Save, X, Wrench, ChevronRight, ChevronDown, ChevronUp, Search } from '@lucide/vue'
import { apiSaveSimulation } from '../utils/api.js'

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

const registrationPlate = ref('')
const searchingPlate = ref(false)
const plateSuccessMessage = ref(null)
const plateErrorMessage = ref(null)

const currentVehicle = ref({
  name: 'Peugeot 208 Essence',
  purchasePrice: 0,
  fuelType: 'PETROL',
  consumption: 7.2,
  annualMileage: 18000,
  insuranceCost: 750,
  maintenanceCost: 550,
  resaleValue: 9500
})

const targetVehicle = ref({
  name: 'Renault Megane E-Tech',
  purchasePrice: 38000,
  fuelType: 'ELECTRIC',
  consumption: 16.2,
  annualMileage: 18000,
  insuranceCost: 650,
  maintenanceCost: 280,
  resaleValue: 0
})

const fuelPrices = ref({
  PETROL: 1.88,
  DIESEL: 1.74,
  HYBRID: 1.82,
  ELECTRIC: 0.25
})

const maxYears = ref(10)
const immediateRepairCost = ref(0)
const loading = ref(false)
const error = ref(null)
const result = ref(null)
const isAdvanced = ref(false)
const showProjectionsTable = ref(false)

// Autocomplétion du catalogue
const catalogVehicles = ref([])
const showCurrentSuggestions = ref(false)
const showTargetSuggestions = ref(false)
const currentSuggestions = ref([])
const targetSuggestions = ref([])

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

const filterCurrentSuggestions = () => {
  const query = currentVehicle.value.name.trim().toLowerCase()
  if (!query) {
    currentSuggestions.value = []
    return
  }
  currentSuggestions.value = catalogVehicles.value
    .filter(v => v.name.toLowerCase().includes(query))
    .slice(0, 5)
}

const filterTargetSuggestions = () => {
  const query = targetVehicle.value.name.trim().toLowerCase()
  if (!query) {
    targetSuggestions.value = []
    return
  }
  targetSuggestions.value = catalogVehicles.value
    .filter(v => v.name.toLowerCase().includes(query))
    .slice(0, 5)
}

const selectCurrentSuggestion = (v) => {
  currentVehicle.value.name = v.name
  currentVehicle.value.fuelType = v.fuelType
  currentVehicle.value.consumption = v.consumption
  if (v.annualMileage) currentVehicle.value.annualMileage = v.annualMileage
  if (v.insuranceCost) currentVehicle.value.insuranceCost = v.insuranceCost
  if (v.maintenanceCost) currentVehicle.value.maintenanceCost = v.maintenanceCost
  if (v.resaleValue) currentVehicle.value.resaleValue = v.resaleValue
  
  targetVehicle.value.annualMileage = currentVehicle.value.annualMileage

  currentSuggestions.value = []
  showCurrentSuggestions.value = false
}

const selectTargetSuggestion = (v) => {
  targetVehicle.value.name = v.name
  targetVehicle.value.fuelType = v.fuelType
  targetVehicle.value.consumption = v.consumption
  if (v.purchasePrice) targetVehicle.value.purchasePrice = v.purchasePrice
  if (v.insuranceCost) targetVehicle.value.insuranceCost = v.insuranceCost
  if (v.maintenanceCost) targetVehicle.value.maintenanceCost = v.maintenanceCost
  if (v.resaleValue !== undefined) targetVehicle.value.resaleValue = v.resaleValue

  targetSuggestions.value = []
  showTargetSuggestions.value = false
}

const closeCurrentSuggestionsWithDelay = () => {
  setTimeout(() => {
    showCurrentSuggestions.value = false
  }, 200)
}

const closeTargetSuggestionsWithDelay = () => {
  setTimeout(() => {
    showTargetSuggestions.value = false
  }, 200)
}

// Handlers dédiés pour le focus — évite les handlers inline composés (ex: "a = true; b()")
// qui génèrent des arrow functions avec corps de bloc dans une expression groupée,
// un pattern que le parser JavaScriptCore de Safari ne sait pas gérer correctement.
const onCurrentNameFocus = () => {
  showCurrentSuggestions.value = true
  filterCurrentSuggestions()
}

const onTargetNameFocus = () => {
  showTargetSuggestions.value = true
  filterTargetSuggestions()
}

const formatFuelType = (type) => {
  switch (type) {
    case 'PETROL': return 'Essence'
    case 'DIESEL': return 'Diesel'
    case 'HYBRID': return 'Hybride'
    case 'ELECTRIC': return 'Électrique'
    default: return type
  }
}

onMounted(() => {
  fetchCatalog()
})

// Sauvegarde Modal
const showSaveModal = ref(false)
const saveNote = ref('')

// Watcher pour charger une simulation existante
// NOTE: Le callback est extrait en variable nommée pour éviter le pattern `}, {`
// qui déclenche un bug du parser JavaScriptCore de Safari :
// watch(source, (v) => { ... }, { immediate: true })
//                              ↑ Safari voit '{' mais attend ')'
const onLoadedSimulationChange = (newVal) => {
  if (newVal) {
    currentVehicle.value = { ...newVal.currentVehicle }
    targetVehicle.value = { ...newVal.targetVehicle }
    fuelPrices.value = { ...newVal.fuelPricesByType }
    maxYears.value = newVal.maxYears
    immediateRepairCost.value = newVal.immediateRepairCost || 0
    result.value = { ...newVal.result }
  }
}
watch(() => props.loadedSimulation, onLoadedSimulationChange, { immediate: true })

const calculate = async () => {
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
    loading.value = false
  }
}

// Générer les détails cumulatifs année par année
const getYearlyForecast = () => {
  if (!result.value) return []
  
  const forecast = []
  let cumulativeCurrent = immediateRepairCost.value // Démarre avec les réparations !
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

// Exporter en CSV
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

// Sauvegarde dans l'espace personnel (via API — persistance serveur)
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

// Charger un véhicule recommandé depuis l'API H2
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

const searchByPlate = async () => {
  if (!registrationPlate.value || !registrationPlate.value.trim()) return
  searchingPlate.value = true
  plateSuccessMessage.value = null
  plateErrorMessage.value = null
  
  try {
    const formattedPlate = registrationPlate.value.trim().toUpperCase()
    const res = await fetch(`/api/v1/immatriculation/${formattedPlate}`)
    if (!res.ok) {
      const err = await res.json()
      throw new Error(err.error || "Plaque d'immatriculation introuvable.")
    }
    
    const carData = await res.json()
    
    // Remplissage automatique des informations
    currentVehicle.value.name = carData.name
    currentVehicle.value.fuelType = carData.fuelType
    currentVehicle.value.consumption = carData.consumption
    
    if (carData.annualMileage) currentVehicle.value.annualMileage = carData.annualMileage
    if (carData.insuranceCost) currentVehicle.value.insuranceCost = carData.insuranceCost
    if (carData.maintenanceCost) currentVehicle.value.maintenanceCost = carData.maintenanceCost
    if (carData.resaleValue) currentVehicle.value.resaleValue = carData.resaleValue
    
    // Mettre à jour aussi le kilométrage cible par défaut
    targetVehicle.value.annualMileage = currentVehicle.value.annualMileage
    
    let sourceLabel = 'Base locale de secours'
    if (carData.source === 'OSCARO') {
      sourceLabel = 'Oscaro API'
    }
    plateSuccessMessage.value = `Véhicule identifié avec succès via ${sourceLabel} !`
    
    // Lancer automatiquement le calcul de rentabilité pour fluidifier l'expérience !
    calculate()
  } catch (err) {
    plateErrorMessage.value = err.message
  } finally {
    searchingPlate.value = false
  }
}
</script>

<template>
  <div class="direct-simulator-container">
    <div class="header-section text-center mb-5">
      <h2 class="text-gradient mb-2">Simulateur de Rentabilité à la volée</h2>
      <p class="text-muted">Comparez instantanément le coût total de possession de deux véhicules pour identifier le point d'équilibre financier.</p>
    </div>

    <div class="grid-cols-2">
      <!-- Section Formulaire -->
      <section class="card-glass glow-teal">
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
        <div class="vehicle-form-block mb-4 p-3 border-glass rounded">
          <h4 class="mb-3 text-cyan flex-between">
            <span>Véhicule Actuel (À remplacer)</span>
            <span class="badge badge-amber badge-small">Actuel</span>
          </h4>

          <!-- Recherche par Plaque d'Immatriculation -->
          <div class="form-group mb-3 pb-3 border-b border-glass">
            <label class="form-label text-xxs text-dimmed uppercase">⚡ Remplissage rapide par plaque d'immatriculation</label>
            <div class="flex gap-2">
              <input v-model="registrationPlate" type="text" class="form-control text-center font-bold tracking-wider border-cyan-focus text-cyan" placeholder="ex: EZ-999-ZZ" @keyup.enter="searchByPlate" />
              <button type="button" :disabled="searchingPlate || !registrationPlate" class="btn btn-secondary flex-center gap-1 py-2 px-3 text-xs" @click="searchByPlate">
                <span v-if="searchingPlate" class="spinner-small mr-1"></span>
                <span v-else class="flex items-center gap-1"><Search size="14" /> Rechercher</span>
              </button>
            </div>
            <p v-if="plateSuccessMessage" class="text-xxs text-teal mt-1 font-semibold flex items-center gap-1">
              <CheckCircle2 size="12" /> {{ plateSuccessMessage }}
            </p>
            <p v-if="plateErrorMessage" class="text-xxs text-rose mt-1 font-semibold flex items-center gap-1">
              <AlertCircle size="12" /> {{ plateErrorMessage }}
            </p>
          </div>

          <div class="form-group relative">
            <label class="form-label">Nom du modèle</label>
            <input v-model="currentVehicle.name" type="text" class="form-control" placeholder="ex: Peugeot 208" required 
                   @input="filterCurrentSuggestions" 
                   @focus="onCurrentNameFocus" 
                   @blur="closeCurrentSuggestionsWithDelay" />
            
            <div v-if="showCurrentSuggestions && currentSuggestions.length > 0" class="autocomplete-dropdown card-glass">
              <div v-for="v in currentSuggestions" :key="v.id" class="suggestion-item" @mousedown="selectCurrentSuggestion(v)">
                <div class="suggestion-info">
                  <span class="suggestion-name">{{ v.name }}</span>
                  <span class="suggestion-meta">
                    {{ formatFuelType(v.fuelType) }} &middot; {{ v.consumption }} {{ v.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km
                  </span>
                </div>
                <span class="badge badge-teal badge-small">Base</span>
              </div>
            </div>
          </div>
          <div class="grid-2-fields">
            <div class="form-group">
              <label class="form-label">Type d'énergie</label>
              <select v-model="currentVehicle.fuelType" class="form-control form-select">
                <option value="PETROL">Essence</option>
                <option value="DIESEL">Diesel</option>
                <option value="HYBRID">Hybride</option>
                <option value="ELECTRIC">Électrique</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Consommation (L ou kWh/100km)</label>
              <input v-model.number="currentVehicle.consumption" type="number" step="0.1" class="form-control" required />
            </div>
          </div>

          <!-- Mode Avancé : Assurance et Entretien -->
          <div v-if="isAdvanced" class="grid-3-fields mb-3">
            <div class="form-group">
              <label class="form-label">Assurance (€/an)</label>
              <input v-model.number="currentVehicle.insuranceCost" type="number" class="form-control" required />
            </div>
            <div class="form-group">
              <label class="form-label">Entretien (€/an)</label>
              <input v-model.number="currentVehicle.maintenanceCost" type="number" class="form-control" required />
            </div>
            <div class="form-group">
              <label class="form-label">Reprise actuelle (€)</label>
              <input v-model.number="currentVehicle.resaleValue" type="number" class="form-control" required />
            </div>
          </div>

          <!-- Mode Simple : Reprise et Kilométrage -->
          <div v-else class="grid-2-fields mb-3">
            <div class="form-group">
              <label class="form-label">Reprise actuelle (€)</label>
              <input v-model.number="currentVehicle.resaleValue" type="number" class="form-control" required />
            </div>
            <div class="form-group">
              <label class="form-label">Kilométrage annuel (km/an)</label>
              <input v-model.number="currentVehicle.annualMileage" type="number" class="form-control" @input="targetVehicle.annualMileage = currentVehicle.annualMileage" required />
            </div>
          </div>

          <!-- AJOUT : Frais de réparations immédiats -->
          <div class="form-group border-t border-glass pt-3">
            <label class="form-label text-rose font-semibold flex-between">
              <span class="flex items-center gap-1"><Wrench size="14" /> Frais de réparations immédiats (€)</span>
              <span class="badge badge-rose badge-small">Frais de garage</span>
            </label>
            <input v-model.number="immediateRepairCost" type="number" min="0" class="form-control border-rose-focus" placeholder="ex: 3000" />
            <p class="text-xxs text-dimmed mt-1">Saisissez le coût des réparations requises si vous décidez de conserver votre voiture actuelle.</p>
          </div>
        </div>

        <!-- Formulaire Véhicule Cible -->
        <div class="vehicle-form-block mb-4 p-3 border-glass rounded">
          <h4 class="mb-3 text-teal flex-between">
            <span>Nouveau Véhicule (Cible)</span>
            <span class="badge badge-teal badge-small">Cible</span>
          </h4>
          <div class="form-group relative">
            <label class="form-label">Nom du modèle</label>
            <input v-model="targetVehicle.name" type="text" class="form-control" placeholder="ex: Tesla Model 3" required 
                   @input="filterTargetSuggestions" 
                   @focus="onTargetNameFocus" 
                   @blur="closeTargetSuggestionsWithDelay" />
            
            <div v-if="showTargetSuggestions && targetSuggestions.length > 0" class="autocomplete-dropdown card-glass">
              <div v-for="v in targetSuggestions" :key="v.id" class="suggestion-item" @mousedown="selectTargetSuggestion(v)">
                <div class="suggestion-info">
                  <span class="suggestion-name">{{ v.name }}</span>
                  <span class="suggestion-meta">
                    {{ formatFuelType(v.fuelType) }} &middot; {{ v.consumption }} {{ v.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km
                  </span>
                </div>
                <span class="badge badge-teal badge-small">Base</span>
              </div>
            </div>
          </div>
          <div class="grid-2-fields">
            <div class="form-group">
              <label class="form-label">Type d'énergie</label>
              <select v-model="targetVehicle.fuelType" class="form-control form-select">
                <option value="PETROL">Essence</option>
                <option value="DIESEL">Diesel</option>
                <option value="HYBRID">Hybride</option>
                <option value="ELECTRIC">Électrique</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Consommation (L ou kWh/100km)</label>
              <input v-model.number="targetVehicle.consumption" type="number" step="0.1" class="form-control" required />
            </div>
          </div>

          <!-- Mode Avancé : Prix, Assurance, Entretien -->
          <div v-if="isAdvanced" class="grid-3-fields">
            <div class="form-group">
              <label class="form-label">Prix d'achat (€)</label>
              <input v-model.number="targetVehicle.purchasePrice" type="number" class="form-control" required />
            </div>
            <div class="form-group">
              <label class="form-label">Assurance (€/an)</label>
              <input v-model.number="targetVehicle.insuranceCost" type="number" class="form-control" required />
            </div>
            <div class="form-group">
              <label class="form-label">Entretien (€/an)</label>
              <input v-model.number="targetVehicle.maintenanceCost" type="number" class="form-control" required />
            </div>
          </div>

          <!-- Mode Simple : Prix d'achat uniquement -->
          <div v-else class="form-group">
            <label class="form-label">Prix d'achat (€)</label>
            <input v-model.number="targetVehicle.purchasePrice" type="number" class="form-control" required />
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
      <section class="card-glass flex flex-column justify-between">
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

          <!-- AJOUT : Rapport d'Arbitrage Réparer vs Remplacer (si frais de garage) -->
          <div v-if="immediateRepairCost > 0" class="arbitrage-card p-3 border-glass rounded mb-4 bg-card-glass">
            <h4 class="text-sm font-heading text-rose mb-3 flex items-center gap-1">
              <Wrench size="16" /> Arbitrage Financier : Réparer vs Remplacer
            </h4>
            <div class="grid-cols-2 text-xs gap-3">
              <div class="p-2 border-glass rounded bg-deep-glass">
                <div class="text-dimmed uppercase text-xxs">Scénario Réparer (Upfront)</div>
                <div class="font-heading text-md mt-1 text-rose font-bold">{{ formatCurrency(immediateRepairCost) }}</div>
              </div>
              <div class="p-2 border-glass rounded bg-deep-glass">
                <div class="text-dimmed uppercase text-xxs">Scénario Remplacer (Upfront Net)</div>
                <div class="font-heading text-md mt-1 text-cyan font-bold">{{ formatCurrency(result.switchInvestment) }}</div>
              </div>
            </div>
            <div class="mt-3 border-t border-glass pt-2 text-xxs text-dimmed flex-between">
              <span>Effort de trésorerie net initial pour le remplacement :</span>
              <span class="font-semibold" :class="result.switchInvestment - immediateRepairCost > 0 ? 'text-rose' : 'text-teal'">
                {{ formatCurrency(result.switchInvestment - immediateRepairCost) }}
              </span>
            </div>

            <!-- Verdict dynamique intelligent -->
            <div class="mt-3 p-2 bg-deep-glass rounded text-xxs text-muted border-glass">
              <strong class="text-gradient">Verdict de l'Assistant : </strong>
              <span v-if="result.breakEvenYear && result.breakEvenYear <= 3">
                Il est fortement recommandé de **remplacer le véhicule**. Bien que le remplacement exige un effort financier net de {{ formatCurrency(result.switchInvestment - immediateRepairCost) }}, les économies d'énergie substantielles amortiront ce surcoût en seulement **{{ result.breakEvenYear }} ans**. Réparer une carrosserie abîmée à hauteur de {{ formatCurrency(immediateRepairCost) }} sur un modèle ancien n'est pas viable.
              </span>
              <span v-else-if="result.breakEvenYear && result.breakEvenYear <= 7">
                Le **remplacement est pertinent sur le moyen terme** (seuil à **{{ result.breakEvenYear }} ans**). La facture de garage de {{ formatCurrency(immediateRepairCost) }} représente une trop grande partie de la valeur de revente de votre véhicule actuel ({{ formatCurrency(currentVehicle.resaleValue) }}). Il est conseillé de sauter le pas.
              </span>
              <span v-else-if="result.breakEvenYear">
                Le remplacement s'amortit lentement (seuil à **{{ result.breakEvenYear }} ans**). Si votre trésorerie est limitée, la **réparation à {{ formatCurrency(immediateRepairCost) }} est envisageable**, bien que le changement reste rentable sur le très long terme.
              </span>
              <span v-else>
                Il est plus raisonnable de **faire réparer votre véhicule actuel**. L'achat du nouveau modèle exige un effort financier trop important par rapport aux économies d'énergie réelles obtenues.
              </span>
            </div>
          </div>

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

          <!-- AJOUT : Moteur de Recommandations Intelligentes du Catalogue -->
          <div v-if="result.recommendations && result.recommendations.length > 0" class="recommendations-container p-3 border-glass rounded mb-4 bg-card-glass glow-teal">
            <h4 class="text-xs font-heading text-teal mb-3 flex items-center gap-1">
              <Sparkles size="14" /> 💡 Recommandations de notre Assistant SaaS
            </h4>
            <p class="text-xxs text-dimmed mb-3">Voici les véhicules du catalogue les plus rentables par rapport à votre voiture actuelle :</p>
            
            <div class="rec-grid flex flex-column gap-2">
              <div v-for="rec in result.recommendations" :key="rec.vehicleId" 
                   class="rec-item p-2 border-glass rounded flex-between bg-deep-glass hover-border-cyan transition"
                   @click="loadAlternative(rec)">
                <div>
                  <div class="text-xxs font-semibold text-gradient">{{ rec.vehicleName }}</div>
                  <div class="text-xxs text-dimmed">
                    Économie : <strong class="text-teal">{{ formatCurrency(rec.annualSavings) }}/an</strong> &middot; 
                    Achat : {{ formatCurrency(rec.switchInvestment) }}
                  </div>
                </div>
                <button class="btn btn-secondary btn-small text-xxs flex-center py-1 px-2">
                  <span>Charger</span>
                  <ChevronRight size="12" />
                </button>
              </div>
            </div>
          </div>

          <!-- Bouton Accordéon pour le Tableau des Projections -->
          <div class="mt-4 border-t border-glass pt-3 flex-center">
            <button type="button" class="btn btn-secondary w-100 flex-center gap-2 text-xxs font-semibold" @click="showProjectionsTable = !showProjectionsTable">
              <span>{{ showProjectionsTable ? 'Masquer le tableau des projections' : 'Afficher le tableau des projections détaillées' }}</span>
              <component :is="showProjectionsTable ? ChevronUp : ChevronDown" size="14" class="text-teal" />
            </button>
          </div>

          <!-- Projections année par année -->
          <div v-if="showProjectionsTable" class="table-container mt-3">
            <table class="table-glass">
              <thead>
                <tr>
                  <th>Année</th>
                  <th>Coût Actuel (Réparé)</th>
                  <th>Coût Cible</th>
                  <th>Bilan</th>
                  <th>État</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in getYearlyForecast()" :key="row.year">
                  <td>{{ row.year }}</td>
                  <td>{{ formatCurrency(row.currentCost) }}</td>
                  <td>{{ formatCurrency(row.targetCost) }}</td>
                  <td :class="row.difference <= 0 ? 'text-teal' : 'text-rose'">
                    {{ row.difference <= 0 ? '-' : '+' }}{{ formatCurrency(Math.abs(row.difference)) }}
                  </td>
                  <td>
                    <span class="badge" :class="row.isProfitable ? 'badge-teal' : 'badge-rose'">
                      {{ row.isProfitable ? 'Rentable' : 'Déficit' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
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
  </div>
</template>

<style scoped>
.grid-2-fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.grid-3-fields {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.grid-4-fields {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
@media (max-width: 576px) {
  .grid-2-fields, .grid-3-fields, .grid-4-fields {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
.vehicle-form-block {
  background: rgba(255, 255, 255, 0.02);
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
@media (max-width: 576px) {
  .stats-grid {
    grid-template-columns: 1fr;
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
.border-rose-focus:focus {
  border-color: hsl(var(--accent-rose)) !important;
  box-shadow: 0 0 0 3px rgba(225, 29, 72, 0.2) !important;
}
.text-teal { color: hsl(var(--accent-teal)); }
.text-rose { color: hsl(var(--accent-rose)); }
.text-cyan { color: hsl(var(--accent-cyan)); }
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
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.px-2 { padding-left: 8px; padding-right: 8px; }

/* Arbitrage & recommendations classes */
.arbitrage-card {
  border-color: rgba(225, 29, 72, 0.25);
  background: rgba(225, 29, 72, 0.02);
}
.bg-deep-glass {
  background: rgba(0, 0, 0, 0.2);
}
.text-md { font-size: 1rem; }
.font-bold { font-weight: 700; }
.rec-grid {
  display: flex;
  flex-direction: column;
}
.rec-item {
  cursor: pointer;
  background: rgba(255, 255, 255, 0.01);
  transition: all 0.2s ease;
}
.rec-item:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: hsl(var(--accent-cyan) / 0.4);
}
.hover-border-cyan:hover {
  border-color: hsl(var(--accent-cyan));
}
.transition {
  transition: all 0.2s ease;
}

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

/* Autocomplete Suggestion Styles */
.relative {
  position: relative;
}
.autocomplete-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 50;
  max-height: 250px;
  overflow-y: auto;
  margin-top: 4px;
  background: hsl(var(--bg-deep) / 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5);
  padding: 4px;
}
.suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.suggestion-item:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.05);
}
.suggestion-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
}
.suggestion-name {
  font-size: 0.8rem;
  font-weight: 600;
  color: hsl(var(--text-main));
}
.suggestion-meta {
  font-size: 0.65rem;
  color: hsl(var(--text-muted));
  margin-top: 2px;
}
</style>
