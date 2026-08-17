<script setup>
import { ref, onMounted } from 'vue'
import { Calendar, Trash2, ArrowUpRight, Zap, HelpCircle, Sparkles, AlertCircle } from '@lucide/vue'
import { apiGetSimulations, apiDeleteSimulation } from '../utils/api.js'

const props = defineProps({
  currentUser: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['load-simulation'])

const savedList = ref([])
const isLoading = ref(false)
const loadError = ref('')

const loadSavedSimulations = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const data = await apiGetSimulations()
    savedList.value = data.map(sim => ({
      ...JSON.parse(sim.simulationData),
      id: sim.id,
      name: sim.name,
      createdAt: sim.savedAt
    }))
  } catch (err) {
    if (err.message === 'SESSION_EXPIRED') {
      loadError.value = 'Votre session a expiré. Reconnectez-vous pour voir vos simulations.'
      localStorage.removeItem('saas_user')
      localStorage.removeItem('saas_token')
    } else {
      loadError.value = 'Impossible de charger les simulations : ' + err.message
    }
  } finally {
    isLoading.value = false
  }
}

const deleteSimulation = async (id) => {
  if (!confirm('Voulez-vous supprimer cette simulation enregistrée ?')) return
  try {
    await apiDeleteSimulation(id)
    savedList.value = savedList.value.filter(sim => sim.id !== id)
  } catch (err) {
    if (err.message === 'SESSION_EXPIRED') {
      alert('Session expirée. Veuillez vous reconnecter.')
      window.location.reload()
    } else {
      alert('Erreur lors de la suppression : ' + err.message)
    }
  }
}

const triggerLoad = (sim) => {
  emit('load-simulation', sim)
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

const formatDate = (dateStr) => {
  return new Date(dateStr).toLocaleDateString('fr-FR', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(() => {
  loadSavedSimulations()
})
</script>

<template>
  <div class="saved-simulations-container animation-fadeIn">
    <div class="header-section mb-4">
      <h2 class="text-main font-heading text-xl font-bold mb-1">Vos Simulations Enregistrées</h2>
      <p class="text-muted text-xs m-0">Retrouvez toutes vos analyses de rentabilité et rouvrez-les dans le simulateur.</p>
    </div>

    <!-- Chargement -->
    <div v-if="isLoading" class="flex-center py-5 text-muted text-xs">
      <Sparkles class="spinner text-teal mr-2" size="20" /> Chargement de vos simulations...
    </div>

    <!-- Erreur -->
    <div v-else-if="loadError" class="card-glass p-4 text-center">
      <AlertCircle size="32" class="text-rose mx-auto mb-2" />
      <p class="text-rose text-xs font-semibold m-0">{{ loadError }}</p>
    </div>

    <!-- Vide -->
    <div v-else-if="savedList.length === 0" class="flex-center flex-column py-5 text-center card-glass">
      <HelpCircle size="40" class="text-dimmed opacity-40 mb-2" />
      <h4 class="text-main font-heading text-sm font-bold m-0">Aucune simulation enregistrée</h4>
      <p class="text-xs text-muted max-w-sm mt-1 m-0">
        Effectuez un calcul dans le Simulateur et cliquez sur "Enregistrer" pour conserver vos analyses.
      </p>
    </div>

    <!-- Grille des simulations -->
    <div v-else class="simulations-grid">
      <div
        v-for="sim in savedList"
        :key="sim.id"
        class="card-glass p-4 flex flex-column justify-between relative"
      >
        <div>
          <!-- En-tête -->
          <div class="flex-between items-center mb-2.5">
            <span class="badge badge-small" :class="sim.result?.breakEvenYear ? 'badge-teal' : 'badge-amber'">
              {{ sim.result?.breakEvenYear ? `Rentable en ${sim.result.breakEvenYear} ans` : 'Long terme' }}
            </span>
            <span class="text-xxs text-dimmed flex items-center gap-1">
              <Calendar size="12" /> {{ formatDate(sim.createdAt) }}
            </span>
          </div>

          <!-- Titre & Note -->
          <div class="mb-3">
            <h3 class="text-main font-bold text-sm mb-1">{{ sim.name }}</h3>
            <p v-if="sim.note" class="text-xxs text-muted italic m-0">« {{ sim.note }} »</p>
          </div>

          <!-- Détails -->
          <div class="specs-box py-2 border-t border-glass text-xs flex flex-column gap-1">
            <div class="flex-between">
              <span class="text-dimmed">Actuel :</span>
              <span class="font-semibold text-main truncate max-w-150">{{ sim.currentVehicle?.name }}</span>
            </div>
            <div class="flex-between">
              <span class="text-dimmed">Cible :</span>
              <span class="font-semibold text-teal truncate max-w-150">{{ sim.targetVehicle?.name }}</span>
            </div>
            <div class="flex-between">
              <span class="text-dimmed">Économie / an :</span>
              <span class="font-bold text-teal">{{ formatCurrency(sim.result?.annualSavings ?? 0) }}</span>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex-between items-center border-t border-glass pt-2.5 mt-3">
          <button class="btn btn-primary btn-small flex items-center gap-1 text-xs font-bold" @click="triggerLoad(sim)">
            <span>Recharger</span>
            <ArrowUpRight size="13" />
          </button>
          <button class="icon-btn hover-rose" @click="deleteSimulation(sim.id)" title="Supprimer">
            <Trash2 size="13" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.simulations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 16px;
}

.max-w-150 {
  max-width: 150px;
}

.icon-btn {
  background: hsl(var(--bg-card-subtle));
  border: 1px solid hsl(var(--border-glass));
  color: hsl(var(--text-dimmed));
  width: 28px;
  height: 28px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}
.icon-btn:hover.hover-rose {
  color: hsl(var(--accent-rose));
  border-color: hsl(var(--accent-rose));
}
</style>
