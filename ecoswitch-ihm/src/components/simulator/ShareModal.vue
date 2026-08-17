<script setup>
import { ref } from 'vue'
import { Copy, X, Check, Share2, MessageCircle, Send } from '@lucide/vue'

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
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

const getReportText = () => {
  let text = `⚡ Bilan de transition EcoSwitch : ${props.currentVehicle.name} ➡️ ${props.targetVehicle.name}\n\n`
  
  if (props.result.breakEvenYear) {
    text += `✅ Changement amorti en ${props.result.breakEvenYear} an${props.result.breakEvenYear > 1 ? 's' : ''} !\n`
  } else {
    text += `ℹ️ Transition à horizon long terme.\n`
  }
  
  text += `💰 Économie d'énergie : ${formatCurrency(props.result.annualSavings)}/an\n`
  if (props.result.annualCO2Savings > 0) {
    text += `🌍 Réduction carbone : ${(props.result.annualCO2Savings).toFixed(0)} kg CO₂/an (env. ${(props.result.annualCO2Savings / 25).toFixed(0)} arbres)\n`
  }
  
  if (props.result.monthlySavings > 0) {
    text += `📈 Reste à vivre mensuel : +${formatCurrency(props.result.monthlySavings)}/mois !\n`
  }
  
  text += `\nSimulez gratuitement votre transition sur EcoSwitch !`
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
    <div class="card-glass auth-modal-card p-4 relative max-w-md w-100 animation-fadeIn">
      <button class="icon-btn-close absolute top-4 right-4" @click="emit('close')">
        <X size="18" />
      </button>

      <div class="flex items-center gap-2 mb-2">
        <div class="share-icon-badge flex-center">
          <Share2 size="16" class="text-teal" />
        </div>
        <h3 class="text-main font-heading text-md font-bold m-0">Partager mon Bilan</h3>
      </div>
      <p class="text-xs text-muted mb-3">
        Partagez votre calcul de rentabilité ou copiez le texte récapitulatif :
      </p>

      <!-- Aperçu du rapport -->
      <div class="report-preview p-3 rounded-xl border-glass bg-card-subtle text-xs text-left mb-3.5 select-all">
        <pre class="m-0 font-mono text-xxs text-main" style="white-space: pre-wrap; font-family: var(--font-mono);">{{ getReportText() }}</pre>
      </div>

      <!-- Actions de partage -->
      <div class="flex flex-column gap-2">
        <button
          class="btn btn-primary w-100 text-xs font-bold flex items-center justify-center gap-2"
          @click="copyToClipboard"
        >
          <Check v-if="copied" size="14" />
          <Copy v-else size="14" />
          <span>{{ copied ? 'Copié dans le presse-papier !' : 'Copier le texte' }}</span>
        </button>

        <div class="grid-2-fields gap-2">
          <button
            class="btn btn-secondary w-100 text-xs font-semibold flex items-center justify-center gap-1.5"
            @click="shareOnWhatsApp"
          >
            <MessageCircle size="14" class="text-teal" />
            <span>WhatsApp</span>
          </button>
          <button
            class="btn btn-secondary w-100 text-xs font-semibold flex items-center justify-center gap-1.5"
            @click="shareOnTwitter"
          >
            <Send size="14" class="text-cyan" />
            <span>X / Twitter</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.share-icon-badge {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: hsla(var(--accent-teal) / 0.12);
  border: 1px solid hsla(var(--accent-teal) / 0.25);
}

.report-preview {
  max-height: 160px;
  overflow-y: auto;
}
</style>
