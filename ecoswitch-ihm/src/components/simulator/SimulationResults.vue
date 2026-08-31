<script setup>
import { ref, computed } from 'vue'
import {
  ArrowLeft,
  DollarSign,
  TrendingUp,
  CheckCircle2,
  AlertCircle,
  FileSpreadsheet,
  Save,
  X,
  Share2,
  PiggyBank,
  Scale,
  Sparkles,
  Fuel,
  CreditCard,
  ChevronDown,
  ChevronUp,
  Layers,
  Check
} from '@lucide/vue'
import { apiSaveSimulation } from '../../utils/api.js'

// Import des sous-composants
import MobilityInsightCard from './MobilityInsightCard.vue'
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
  homeChargingRatio: {
    type: Number,
    default: 0.85
  },
  taxIncome: {
    type: Number,
    default: null
  },
  scrapVehicle: {
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
const showDetailedTables = ref(false)
const saveSuccessToast = ref(false)

// Labels d'affichage : fallback sur "Votre véhicule" si le nom n'est pas renseigné
const currentVehicleLabel = computed(() => props.currentVehicle?.name?.trim() || 'Votre véhicule')
const targetVehicleLabel  = computed(() => props.targetVehicle?.name?.trim()  || 'Véhicule cible')

// Calculs du budget mensuel
const currentMonthlyUsage = computed(() => (props.result?.currentAnnualCost || 0) / 12.0)
const targetMonthlyUsage = computed(() => (props.result?.targetAnnualCost || 0) / 12.0)
const monthlyUsageSavings = computed(() => currentMonthlyUsage.value - targetMonthlyUsage.value)

const currentMonthlyFinancing = computed(() => 0.0)
const targetMonthlyFinancing = computed(() => {
  if (props.isLeasing && props.result?.targetMonthlyTotalCost !== undefined) {
    return Math.max(0, props.result.targetMonthlyTotalCost - targetMonthlyUsage.value)
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
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

const exportToCSV = () => {
  if (!props.result) return
  
  const yearly = getYearlyForecast()
  let csvContent = "data:text/csv;charset=utf-8,"
    + "Annee,Cout Actuel Cumule (EUR),Cout Cible Cumule (EUR),Bilan Net Cumule (EUR),Etat Rentabilite\n"

  yearly.forEach(row => {
    csvContent += `${row.year},${row.currentCost.toFixed(2)},${row.targetCost.toFixed(2)},${row.difference.toFixed(2)},${row.isProfitable ? 'Rentable' : 'Deficit'}\n`
  })

  const encodedUri = encodeURI(csvContent)
  const link = document.createElement("a")
  link.setAttribute("href", encodedUri)
  const currentName = currentVehicleLabel.value.replace(/\s+/g, '_')
  const targetName  = targetVehicleLabel.value.replace(/\s+/g, '_')
  link.setAttribute("download", `Bilan_EcoSwitch_${currentName}_vs_${targetName}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const triggerSave = () => {
  if (!props.currentUser) {
    alert("Veuillez vous connecter à votre Espace Client pour enregistrer vos simulations.")
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
  const simName = `${currentVehicleLabel.value} ➔ ${targetVehicleLabel.value}`

  try {
    await apiSaveSimulation(simName, simData)
    showSaveModal.value = false
    saveNote.value = ''
    saveSuccessToast.value = true
    setTimeout(() => {
      saveSuccessToast.value = false
    }, 4000)
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
  <div class="results-dashboard animation-fadeIn">
    
    <!-- Barre d'outils Apple Style -->
    <div class="results-toolbar mb-4">
      <button class="btn-back" @click="emit('back')">
        <ArrowLeft size="16" />
        <span>Modifier la saisie</span>
      </button>

      <div class="toolbar-actions flex items-center gap-2">
        <button class="btn btn-secondary btn-small" @click="showShareModal = true" title="Partager">
          <Share2 size="14" />
          <span class="hide-on-xs">Partager</span>
        </button>
        <button class="btn btn-secondary btn-small" @click="exportToCSV" title="Exporter CSV">
          <FileSpreadsheet size="14" />
          <span class="hide-on-xs">Exporter CSV</span>
        </button>
        <button class="btn btn-primary btn-small" @click="triggerSave" title="Enregistrer">
          <Save size="14" />
          <span>Enregistrer</span>
        </button>
      </div>
    </div>

    <!-- Notification Toast Sauvegarde Réussie -->
    <div v-if="saveSuccessToast" class="toast-success p-3 rounded-xl mb-4 flex items-center gap-2 border-glass bg-card animation-fadeIn">
      <CheckCircle2 size="18" class="text-teal" />
      <span class="text-xs font-semibold text-main">Simulation enregistrée avec succès dans votre Espace Client !</span>
    </div>

    <!-- Bannière Diagnostic Hero Apple Style -->
    <div 
      class="hero-diagnostic-card mb-4" 
      :class="isLeasing 
        ? (netMonthlySavings >= 0 ? 'roi-profitable' : 'roi-neutral')
        : (result.breakEvenYear ? 'roi-profitable' : 'roi-neutral')"
    >
      <div class="diagnostic-icon-wrapper flex-center">
        <CheckCircle2 v-if="isLeasing ? netMonthlySavings >= 0 : result.breakEvenYear" size="26" class="text-teal" />
        <AlertCircle v-else size="26" class="text-amber" />
      </div>

      <div class="diagnostic-content flex-1">
        <div class="flex items-center gap-2 mb-1">
          <span 
            class="badge badge-small" 
            :class="(isLeasing ? netMonthlySavings >= 0 : result.breakEvenYear) ? 'badge-teal' : 'badge-amber'"
          >
            {{ isLeasing 
              ? (netMonthlySavings >= 0 ? 'Budget Positif' : 'Effort Mensuel')
              : (result.breakEvenYear ? 'Rentable' : 'Long terme') 
            }}
          </span>
          <span class="badge badge-small badge-cyan" v-if="isLeasing">Contrat LOA/LLD (3 à 5 ans)</span>
          <span class="text-xxs font-bold uppercase text-dimmed">
            {{ currentVehicleLabel }} ➔ {{ targetVehicleLabel }}
          </span>
        </div>
        
        <!-- Cas LOA / LLD : Analyse en budget mensuel de trésorerie (contrat 36/48/60 mois) -->
        <template v-if="isLeasing">
          <h3 class="diagnostic-title text-main font-heading m-0">
            {{ netMonthlySavings >= 0 
              ? `Opération Blanche ou Positive : +${formatCurrency(netMonthlySavings)} / mois de gain net` 
              : `Effort de trésorerie net de ${formatCurrency(Math.abs(netMonthlySavings))} / mois`
            }}
          </h3>
          <p class="diagnostic-description text-xs text-muted mt-1 m-0">
            {{ netMonthlySavings >= 0 
              ? `Vos économies d'énergie (${formatCurrency(monthlyUsageSavings)}/mois) absorbent entièrement votre loyer de leasing de ${formatCurrency(targetMonthlyFinancing)}/mois.` 
              : `En leasing (contrat 36-48 mois), le loyer de ${formatCurrency(targetMonthlyFinancing)}/mois est partiellement compensé par ${formatCurrency(monthlyUsageSavings)}/mois d'économies de carburant.`
            }}
          </p>
        </template>

        <!-- Cas Achat Comptant / Crédit : Analyse en amortissement de l'investissement initial -->
        <template v-else>
          <h3 class="diagnostic-title text-main font-heading m-0">
            {{ result.breakEvenYear 
              ? `Achat amorti en ${result.breakEvenYear} an${result.breakEvenYear > 1 ? 's' : ''}`
              : `Investissement amorti au-delà de votre horizon de ${maxYears} ans`
            }}
          </h3>
          <p class="diagnostic-description text-xs text-muted mt-1 m-0">
            {{ result.breakEvenYear 
              ? `Vos économies d'énergie et d'entretien (${formatCurrency(result.annualSavings)}/an) compensent votre investissement initial net.`
              : `Le surcoût d'acquisition (${formatCurrency(result.switchInvestment)}) nécessite plus de ${maxYears} ans pour être amorti par les économies d'énergie.`
            }}
          </p>
        </template>
      </div>

      <div class="diagnostic-stat shrink-0 text-right">
        <template v-if="isLeasing">
          <div class="stat-label text-xxs uppercase font-bold text-dimmed">Gain Net / Mois</div>
          <div class="stat-value font-heading" :class="netMonthlySavings >= 0 ? 'text-teal' : 'text-rose'">
            {{ netMonthlySavings >= 0 ? `+${formatCurrency(netMonthlySavings)}` : `-${formatCurrency(Math.abs(netMonthlySavings))}` }}
          </div>
        </template>
        <template v-else>
          <div class="stat-label text-xxs uppercase font-bold text-dimmed">Point Mort</div>
          <div class="stat-value font-heading" :class="result.breakEvenYear ? 'text-teal' : 'text-amber'">
            {{ result.breakEvenYear ? `${result.breakEvenYear} ans` : `> ${maxYears} ans` }}
          </div>
        </template>
      </div>
    </div>

    <!-- Bento Grid : Analyse Financière Mensuelle (Reste à vivre) -->
    <div class="mb-4">
      <div class="mb-2.5">
        <h4 class="text-sm font-bold text-main m-0">Impact sur votre Budget Mensuel</h4>
        <p class="text-xxs text-muted m-0">Dépenses mensuelles comparées d'usage et de financement</p>
      </div>

      <div class="bento-grid">
        <!-- Budget Actuel -->
        <div class="bento-card">
          <span class="badge badge-amber badge-small mb-2">Actuel</span>
          <div class="text-xs font-bold text-main truncate">{{ currentVehicleLabel }}</div>
          <div class="metric-value font-heading text-rose mt-1">{{ formatCurrency(currentMonthlyTotal) }}<span class="metric-unit">/mois</span></div>
          
          <div class="card-divider my-2.5"></div>
          
          <div class="detail-line flex-between text-xs py-0.5">
            <span class="text-dimmed">Carburant + Entretien</span>
            <span class="font-bold text-main">{{ formatCurrency(currentMonthlyUsage) }}</span>
          </div>
          <div class="detail-line flex-between text-xs py-0.5">
            <span class="text-dimmed">Loyer / Financement</span>
            <span class="font-bold text-main">{{ formatCurrency(currentMonthlyFinancing) }}</span>
          </div>
        </div>

        <!-- Budget Nouveau -->
        <div class="bento-card">
          <span class="badge badge-cyan badge-small mb-2">Nouveau</span>
          <div class="text-xs font-bold text-main truncate">{{ targetVehicle.name }}</div>
          <div class="metric-value font-heading text-cyan mt-1">{{ formatCurrency(targetMonthlyTotal) }}<span class="metric-unit">/mois</span></div>
          
          <div class="card-divider my-2.5"></div>
          
          <div class="detail-line flex-between text-xs py-0.5">
            <span class="text-dimmed">Énergie + Entretien</span>
            <span class="font-bold text-main">{{ formatCurrency(targetMonthlyUsage) }}</span>
          </div>
          <div class="detail-line flex-between text-xs py-0.5">
            <span class="text-dimmed">Loyer / Financement</span>
            <span class="font-bold text-main">{{ formatCurrency(targetMonthlyFinancing) }}</span>
          </div>
        </div>

        <!-- Impact Direct Trésorerie -->
        <div class="bento-card">
          <span class="badge badge-small mb-2" :class="netMonthlySavings > 0 ? 'badge-teal' : 'badge-rose'">
            {{ netMonthlySavings > 0 ? 'Reste à vivre' : 'Surcoût mensuel' }}
          </span>
          <div class="text-xs font-bold text-main">Gain net en poche</div>
          <div class="metric-value font-heading mt-1" :class="netMonthlySavings > 0 ? 'text-teal' : 'text-rose'">
            {{ netMonthlySavings > 0 ? '+' : '' }}{{ formatCurrency(netMonthlySavings) }}<span class="metric-unit">/mois</span>
          </div>
          
          <div class="card-divider my-2.5"></div>
          
          <p class="text-xxs text-muted m-0" style="line-height: 1.4;">
            <span v-if="!isLeasing">
              Achat comptant : réduction immédiate des dépenses d'énergie de <strong>{{ formatCurrency(monthlyUsageSavings) }} / mois</strong>.
            </span>
            <span v-else>
              {{ netMonthlySavings > 0 
                ? `L'économie d'énergie mensuelle de ${formatCurrency(monthlyUsageSavings)} absorbe entièrement le loyer.` 
                : `Le loyer de ${formatCurrency(targetMonthlyFinancing)} / mois dépasse les économies de carburant.`
              }}
            </span>
          </p>
        </div>
      </div>
    </div>

    <!-- Carte Diagnostic Mobilité Personnalisé (IA Gemini) -->
    <MobilityInsightCard
      :result="result"
      :currentVehicle="currentVehicle"
      :targetVehicle="targetVehicle"
      :fuelPrices="fuelPrices"
      :isLeasing="isLeasing"
      :homeChargingRatio="homeChargingRatio"
      :taxIncome="taxIncome"
      :scrapVehicle="scrapVehicle"
    />

    <!-- Bilan Carbone & Climat -->
    <CarbonFootprintCard :result="result" class="mb-4" />

    <!-- Recommandations d'Alternatives Intelligentes -->
    <AdvisorRecommendations
      v-if="result.recommendations && result.recommendations.length > 0"
      :recommendations="result.recommendations"
      class="mb-4"
      @load-alternative="handleLoadAlternative"
    />

    <!-- Accordéon Détails Techniques & Amortissement -->
    <div class="detailed-toggle-bar p-3.5 rounded-xl border-glass mb-4 flex-between items-center bg-card">
      <div class="flex items-center gap-2">
        <Layers size="16" class="text-teal" />
        <span class="text-xs font-bold text-main">
          {{ isLeasing ? 'Projections financières sur la durée du leasing (3 à 5 ans)' : `Projections financières détaillées (${maxYears} ans)` }}
        </span>
      </div>
      <button
        type="button"
        class="btn btn-secondary btn-small py-1 px-3 text-xs font-semibold flex items-center gap-1.5"
        @click="showDetailedTables = !showDetailedTables"
      >
        <span>{{ showDetailedTables ? 'Masquer' : 'Afficher le tableau' }}</span>
        <component :is="showDetailedTables ? ChevronUp : ChevronDown" size="13" />
      </button>
    </div>

    <!-- Volet Dépliable -->
    <div v-if="showDetailedTables" class="collapsible-projections animation-fadeIn flex flex-column gap-4 mb-4">
      <div class="bento-grid">
        <div class="bento-card">
          <span class="badge badge-teal badge-small mb-1.5">Moyenne annuelle</span>
          <div class="text-xs font-bold text-main">Économie Énergie / An</div>
          <div class="metric-value font-heading text-teal mt-1">{{ formatCurrency(result.annualSavings) }}<span class="metric-unit">/an</span></div>
        </div>

        <div class="bento-card">
          <span class="badge badge-small mb-1.5" :class="isLeasing ? 'badge-cyan' : 'badge-rose'">
            {{ isLeasing ? 'Loyer Mensuel' : 'Effort initial' }}
          </span>
          <div class="text-xs font-bold text-main">
            {{ isLeasing ? 'Loyer de Financement' : "Coût Net d'Acquisition" }}
          </div>
          <div class="metric-value font-heading mt-1" :class="isLeasing ? 'text-cyan' : 'text-rose'">
            {{ isLeasing ? `${formatCurrency(targetMonthlyFinancing)}/mois` : formatCurrency(result.switchInvestment) }}
          </div>
          <p class="text-xxs text-dimmed mt-1 m-0" v-if="result.totalSubsidies > 0">
            Aides déduites : -{{ formatCurrency(result.totalSubsidies) }}
          </p>
        </div>

        <div class="bento-card">
          <span class="badge badge-small mb-1.5" :class="(isLeasing ? netMonthlySavings >= 0 : result.totalCostDeltaAtHorizon <= 0) ? 'badge-teal' : 'badge-amber'">
            {{ isLeasing ? 'Bilan Mensuel Global' : `Horizon ${maxYears} ans` }}
          </span>
          <div class="text-xs font-bold text-main">
            {{ isLeasing ? 'Reste à Vivre Net' : 'Bilan Cumulé Global' }}
          </div>
          <div class="metric-value font-heading mt-1" :class="(isLeasing ? netMonthlySavings >= 0 : result.totalCostDeltaAtHorizon <= 0) ? 'text-teal' : 'text-amber'">
            <template v-if="isLeasing">
              {{ netMonthlySavings >= 0 ? '+' : '' }}{{ formatCurrency(netMonthlySavings) }}<span class="metric-unit">/mois</span>
            </template>
            <template v-else>
              {{ result.totalCostDeltaAtHorizon <= 0 ? '+' : '' }}{{ formatCurrency(-result.totalCostDeltaAtHorizon) }}
            </template>
          </div>
        </div>
      </div>

      <!-- Arbitrage Garagiste (si frais de réparations) -->
      <ArbitrageCard
        v-if="immediateRepairCost > 0"
        :result="result"
        :immediateRepairCost="immediateRepairCost"
        :currentVehicle="currentVehicle"
      />

      <!-- Tableau Annuel -->
      <div class="card-glass p-4">
        <h4 class="text-sm text-main font-bold mb-3">Projection Annuelle Cumulative</h4>
        <ProjectionsTable :yearlyForecast="getYearlyForecast()" />
      </div>
    </div>

    <!-- Modal d'enregistrement -->
    <div v-if="showSaveModal" class="auth-modal-overlay flex-center">
      <div class="card-glass auth-modal-card p-4 relative max-w-md w-100 animation-fadeIn">
        <button class="icon-btn-close absolute top-4 right-4" @click="showSaveModal = false">
          ✕
        </button>
        <h3 class="text-main font-heading mb-1 text-md font-bold">Enregistrer cette Simulation</h3>
        <p class="text-xs text-muted mb-4">Ajoutez un titre pour retrouver cette simulation dans votre Espace Client.</p>
        
        <div class="form-group mb-4">
          <label class="form-label text-xxs">Titre / Note du projet</label>
          <input v-model="saveNote" type="text" class="form-control text-xs" placeholder="ex: Transition Toyota Yaris 2026" required />
        </div>

        <div class="flex-between gap-2">
          <button class="btn btn-secondary w-50 text-xs" @click="showSaveModal = false">Annuler</button>
          <button class="btn btn-primary w-50 text-xs font-bold" @click="confirmSave">Enregistrer</button>
        </div>
      </div>
    </div>

    <!-- Modal Partage -->
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
.results-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-back {
  background: transparent;
  border: none;
  font-family: var(--font-sans);
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--accent-blue);
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: opacity 0.15s ease;
}
.btn-back:hover {
  opacity: 0.75;
}

.hero-diagnostic-card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-xl);
  padding: 20px 24px;
  box-shadow: var(--shadow-card);
}
.roi-profitable {
  border-left: 4px solid var(--accent-teal);
}
.roi-neutral {
  border-left: 4px solid var(--accent-amber);
}

.diagnostic-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--accent-teal-soft);
  flex-shrink: 0;
}
.roi-neutral .diagnostic-icon-wrapper {
  background: var(--accent-amber-soft);
}

.diagnostic-title {
  font-size: 1.15rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 800;
  line-height: 1;
}

.metric-value {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.metric-unit {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-dimmed);
  margin-left: 2px;
}

.card-divider {
  height: 1px;
  background: var(--border-subtle);
}

.toast-success {
  background: var(--accent-teal-soft);
  border-color: rgba(16, 124, 65, 0.25);
}

@media (max-width: 768px) {
  .hero-diagnostic-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .diagnostic-stat {
    text-align: left;
  }
  .hide-on-xs {
    display: none;
  }
}
</style>
