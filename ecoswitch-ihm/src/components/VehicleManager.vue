<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Edit2, Trash2, Shield, Settings, Eye, HelpCircle, Save, X, Sparkles, Check, Info } from '@lucide/vue'

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
const size = ref(6) // 6 véhicules par page
const totalPages = ref(1)
const searchName = ref('')
const filterFuelType = ref('')

// Formulaire
const formOpen = ref(false)
const editMode = ref(false)
const activeVehicleId = ref(null)

const form = ref({
  name: '',
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

const fetchVehicles = async () => {
  loading.value = true
  error.value = null
  try {
    const params = new URLSearchParams()
    params.append('page', page.value)
    params.append('size', size.value)
    if (searchName.value.trim()) {
      params.append('name', searchName.value.trim())
    }
    if (filterFuelType.value) {
      params.append('fuelType', filterFuelType.value)
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

const onSearch = () => {
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
    url: vehicle.url || '',
    visibility: vehicle.visibility || 'PUBLIC'
  }
  formOpen.value = true
}

const saveVehicle = async () => {
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
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
}

const goToVehicle = (url) => {
  if (url) {
    window.open(url, '_blank')
  }
}

const getFuelPrice = (fuelType) => {
  if (!props.userProfile) return 1.5 // Fallback generic
  switch (fuelType) {
    case 'PETROL': return props.userProfile.petrolPrice
    case 'DIESEL': return props.userProfile.dieselPrice
    case 'ELECTRIC': return props.userProfile.electricPrice
    case 'HYBRID': return props.userProfile.petrolPrice // Approximation pour hybride
    default: return 1.5
  }
}

const calculateSavings = (vehicle) => {
  if (!props.userProfile) return null
  
  const myProfile = props.userProfile
  
  // Coût du profil actuel
  const myFuelCost = (myProfile.consumption * getFuelPrice(myProfile.fuelType) * myProfile.annualMileage) / 100
  const myTotalCost = myFuelCost + myProfile.insuranceCost + myProfile.maintenanceCost
  
  // Coût du véhicule cible (on utilise le kilométrage annuel de l'utilisateur pour la simulation)
  const targetFuelCost = (vehicle.consumption * getFuelPrice(vehicle.fuelType) * myProfile.annualMileage) / 100
  const targetTotalCost = targetFuelCost + vehicle.insuranceCost + vehicle.maintenanceCost
  
  const savings = myTotalCost - targetTotalCost
  return savings
}

const openSimulatorForVehicle = (vehicle) => {
  // Déclencher un event global ou juste envoyer à App.vue avec event emit pour ouvrir le simulateur
  // On sauvegarde le targetVehicle temporairement dans le localStorage ou un store pour que DirectSimulator le récupère
  localStorage.setItem('eco_target_vehicle_id', vehicle.id)
  emit('open-simulator')
}

onMounted(() => {
  fetchVehicles()
})
</script>

<template>
  <div class="vehicle-manager-container">
    <div class="header-section flex-between mb-5">
      <div>
        <h2 class="text-gradient mb-2">Catalogue des Véhicules</h2>
        <p class="text-muted">Gérez la base de données des véhicules disponibles pour vos comparaisons et analyses.</p>
      </div>
      <button v-if="!formOpen && currentUser" class="btn btn-primary" @click="openAddForm">
        <Plus size="18" /> Ajouter un véhicule
      </button>
      <div v-else-if="!formOpen" class="text-xs text-rose flex-center bg-warning-glass border-rose px-3 py-2 rounded">
        <Info size="14" class="mr-1" /> Connectez-vous pour ajouter un véhicule.
      </div>
    </div>

    <!-- Notifications et erreurs -->
    <div v-if="successMsg" class="alert-banner bg-success-glass border-teal text-teal p-3 rounded mb-4 flex-between">
      <div class="flex-center">
        <Check size="18" class="mr-2" /> {{ successMsg }}
      </div>
    </div>
    <div v-if="error" class="alert-banner bg-warning-glass border-rose text-rose p-3 rounded mb-4 flex-between">
      <div class="flex-center">
        <Info size="18" class="mr-2" /> {{ error }}
      </div>
    </div>

    <!-- Filtres et Recherche -->
    <div v-if="!formOpen" class="filters-bar card-glass p-3 mb-4 flex-between gap-3">
      <div class="flex-1">
        <input 
          v-model="searchName" 
          @input="onSearch" 
          type="text" 
          class="form-control" 
          placeholder="Rechercher par nom (ex: Peugeot)..." 
        />
      </div>
      <div style="min-width: 180px;">
        <select v-model="filterFuelType" @change="onSearch" class="form-control form-select">
          <option value="">Tous les carburants</option>
          <option value="PETROL">Essence</option>
          <option value="DIESEL">Diesel</option>
          <option value="HYBRID">Hybride</option>
          <option value="ELECTRIC">Électrique</option>
        </select>
      </div>
    </div>

    <!-- Formulaire d'Ajout/Édition -->
    <section v-if="formOpen" class="card-glass glow-teal mb-5">
      <h3 class="text-gradient mb-4 flex-between">
        <span>{{ editMode ? 'Modifier le véhicule' : 'Nouveau Véhicule' }}</span>
        <button class="btn btn-secondary btn-small flex-center" @click="formOpen = false">
          <X size="16" /> Fermer
        </button>
      </h3>

      <div class="grid-cols-2">
        <div>
          <div class="form-group">
            <label class="form-label">Nom du modèle</label>
            <input v-model="form.name" type="text" class="form-control" placeholder="ex: Peugeot 308 BlueHDi" required />
          </div>
          <div class="form-group">
            <label class="form-label">Type d'énergie</label>
            <select v-model="form.fuelType" class="form-control form-select">
              <option value="PETROL">Essence</option>
              <option value="DIESEL">Diesel</option>
              <option value="HYBRID">Hybride</option>
              <option value="ELECTRIC">Électrique</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Consommation (L/100km ou kWh/100km)</label>
            <input v-model.number="form.consumption" type="number" step="0.1" class="form-control" required />
          </div>
          <div class="form-group">
            <label class="form-label">Prix d'achat ou de référence (€)</label>
            <input v-model.number="form.purchasePrice" type="number" class="form-control" required />
          </div>
          <div class="form-group">
            <label class="form-label">Lien vers le véhicule (URL de redirection)</label>
            <input v-model="form.url" type="url" class="form-control" placeholder="https://example.com/vehicule" />
          </div>
        </div>

        <div>
          <div class="form-group">
            <label class="form-label">Kilométrage annuel moyen (km/an)</label>
            <input v-model.number="form.annualMileage" type="number" class="form-control" required />
          </div>
          <div class="form-group">
            <label class="form-label">Assurance annuelle (€/an)</label>
            <input v-model.number="form.insuranceCost" type="number" class="form-control" required />
          </div>
          <div class="form-group">
            <label class="form-label">Entretien annuel moyen (€/an)</label>
            <input v-model.number="form.maintenanceCost" type="number" class="form-control" required />
          </div>
          <div class="form-group">
            <label class="form-label">Valeur résiduelle/de revente estimée (€)</label>
            <input v-model.number="form.resaleValue" type="number" class="form-control" required />
          </div>
          <div class="form-group">
            <label class="form-label">Visibilité</label>
            <select v-model="form.visibility" class="form-control form-select">
              <option value="PUBLIC">Publique (visible de tous)</option>
              <option value="PRIVATE">Privée (visible uniquement par vous et les administrateurs)</option>
            </select>
          </div>
        </div>
      </div>

      <div class="flex-end gap-3 mt-4">
        <button class="btn btn-secondary" @click="formOpen = false">Annuler</button>
        <button :disabled="loading" class="btn btn-primary" @click="saveVehicle">
          <Save size="18" /> {{ editMode ? 'Mettre à jour' : 'Créer le véhicule' }}
        </button>
      </div>
    </section>

    <!-- Liste des Véhicules sous forme de cartes premium -->
    <div v-if="loading && vehicles.length === 0" class="flex-center py-5">
      <Sparkles class="spinner text-teal" size="32" />
      <span class="ml-2 text-dimmed">Chargement des véhicules...</span>
    </div>

    <div v-else-if="vehicles.length === 0" class="flex-center flex-column py-5 text-dimmed text-center border-glass rounded p-5 bg-card">
      <HelpCircle size="48" class="text-dimmed opacity-40 mb-3" />
      <h4>Aucun véhicule enregistré</h4>
      <p class="max-w-sm mt-1">Le catalogue est actuellement vide. Cliquez sur "Ajouter un véhicule" en haut à droite pour insérer votre premier modèle.</p>
    </div>

    <div v-else class="vehicles-grid">
      <div v-for="vehicle in vehicles" 
           :key="vehicle.id" 
           @click="goToVehicle(vehicle.url)"
           :class="['card-glass', 'card-glass-hover', 'flex', 'flex-column', 'justify-between', 'relative', 'overflow-hidden', vehicle.url ? 'cursor-pointer' : '']">
        
        <!-- Dégradé lumineux en arrière plan de carte -->
        <div class="glow-accent-overlay" :class="'overlay-' + vehicle.fuelType.toLowerCase()"></div>

        <div class="card-header-top mb-3 flex-between">
          <div class="flex gap-1 items-center">
            <span class="badge" :class="getFuelBadgeClass(vehicle.fuelType)">
              {{ getFuelLabel(vehicle.fuelType) }}
            </span>
            <span v-if="currentUser && vehicle.visibility" class="badge" :class="vehicle.visibility === 'PRIVATE' ? 'badge-rose' : 'badge-teal'">
              {{ vehicle.visibility === 'PRIVATE' ? 'Privé' : 'Public' }}
            </span>
          </div>
          <div v-if="currentUser && (currentUser.role === 'ADMIN' || currentUser.email === vehicle.createdBy)" class="actions flex gap-2">
            <button class="icon-btn btn-secondary-edit" @click.stop="openEditForm(vehicle)" title="Modifier">
              <Edit2 size="14" />
            </button>
            <button class="icon-btn btn-danger-delete" @click.stop="deleteVehicle(vehicle.id)" title="Supprimer">
              <Trash2 size="14" />
            </button>
          </div>
        </div>

        <h3 class="vehicle-title text-gradient text-md mb-3 flex items-center gap-1">
          {{ vehicle.name }}
          <svg v-if="vehicle.url" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-cyan"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
        </h3>

        <!-- Calcul rapide des économies (si profil présent) -->
        <div v-if="userProfile" class="savings-badge mb-3 text-center py-2 px-3 rounded" :class="calculateSavings(vehicle) > 0 ? 'bg-success-glass border-teal' : 'bg-warning-glass border-rose'">
          <div class="text-xs text-dimmed uppercase mb-1">Bilan Économique</div>
          <div class="font-bold font-heading text-lg" :class="calculateSavings(vehicle) > 0 ? 'text-teal' : 'text-rose'">
            {{ calculateSavings(vehicle) > 0 ? '+' : '' }}{{ formatCurrency(calculateSavings(vehicle)) }} / an
          </div>
        </div>
        <div v-else-if="currentUser" class="savings-badge mb-3 text-center py-2 px-3 rounded bg-deep-glass border-glass text-xs text-dimmed">
          Complétez votre profil pour voir vos économies.
        </div>

        <!-- Fiche technique -->
        <div class="specifications-sheet py-2 border-t border-glass text-xs">
          <div class="flex-between py-1">
            <span class="text-dimmed">Consommation :</span>
            <span class="font-semibold">{{ vehicle.consumption }} {{ vehicle.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km</span>
          </div>
          <div class="flex-between py-1">
            <span class="text-dimmed">Prix d'achat :</span>
            <span class="font-semibold text-cyan">{{ formatCurrency(vehicle.purchasePrice) }}</span>
          </div>
          <div class="flex-between py-1">
            <span class="text-dimmed">Usage annuel :</span>
            <span class="font-semibold">{{ vehicle.annualMileage }} km/an</span>
          </div>
          <div class="flex-between py-1">
            <span class="text-dimmed">Assurance :</span>
            <span class="font-semibold">{{ formatCurrency(vehicle.insuranceCost) }}/an</span>
          </div>
          <div class="flex-between py-1">
            <span class="text-dimmed">Entretien :</span>
            <span class="font-semibold">{{ formatCurrency(vehicle.maintenanceCost) }}/an</span>
          </div>
          <div class="flex-between py-1">
            <span class="text-dimmed">Valeur revente :</span>
            <span class="font-semibold text-amber">{{ formatCurrency(vehicle.resaleValue) }}</span>
          </div>
          <div v-if="currentUser && currentUser.role === 'ADMIN' && vehicle.createdBy" class="flex-between py-1 border-t border-glass text-cyan mt-1">
            <span class="text-dimmed">Ajouté par :</span>
            <span class="font-semibold">{{ vehicle.createdBy }}</span>
          </div>
        </div>
        
        <div class="mt-3 pt-3 border-t border-glass">
          <button class="btn btn-secondary w-100 flex-center gap-1 glow-teal" @click.stop="openSimulatorForVehicle(vehicle)">
            <Sparkles size="16" class="text-teal" /> Simuler l'achat
          </button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="!formOpen && totalPages > 1" class="pagination-controls flex-center gap-3 mt-5">
      <button 
        class="btn btn-secondary btn-small" 
        :disabled="page === 0" 
        @click="prevPage"
      >
        Précédent
      </button>
      <span class="text-xs text-dimmed">Page {{ page + 1 }} sur {{ totalPages }}</span>
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
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}
.flex-end {
  display: flex;
  justify-content: flex-end;
}
.gap-3 {
  gap: 12px;
}
.gap-2 {
  gap: 8px;
}
.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid hsl(var(--border-glass));
  background: rgba(255, 255, 255, 0.02);
  color: hsl(var(--text-muted));
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-secondary-edit:hover {
  border-color: hsl(var(--accent-cyan));
  color: hsl(var(--accent-cyan));
  background: rgba(8, 145, 178, 0.1);
}
.btn-danger-delete:hover {
  border-color: hsl(var(--accent-rose));
  color: hsl(var(--accent-rose));
  background: rgba(225, 29, 72, 0.1);
}
.vehicle-title {
  font-size: 1.1rem;
  line-height: 1.3;
}
.card-header-top {
  position: relative;
  z-index: 2;
}
.specifications-sheet {
  position: relative;
  z-index: 2;
}

/* Glow overlays for premium visuals */
.glow-accent-overlay {
  position: absolute;
  top: -60px;
  right: -60px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.15;
  pointer-events: none;
  z-index: 1;
  transition: all 0.3s ease;
}

.card-glass:hover .glow-accent-overlay {
  opacity: 0.3;
  width: 140px;
  height: 140px;
}

.overlay-electric { background-color: hsl(var(--accent-teal)); }
.overlay-hybrid { background-color: hsl(var(--accent-cyan)); }
.overlay-diesel { background-color: hsl(var(--accent-blue)); }
.overlay-petrol { background-color: hsl(var(--accent-amber)); }

.bg-success-glass { background: rgba(16, 185, 129, 0.08); }
.bg-warning-glass { background: rgba(225, 29, 72, 0.08); }
.border-teal { border: 1px solid rgba(16, 185, 129, 0.3); }
.border-rose { border: 1px solid rgba(225, 29, 72, 0.3); }
.text-teal { color: hsl(var(--accent-teal)); }
.text-rose { color: hsl(var(--accent-rose)); }
.text-cyan { color: hsl(var(--accent-cyan)); }
.text-amber { color: hsl(var(--accent-amber)); }
.text-dimmed { color: hsl(var(--text-dimmed)); }
.text-xs { font-size: 0.75rem; }
.text-sm { font-size: 0.875rem; }
.text-md { font-size: 1.05rem; }
.font-semibold { font-weight: 600; }
.border-t { border-top: 1px solid; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.py-2 { padding-top: 8px; padding-bottom: 8px; }
.mr-2 { margin-right: 0.5rem; }
.relative { position: relative; }
.overflow-hidden { overflow: hidden; }
.p-5 { padding: 3rem; }
.bg-card { background: hsl(var(--bg-glass)); }
.cursor-pointer { cursor: pointer; }
</style>
