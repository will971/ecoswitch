<script setup>
import { ref } from 'vue'
import { Copy, X, Check, Share2 } from '@lucide/vue'

const props = defineProps({
  show: {
    type: Boolean,
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
  result: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close'])

const copied = ref(false)

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val)
}

const getReportText = () => {
  const isElectric = props.targetVehicle.fuelType === 'ELECTRIC'
  const energyWord = isElectric ? 'électrique' : 'plus propre'
  let text = `⚡ Bilan de ma transition EcoSwitch : ${props.currentVehicle.name} ➡️ ${props.targetVehicle.name}\n\n`
  
  if (props.result.breakEvenYear) {
    text += `✅ Changement rentable en ${props.result.breakEvenYear} ans !\n`
  } else {
    text += `ℹ️ Transition orientée long terme.\n`
  }
  
  text += `💰 Économie d'énergie : ${formatCurrency(props.result.annualSavings)}/an\n`
  text += `🌍 Émissions CO₂ évitées : ${(props.result.annualCO2Savings).toFixed(0)} kg CO₂/an (soit env. ${(props.result.annualCO2Savings / 25).toFixed(0)} arbres plantés !)\n\n`
  
  if (props.result.monthlySavings > 0) {
    text += `📈 Gain mensuel en leasing : +${formatCurrency(props.result.monthlySavings)}/mois de reste à vivre !\n`
  }
  
  text += `Calculez votre rentabilité gratuitement sur EcoSwitch ! 🚗`
  return text
}

const copyToClipboard = async () => {
  try {
    await navigator.clipboard.writeText(getReportText())
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    console.error('Erreur lors de la copie', err)
  }
}

const shareOnTwitter = () => {
  const text = encodeURIComponent(getReportText())
  window.open(`https://twitter.com/intent/tweet?text=${text}`, '_blank')
}

const shareOnWhatsApp = () => {
  const text = encodeURIComponent(getReportText())
  window.open(`https://api.whatsapp.com/send?text=${text}`, '_blank')
}
</script>

<template>
  <div v-if="show" class="auth-modal-overlay flex-center">
    <div class="card-glass glow-teal auth-modal-card p-4 relative max-w-md w-100">
      <!-- Bouton fermer -->
      <button class="absolute top-4 right-4 text-dimmed hover-text-main" @click="emit('close')">
        <X size="20" />
      </button>

      <h3 class="text-gradient mb-3 flex items-center gap-2">
        <Share2 size="22" class="text-teal" />
        <span>Partager mon Bilan</span>
      </h3>
      <p class="text-xs text-muted mb-4">
        Partagez vos résultats ou copiez le récapitulatif pour en discuter avec vos proches.
      </p>

      <!-- Aperçu du rapport -->
      <div class="report-preview p-3 rounded border-glass bg-deep-glass text-xs text-left mb-4 select-all">
        <pre>{{ getReportText() }}</pre>
      </div>

      <!-- Actions -->
      <div class="flex flex-column gap-2">
        <!-- Copier -->
        <button class="btn btn-secondary w-100 flex-center gap-2 py-2 text-xs font-semibold" @click="copyToClipboard">
          <component :is="copied ? Check : Copy" size="16" :class="copied ? 'text-teal' : ''" />
          <span>{{ copied ? 'Copié dans le presse-papier !' : 'Copier le récapitulatif' }}</span>
        </button>

        <div class="flex gap-2 mt-2">
          <!-- Partager Twitter/X -->
          <button class="btn btn-secondary w-50 flex-center gap-2 py-2 text-xs font-semibold" @click="shareOnTwitter">
            <span>Partager sur X</span>
          </button>
          
          <!-- Partager WhatsApp -->
          <button class="btn btn-secondary w-50 flex-center gap-2 py-2 text-xs font-semibold" @click="shareOnWhatsApp">
            <span>Envoyer sur WhatsApp</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report-preview {
  font-family: inherit;
  white-space: pre-wrap;
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.08);
  max-height: 180px;
  overflow-y: auto;
  line-height: 1.4;
  color: hsl(var(--text-main));
}
pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
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
  background: hsl(var(--bg-deep) / 0.9);
}
.absolute { position: absolute; }
.top-4 { top: 1rem; }
.right-4 { right: 1rem; }
.w-100 { width: 100%; }
.w-50 { width: 50%; }
.gap-2 { gap: 8px; }
.mt-2 { margin-top: 8px; }
</style>
