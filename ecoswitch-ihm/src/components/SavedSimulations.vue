<script setup>
import { ref, onMounted } from 'vue'
import { Calendar, Trash2, ArrowUpRight, Zap, HelpCircle } from '@lucide/vue'
import { apiGetSimulations, apiDeleteSimulation } from '../utils/api.js'

const props = defineProps({
  currentUser: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['load-simulation'])

const savedList  = ref([])
const isLoading  = ref(false)
const loadError  = ref('')

// ── Chargement ─────────────────────────────────────────────────────────────

const loadSavedSimulations = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const data = await apiGetSimulations()
    // Chaque élément a { id, name, savedAt, simulationData (JSON string) }
    savedList.value = data.map(sim => ({
      ...JSON.parse(sim.simulationData),
      id:        sim.id,
      name:      sim.name,
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

// ── Suppression ────────────────────────────────────────────────────────────

const deleteSimulation = async (id) => {
  if (!confirm('Voulez-vous supprimer cette simulation de votre espace ?')) return
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

// ── Chargement dans le simulateur ─────────────────────────────────────────

const triggerLoad = (sim) => {
  emit('load-simulation', sim)
}

// ── Formatage ──────────────────────────────────────────────────────────────

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
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
  <div class="saved-simulations-container">
    <div class="header-section text-center mb-5">
      <h2 class="text-gradient mb-2">Vos Simulations Sauvegardées</h2>
      <p class="text-muted">Retrouvez toutes vos analyses financières personnalisées et rechargez-les instantanément.</p>
    </div>

    <!-- Chargement -->
    <div v-if="isLoading" class="flex-center py-5 text-dimmed">
      <span class="spinner-mini mr-2"></span> Chargement de vos simulations…
    </div>

    <!-- Erreur -->
    <div v-else-if="loadError" class="flex-center flex-column py-5 text-center border-glass rounded p-5 bg-card">
      <p class="text-rose text-sm">{{ loadError }}</p>
    </div>

    <!-- Vide -->
    <div v-else-if="savedList.length === 0" class="flex-center flex-column py-5 text-dimmed text-center border-glass rounded p-5 bg-card">
      <HelpCircle size="48" class="text-dimmed opacity-40 mb-3" />
      <h4>Aucune simulation sauvegardée</h4>
      <p class="max-w-sm mt-1">Vous n'avez pas encore enregistré de simulation. Rendez-vous sur le "Simulateur direct", effectuez un calcul et cliquez sur "Sauvegarder".</p>
    </div>

    <!-- Liste -->
    <div v-else class="simulations-grid">
      <div v-for="sim in savedList" :key="sim.id" class="card-glass card-glass-hover flex flex-column justify-between relative overflow-hidden">

        <!-- En-tête -->
        <div class="flex-between mb-3">
          <span class="badge badge-teal flex-center gap-1">
            <Zap size="12" /> Rentable
          </span>
          <span class="text-xxs text-dimmed flex-center gap-1">
            <Calendar size="12" /> {{ formatDate(sim.createdAt) }}
          </span>
        </div>

        <!-- Titre -->
        <div class="mb-3">
          <h3 class="text-gradient text-md mb-1">{{ sim.name }}</h3>
          <p v-if="sim.note" class="text-xs text-muted italic">"{{ sim.note }}"</p>
        </div>

        <!-- Comparaison rapide -->
        <div class="specifications-sheet py-2 border-t border-glass text-xs mb-4">
          <div class="flex-between py-1">
            <span class="text-dimmed">Actuel :</span>
            <span class="font-semibold">{{ sim.currentVehicle?.name }}</span>
          </div>
          <div class="flex-between py-1">
            <span class="text-dimmed">Cible :</span>
            <span class="font-semibold text-teal">{{ sim.targetVehicle?.name }}</span>
          </div>
          <div class="flex-between py-1">
            <span class="text-dimmed">Seuil rentabilité :</span>
            <span class="font-semibold text-cyan">{{ sim.result?.breakEvenYear ? sim.result.breakEvenYear + ' ans' : 'Non rentable' }}</span>
          </div>
          <div class="flex-between py-1">
            <span class="text-dimmed">Économies :</span>
            <span class="font-semibold text-teal">{{ formatCurrency(sim.result?.annualSavings ?? 0) }}/an</span>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex-between border-t border-glass pt-3 mt-auto">
          <button class="btn btn-secondary btn-small flex-center gap-1" @click="triggerLoad(sim)">
            <span>Recharger</span>
            <ArrowUpRight size="14" />
          </button>
          <button class="icon-btn btn-danger-delete" @click="deleteSimulation(sim.id)" title="Supprimer">
            <Trash2 size="14" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.simulations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.specifications-sheet {
  background: rgba(255, 255, 255, 0.01);
}
.icon-btn {
  display: flex; align-items: center; justify-content: center;
  width: 28px; height: 28px;
  border-radius: 6px;
  border: 1px solid hsl(var(--border-glass));
  background: rgba(255, 255, 255, 0.02);
  color: hsl(var(--text-muted));
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-danger-delete:hover {
  border-color: hsl(var(--accent-rose));
  color: hsl(var(--accent-rose));
  background: rgba(225, 29, 72, 0.1);
}
.spinner-mini {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.15);
  border-top-color: hsl(var(--accent-cyan));
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  display: inline-block;
}
@keyframes spin { to { transform: rotate(360deg); } }
.text-rose { color: hsl(355, 80%, 72%); }
.text-gradient {
  background: linear-gradient(135deg, hsl(var(--text-main)) 30%, hsl(var(--accent-cyan)) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.text-teal  { color: hsl(var(--accent-teal)); }
.text-cyan  { color: hsl(var(--accent-cyan)); }
.text-muted { color: hsl(var(--text-muted)); }
.text-dimmed{ color: hsl(var(--text-dimmed)); }
.text-xs  { font-size: 0.75rem; }
.text-xxs { font-size: 0.65rem; }
.text-sm  { font-size: 0.875rem; }
.text-md  { font-size: 1.05rem; }
.font-semibold { font-weight: 600; }
.border-t  { border-top: 1px solid; }
.pt-3      { padding-top: 12px; }
.py-1      { padding-top: 4px; padding-bottom: 4px; }
.py-2      { padding-top: 8px; padding-bottom: 8px; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-center  { display: flex; align-items: center; justify-content: center; }
.gap-1 { gap: 4px; }
.gap-2 { gap: 8px; }
.bg-card { background: hsl(var(--bg-glass)); }
.mr-2 { margin-right: 8px; }
</style>
