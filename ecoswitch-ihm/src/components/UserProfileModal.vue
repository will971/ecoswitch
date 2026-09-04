<script setup>
import { ref, watch, computed } from 'vue'
import { X, Save, Car, Fuel, Zap, AlertCircle, Plus, Star, Trash2 } from '@lucide/vue'
import { apiCreateUserVehicleProfile, apiUpdateUserVehicleProfile, apiDeleteUserVehicleProfile, apiGetLiveFuelPrices } from '../utils/api.js'
import CatalogCascadeSelector from '@/components/common/CatalogCascadeSelector.vue'

const props = defineProps({
  show: Boolean,
  profiles: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'profiles-updated'])

const loading = ref(false)
const error = ref(null)
const successMsg = ref(null)

const activeProfileId = ref('new') // 'new' ou ID du profil

const defaultForm = {
  name: '',
  fuelType: 'PETROL',
  consumption: 6.5,
  annualMileage: 15000,
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
      const inheritedPrices = props.profiles.length > 0 ? {
        petrolPrice: props.profiles[props.profiles.length - 1].petrolPrice,
        dieselPrice: props.profiles[props.profiles.length - 1].dieselPrice,
        electricPrice: props.profiles[props.profiles.length - 1].electricPrice,
      } : {}
      form.value = { ...defaultForm, ...inheritedPrices, default: props.profiles.length === 0 }
      if (props.profiles.length === 0) {
        apiGetLiveFuelPrices().then(data => {
          if (data && data.prices) {
            if (data.prices.PETROL) form.value.petrolPrice = data.prices.PETROL
            if (data.prices.DIESEL) form.value.dieselPrice = data.prices.DIESEL
            if (data.prices.ELECTRIC) form.value.electricPrice = data.prices.ELECTRIC
          }
        }).catch(() => {})
      }
    } else {
      const p = props.profiles.find(p => p.id === activeProfileId.value)
      if (p) {
        form.value = { ...p }
      }
    }
  }
}, { immediate: true })

watch(() => props.show, (newVal) => {
  if (newVal) {
    error.value = null
    successMsg.value = null
    if (props.profiles.length > 0) {
      const defaultP = props.profiles.find(p => p.default) || props.profiles[props.profiles.length - 1]
      activeProfileId.value = defaultP.id
    } else {
      activeProfileId.value = 'new'
    }
  }
})

const onCascadeVariantSelected = (v) => {
  form.value.name = `${v.brand} ${v.model} ${v.version}`
  form.value.fuelType = v.fuelType
  form.value.consumption = v.consumption
  form.value.maintenanceCost = v.maintenanceCost || 400
  if (v.resaleValue) form.value.resaleValue = v.resaleValue

  successMsg.value = `Véhicule configuré depuis le catalogue : ${form.value.name}`
  setTimeout(() => successMsg.value = null, 3500)
}

const saveProfile = async () => {
  if (!form.value.name || !form.value.name.trim()) {
    error.value = "Le nom du véhicule est obligatoire."
    return
  }

  loading.value = true
  error.value = null
  successMsg.value = null

  try {
    if (activeProfileId.value === 'new') {
      await apiCreateUserVehicleProfile(form.value)
      successMsg.value = 'Véhicule ajouté au garage !'
    } else {
      await apiUpdateUserVehicleProfile(activeProfileId.value, form.value)
      successMsg.value = 'Profil mis à jour !'
    }
    emit('profiles-updated')
    setTimeout(() => {
      emit('close')
    }, 1200)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const deleteProfile = async () => {
  if (activeProfileId.value === 'new') return
  if (!confirm("Voulez-vous vraiment supprimer ce véhicule de votre garage ?")) return

  loading.value = true
  error.value = null
  try {
    await apiDeleteUserVehicleProfile(activeProfileId.value)
    successMsg.value = 'Véhicule retiré du garage.'
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
    <div class="card-glass auth-modal-card p-4 relative w-100 max-w-2xl animation-fadeIn max-h-[90vh] overflow-y-auto">
      <button class="icon-btn-close absolute top-4 right-4" @click="emit('close')">
        <X size="18" />
      </button>

      <div class="flex items-center gap-2.5 mb-3">
        <div class="garage-icon-badge flex-center">
          <Car size="18" class="text-teal" />
        </div>
        <div>
          <h3 class="text-main font-heading text-md font-bold m-0">Mon Garage Virtuel</h3>
          <p class="text-muted text-xxs m-0">Gérez vos véhicules enregistrés pour pré-remplir instantanément les simulateurs</p>
        </div>
      </div>
      
      <!-- Sélecteur de profils d'onglets Apple -->
      <div class="profiles-tabs flex gap-2 mb-4 overflow-x-auto pb-1">
        <button 
          v-for="p in profiles" :key="p.id"
          class="btn-tab flex items-center gap-1.5 text-xs font-semibold"
          :class="{ active: activeProfileId === p.id }"
          @click="activeProfileId = p.id; error = null; successMsg = null"
        >
          <Star v-if="p.default" size="12" class="text-amber fill-amber" />
          <span>{{ p.name }}</span>
        </button>
        <button 
          class="btn-tab btn-tab-new flex items-center gap-1 text-xs font-semibold text-teal"
          :class="{ active: activeProfileId === 'new' }"
          @click="activeProfileId = 'new'; error = null; successMsg = null"
        >
          <Plus size="14" /> <span>Nouveau véhicule</span>
        </button>
      </div>

      <div v-if="error" class="p-2.5 rounded-xl border-glass bg-card text-rose text-xs mb-3 flex items-center gap-2">
        <AlertCircle size="16" class="shrink-0" />
        <span>{{ error }}</span>
      </div>
      
      <div v-if="successMsg" class="p-2.5 rounded-xl border-glass bg-card text-teal text-xs mb-3 flex items-center gap-2">
        <Zap size="16" class="shrink-0" />
        <span>{{ successMsg }}</span>
      </div>

      <!-- Sélection via Catalogue si Nouveau véhicule -->
      <div v-if="activeProfileId === 'new'" class="p-3.5 mb-4 rounded-2xl border-glass bg-card-subtle">
        <CatalogCascadeSelector
          label="Pré-remplissage depuis le catalogue officiel"
          @select-variant="onCascadeVariantSelected"
        />
      </div>

      <div class="profile-grid">
        <!-- Colonne 1 : Caractéristiques du véhicule -->
        <div>
          <div class="flex-between items-center mb-2 pb-1 border-b border-glass">
            <h4 class="text-xs font-bold text-main uppercase m-0">Caractéristiques</h4>
            <label class="flex items-center gap-1 text-amber cursor-pointer text-xxs font-bold">
              <input type="checkbox" v-model="form.default" />
              <Star size="12" :class="form.default ? 'fill-amber' : ''" /> Véhicule favori
            </label>
          </div>
          
          <div class="form-group mb-2.5">
            <label class="form-label text-xxs">Nom du modèle</label>
            <input v-model="form.name" type="text" class="form-control text-xs font-semibold" placeholder="ex: Renault Megane E-Tech" required />
          </div>
          <div class="form-group mb-2.5">
            <label class="form-label text-xxs">Énergie</label>
            <select v-model="form.fuelType" class="form-control form-select text-xs">
              <option value="ELECTRIC">Électrique</option>
              <option value="HYBRID">Hybride</option>
              <option value="PETROL">Essence</option>
              <option value="DIESEL">Diesel</option>
            </select>
          </div>
          <div class="grid-2-fields mb-2.5">
            <div class="form-group mb-0">
              <label class="form-label text-xxs">Conso ({{ form.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km)</label>
              <input v-model.number="form.consumption" type="number" step="0.1" class="form-control text-xs font-semibold" required />
            </div>
            <div class="form-group mb-0">
              <label class="form-label text-xxs">Km annuel (km/an)</label>
              <input v-model.number="form.annualMileage" type="number" class="form-control text-xs" required />
            </div>
          </div>
          <div class="form-group mb-0">
            <label class="form-label text-xxs">Valeur de revente estimée (€)</label>
            <input v-model.number="form.resaleValue" type="number" class="form-control text-xs" required />
          </div>
        </div>

        <!-- Colonne 2 : Budget Annuel & Prix locaux -->
        <div>
          <div class="mb-2 pb-1 border-b border-glass">
            <h4 class="text-xs font-bold text-main uppercase m-0 flex items-center gap-1.5">
              <Fuel size="14" class="text-teal" /> <span>Budget Annuel & Tarifs Énergies</span>
            </h4>
          </div>
          
          <div class="form-group mb-2.5">
            <label class="form-label text-xxs">Entretien (€/an)</label>
            <input v-model.number="form.maintenanceCost" type="number" class="form-control text-xs" required />
          </div>

          <div class="p-2.5 rounded-xl border-glass bg-card-subtle mt-3">
            <label class="form-label text-xxs text-dimmed uppercase mb-2 block font-bold">Prix des énergies (€/L ou €/kWh)</label>
            <div class="grid-3-fields gap-1.5">
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Essence</label>
                <input v-model.number="form.petrolPrice" type="number" step="0.01" class="form-control text-xs" />
              </div>
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Diesel</label>
                <input v-model.number="form.dieselPrice" type="number" step="0.01" class="form-control text-xs" />
              </div>
              <div class="form-group mb-0">
                <label class="form-label text-xxs">Élec.</label>
                <input v-model.number="form.electricPrice" type="number" step="0.01" class="form-control text-xs" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex-between items-center mt-4 pt-3 border-t border-glass">
        <button v-if="activeProfileId !== 'new'" class="icon-btn hover-rose" @click="deleteProfile" :disabled="loading" title="Supprimer ce véhicule">
          <Trash2 size="15" />
        </button>
        <div v-else></div>
        
        <div class="flex gap-2">
          <button class="btn btn-secondary text-xs" @click="emit('close')">Fermer</button>
          <button :disabled="loading" class="btn btn-primary text-xs font-bold flex items-center gap-1.5" @click="saveProfile">
            <Save size="14" />
            <span>{{ loading ? 'Sauvegarde...' : 'Enregistrer le véhicule' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.garage-icon-badge {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: hsla(var(--accent-teal) / 0.12);
  border: 1px solid hsla(var(--accent-teal) / 0.25);
}

.profiles-tabs {
  border-bottom: 1px solid hsl(var(--border-glass));
}

.btn-tab {
  background: hsl(var(--bg-card-subtle));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 8px;
  padding: 5px 12px;
  color: hsl(var(--text-muted));
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-tab:hover {
  color: hsl(var(--text-main));
  border-color: hsl(var(--text-dimmed));
}
.btn-tab.active {
  background: hsla(var(--accent-teal) / 0.15);
  border-color: hsl(var(--accent-teal));
  color: hsl(var(--accent-teal));
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
@media (max-width: 640px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

.icon-btn {
  background: hsl(var(--bg-card-subtle));
  border: 1px solid hsl(var(--border-glass));
  color: hsl(var(--text-dimmed));
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}
.icon-btn:hover.hover-rose {
  color: hsl(var(--accent-rose));
  border-color: hsl(var(--accent-rose));
  background: hsla(var(--accent-rose) / 0.1);
}

.text-amber { color: hsl(var(--accent-amber)); }
.fill-amber { fill: hsl(var(--accent-amber)); }
.max-w-2xl { max-width: 42rem; }
</style>
