<script setup>
import { ref, computed } from 'vue'
import { CheckCircle2, AlertCircle, Wrench, Search, Car, Sparkles, SlidersHorizontal } from '@lucide/vue'
import CatalogCascadeSelector from '@/components/common/CatalogCascadeSelector.vue'

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

const showCascadeSelector = ref(true)
const showCustomInputs = ref(false)

const localRepairCost = computed({
  get: () => props.immediateRepairCost,
  set: (val) => emit('update:immediateRepairCost', val)
})

const getDefaultMaintenanceCost = (fuelType) => {
  switch (fuelType) {
    case 'ELECTRIC': return 250
    case 'HYBRID': return 350
    case 'DIESEL': return 500
    default: return 450 // PETROL
  }
}

const handleVariantSelected = (v) => {
  props.vehicle.name = `${v.brand} ${v.model} ${v.version}`
  props.vehicle.brand = v.brand
  props.vehicle.model = v.model
  props.vehicle.version = v.version
  props.vehicle.fuelType = v.fuelType
  props.vehicle.consumption = v.consumption
  props.vehicle.maintenanceCost = v.maintenanceCost || getDefaultMaintenanceCost(v.fuelType)
  
  if (props.type === 'current' && v.resaleValue !== undefined) {
    props.vehicle.resaleValue = v.resaleValue
  }
  if (props.type === 'target' && v.purchasePrice !== undefined) {
    props.vehicle.purchasePrice = v.purchasePrice
  }

  emit('suggestion-selected', props.vehicle)
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
</script>

<template>
  <div class="vehicle-form-block card-glass p-4 mb-4">
    
    <!-- Sélecteur Relationnel en Cascade -->
    <div class="catalog-cascade-box mb-4 p-3.5 rounded-2xl border-glass bg-card-subtle">
      <CatalogCascadeSelector
        :label="type === 'current' ? 'Sélectionnez votre véhicule actuel' : 'Sélectionnez votre futur véhicule'"
        :initial-brand="vehicle.brand || ''"
        :initial-model="vehicle.model || ''"
        @select-variant="handleVariantSelected"
      />
    </div>

    <!-- Toggle d'ajustement / personnalisation des champs -->
    <div class="flex items-center justify-between py-2 border-t border-glass mb-3">
      <div class="flex items-center space-x-1.5 text-xs text-dimmed font-medium">
        <SlidersHorizontal size="13" class="text-teal" />
        <span>Détails & Personnalisation libre</span>
      </div>
      <button
        type="button"
        @click="showCustomInputs = !showCustomInputs"
        class="text-xs text-teal font-semibold hover:underline"
      >
        {{ showCustomInputs ? 'Masquer les détails' : 'Afficher / Ajuster les détails' }}
      </button>
    </div>

    <!-- Nom du modèle -->
    <div class="form-group mb-3">
      <label class="form-label text-xs">Libellé du véhicule</label>
      <input
        v-model="vehicle.name"
        type="text"
        class="form-control text-xs font-semibold"
        :placeholder="type === 'current' ? 'ex: Peugeot 208 PureTech 100' : 'ex: Tesla Model 3 RWD'"
        required 
      />
    </div>

    <!-- Champs principaux : Énergie, Consommation et Prix/Reprise -->
    <div class="grid-2-fields gap-3 mb-3">
      <div class="form-group mb-0">
        <label class="form-label text-xs">Type d'énergie</label>
        <select v-model="vehicle.fuelType" class="form-control form-select text-xs">
          <option value="" disabled>Sélectionnez une énergie</option>
          <option value="ELECTRIC">Électrique</option>
          <option value="HYBRID">Hybride</option>
          <option value="PETROL">Essence</option>
          <option value="DIESEL">Diesel</option>
        </select>
      </div>
      <div class="form-group mb-0">
        <label class="form-label text-xs">Consommation ({{ vehicle.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km)</label>
        <input v-model.number="vehicle.consumption" type="number" step="0.1" class="form-control text-xs font-semibold" placeholder="ex: 15.5" required />
      </div>
    </div>

    <!-- Mode Avancé : Entretien et Reprise/Prix -->
    <div v-if="isAdvanced || showCustomInputs" class="grid-2-fields gap-2 mb-3">
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
.catalog-cascade-box {
  background: hsla(var(--bg-card-subtle));
}
</style>
