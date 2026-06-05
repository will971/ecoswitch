<script setup>
import { ref, computed } from 'vue'
import { ArrowLeft, DollarSign, TrendingUp, CheckCircle2, AlertCircle, FileSpreadsheet, Save, X, Share2, PiggyBank, Scale, Sparkles, Fuel, CreditCard } from '@lucide/vue'
import { apiSaveSimulation } from '../../utils/api.js'

// Import des sous-composants dans le même dossier
import ArbitrageCard from './ArbitrageCard.vue'
import AdvisorRecommendations from './AdvisorRecommendations.vue'
import ProjectionsTable from './ProjectionsTable.vue'
import CarbonFootprintCard from './CarbonFootprintCard.vue'
import ShareModal from './ShareModal.vue'

const props = defineProps({
  result: {
    type: Object,
    required: true
  },
  currentVehicle: {
    type: Object,
    required: true
  },
  targetVehicle: {
    type: Object,
    required: true
  },
  fuelPrices: {
    type: Object,
    required: true
  },
  maxYears: {
    type: Number,
    required: true
  },
  immediateRepairCost: {
    type: Number,
    default: 0
  },
  isLeasing: {
    type: Boolean,
    default: false
  },
  currentUser: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['back', 'load-alternative'])

const showShareModal = ref(false)
const showSaveModal = ref(false)
const saveNote = ref('')

// Calculs du budget mensuel détaillé
const currentMonthlyUsage = computed(() => props.result.currentAnnualCost / 12.0)
const targetMonthlyUsage = computed(() => props.result.targetAnnualCost / 12.0)
const monthlyUsageSavings = computed(() => currentMonthlyUsage.value - targetMonthlyUsage.value)

const currentMonthlyFinancing = computed(() => 0.0) // véhicule actuel supposé payé
const targetMonthlyFinancing = computed(() => {
  if (props.isLeasing) {
    return props.result.targetMonthlyTotalCost - targetMonthlyUsage.value
  }
  return 0.0
})

const currentMonthlyTotal = computed(() => currentMonthlyUsage.value + currentMonthlyFinancing.value)
const targetMonthlyTotal = computed(() => targetMonthlyUsage.value + targetMonthlyFinancing.value)
const netMonthlySavings = computed(() => currentMonthlyTotal.value - targetMonthlyTotal.value)

const getYearlyForecast = () => {
  if (!props.result) return []
  
  const forecast = []
  let cumulativeCurrent = props.immediateRepairCost
  let cumulativeTarget = props.result.switchInvestment

  for (let year = 1; year <= props.maxYears; year++) {
    cumulativeCurrent += props.result.currentAnnualCost
    cumulativeTarget += props.result.targetAnnualCost
    const diff = cumulativeTarget - cumulativeCurrent
    const isProfitable = diff <= 0

    forecast.push({
      year,
      currentCost: cumulativeCurrent,
      targetCost: cumulativeTarget,
      difference: diff,
      isProfitable
    })
  }
  return forecast
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
}

const exportToCSV = () => {
  if (!props.result) return
  
  const yearly = getYearlyForecast()
  let csvContent = "data:text/csv;charset=utf-8,"
    + "Annee,Cout Actuel Cumule (avec reparations) (EUR),Cout Cible Cumule (EUR),Bilan Net Cumule (EUR),Etat Rentabilite\n"

  yearly.forEach(row => {
    csvContent += `${row.year},${row.currentCost.toFixed(2)},${row.targetCost.toFixed(2)},${row.difference.toFixed(2)},${row.isProfitable ? 'Rentable' : 'Deficit'}\n`
  })

  const encodedUri = encodeURI(csvContent)
  const link = document.createElement("a")
  link.setAttribute("href", encodedUri)
  const currentName = props.currentVehicle.name.replace(/\s+/g, '_')
  const targetName = props.targetVehicle.name.replace(/\s+/g, '_')
  link.setAttribute("download", `Simulation_Rentabilite_${currentName}_vs_${targetName}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const triggerSave = () => {
  if (!props.currentUser) {
    alert("Veuillez d'abord vous connecter pour sauvegarder vos simulations.")
    return
  }
  showSaveModal.value = true
}

const confirmSave = async () => {
  const simData = {
    currentVehicle:    { ...props.currentVehicle },
    targetVehicle:     { ...props.targetVehicle },
    fuelPricesByType:  { ...props.fuelPrices },
    maxYears:          props.maxYears,
    immediateRepairCost: props.immediateRepairCost,
    isLeasing:         props.isLeasing,
    result:            { ...props.result },
    note:              saveNote.value
  }
  const simName = `${props.currentVehicle.name} vs ${props.targetVehicle.name}`

  try {
    await apiSaveSimulation(simName, simData)
    showSaveModal.value = false
    saveNote.value = ''
    alert('Simulation enregistrée avec succès dans votre espace !')
  } catch (err) {
    if (err.message === 'SESSION_EXPIRED') {
      alert('Votre session a expiré. Veuillez vous reconnecter.')
      localStorage.removeItem('saas_user')
      localStorage.removeItem('saas_token')
      window.location.reload()
    } else {
      alert('Erreur lors de la sauvegarde : ' + err.message)
    }
  }
}

const handleLoadAlternative = (rec) => {
  emit('load-alternative', rec)
}
</script>

<template>
  <div class="apple-results-dashboard">
    
    <!-- Header / Actions de type Apple Toolbar -->
    <div class="apple-toolbar mb-5">
      <button class="apple-btn-back" @click="emit('back')">
        <ArrowLeft size="16" />
        <span>Modifier la saisie</span>
      </button>
      <div class="apple-toolbar-actions">
        <button class="apple-btn-secondary" @click="showShareModal = true" title="Partager">
          <Share2 size="14" />
          <span>Partager</span>
        </button>
        <button class="apple-btn-secondary" @click="exportToCSV" title="Exporter">
          <FileSpreadsheet size="14" />
          <span>Exporter</span>
        </button>
        <button class="apple-btn-primary" @click="triggerSave" title="Enregistrer">
          <Save size="14" />
          <span>Enregistrer</span>
        </button>
      </div>
    </div>

    <!-- Bannière Diagnostic iOS Style -->
    <div class="apple-alert-card mb-5" :class="result.breakEvenYear ? 'alert-success' : 'alert-warning'">
      <div class="alert-icon-wrapper">
        <CheckCircle2 v-if="result.breakEvenYear" size="24" class="text-teal" />
        <AlertCircle v-else size="24" class="text-rose" />
      </div>
      <div class="alert-content">
        <h4 class="alert-title">
          {{ result.breakEvenYear ? 'Changement rentable' : 'Investissement à long terme' }}
        </h4>
        <p class="alert-description">
          {{ result.breakEvenYear 
            ? `L'économie réalisée sur les coûts d'usage (énergie, assurance et entretien) amortit l'investissement de transition en ${result.breakEvenYear} ans.`
            : `Le coût cumulé du nouveau véhicule (incluant son coût d'acquisition ou loyers) reste supérieur à votre véhicule actuel sur votre horizon de ${maxYears} ans.`
          }}
        </p>
      </div>
      <div class="alert-badge" v-if="result.breakEvenYear">
        <span class="badge-label">Point Mort</span>
        <span class="badge-value text-teal">{{ result.breakEvenYear }} {{ result.breakEvenYear > 1 ? 'ans' : 'an' }}</span>
      </div>
      <div class="alert-badge" v-else>
        <span class="badge-label">Horizon</span>
        <span class="badge-value text-rose">> {{ maxYears }} ans</span>
      </div>
    </div>

    <!-- Section titre de section minimaliste -->
    <div class="section-title-wrapper mb-4">
      <h3 class="apple-section-title">Analyse Financière Mensuelle</h3>
      <p class="apple-section-subtitle">Comparaison de l'impact de la transition sur votre budget mensuel courant.</p>
    </div>

    <!-- Bento Grid : Partie Budget Mensuel (3 cartes) -->
    <div class="apple-bento-grid mb-5">
      
      <!-- Carte Budget Actuel -->
      <div class="apple-bento-card">
        <span class="card-tag">Budget Mensuel Actuel</span>
        <h5 class="card-vehicle-title">{{ currentVehicle.name }}</h5>
        <div class="card-primary-value text-rose">{{ formatCurrency(currentMonthlyTotal) }}<span class="value-period">/mois</span></div>
        
        <div class="card-details-divider"></div>
        
        <div class="card-detail-row">
          <div class="detail-label flex items-center gap-1.5">
            <Fuel size="14" class="text-rose" />
            <span>Usage & Énergie</span>
          </div>
          <span class="detail-value font-semibold">{{ formatCurrency(currentMonthlyUsage) }}</span>
        </div>
        <div class="card-detail-row">
          <div class="detail-label flex items-center gap-1.5">
            <CreditCard size="14" class="text-rose" />
            <span>Financement / Loyer</span>
          </div>
          <span class="detail-value font-semibold">{{ formatCurrency(currentMonthlyFinancing) }}</span>
        </div>
      </div>

      <!-- Carte Budget Nouveau -->
      <div class="apple-bento-card">
        <span class="card-tag">Budget Mensuel Nouveau</span>
        <h5 class="card-vehicle-title">{{ targetVehicle.name }}</h5>
        <div class="card-primary-value text-cyan">{{ formatCurrency(targetMonthlyTotal) }}<span class="value-period">/mois</span></div>
        
        <div class="card-details-divider"></div>
        
        <div class="card-detail-row">
          <div class="detail-label flex items-center gap-1.5">
            <Fuel size="14" class="text-teal" />
            <span>Usage & Électricité</span>
          </div>
          <span class="detail-value font-semibold">{{ formatCurrency(targetMonthlyUsage) }}</span>
        </div>
        <div class="card-detail-row">
          <div class="detail-label flex items-center gap-1.5">
            <CreditCard size="14" class="text-cyan" />
            <span>Financement / Loyer</span>
          </div>
          <span class="detail-value font-semibold">{{ formatCurrency(targetMonthlyFinancing) }}</span>
        </div>
      </div>

      <!-- Carte Bilan Mensuel (Impact direct) -->
      <div class="apple-bento-card card-highlighted" :class="netMonthlySavings > 0 ? 'highlight-success' : 'highlight-warning'">
        <span class="card-tag">Impact Direct Trésorerie</span>
        <h5 class="card-vehicle-title">{{ netMonthlySavings > 0 ? 'Gain mensuel' : 'Surcoût mensuel' }}</h5>
        <div class="card-primary-value font-bold" :class="netMonthlySavings > 0 ? 'text-teal' : 'text-rose'">
          {{ netMonthlySavings > 0 ? '+' : '' }}{{ formatCurrency(netMonthlySavings) }}<span class="value-period">/mois</span>
        </div>
        
        <div class="card-details-divider"></div>
        
        <div class="verdict-bubble">
          <Sparkles size="14" class="text-teal shrink-0 mt-0.5" />
          <p class="verdict-description">
            <span v-if="!isLeasing">
              Achat comptant : vous réduisez vos coûts énergétiques mensuels de <strong>{{ formatCurrency(monthlyUsageSavings) }} / mois</strong>.
            </span>
            <span v-else>
              <span v-if="netMonthlySavings > 0">
                L'économie d'énergie de <strong>{{ formatCurrency(monthlyUsageSavings) }} / mois</strong> compense entièrement le loyer de leasing.
              </span>
              <span v-else>
                Le loyer de leasing de <strong>{{ formatCurrency(targetMonthlyFinancing) }} / mois</strong> surpasse vos économies d'énergie mensuelles.
              </span>
            </span>
          </p>
        </div>
      </div>

    </div>

    <!-- Section titre de section long terme -->
    <div class="section-title-wrapper mb-4">
      <h3 class="apple-section-title">Synthèse Financière à long terme</h3>
      <p class="apple-section-subtitle">Bilan projeté et retour sur investissement sur l'horizon de {{ maxYears }} ans.</p>
    </div>

    <!-- Bento Grid : Partie Long Terme (3 cartes) -->
    <div class="apple-bento-grid mb-5">
      
      <!-- Carte Économie Annuelle -->
      <div class="apple-bento-card">
        <span class="card-tag">Lissé à l'année</span>
        <h5 class="card-vehicle-title">Économie Annuelle Moyenne</h5>
        <div class="card-primary-value" :class="result.annualSavings > 0 ? 'text-teal' : 'text-rose'">
          {{ formatCurrency(result.annualSavings) }}<span class="value-period">/an</span>
        </div>
        <div class="card-details-divider"></div>
        <p class="card-bottom-caption text-xs text-muted">Économie moyenne d'usage calculée sur l'ensemble de la période.</p>
      </div>

      <!-- Carte Investissement Transition -->
      <div class="apple-bento-card">
        <span class="card-tag">Investissement Initial</span>
        <h5 class="card-vehicle-title">Coût Net de Transition</h5>
        <div class="card-primary-value text-rose">{{ formatCurrency(result.switchInvestment) }}</div>
        <div class="card-details-divider"></div>
        <p class="card-bottom-caption text-xs text-muted" v-if="result.totalSubsidies > 0">
          Aides d'État déduites (bonus et conversion) : -{{ formatCurrency(result.totalSubsidies) }}
        </p>
        <p class="card-bottom-caption text-xs text-muted" v-else>
          Aucune aide d'État applicable estimée pour ce véhicule.
        </p>
      </div>

      <!-- Carte Bilan cumulé -->
      <div class="apple-bento-card">
        <span class="card-tag">Horizon à {{ maxYears }} ans</span>
        <h5 class="card-vehicle-title">Bilan Cumulé Final</h5>
        <div class="card-primary-value" :class="result.totalCostDeltaAtHorizon <= 0 ? 'text-teal' : 'text-rose'">
          {{ result.totalCostDeltaAtHorizon <= 0 ? '+' : '' }}{{ formatCurrency(-result.totalCostDeltaAtHorizon) }}
        </div>
        <div class="card-details-divider"></div>
        <p class="card-bottom-caption text-xs text-muted">
          {{ result.totalCostDeltaAtHorizon <= 0 
            ? 'Gain financier net accumulé sur votre budget global.' 
            : 'Coût net cumulé de la transition sur cette période.' 
          }}
        </p>
      </div>

    </div>

    <!-- Éléments larges du bas (Projections, CarbonFootprint, Alternatives) -->
    <div class="apple-wide-sections flex flex-column gap-5">
      
      <!-- Bilan Écologique CO2 -->
      <CarbonFootprintCard :result="result" />

      <!-- Arbitrage Financier (Si frais de réparation) -->
      <ArbitrageCard
        v-if="immediateRepairCost > 0"
        :result="result"
        :immediateRepairCost="immediateRepairCost"
        :currentVehicle="currentVehicle"
      />

      <!-- Recommandations Intelligentes -->
      <AdvisorRecommendations
        v-if="result.recommendations && result.recommendations.length > 0"
        :recommendations="result.recommendations"
        @load-alternative="handleLoadAlternative"
      />

      <!-- Tableau des Projections -->
      <div class="apple-bento-large-card p-4">
        <h4 class="font-heading text-sm text-gradient-teal mb-4">Tableau de Projection Annuelle Cumulative</h4>
        <ProjectionsTable :yearlyForecast="getYearlyForecast()" />
      </div>

    </div>

    <!-- Modal d'enregistrement des simulations -->
    <div v-if="showSaveModal" class="auth-modal-overlay flex-center">
      <div class="card-glass glow-teal auth-modal-card p-4 relative max-w-md w-100">
        <button class="absolute top-4 right-4 text-dimmed hover-text-main" @click="showSaveModal = false">
          <X size="20" />
        </button>
        <h3 class="text-gradient mb-3 font-heading">Enregistrer la Simulation</h3>
        <p class="text-xs text-muted mb-4">Ajoutez une note pour retrouver facilement cette simulation dans votre espace.</p>
        
        <div class="form-group mb-4">
          <label class="form-label">Note / Mémo descriptif</label>
          <input v-model="saveNote" type="text" class="form-control" placeholder="ex: Projet Tesla été 2026, km augmenté" required />
        </div>

        <div class="flex-between gap-3">
          <button class="btn btn-secondary w-50" @click="showSaveModal = false">Annuler</button>
          <button class="btn btn-primary w-50" @click="confirmSave">Enregistrer</button>
        </div>
      </div>
    </div>

    <!-- Modal de Partage Social -->
    <ShareModal
      :show="showShareModal"
      :currentVehicle="currentVehicle"
      :targetVehicle="targetVehicle"
      :result="result"
      @close="showShareModal = false"
    />
  </div>
</template>

<style scoped>
.apple-results-dashboard {
  animation: fadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  padding: 1rem 0;
}

/* ── Apple Style Toolbar ── */
.apple-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.apple-btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: none;
  font-family: var(--font-heading);
  font-size: 0.9rem;
  font-weight: 500;
  color: hsl(var(--accent-teal));
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}
.apple-btn-back:hover {
  background: hsla(var(--accent-teal) / 0.08);
}
.apple-toolbar-actions {
  display: flex;
  gap: 8px;
}
.apple-btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: hsl(var(--bg-deep) / 0.4);
  border: 1px solid hsl(var(--border-glass));
  border-radius: 20px;
  padding: 8px 16px;
  font-family: var(--font-heading);
  font-size: 0.85rem;
  font-weight: 500;
  color: hsl(var(--text-main));
  cursor: pointer;
  transition: all 0.2s ease;
}
.apple-btn-secondary:hover {
  background: hsl(var(--bg-deep) / 0.8);
  border-color: hsl(var(--accent-cyan) / 0.4);
}
.apple-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, hsl(var(--accent-teal)) 0%, hsl(var(--accent-cyan)) 100%);
  border: none;
  border-radius: 20px;
  padding: 8px 16px;
  font-family: var(--font-heading);
  font-size: 0.85rem;
  font-weight: 600;
  color: hsl(var(--bg-deep));
  cursor: pointer;
  box-shadow: 0 4px 12px hsla(var(--accent-cyan) / 0.15);
  transition: all 0.2s ease;
}
.apple-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px hsla(var(--accent-cyan) / 0.25);
}

/* ── iOS Style Notification Card ── */
.apple-alert-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background: hsl(var(--bg-glass));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 20px;
  padding: 20px 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.03);
}
.alert-icon-wrapper {
  margin-top: 2px;
  flex-shrink: 0;
}
.alert-content {
  flex: 1;
}
.alert-title {
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: 700;
  margin: 0 0 4px 0;
  color: hsl(var(--text-main));
}
.alert-description {
  font-size: 0.825rem;
  line-height: 1.5;
  color: hsl(var(--text-muted));
  margin: 0;
}
.alert-badge {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  flex-shrink: 0;
  padding-left: 16px;
  border-left: 1px solid hsl(var(--border-glass));
}
.badge-label {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: hsl(var(--text-dimmed));
  font-weight: 600;
}
.badge-value {
  font-family: var(--font-heading);
  font-size: 1.4rem;
  font-weight: 800;
  margin-top: 2px;
}

/* ── Section Titles ── */
.section-title-wrapper {
  border-left: 3px solid hsl(var(--accent-teal));
  padding-left: 12px;
}
.apple-section-title {
  font-family: var(--font-heading);
  font-size: 1.15rem;
  font-weight: 700;
  margin: 0;
  color: hsl(var(--text-main));
}
.apple-section-subtitle {
  font-size: 0.8rem;
  color: hsl(var(--text-muted));
  margin: 4px 0 0 0;
}

/* ── Bento Grid Layout ── */
.apple-bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
@media (max-width: 992px) {
  .apple-bento-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 680px) {
  .apple-bento-grid {
    grid-template-columns: 1fr;
  }
}

.apple-bento-card {
  background: hsl(var(--bg-glass));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 22px;
  padding: 24px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.02);
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), border-color 0.2s ease;
  display: flex;
  flex-direction: column;
}
.apple-bento-card:hover {
  transform: translateY(-2px);
  border-color: hsl(var(--border-glass-focus));
}

.card-tag {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: hsl(var(--text-dimmed));
  margin-bottom: 6px;
}
.card-vehicle-title {
  font-family: var(--font-heading);
  font-size: 0.95rem;
  font-weight: 700;
  color: hsl(var(--text-main));
  margin: 0 0 12px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-primary-value {
  font-family: var(--font-heading);
  font-size: 2.1rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  margin-bottom: auto;
}
.value-period {
  font-size: 0.85rem;
  font-weight: 400;
  letter-spacing: normal;
  color: hsl(var(--text-muted));
  margin-left: 2px;
}

.card-details-divider {
  height: 1px;
  background: hsl(var(--border-glass));
  margin: 18px 0;
  width: 100%;
}

.card-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.card-detail-row:last-child {
  margin-bottom: 0;
}
.detail-label {
  font-size: 0.775rem;
  color: hsl(var(--text-muted));
}
.detail-value {
  font-size: 0.825rem;
  color: hsl(var(--text-main));
}

/* Delta / Highlight card styling */
.highlight-success {
  background: hsla(var(--accent-teal) / 0.04);
  border-color: hsla(var(--accent-teal) / 0.25);
}
.highlight-warning {
  background: hsla(var(--accent-rose) / 0.04);
  border-color: hsla(var(--accent-rose) / 0.25);
}

.verdict-bubble {
  display: flex;
  gap: 10px;
  background: hsl(var(--bg-deep) / 0.3);
  border-radius: 12px;
  padding: 12px;
  margin-top: auto;
}
.verdict-description {
  font-size: 0.75rem;
  line-height: 1.4;
  color: hsl(var(--text-muted));
  margin: 0;
}

/* ── Wide Sections (Bottom) ── */
.apple-wide-sections {
  margin-top: 20px;
}
.apple-bento-large-card {
  background: hsl(var(--bg-glass));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 22px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.02);
}

/* Modal overlays */
.auth-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 1000;
}
.auth-modal-card {
  z-index: 1001;
  background: hsl(var(--bg-deep) / 0.95);
  border-radius: 20px;
}

.text-teal { color: hsl(var(--accent-teal)) !important; }
.text-rose { color: hsl(var(--accent-rose)) !important; }
.text-cyan { color: hsl(var(--accent-cyan)) !important; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
