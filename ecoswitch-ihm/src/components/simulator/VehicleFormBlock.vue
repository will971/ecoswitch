<script setup>
import { ref, computed, onMounted } from 'vue'
import { CheckCircle2, AlertCircle, Wrench } from '@lucide/vue'

const props = defineProps({
  vehicle: {
    type: Object,
    required: true
  },
  type: {
    type: String, // 'current' | 'target'
    required: true
  },
  isAdvanced: {
    type: Boolean,
    required: true
  },
  catalogVehicles: {
    type: Array,
    default: () => []
  },
  immediateRepairCost: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits([
  'update:vehicle',
  'update:immediateRepairCost',
  'suggestion-selected',
  'annual-mileage-change'
])

// ADEME select states
const brands = ref([])
const models = ref([])
const versions = ref([])
const selectedBrand = ref('')
const selectedModel = ref('')
const selectedVersion = ref(null)

const showSuggestions = ref(false)
const suggestions = ref([])

const localRepairCost = computed({
  get: () => props.immediateRepairCost,
  set: (val) => emit('update:immediateRepairCost', val)
})

const filterSuggestions = () => {
  const query = props.vehicle.name.trim().toLowerCase()
  if (!query) {
    suggestions.value = []
    return
  }
  suggestions.value = props.catalogVehicles
    .filter(v => v.name.toLowerCase().includes(query))
    .slice(0, 5)
}

const getDefaultInsuranceCost = (fuelType) => {
  switch (fuelType) {
    case 'ELECTRIC': return 600
    case 'HYBRID': return 650
    case 'DIESEL': return 750
    default: return 700 // PETROL
  }
}

const getDefaultMaintenanceCost = (fuelType) => {
  switch (fuelType) {
    case 'ELECTRIC': return 250
    case 'HYBRID': return 350
    case 'DIESEL': return 500
    default: return 450 // PETROL
  }
}

const selectSuggestion = (v) => {
  // Remplir les champs du véhicule
  props.vehicle.name = v.name
  props.vehicle.fuelType = v.fuelType
  props.vehicle.consumption = v.consumption
  if (v.annualMileage) props.vehicle.annualMileage = v.annualMileage
  props.vehicle.insuranceCost = v.insuranceCost || getDefaultInsuranceCost(v.fuelType)
  props.vehicle.maintenanceCost = v.maintenanceCost || getDefaultMaintenanceCost(v.fuelType)
  if (props.type === 'current' && v.resaleValue !== undefined) {
    props.vehicle.resaleValue = v.resaleValue
  }
  if (props.type === 'target' && v.purchasePrice !== undefined) {
    props.vehicle.purchasePrice = v.purchasePrice
  }

  suggestions.value = []
  showSuggestions.value = false

  emit('suggestion-selected', props.vehicle)
}

const closeSuggestionsWithDelay = () => {
  setTimeout(() => {
    showSuggestions.value = false
  }, 200)
}

const onNameFocus = () => {
  showSuggestions.value = true
  filterSuggestions()
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

const onMileageInput = () => {
  if (props.type === 'current') {
    emit('annual-mileage-change', props.vehicle.annualMileage)
  }
}

// ADEME Service fetchers
const fetchBrands = async () => {
  try {
    const res = await fetch('/api/v1/ademe/brands')
    if (res.ok) {
      brands.value = await res.json()
    }
  } catch (err) {
    console.error('Erreur de chargement des marques:', err)
  }
}

const fetchModels = async () => {
  models.value = []
  versions.value = []
  selectedModel.value = ''
  selectedVersion.value = null
  
  if (!selectedBrand.value) return

  try {
    const res = await fetch(`/api/v1/ademe/models?brand=${encodeURIComponent(selectedBrand.value)}`)
    if (res.ok) {
      models.value = await res.json()
    }
  } catch (err) {
    console.error('Erreur de chargement des modèles:', err)
  }
}

const fetchVersions = async () => {
  versions.value = []
  selectedVersion.value = null
  
  if (!selectedBrand.value || !selectedModel.value) return

  try {
    const res = await fetch(`/api/v1/ademe/versions?brand=${encodeURIComponent(selectedBrand.value)}&model=${encodeURIComponent(selectedModel.value)}`)
    if (res.ok) {
      versions.value = await res.json()
    }
  } catch (err) {
    console.error('Erreur de chargement des versions:', err)
  }
}

const onVersionChange = () => {
  if (!selectedVersion.value) return

  const v = selectedVersion.value
  props.vehicle.name = `${v.brand} ${v.model} ${v.version}`
  props.vehicle.fuelType = v.fuelType
  props.vehicle.consumption = v.consumption
  
  if (v.annualMileage) props.vehicle.annualMileage = v.annualMileage
  props.vehicle.insuranceCost = v.insuranceCost || getDefaultInsuranceCost(v.fuelType)
  props.vehicle.maintenanceCost = v.maintenanceCost || getDefaultMaintenanceCost(v.fuelType)
  
  if (props.type === 'current' && v.resaleValue !== undefined) {
    props.vehicle.resaleValue = v.resaleValue
  }
  if (props.type === 'target' && v.purchasePrice !== undefined) {
    props.vehicle.purchasePrice = v.purchasePrice
  }

  emit('suggestion-selected', props.vehicle)
}

onMounted(() => {
  fetchBrands()
})
</script>

<template>
  <div class="vehicle-form-block mb-4 p-3 border-glass rounded">
    <h4 v-if="type === 'current'" class="mb-3 text-cyan flex-between">
      <span>Véhicule Actuel (À remplacer)</span>
      <span class="badge badge-amber badge-small">Actuel</span>
    </h4>
    <h4 v-else class="mb-3 text-teal flex-between">
      <span>Nouveau Véhicule (Cible)</span>
      <span class="badge badge-teal badge-small">Cible</span>
    </h4>

    <!-- Sélection ADEME à 3 critères (Remplacement de la plaque d'immatriculation) -->
    <div class="form-group mb-3 pb-3 border-b border-glass">
      <label class="form-label text-xxs text-cyan uppercase mb-2 block font-semibold">⚡ Remplissage rapide par Marque / Modèle / Version</label>
      <div class="grid-3-fields">
        <div class="form-group mb-0">
          <select v-model="selectedBrand" class="form-control form-select text-xs" @change="fetchModels">
            <option value="">Marque</option>
            <option v-for="b in brands" :key="b" :value="b">{{ b }}</option>
          </select>
        </div>
        <div class="form-group mb-0">
          <select v-model="selectedModel" :disabled="!selectedBrand" class="form-control form-select text-xs" @change="fetchVersions">
            <option value="">Modèle</option>
            <option v-for="m in models" :key="m" :value="m">{{ m }}</option>
          </select>
        </div>
        <div class="form-group mb-0">
          <select v-model="selectedVersion" :disabled="!selectedModel" class="form-control form-select text-xs" @change="onVersionChange">
            <option :value="null">Version</option>
            <option v-for="v in versions" :key="v.version" :value="v">{{ v.version }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Nom du modèle -->
    <div class="form-group relative">
      <label class="form-label">Nom du modèle</label>
      <input v-model="vehicle.name" type="text" class="form-control" :placeholder="type === 'current' ? 'ex: Peugeot 208' : 'ex: Tesla Model 3'" required 
             @input="filterSuggestions" 
             @focus="onNameFocus" 
             @blur="closeSuggestionsWithDelay" />
      
      <div v-if="showSuggestions && suggestions.length > 0" class="autocomplete-dropdown card-glass">
        <div v-for="v in suggestions" :key="v.id" class="suggestion-item" @mousedown="selectSuggestion(v)">
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

    <!-- Type d'énergie & Consommation -->
    <div class="grid-2-fields">
      <div class="form-group">
        <label class="form-label">Type d'énergie</label>
        <select v-model="vehicle.fuelType" class="form-control form-select">
          <option value="" disabled>Sélectionnez un carburant</option>
          <option value="PETROL">Essence</option>
          <option value="DIESEL">Diesel</option>
          <option value="HYBRID">Hybride</option>
          <option value="ELECTRIC">Électrique</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Consommation (L ou kWh/100km)</label>
        <input v-model.number="vehicle.consumption" type="number" step="0.1" class="form-control" required />
      </div>
    </div>

    <!-- Mode Avancé : Assurance, Entretien et Reprise/Prix -->
    <div v-if="isAdvanced" class="grid-3-fields mb-3">
      <div class="form-group">
        <label class="form-label">Assurance (€/an)</label>
        <input v-model.number="vehicle.insuranceCost" type="number" class="form-control" required />
      </div>
      <div class="form-group">
        <label class="form-label">Entretien (€/an)</label>
        <input v-model.number="vehicle.maintenanceCost" type="number" class="form-control" required />
      </div>
      <div class="form-group">
        <label class="form-label">{{ type === 'current' ? 'Reprise actuelle (€)' : "Prix d'achat (€)" }}</label>
        <input v-if="type === 'current'" v-model.number="vehicle.resaleValue" type="number" class="form-control" required />
        <input v-else v-model.number="vehicle.purchasePrice" type="number" class="form-control" required />
      </div>
    </div>

    <!-- Mode Simple : Reprise et Kilométrage (Véhicule Actuel) -->
    <div v-else-if="type === 'current'" class="grid-2-fields mb-3">
      <div class="form-group">
        <label class="form-label">Reprise actuelle (€)</label>
        <input v-model.number="vehicle.resaleValue" type="number" class="form-control" required />
      </div>
      <div class="form-group">
        <label class="form-label">Kilométrage annuel (km/an)</label>
        <input v-model.number="vehicle.annualMileage" type="number" class="form-control" @input="onMileageInput" required />
      </div>
    </div>

    <!-- Mode Simple : Prix d'achat uniquement (Véhicule Cible) -->
    <div v-else class="form-group mb-3">
      <label class="form-label">Prix d'achat (€)</label>
      <input v-model.number="vehicle.purchasePrice" type="number" class="form-control" required />
    </div>

    <!-- Frais de réparations immédiats (Uniquement pour le véhicule actuel) -->
    <div v-if="type === 'current'" class="form-group border-t border-glass pt-3">
      <label class="form-label text-rose font-semibold flex-between">
        <span class="flex items-center gap-1"><Wrench size="14" /> Frais de réparations immédiats (€)</span>
        <span class="badge badge-rose badge-small">Frais de garage</span>
      </label>
      <input v-model.number="localRepairCost" type="number" min="0" class="form-control border-rose-focus" placeholder="ex: 3000" />
      <p class="text-xxs text-dimmed mt-1">Saisissez le coût des réparations requises si vous décidez de conserver votre voiture actuelle.</p>
    </div>
  </div>
</template>
