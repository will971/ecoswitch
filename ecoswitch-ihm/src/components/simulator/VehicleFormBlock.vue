<script setup>
import { ref, computed, onMounted } from 'vue'
import { CheckCircle2, AlertCircle, Wrench, Search, Car, Sparkles } from '@lucide/vue'

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
    console.error('Erreur chargement marques ADEME:', err)
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
    console.error('Erreur chargement modèles ADEME:', err)
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
    console.error('Erreur chargement versions ADEME:', err)
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
  <div class="vehicle-form-block card-glass p-4 mb-4">
    
    <!-- Sélecteur ADEME Rapide -->
    <div class="ademe-quick-box mb-3.5 p-3 rounded-xl border-glass bg-card-subtle">
      <label class="form-label text-xxs uppercase mb-2 block font-bold text-teal flex items-center gap-1">
        <Sparkles size="13" />
        <span>Remplissage automatique ADEME (Marque / Modèle / Version)</span>
      </label>
      <div class="grid-3-fields gap-2">
        <div class="form-group mb-0">
          <select v-model="selectedBrand" class="form-control form-select text-xs" @change="fetchModels">
            <option value="">1. Marque</option>
            <option v-for="b in brands" :key="b" :value="b">{{ b }}</option>
          </select>
        </div>
        <div class="form-group mb-0">
          <select v-model="selectedModel" :disabled="!selectedBrand" class="form-control form-select text-xs" @change="fetchVersions">
            <option value="">2. Modèle</option>
            <option v-for="m in models" :key="m" :value="m">{{ m }}</option>
          </select>
        </div>
        <div class="form-group mb-0">
          <select v-model="selectedVersion" :disabled="!selectedModel" class="form-control form-select text-xs" @change="onVersionChange">
            <option :value="null">3. Version / Moteur</option>
            <option v-for="v in versions" :key="v.version" :value="v">{{ v.version }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Nom du modèle -->
    <div class="form-group relative mb-3">
      <label class="form-label text-xs">Nom du modèle</label>
      <input
        v-model="vehicle.name"
        type="text"
        class="form-control text-xs"
        :placeholder="type === 'current' ? 'ex: Peugeot 208 II PureTech 100' : 'ex: Tesla Model 3 RWD'"
        required 
        @input="filterSuggestions" 
        @focus="onNameFocus" 
        @blur="closeSuggestionsWithDelay"
      />
      
      <div v-if="showSuggestions && suggestions.length > 0" class="autocomplete-dropdown card-glass">
        <div v-for="v in suggestions" :key="v.id" class="suggestion-item" @mousedown="selectSuggestion(v)">
          <div class="suggestion-info">
            <span class="suggestion-name text-main">{{ v.name }}</span>
            <span class="suggestion-meta text-muted">
              {{ formatFuelType(v.fuelType) }} &middot; {{ v.consumption }} {{ v.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km
            </span>
          </div>
          <span class="badge badge-teal badge-small">Base</span>
        </div>
      </div>
    </div>

    <!-- Type d'énergie & Consommation -->
    <div class="grid-2-fields gap-3 mb-3">
      <div class="form-group mb-0">
        <label class="form-label text-xs">Type d'énergie</label>
        <select v-model="vehicle.fuelType" class="form-control form-select text-xs">
          <option value="" disabled>Sélectionnez une énergie</option>
          <option value="PETROL">Essence</option>
          <option value="DIESEL">Diesel</option>
          <option value="HYBRID">Hybride</option>
          <option value="ELECTRIC">Électrique</option>
        </select>
      </div>
      <div class="form-group mb-0">
        <label class="form-label text-xs">Consommation ({{ vehicle.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km)</label>
        <input v-model.number="vehicle.consumption" type="number" step="0.1" class="form-control text-xs" placeholder="ex: 5.5" required />
      </div>
    </div>

    <!-- Mode Avancé : Assurance, Entretien et Reprise/Prix -->
    <div v-if="isAdvanced" class="grid-3-fields gap-2 mb-3">
      <div class="form-group mb-0">
        <label class="form-label text-xxs">Assurance (€/an)</label>
        <input v-model.number="vehicle.insuranceCost" type="number" class="form-control text-xs" required />
      </div>
      <div class="form-group mb-0">
        <label class="form-label text-xxs">Entretien (€/an)</label>
        <input v-model.number="vehicle.maintenanceCost" type="number" class="form-control text-xs" required />
      </div>
      <div class="form-group mb-0">
        <label class="form-label text-xxs">{{ type === 'current' ? 'Reprise (€)' : "Prix d'achat (€)" }}</label>
        <input v-if="type === 'current'" v-model.number="vehicle.resaleValue" type="number" class="form-control text-xs" required />
        <input v-else v-model.number="vehicle.purchasePrice" type="number" class="form-control text-xs" required />
      </div>
    </div>

    <!-- Mode Standard : Reprise et Kilométrage (Véhicule Actuel) -->
    <div v-else-if="type === 'current'" class="grid-2-fields gap-3 mb-3">
      <div class="form-group mb-0">
        <label class="form-label text-xs">Valeur de reprise estimée (€)</label>
        <input v-model.number="vehicle.resaleValue" type="number" class="form-control text-xs" placeholder="ex: 8000" required />
      </div>
      <div class="form-group mb-0">
        <label class="form-label text-xs">Kilométrage annuel (km/an)</label>
        <input v-model.number="vehicle.annualMileage" type="number" class="form-control text-xs" @input="onMileageInput" placeholder="ex: 15000" required />
      </div>
    </div>

    <!-- Mode Standard : Prix d'achat uniquement (Véhicule Cible) -->
    <div v-else class="form-group mb-3">
      <label class="form-label text-xs">Prix d'achat TTC (€)</label>
      <input v-model.number="vehicle.purchasePrice" type="number" class="form-control text-xs" placeholder="ex: 35000" required />
    </div>

    <!-- Frais de réparations immédiats (Véhicule actuel uniquement) -->
    <div v-if="type === 'current'" class="form-group border-t border-glass pt-3 mb-0">
      <div class="flex-between items-center mb-1.5">
        <label class="form-label text-rose m-0 flex items-center gap-1.5 text-xs">
          <Wrench size="13" />
          <span>Frais de réparations immédiats du garage (€)</span>
        </label>
        <span class="badge badge-rose badge-small">Optionnel</span>
      </div>
      <input v-model.number="localRepairCost" type="number" min="0" class="form-control text-xs" placeholder="ex: 2500" />
      <p class="text-xxs text-dimmed mt-1 m-0">Indiquez le devis de votre garagiste si des réparations sont nécessaires pour continuer à rouler.</p>
    </div>
  </div>
</template>

<style scoped>
.autocomplete-dropdown {
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

.suggestion-item {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.15s ease;
}
.suggestion-item:hover {
  background: hsla(var(--accent-teal) / 0.15);
}

.suggestion-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.suggestion-name {
  font-weight: 700;
  font-size: 0.8rem;
}
.suggestion-meta {
  font-size: 0.72rem;
}
</style>
