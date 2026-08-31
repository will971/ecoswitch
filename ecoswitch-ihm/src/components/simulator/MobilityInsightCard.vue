<script setup>
import { Sparkles, RefreshCw, CheckCircle2, ChevronDown, ChevronUp, ShieldCheck } from '@lucide/vue'
import { ref, computed, onMounted } from 'vue'
import { apiGetAiAdvisorSummary } from '../../utils/api.js'

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
  isLeasing: {
    type: Boolean,
    default: false
  },
  homeChargingRatio: {
    type: Number,
    default: 80
  },
  taxIncome: {
    type: Number,
    default: null
  },
  scrapVehicle: {
    type: Boolean,
    default: false
  }
})

const aiData = ref(null)
const aiLoading = ref(false)
const aiError = ref(null)
const openedFaq = ref(null)

const annualMileage = computed(() => {
  return props.currentVehicle.annualMileage || props.targetVehicle.annualMileage || 15000
})

const currentVehicleLabel = computed(() => {
  return props.currentVehicle.name || props.currentVehicle.model || 'Véhicule actuel'
})

const targetVehicleLabel = computed(() => {
  return props.targetVehicle.name || props.targetVehicle.model || 'Véhicule cible'
})

const savingsPer1000km = computed(() => {
  const km = annualMileage.value
  if (!km || km <= 0) return 0
  const totalFuelSavings = props.result.annualSavings || 0
  return Math.round((totalFuelSavings / km) * 1000)
})

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

const toggleFaq = (index) => {
  openedFaq.value = openedFaq.value === index ? null : index
}

const faqItems = computed(() => {
  const isTargetElectric = props.targetVehicle.fuelType === 'ELECTRIC'
  if (isTargetElectric) {
    return [
      {
        question: 'Comment installer une prise ou borne à domicile ?',
        answer: 'Une prise renforcée (3,7 kW) coûte environ 500 € et couvre 80% des besoins quotidiens. Une borne Wallbox (7,4 kW) coûte entre 1 200 € et 1 500 € avant crédit d\'impôt de 500 €.'
      },
      {
        question: 'Comment se déroulent les trajets sur autoroute ?',
        answer: 'Le réseau autoroutier français compte plus de 100 000 points de recharge. Une pause de 20 minutes toutes les 2 heures suffit généralement pour passer de 20% à 80% de batterie.'
      },
      {
        question: 'Quelle est la garantie et longévité de la batterie ?',
        answer: 'Les constructeurs garantissent légalement leurs batteries pendant 8 ans ou 160 000 km avec maintien d\'au moins 70% de la capacité initiale.'
      }
    ]
  } else {
    return [
      {
        question: 'Quel est le coût réel de maintenance d\'un véhicule hybride ?',
        answer: 'Grâce au freinage régénératif, les disques et plaquettes de frein s\'usent 2 fois moins vite qu\'un véhicule thermique standard.'
      },
      {
        question: 'Quelle est la durée de vie du système hybride ?',
        answer: 'La batterie de traction hybride est conçue pour durer toute la vie du véhicule (plus de 10 à 15 ans sans remplacement).'
      }
    ]
  }
})

const fetchAiAdvice = async () => {
  aiLoading.value = true
  aiError.value = null
  try {
    const request = {
      currentVehicleName: currentVehicleLabel.value,
      currentFuelType: props.currentVehicle.fuelType,
      currentConsumption: props.currentVehicle.consumption || 6.5,
      targetVehicleName: targetVehicleLabel.value,
      targetFuelType: props.targetVehicle.fuelType,
      targetConsumption: props.targetVehicle.consumption || 15.0,
      annualMileage: annualMileage.value,
      annualFuelSavings: props.result.annualSavings || 0,
      annualCO2Savings: props.result.annualCO2Savings || 0,
      breakEvenYear: props.result.breakEvenYear,
      switchInvestment: props.result.switchInvestment || 0,
      homeChargingRatio: props.homeChargingRatio,
      taxIncome: props.taxIncome,
      scrapVehicle: props.scrapVehicle,
      isLeasing: props.isLeasing,
      targetMonthlyPrice: props.targetVehicle.monthlyLoa || props.targetVehicle.monthlyLld || 0
    }
    const data = await apiGetAiAdvisorSummary(request)
    aiData.value = data
  } catch (err) {
    aiError.value = err.message
  } finally {
    aiLoading.value = false
  }
}

onMounted(() => {
  fetchAiAdvice()
})
</script>

<template>
  <div class="insight-card mb-4">
    <!-- En-tête -->
    <div class="flex-between items-center mb-3 pb-3 border-b border-glass">
      <div class="flex items-center gap-2">
        <h4 class="text-xs font-semibold text-main m-0 uppercase tracking-wider">Synthèse du Diagnostic</h4>
        <span class="badge badge-teal badge-small">Conseiller Mobilité</span>
      </div>

      <button
        type="button"
        class="btn-refresh"
        :disabled="aiLoading"
        @click="fetchAiAdvice"
        title="Actualiser l'analyse"
      >
        <RefreshCw size="12" :class="{ 'spin-animate': aiLoading }" />
        <span>{{ aiLoading ? 'Actualisation...' : 'Actualiser' }}</span>
      </button>
    </div>

    <!-- Synthèse textuelle -->
    <div class="narrative-box mb-3.5">
      <p v-if="aiData && aiData.financialAdvice" class="narrative-text text-main m-0">
        {{ aiData.financialAdvice }}
      </p>
      <p v-else class="narrative-text text-main m-0">
        Le passage à la <strong>{{ targetVehicleLabel }}</strong> permet de réduire vos dépenses énergétiques de <strong>{{ formatCurrency(result.annualSavings) }}/an</strong> pour un profil de <strong>{{ Number(annualMileage).toLocaleString('fr-FR') }} km/an</strong>.
      </p>
    </div>

    <!-- 3 Métriques clés alignées -->
    <div class="metrics-grid mb-3.5">
      <div class="metric-tile">
        <span class="metric-tile-label">Gain aux 1 000 km</span>
        <div class="metric-tile-val text-teal font-mono">
          +{{ formatCurrency(savingsPer1000km) }}
        </div>
        <span class="metric-tile-sub">sur votre budget carburant</span>
      </div>

      <div class="metric-tile">
        <span class="metric-tile-label">Consommation homologuée</span>
        <div class="metric-tile-val text-cyan font-mono">
          {{ targetVehicle.consumption }} <span class="metric-unit font-sans">{{ targetVehicle.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km</span>
        </div>
        <span class="metric-tile-sub">contre {{ currentVehicle.consumption }} L/100km</span>
      </div>

      <div class="metric-tile">
        <span class="metric-tile-label">Aides publiques</span>
        <div class="metric-tile-val font-mono" :class="result.totalSubsidies > 0 ? 'text-teal' : 'text-main'">
          {{ result.totalSubsidies > 0 ? formatCurrency(result.totalSubsidies) : '0 €' }}
        </div>
        <span class="metric-tile-sub">{{ result.totalSubsidies > 0 ? 'Bonus déduit' : 'Non éligible' }}</span>
      </div>
    </div>

    <!-- Recommandations d'actions -->
    <div v-if="aiData && aiData.keyRecommendations && aiData.keyRecommendations.length" class="recs-section mb-3 pt-3 border-t border-glass">
      <div class="text-xxs font-semibold text-dimmed uppercase tracking-wider mb-2">
        Points d'attention recommandés :
      </div>
      <div class="recs-list flex flex-column gap-1.5">
        <div
          v-for="(rec, idx) in aiData.keyRecommendations"
          :key="idx"
          class="rec-row flex items-start gap-2 text-xs"
        >
          <CheckCircle2 size="14" class="text-teal shrink-0 mt-0.5" />
          <span class="text-muted leading-normal">{{ rec }}</span>
        </div>
      </div>
    </div>

    <!-- Questions fréquentes -->
    <div class="faq-container pt-3 border-t border-glass">
      <div class="flex items-center gap-1.5 mb-2 text-xxs font-semibold text-dimmed uppercase tracking-wider">
        <ShieldCheck size="13" class="text-teal" />
        <span>Questions pratiques</span>
      </div>

      <div class="faq-items flex flex-column gap-1.5">
        <div
          v-for="(item, idx) in faqItems"
          :key="idx"
          class="faq-item p-2.5 rounded-lg border-glass bg-card-subtle cursor-pointer"
          @click="toggleFaq(idx)"
        >
          <div class="flex-between items-center">
            <span class="text-xs font-medium text-main">{{ item.question }}</span>
            <component :is="openedFaq === idx ? ChevronUp : ChevronDown" size="13" class="text-dimmed shrink-0 ml-2" />
          </div>
          <p v-if="openedFaq === idx" class="text-xxs text-muted mt-2 pt-2 border-t border-glass m-0 leading-relaxed animation-fadeIn">
            {{ item.answer }}
          </p>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.insight-card {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  box-shadow: var(--shadow-card);
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 5px;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-refresh:hover {
  background: var(--bg-card-hover);
  color: var(--text-main);
  border-color: var(--border-hover);
}

.narrative-text {
  font-size: 0.88rem;
  line-height: 1.55;
  color: var(--text-main);
}

/* 3 Metrics Grid */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}

.metric-tile {
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}

.metric-tile-label {
  display: block;
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-dimmed);
  margin-bottom: 4px;
}

.metric-tile-val {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.1;
  margin-bottom: 2px;
}

.metric-unit {
  font-size: 0.72rem;
  font-weight: 500;
  color: var(--text-dimmed);
  margin-left: 2px;
}

.metric-tile-sub {
  font-size: 0.68rem;
  color: var(--text-muted);
}

.faq-item {
  transition: border-color 0.15s ease;
}
.faq-item:hover {
  border-color: var(--border-hover);
}

.spin-animate {
  animation: spin 0.8s linear infinite;
}
</style>
