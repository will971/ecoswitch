<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import {
  Sparkles,
  Zap,
  TrendingUp,
  Fuel,
  Gift,
  Coins,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  ThumbsUp,
  AlertTriangle,
  Lightbulb,
  ShieldCheck,
  RefreshCw
} from '@lucide/vue'
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
    default: 0.85
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

const annualMileage = computed(() => props.currentVehicle.annualMileage || props.targetVehicle.annualMileage || 15000)

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

// Calcul du coût aux 1 000 km
const currentCostPer1000km = computed(() => {
  const price = props.fuelPrices[props.currentVehicle.fuelType] || 1.88
  return (props.currentVehicle.consumption * price * 10)
})

const targetCostPer1000km = computed(() => {
  if (props.targetVehicle.fuelType === 'ELECTRIC') {
    const homePrice = props.fuelPrices.ELECTRIC || 0.25
    const publicPrice = 0.55
    const ratio = props.homeChargingRatio || 0.85
    const blendedKwhPrice = (homePrice * ratio) + (publicPrice * (1 - ratio))
    return (props.targetVehicle.consumption * blendedKwhPrice * 10)
  }
  const price = props.fuelPrices[props.targetVehicle.fuelType] || 1.88
  return (props.targetVehicle.consumption * price * 10)
})

const savingsPer1000km = computed(() => currentCostPer1000km.value - targetCostPer1000km.value)

// ── Gestion IA ──
const aiLoading = ref(false)
const aiData = ref(null)

const fetchAiAdvice = async () => {
  aiLoading.value = true
  try {
    const payload = {
      currentVehicle: props.currentVehicle,
      targetVehicle: props.targetVehicle,
      annualMileage: annualMileage.value,
      homeChargingRatio: props.homeChargingRatio || 0.85,
      taxIncome: props.taxIncome,
      scrapVehicle: props.scrapVehicle,
      isLeasing: props.isLeasing,
      monthlySavings: props.result.monthlySavings,
      annualSavings: props.result.annualSavings,
      totalSubsidies: props.result.totalSubsidies,
      breakEvenYear: props.result.breakEvenYear,
      annualCO2Savings: props.result.annualCO2Savings
    }
    const response = await apiGetAiAdvisorSummary(payload)
    aiData.value = response
  } catch (err) {
    console.warn("Erreur chargement IA Advisor :", err)
  } finally {
    aiLoading.value = false
  }
}

onMounted(() => {
  fetchAiAdvice()
})

watch(() => [props.targetVehicle.name, props.currentVehicle.name], () => {
  fetchAiAdvice()
})

// FAQ
const openedFaq = ref(null)
const toggleFaq = (idx) => {
  openedFaq.value = openedFaq.value === idx ? null : idx
}

const faqItems = [
  {
    question: "Comment installer une prise ou borne à domicile ?",
    answer: "Une prise renforcée (ex: Green'Up à ~350 €) suffit pour recharger 150 km chaque nuit. L'installation d'une borne 7.4 kW ouvre droit à un crédit d'impôt forfaitaire de 500 €."
  },
  {
    question: "Comment se déroulent les trajets sur autoroute ?",
    answer: "Le réseau autoroutier français compte plus de 130 000 points de charge. Les bornes rapides 150 kW+ permettent de récupérer 80% d'autonomie en 20 à 25 minutes, soit le temps d'une pause café."
  },
  {
    question: "Quelle est la garantie et longévité de la batterie ?",
    answer: "Les constructeurs garantissent la batterie pendant 8 ans ou 160 000 km (avec capacité minimale garantie de 70%). La perte moyenne observée est de seulement 1% à 1.5% par an."
  }
]
</script>

<template>
  <div class="card-glass p-5 mb-4">
    
    <!-- En-tête épuré avec 1 seul badge discret -->
    <div class="flex-between items-center mb-4 pb-3 border-b border-glass">
      <div class="flex items-center gap-2.5">
        <div class="ai-icon-badge flex-center">
          <Sparkles size="18" class="text-teal" />
        </div>
        <div>
          <div class="flex items-center gap-2">
            <h4 class="text-main font-heading text-md font-bold m-0">Analyse Personnalisée</h4>
            <span class="badge badge-teal badge-small">Conseiller IA</span>
          </div>
          <p class="text-xs text-muted m-0">
            Profil de conduite : {{ Number(annualMileage).toLocaleString('fr-FR') }} km / an
          </p>
        </div>
      </div>

      <button
        type="button"
        class="btn-refresh-ai"
        :disabled="aiLoading"
        @click="fetchAiAdvice"
        title="Actualiser l'analyse"
      >
        <RefreshCw size="13" :class="{ 'spin-animate': aiLoading }" />
        <span class="hide-on-mobile">{{ aiLoading ? 'Actualisation...' : 'Actualiser' }}</span>
      </button>
    </div>

    <!-- Synthèse textuelle de l'IA (Typographie haute lisibilité) -->
    <div class="ai-narrative mb-4">
      <p v-if="aiData && aiData.financialAdvice" class="narrative-text text-main m-0">
        {{ aiData.financialAdvice }}
      </p>
      <p v-else class="narrative-text text-main m-0">
        Passer de votre <strong>{{ currentVehicle.name }}</strong> à la <strong>{{ targetVehicle.name }}</strong> vous permet de réduire directement vos dépenses d'énergie de <strong>{{ formatCurrency(result.annualSavings) }}/an</strong>.
      </p>
    </div>

    <!-- 3 Métriques clés alignées -->
    <div class="metrics-grid mb-4">
      <div class="metric-tile">
        <span class="metric-tile-label">Gain carburant</span>
        <div class="metric-tile-val text-teal">
          +{{ formatCurrency(savingsPer1000km) }}
        </div>
        <span class="metric-tile-sub">tous les 1 000 km</span>
      </div>

      <div class="metric-tile">
        <span class="metric-tile-label">Consommation cible</span>
        <div class="metric-tile-val text-cyan">
          {{ targetVehicle.consumption }} <span class="text-xs font-semibold">{{ targetVehicle.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km</span>
        </div>
        <span class="metric-tile-sub">contre {{ currentVehicle.consumption }} L/100km avant</span>
      </div>

      <div class="metric-tile">
        <span class="metric-tile-label">Aides publiques</span>
        <div class="metric-tile-val" :class="result.totalSubsidies > 0 ? 'text-teal' : 'text-main'">
          {{ result.totalSubsidies > 0 ? formatCurrency(result.totalSubsidies) : '0 €' }}
        </div>
        <span class="metric-tile-sub">{{ result.totalSubsidies > 0 ? 'Bonus & prime déduits' : 'Aucune aide applicable' }}</span>
      </div>
    </div>

    <!-- Recommandations d'actions sous forme de liste fluide -->
    <div v-if="aiData && aiData.keyRecommendations && aiData.keyRecommendations.length" class="recs-section mb-4 pt-3 border-t border-glass">
      <h5 class="text-xs font-bold text-main uppercase tracking-wider mb-2.5">
        Points d'attention recommandés :
      </h5>
      <div class="recs-list flex flex-column gap-2">
        <div
          v-for="(rec, idx) in aiData.keyRecommendations"
          :key="idx"
          class="rec-row flex items-start gap-2 text-xs text-muted"
        >
          <CheckCircle2 size="15" class="text-teal shrink-0 mt-0.5" />
          <span class="leading-relaxed text-main">{{ rec }}</span>
        </div>
      </div>
    </div>

    <!-- Questions fréquentes (Accordéon intégré propre) -->
    <div class="faq-container pt-3 border-t border-glass">
      <div class="flex items-center gap-1.5 mb-2.5 text-xs font-bold text-main uppercase">
        <ShieldCheck size="15" class="text-teal" />
        <span>Questions pratiques & réassurance</span>
      </div>

      <div class="faq-items flex flex-column gap-1.5">
        <div
          v-for="(item, idx) in faqItems"
          :key="idx"
          class="faq-item p-3 rounded-xl border-glass bg-card-subtle cursor-pointer"
          @click="toggleFaq(idx)"
        >
          <div class="flex-between items-center">
            <span class="text-xs font-bold text-main">{{ item.question }}</span>
            <component :is="openedFaq === idx ? ChevronUp : ChevronDown" size="14" class="text-dimmed shrink-0 ml-2" />
          </div>
          <p v-if="openedFaq === idx" class="text-xs text-muted mt-2 pt-2 border-t border-glass m-0 leading-relaxed animation-fadeIn">
            {{ item.answer }}
          </p>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.ai-icon-badge {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--accent-teal-soft);
  border: 1px solid rgba(16, 124, 65, 0.15);
}

.btn-refresh-ai {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  color: var(--text-muted);
  font-size: 0.76rem;
  font-weight: 600;
  padding: 5px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-refresh-ai:hover {
  background: var(--bg-card-hover);
  color: var(--text-main);
  border-color: var(--border-hover);
}

.narrative-text {
  font-size: 0.94rem;
  line-height: 1.6;
  color: var(--text-main);
  font-weight: 500;
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
  padding: 14px 16px;
}

.metric-tile-label {
  display: block;
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-dimmed);
  margin-bottom: 4px;
}

.metric-tile-val {
  font-family: var(--font-heading);
  font-size: 1.4rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.1;
  margin-bottom: 4px;
}

.metric-tile-sub {
  font-size: 0.75rem;
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

@media (max-width: 600px) {
  .hide-on-mobile {
    display: none;
  }
}
</style>
