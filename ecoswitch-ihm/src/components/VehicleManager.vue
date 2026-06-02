<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Edit2, Trash2, Shield, Settings, Eye, HelpCircle, Save, X, Sparkles, Check, Info } from '@lucide/vue'

const vehicles = ref([])
const loading = ref(false)
const error = ref(null)
const successMsg = ref(null)

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
  resaleValue: 5000
})

const fetchVehicles = async () => {
  loading.value = true
  error.value = null
  try {
    const response = await fetch('/api/v1/vehicules')
    if (!response.ok) throw new Error('Impossible de charger le catalogue de véhicules.')
    vehicles.value = await response.json()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
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
    resaleValue: 8000
  }
  formOpen.value = true
}

const openEditForm = (vehicle) => {
  editMode.value = true
  activeVehicleId.value = vehicle.id
  form.value = { ...vehicle }
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
      headers: { 'Content-Type': 'application/json' },
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
      method: 'DELETE'
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
      <button v-if="!formOpen" class="btn btn-primary" @click="openAddForm">
        <Plus size="18" /> Ajouter un véhicule
      </button>
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
      <div v-for="vehicle in vehicles" :key="vehicle.id" class="card-glass card-glass-hover flex flex-column justify-between relative overflow-hidden">
        
        <!-- Dégradé lumineux en arrière plan de carte -->
        <div class="glow-accent-overlay" :class="'overlay-' + vehicle.fuelType.toLowerCase()"></div>

        <div class="card-header-top mb-3 flex-between">
          <span class="badge" :class="getFuelBadgeClass(vehicle.fuelType)">
            {{ getFuelLabel(vehicle.fuelType) }}
          </span>
          <div class="actions flex gap-2">
            <button class="icon-btn btn-secondary-edit" @click="openEditForm(vehicle)" title="Modifier">
              <Edit2 size="14" />
            </button>
            <button class="icon-btn btn-danger-delete" @click="deleteVehicle(vehicle.id)" title="Supprimer">
              <Trash2 size="14" />
            </button>
          </div>
        </div>

        <h3 class="vehicle-title text-gradient text-md mb-3">{{ vehicle.name }}</h3>

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
        </div>
      </div>
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
.py-5 { padding-top: 3rem; padding-bottom: 3rem; }
.p-5 { padding: 3rem; }
.bg-card { background: hsl(var(--bg-glass)); }
</style>
