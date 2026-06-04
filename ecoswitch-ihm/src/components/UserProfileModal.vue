<script setup>
import { ref, watch, computed } from 'vue'
import { X, Save, Car, Fuel, Zap, AlertCircle, Plus, Star, Search, Trash2 } from '@lucide/vue'
import { apiCreateUserVehicleProfile, apiUpdateUserVehicleProfile, apiDeleteUserVehicleProfile } from '../utils/api.js'

const props = defineProps({
  show: Boolean,
  profiles: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'profiles-updated'])

const loading = ref(false)
const searchLoading = ref(false)
const error = ref(null)
const successMsg = ref(null)

const activeProfileId = ref('new') // 'new' ou ID du profil
const plaqueInput = ref('')

const defaultForm = {
  name: '',
  fuelType: 'PETROL',
  consumption: 6.5,
  annualMileage: 15000,
  insuranceCost: 600,
  maintenanceCost: 400,
  resaleValue: 5000,
  petrolPrice: 1.88,
  dieselPrice: 1.74,
  electricPrice: 0.25,
  default: false
}

const form = ref({ ...defaultForm })

// Synchroniser form quand activeProfileId change ou quand profiles change
watch(() => [props.show, activeProfileId.value, props.profiles], () => {
  if (props.show) {
    if (activeProfileId.value === 'new') {
      // Hériter des prix d'énergie du dernier profil si existant
      const inheritedPrices = props.profiles.length > 0 ? {
        petrolPrice: props.profiles[props.profiles.length - 1].petrolPrice,
        dieselPrice: props.profiles[props.profiles.length - 1].dieselPrice,
        electricPrice: props.profiles[props.profiles.length - 1].electricPrice,
      } : {}
      form.value = { ...defaultForm, ...inheritedPrices, default: props.profiles.length === 0 }
    } else {
      const p = props.profiles.find(p => p.id === activeProfileId.value)
      if (p) {
        form.value = { ...p }
      }
    }
  }
}, { immediate: true })

// Sélectionner par défaut s'il y a des profils
watch(() => props.show, (newVal) => {
  if (newVal) {
    error.value = null
    successMsg.value = null
    plaqueInput.value = ''
    if (props.profiles.length > 0) {
      const defaultP = props.profiles.find(p => p.default) || props.profiles[props.profiles.length - 1]
      activeProfileId.value = defaultP.id
    } else {
      activeProfileId.value = 'new'
    }
  }
})

const searchByPlaque = async () => {
  if (!plaqueInput.value) return
  searchLoading.value = true
  error.value = null
  try {
    const res = await fetch(`/api/v1/immatriculation/${encodeURIComponent(plaqueInput.value)}`)
    if (!res.ok) throw new Error('Véhicule introuvable pour cette plaque.')
    const data = await res.json()
    form.value.name = data.name || form.value.name
    if (data.fuelType) form.value.fuelType = data.fuelType
    if (data.consumption) form.value.consumption = data.consumption
    successMsg.value = `Véhicule trouvé : ${data.name}`
    setTimeout(() => successMsg.value = null, 3000)
  } catch (err) {
    error.value = err.message
  } finally {
    searchLoading.value = false
  }
}

const saveProfile = async () => {
  loading.value = true
  error.value = null
  successMsg.value = null

  try {
    if (activeProfileId.value === 'new') {
      await apiCreateUserVehicleProfile(form.value)
      successMsg.value = 'Véhicule ajouté avec succès !'
    } else {
      await apiUpdateUserVehicleProfile(activeProfileId.value, form.value)
      successMsg.value = 'Profil mis à jour !'
    }
    emit('profiles-updated')
    setTimeout(() => {
      emit('close')
    }, 1500)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const deleteProfile = async () => {
  if (activeProfileId.value === 'new') return
  if (!confirm("Voulez-vous vraiment supprimer ce véhicule ?")) return

  loading.value = true
  error.value = null
  try {
    await apiDeleteUserVehicleProfile(activeProfileId.value)
    successMsg.value = 'Véhicule supprimé.'
    emit('profiles-updated')
    setTimeout(() => {
      activeProfileId.value = 'new'
      successMsg.value = null
    }, 1000)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-if="show" class="auth-modal-overlay flex-center">
    <div class="card-glass glow-teal auth-modal-card p-4 relative w-100" style="max-width: 650px;">
      <button class="absolute top-4 right-4 text-dimmed hover-text-main" @click="emit('close')">
        <X size="20" />
      </button>

      <h3 class="text-gradient mb-3 flex items-center gap-2">
        <Car size="20" /> Mon Garage
      </h3>
      
      <!-- Sélecteur de profils -->
      <div class="profiles-tabs flex gap-2 mb-4 overflow-x-auto pb-2">
        <button 
          v-for="p in profiles" :key="p.id"
          class="btn btn-secondary btn-small flex items-center gap-1"
          :class="{ 'active-tab': activeProfileId === p.id }"
          @click="activeProfileId = p.id; error = null; successMsg = null"
        >
          <Star v-if="p.default" size="12" class="text-amber fill-amber" />
          {{ p.name }}
        </button>
        <button 
          class="btn btn-secondary btn-small flex items-center gap-1 border-teal text-teal"
          :class="{ 'active-tab': activeProfileId === 'new' }"
          @click="activeProfileId = 'new'; error = null; successMsg = null"
        >
          <Plus size="14" /> Nouveau véhicule
        </button>
      </div>

      <div v-if="error" class="alert-banner bg-warning-glass border-rose text-rose p-3 rounded mb-4 flex-between">
        <div class="flex-center">
          <AlertCircle size="18" class="mr-2" /> {{ error }}
        </div>
      </div>
      
      <div v-if="successMsg" class="alert-banner bg-success-glass border-teal text-teal p-3 rounded mb-4 flex-between">
        <div class="flex-center">
          <Zap size="18" class="mr-2" /> {{ successMsg }}
        </div>
      </div>

      <!-- Recherche par Plaque (uniquement si nouveau) -->
      <div v-if="activeProfileId === 'new'" class="p-3 mb-4 rounded border-glass bg-deep-glass">
        <label class="form-label text-cyan text-sm flex items-center gap-1 mb-2">
          <Search size="14" /> Recherche rapide par plaque
        </label>
        <div class="flex gap-2">
          <input v-model="plaqueInput" type="text" class="form-control font-heading text-center tracking-widest uppercase" placeholder="AA-123-AA" @keyup.enter="searchByPlaque" />
          <button class="btn btn-secondary flex-center whitespace-nowrap" @click="searchByPlaque" :disabled="searchLoading">
            <span v-if="searchLoading" class="spinner"><Search size="16" /></span>
            <span v-else>Chercher</span>
          </button>
        </div>
      </div>

      <div class="profile-grid">
        <!-- Colonne 1 : Caractéristiques du véhicule -->
        <div>
          <h4 class="text-sm text-cyan mb-3 border-b border-glass pb-1 flex-between">
            <span>Caractéristiques</span>
            <label class="flex items-center gap-1 text-amber cursor-pointer text-xs">
              <input type="checkbox" v-model="form.default" />
              <Star size="14" :class="form.default ? 'fill-amber' : ''" /> Favori
            </label>
          </h4>
          <div class="form-group mb-3">
            <label class="form-label">Nom du modèle</label>
            <input v-model="form.name" type="text" class="form-control" placeholder="ex: Renault Clio" required />
          </div>
          <div class="form-group mb-3">
            <label class="form-label">Type d'énergie</label>
            <select v-model="form.fuelType" class="form-control form-select">
              <option value="PETROL">Essence</option>
              <option value="DIESEL">Diesel</option>
              <option value="HYBRID">Hybride</option>
              <option value="ELECTRIC">Électrique</option>
            </select>
          </div>
          <div class="form-group mb-3">
            <label class="form-label">Conso moyenne (L ou kWh/100km)</label>
            <input v-model.number="form.consumption" type="number" step="0.1" class="form-control" required />
          </div>
          <div class="form-group mb-3">
            <label class="form-label">Kilométrage annuel (km/an)</label>
            <input v-model.number="form.annualMileage" type="number" class="form-control" required />
          </div>
          <div class="form-group mb-3">
            <label class="form-label">Valeur de revente estimée (€)</label>
            <input v-model.number="form.resaleValue" type="number" class="form-control" required />
          </div>
        </div>

        <!-- Colonne 2 : Tarifs Énergies & Entretien -->
        <div>
          <h4 class="text-sm text-cyan mb-3 border-b border-glass pb-1 flex items-center gap-2">
            <Fuel size="16" /> Budget Annuel & Énergies
          </h4>
          <div class="form-group mb-3">
            <label class="form-label">Assurance annuelle (€/an)</label>
            <input v-model.number="form.insuranceCost" type="number" class="form-control" required />
          </div>
          <div class="form-group mb-3">
            <label class="form-label">Entretien annuel (€/an)</label>
            <input v-model.number="form.maintenanceCost" type="number" class="form-control" required />
          </div>
          <div class="form-group mb-3 mt-4 border-t border-glass pt-3">
            <label class="form-label text-xs text-dimmed mb-2 block">Prix locaux des énergies :</label>
            <div class="flex gap-2 mb-2 items-center">
              <label class="w-20 text-xxs uppercase">Essence</label>
              <input v-model.number="form.petrolPrice" type="number" step="0.01" class="form-control form-control-sm" />
            </div>
            <div class="flex gap-2 mb-2 items-center">
              <label class="w-20 text-xxs uppercase">Diesel</label>
              <input v-model.number="form.dieselPrice" type="number" step="0.01" class="form-control form-control-sm" />
            </div>
            <div class="flex gap-2 mb-2 items-center">
              <label class="w-20 text-xxs uppercase">Élec.</label>
              <input v-model.number="form.electricPrice" type="number" step="0.01" class="form-control form-control-sm" />
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-between gap-3 mt-4 pt-3 border-t border-glass">
        <button v-if="activeProfileId !== 'new'" class="btn btn-secondary text-rose border-rose hover-bg-rose flex-center gap-1" @click="deleteProfile" :disabled="loading">
          <Trash2 size="16" />
        </button>
        <div v-else></div> <!-- spacer -->
        
        <div class="flex gap-3">
          <button class="btn btn-secondary" @click="emit('close')">Fermer</button>
          <button :disabled="loading" class="btn btn-primary flex-center gap-2" @click="saveProfile">
            <Save size="16" /> {{ loading ? 'Sauvegarde...' : 'Enregistrer' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 2000;
}
.auth-modal-card {
  z-index: 2001;
  background: hsl(var(--bg-deep) / 0.95);
  max-height: 90vh;
  overflow-y: auto;
}
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 600px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
.active-tab {
  background: hsl(var(--accent-cyan) / 0.2);
  border-color: hsl(var(--accent-cyan));
  color: white;
}
.border-b { border-bottom: 1px solid rgba(255, 255, 255, 0.1); }
.border-t { border-top: 1px solid rgba(255, 255, 255, 0.1); }
.pt-3 { padding-top: 12px; }
.pb-1 { padding-bottom: 4px; }
.w-20 { width: 5rem; }
.tracking-widest { letter-spacing: 0.1em; }
.text-amber { color: #fbbf24; }
.fill-amber { fill: #fbbf24; }
.hover-bg-rose:hover { background: rgba(225, 29, 72, 0.1); }
</style>
