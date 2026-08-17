<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Edit2, Trash2, Shield, Settings, Eye, HelpCircle, Save, X, Sparkles, Check, Info, ArrowRight, Car, Fuel } from '@lucide/vue'

const props = defineProps({
  currentUser: {
    type: Object,
    default: null
  },
  userProfile: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['open-simulator'])

const vehicles = ref([])
const loading = ref(false)
const error = ref(null)
const successMsg = ref(null)

// Pagination & Filtres
const page = ref(0)
const size = ref(6)
const totalPages = ref(1)

// ADEME Filter states
const brands = ref([])
const models = ref([])
const versions = ref([])
const selectedBrand = ref('')
const selectedModel = ref('')
const selectedVersion = ref(null)

// Formulaire
const formOpen = ref(false)
const editMode = ref(false)
const activeVehicleId = ref(null)

const form = ref({
  name: '',
  brand: '',
  model: '',
  generation: '',
  version: '',
  purchasePrice: 15000,
  fuelType: 'PETROL',
  consumption: 6.2,
  annualMileage: 15000,
  insuranceCost: 600,
  maintenanceCost: 400,
  resaleValue: 5000,
  url: '',
  visibility: 'PUBLIC'
})

const getHeaders = () => {
  const token = localStorage.getItem('saas_token')
  return {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
  }
}

const fetchBrands = async () => {
  try {
    const res = await fetch('/api/v1/vehicules/brands')
    if (res.ok) {
      brands.value = await res.json()
    }
  } catch (err) {
    console.error('Erreur chargement marques:', err)
  }
}

const fetchModels = async () => {
  models.value = []
  versions.value = []
  selectedModel.value = ''
  selectedVersion.value = null
  
  if (!selectedBrand.value) {
    page.value = 0
    fetchVehicles()
    return
  }

  try {
    const res = await fetch(`/api/v1/vehicules/models?brand=${encodeURIComponent(selectedBrand.value)}`)
    if (res.ok) {
      models.value = await res.json()
    }
  } catch (err) {
    console.error('Erreur chargement modèles:', err)
  }
  page.value = 0
  fetchVehicles()
}

const fetchVersions = async () => {
  versions.value = []
  selectedVersion.value = null
  
  if (!selectedBrand.value || !selectedModel.value) {
    page.value = 0
    fetchVehicles()
    return
  }

  try {
    const res = await fetch(`/api/v1/vehicules/versions?brand=${encodeURIComponent(selectedBrand.value)}&model=${encodeURIComponent(selectedModel.value)}`)
    if (res.ok) {
      versions.value = await res.json()
    }
  } catch (err) {
    console.error('Erreur chargement versions:', err)
  }
  page.value = 0
  fetchVehicles()
}

const fetchVehicles = async () => {
  loading.value = true
  error.value = null
  try {
    const params = new URLSearchParams()
    params.append('page', page.value)
    params.append('size', size.value)
    if (selectedBrand.value) {
      params.append('brand', selectedBrand.value)
    }
    if (selectedModel.value) {
      params.append('model', selectedModel.value)
    }
    if (selectedVersion.value) {
      params.append('version', selectedVersion.value.version)
    }
    const response = await fetch(`/api/v1/vehicules?${params.toString()}`, {
      headers: getHeaders()
    })
    if (!response.ok) throw new Error('Impossible de charger le catalogue de véhicules.')
    
    const pagesHeader = response.headers.get('X-Total-Pages')
    totalPages.value = pagesHeader ? parseInt(pagesHeader, 10) : 1
    
    vehicles.value = await response.json()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const onBrandChange = () => {
  fetchModels()
}

const onModelChange = () => {
  fetchVersions()
}

const onVersionChange = () => {
  page.value = 0
  fetchVehicles()
}

const nextPage = () => {
  if (page.value + 1 < totalPages.value) {
    page.value++
    fetchVehicles()
  }
}

const prevPage = () => {
  if (page.value > 0) {
    page.value--
    fetchVehicles()
  }
}

const openAddForm = () => {
  editMode.value = false
  activeVehicleId.value = null
  form.value = {
    name: '',
    brand: '',
    model: '',
    generation: '',
    version: '',
    purchasePrice: 20000,
    fuelType: 'PETROL',
    consumption: 6.0,
    annualMileage: 15000,
    insuranceCost: 650,
    maintenanceCost: 450,
    resaleValue: 8000,
    url: '',
    visibility: 'PUBLIC'
  }
  formOpen.value = true
}

const openEditForm = (vehicle) => {
  editMode.value = true
  activeVehicleId.value = vehicle.id
  form.value = { 
    ...vehicle,
    brand: vehicle.brand || '',
    model: vehicle.model || '',
    generation: vehicle.generation || '',
    version: vehicle.version || '',
    url: vehicle.url || '',
    visibility: vehicle.visibility || 'PUBLIC'
  }
  formOpen.value = true
}

const saveVehicle = async () => {
  if (!form.value.brand || !form.value.brand.trim()) {
    error.value = "La marque est obligatoire."
    return
  }
  if (!form.value.model || !form.value.model.trim()) {
    error.value = "Le modèle est obligatoire."
    return
  }
  if (!form.value.version || !form.value.version.trim()) {
    error.value = "La version est obligatoire."
    return
  }

  loading.value = true
  error.value = null
  successMsg.value = null

  try {
    const url = editMode.value ? `/api/v1/vehicules/${activeVehicleId.value}` : '/api/v1/vehicules'
    const method = editMode.value ? 'PUT' : 'POST'

    const response = await fetch(url, {
      method,
      headers: getHeaders(),
      body: JSON.stringify(form.value)
    })

    if (!response.ok) {
      const errData = await response.json()
      throw new Error(errData.error || 'Erreur lors de la sauvegarde du véhicule.')
    }

    showSuccess(editMode.value ? 'Véhicule mis à jour avec succès !' : 'Véhicule créé avec succès !')
    formOpen.value = false
    await fetchVehicles()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const deleteVehicle = async (id) => {
  if (!confirm('Êtes-vous sûr de vouloir supprimer ce véhicule du catalogue ?')) return

  loading.value = true
  error.value = null
  successMsg.value = null

  try {
    const response = await fetch(`/api/v1/vehicules/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    })

    if (!response.ok) {
      const errData = await response.json()
      throw new Error(errData.error || 'Erreur lors de la suppression.')
    }

    showSuccess('Véhicule supprimé du catalogue.')
    await fetchVehicles()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const showSuccess = (msg) => {
  successMsg.value = msg
  setTimeout(() => {
    successMsg.value = null
  }, 4000)
}

const getFuelBadgeClass = (type) => {
  switch (type) {
    case 'ELECTRIC': return 'badge-teal'
    case 'HYBRID': return 'badge-cyan'
    case 'DIESEL': return 'badge-blue'
    default: return 'badge-amber'
  }
}

const getFuelLabel = (type) => {
  switch (type) {
    case 'ELECTRIC': return 'Électrique'
    case 'HYBRID': return 'Hybride'
    case 'DIESEL': return 'Diesel'
    default: return 'Essence'
  }
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

const goToVehicle = (url) => {
  if (url) {
    window.open(url, '_blank')
  }
}

const getFuelPrice = (fuelType) => {
  if (!props.userProfile) return 1.5
  switch (fuelType) {
    case 'PETROL': return props.userProfile.petrolPrice || 1.88
    case 'DIESEL': return props.userProfile.dieselPrice || 1.74
    case 'ELECTRIC': return props.userProfile.electricPrice || 0.25
    case 'HYBRID': return props.userProfile.petrolPrice || 1.88
    default: return 1.5
  }
}

const calculateSavings = (vehicle) => {
  if (!props.userProfile) return null
  const myProfile = props.userProfile
  const myFuelCost = (myProfile.consumption * getFuelPrice(myProfile.fuelType) * myProfile.annualMileage) / 100
  const myTotalCost = myFuelCost + myProfile.insuranceCost + myProfile.maintenanceCost
  
  const targetFuelCost = (vehicle.consumption * getFuelPrice(vehicle.fuelType) * myProfile.annualMileage) / 100
  const targetTotalCost = targetFuelCost + vehicle.insuranceCost + vehicle.maintenanceCost
  return myTotalCost - targetTotalCost
}

const openSimulatorForVehicle = (vehicle) => {
  localStorage.setItem('eco_target_vehicle_id', vehicle.id)
  emit('open-simulator')
}

onMounted(() => {
  fetchVehicles()
  fetchBrands()
})
</script>

<template>
  <div class="vehicle-manager-container animation-fadeIn">
    <div class="header-section flex-between items-center mb-4">
      <div>
        <h2 class="text-main font-heading text-xl font-bold mb-1">Catalogue des Véhicules</h2>
        <p class="text-muted text-xs m-0">Consultez la base de données certifiée et simulez l'impact financier en un clic.</p>
      </div>
      <button v-if="!formOpen && currentUser" class="btn btn-primary btn-small flex items-center gap-1.5" @click="openAddForm">
        <Plus size="16" /> <span>Ajouter un véhicule</span>
      </button>
    </div>

    <!-- Notifications et erreurs -->
    <div v-if="successMsg" class="alert-banner p-3 rounded-xl mb-4 flex items-center gap-2 border-glass bg-card">
      <Check size="16" class="text-teal" />
      <span class="text-xs font-semibold text-teal">{{ successMsg }}</span>
    </div>
    <div v-if="error" class="alert-banner p-3 rounded-xl mb-4 flex items-center gap-2 border-glass bg-card">
      <Info size="16" class="text-rose" />
      <span class="text-xs font-semibold text-rose">{{ error }}</span>
    </div>

    <!-- Filtres Cascading (Marque -> Modèle -> Version) -->
    <div v-if="!formOpen" class="filters-bar card-glass p-3.5 mb-4 flex flex-column gap-2.5">
      <div class="flex items-center gap-1.5 text-xxs text-dimmed uppercase font-bold">
        <Sparkles size="13" class="text-teal" />
        <span>Filtres de recherche rapides</span>
      </div>
      <div class="grid-3-fields w-100">
        <div class="form-group mb-0">
          <select v-model="selectedBrand" class="form-control form-select text-xs" @change="onBrandChange">
            <option value="">Toutes les marques</option>
            <option v-for="b in brands" :key="b" :value="b">{{ b }}</option>
          </select>
        </div>
        <div class="form-group mb-0">
          <select v-model="selectedModel" :disabled="!selectedBrand" class="form-control form-select text-xs" @change="onModelChange">
            <option value="">Tous les modèles</option>
            <option v-for="m in models" :key="m" :value="m">{{ m }}</option>
          </select>
        </div>
        <div class="form-group mb-0">
          <select v-model="selectedVersion" :disabled="!selectedModel" class="form-control form-select text-xs" @change="onVersionChange">
            <option :value="null">Toutes les versions</option>
            <option v-for="v in versions" :key="v.version" :value="v">{{ v.version }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Formulaire d'Ajout/Édition -->
    <section v-if="formOpen" class="card-glass mb-4">
      <div class="flex-between items-center mb-4 pb-2 border-b border-glass">
        <h3 class="text-main font-heading text-md font-bold m-0">
          {{ editMode ? 'Modifier la fiche véhicule' : 'Ajouter un nouveau véhicule' }}
        </h3>
        <button class="icon-btn-close" @click="formOpen = false">
          <X size="18" />
        </button>
      </div>

      <div class="grid-cols-2">
        <div>
          <div class="form-group">
            <label class="form-label text-xxs">Marque</label>
            <input v-model="form.brand" type="text" class="form-control text-xs" placeholder="ex: Peugeot" @input="form.name = `${form.brand} ${form.model} ${form.generation} ${form.version}`.replace(/\s+/g, ' ').trim()" required />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Modèle</label>
            <input v-model="form.model" type="text" class="form-control text-xs" placeholder="ex: 308" @input="form.name = `${form.brand} ${form.model} ${form.generation} ${form.version}`.replace(/\s+/g, ' ').trim()" required />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Génération</label>
            <input v-model="form.generation" type="text" class="form-control text-xs" placeholder="ex: III" @input="form.name = `${form.brand} ${form.model} ${form.generation} ${form.version}`.replace(/\s+/g, ' ').trim()" />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Version / Finition</label>
            <input v-model="form.version" type="text" class="form-control text-xs" placeholder="ex: BlueHDi 130 EAT8" @input="form.name = `${form.brand} ${form.model} ${form.generation} ${form.version}`.replace(/\s+/g, ' ').trim()" required />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Type d'énergie</label>
            <select v-model="form.fuelType" class="form-control form-select text-xs">
              <option value="PETROL">Essence</option>
              <option value="DIESEL">Diesel</option>
              <option value="HYBRID">Hybride</option>
              <option value="ELECTRIC">Électrique</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Consommation (L ou kWh/100km)</label>
            <input v-model.number="form.consumption" type="number" step="0.1" class="form-control text-xs" required />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Prix d'achat (€)</label>
            <input v-model.number="form.purchasePrice" type="number" class="form-control text-xs" required />
          </div>
        </div>

        <div>
          <div class="form-group">
            <label class="form-label text-xxs">Kilométrage annuel moyen (km/an)</label>
            <input v-model.number="form.annualMileage" type="number" class="form-control text-xs" required />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Assurance annuelle (€/an)</label>
            <input v-model.number="form.insuranceCost" type="number" class="form-control text-xs" required />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Entretien annuel moyen (€/an)</label>
            <input v-model.number="form.maintenanceCost" type="number" class="form-control text-xs" required />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Valeur de revente estimée (€)</label>
            <input v-model.number="form.resaleValue" type="number" class="form-control text-xs" required />
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Visibilité</label>
            <select v-model="form.visibility" class="form-control form-select text-xs">
              <option value="PUBLIC">Publique (partagée au catalogue)</option>
              <option value="PRIVATE">Privée (visible uniquement par vous)</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label text-xxs">Lien externe (Fiche constructeur)</label>
            <input v-model="form.url" type="url" class="form-control text-xs" placeholder="https://..." />
          </div>
        </div>
      </div>

      <div class="flex justify-end gap-2 mt-4 pt-3 border-t border-glass">
        <button class="btn btn-secondary text-xs" @click="formOpen = false">Annuler</button>
        <button :disabled="loading" class="btn btn-primary text-xs font-bold" @click="saveVehicle">
          <Save size="15" /> <span>{{ editMode ? 'Mettre à jour' : 'Créer le véhicule' }}</span>
        </button>
      </div>
    </section>

    <!-- Liste des Véhicules en cartes Bento -->
    <div v-if="loading && vehicles.length === 0" class="flex-center py-5 text-muted text-xs">
      <Sparkles class="spinner text-teal mr-2" size="20" /> Chargement des modèles...
    </div>

    <div v-else-if="vehicles.length === 0" class="flex-center flex-column py-5 text-center card-glass">
      <HelpCircle size="40" class="text-dimmed opacity-40 mb-2" />
      <h4 class="text-main font-heading text-sm font-bold m-0">Aucun véhicule trouvé</h4>
      <p class="text-xs text-muted max-w-sm mt-1 m-0">Modifiez vos critères de recherche ou ajoutez un modèle.</p>
    </div>

    <div v-else class="vehicles-grid">
      <div
        v-for="vehicle in vehicles" 
        :key="vehicle.id" 
        class="vehicle-card card-glass p-4 flex flex-column justify-between relative"
      >
        <div>
          <div class="card-header-top mb-2.5 flex-between items-center">
            <span class="badge badge-small" :class="getFuelBadgeClass(vehicle.fuelType)">
              {{ getFuelLabel(vehicle.fuelType) }}
            </span>
            <div v-if="currentUser && (currentUser.role === 'ADMIN' || currentUser.email === vehicle.createdBy)" class="actions flex gap-1.5">
              <button class="icon-btn" @click.stop="openEditForm(vehicle)" title="Modifier">
                <Edit2 size="13" />
              </button>
              <button class="icon-btn hover-rose" @click.stop="deleteVehicle(vehicle.id)" title="Supprimer">
                <Trash2 size="13" />
              </button>
            </div>
          </div>

          <h3 class="vehicle-title text-main font-bold text-sm mb-2">
            {{ vehicle.name }}
          </h3>

          <!-- Gain estimé si profil garage renseigné -->
          <div v-if="userProfile" class="savings-chip mb-3 py-1.5 px-2.5 rounded-lg text-center" :class="calculateSavings(vehicle) > 0 ? 'bg-teal-soft' : 'bg-rose-soft'">
            <span class="text-xxs uppercase font-bold text-dimmed">Gain estimé : </span>
            <span class="text-xs font-bold" :class="calculateSavings(vehicle) > 0 ? 'text-teal' : 'text-rose'">
              {{ calculateSavings(vehicle) > 0 ? '+' : '' }}{{ formatCurrency(calculateSavings(vehicle)) }} / an
            </span>
          </div>

          <!-- Caractéristiques -->
          <div class="specs-list py-2 border-t border-glass text-xs flex flex-column gap-1">
            <div class="flex-between">
              <span class="text-dimmed">Consommation :</span>
              <span class="font-semibold text-main">{{ vehicle.consumption }} {{ vehicle.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km</span>
            </div>
            <div class="flex-between">
              <span class="text-dimmed">Prix d'achat :</span>
              <span class="font-bold text-cyan">{{ formatCurrency(vehicle.purchasePrice) }}</span>
            </div>
            <div class="flex-between">
              <span class="text-dimmed">Assurance :</span>
              <span class="font-semibold text-main">{{ formatCurrency(vehicle.insuranceCost) }}/an</span>
            </div>
            <div class="flex-between">
              <span class="text-dimmed">Entretien :</span>
              <span class="font-semibold text-main">{{ formatCurrency(vehicle.maintenanceCost) }}/an</span>
            </div>
          </div>
        </div>

        <div class="mt-3 pt-2.5 border-t border-glass">
          <button class="btn btn-secondary w-100 btn-small flex items-center justify-center gap-1.5" @click="openSimulatorForVehicle(vehicle)">
            <Sparkles size="14" class="text-teal" />
            <span>Simuler ce véhicule</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="!formOpen && totalPages > 1" class="pagination-controls flex-center gap-3 mt-4">
      <button 
        class="btn btn-secondary btn-small" 
        :disabled="page === 0" 
        @click="prevPage"
      >
        Précédent
      </button>
      <span class="text-xs text-dimmed font-semibold">Page {{ page + 1 }} sur {{ totalPages }}</span>
      <button 
        class="btn btn-secondary btn-small" 
        :disabled="page + 1 >= totalPages" 
        @click="nextPage"
      >
        Suivant
      </button>
    </div>
  </div>
</template>

<style scoped>
.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 16px;
}

.vehicle-card {
  transition: all 0.2s ease;
}
.vehicle-card:hover {
  border-color: hsl(var(--accent-teal) / 0.45);
  transform: translateY(-2px);
}

.icon-btn {
  background: hsl(var(--bg-card-subtle));
  border: 1px solid hsl(var(--border-glass));
  color: hsl(var(--text-dimmed));
  width: 26px;
  height: 26px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}
.icon-btn:hover {
  color: hsl(var(--text-main));
  border-color: hsl(var(--text-dimmed));
}
.icon-btn.hover-rose:hover {
  color: hsl(var(--accent-rose));
  border-color: hsl(var(--accent-rose));
}

.bg-teal-soft {
  background: hsla(var(--accent-teal) / 0.1);
  border: 1px solid hsla(var(--accent-teal) / 0.25);
}
.bg-rose-soft {
  background: hsla(var(--accent-rose) / 0.1);
  border: 1px solid hsla(var(--accent-rose) / 0.25);
}
</style>
