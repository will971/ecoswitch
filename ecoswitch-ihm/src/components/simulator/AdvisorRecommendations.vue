<script setup>
import { Sparkles, ChevronLeft, ChevronRight } from '@lucide/vue'
import { ref } from 'vue'

defineProps({
  recommendations: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['load-alternative'])

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(val)
}

const carouselRef = ref(null)

const scrollCarousel = (direction) => {
  if (!carouselRef.value) return
  const scrollAmount = 180
  carouselRef.value.scrollBy({ left: direction * scrollAmount, behavior: 'smooth' })
}
</script>

<template>
  <div class="recommendations-container p-3 border-glass rounded mb-4 bg-card-glass glow-teal">
    <div class="rec-header flex-between mb-2">
      <h4 class="text-sm font-heading text-teal flex items-center gap-1">
        <Sparkles size="16" />
        <span>💡 Recommandations de l'Assistant</span>
      </h4>
      <!-- Boutons de navigation du carrousel (desktop) -->
      <div class="carousel-nav-btns flex gap-1">
        <button class="carousel-nav-btn" aria-label="Précédent" @click="scrollCarousel(-1)">
          <ChevronLeft size="16" />
        </button>
        <button class="carousel-nav-btn" aria-label="Suivant" @click="scrollCarousel(1)">
          <ChevronRight size="16" />
        </button>
      </div>
    </div>
    <p class="text-xs text-muted mb-3">Faites défiler et cliquez pour charger une alternative :</p>

    <!-- Carrousel horizontal de pastilles carrées -->
    <div ref="carouselRef" class="rec-carousel" role="listbox" aria-label="Véhicules recommandés">
      <button
        v-for="rec in recommendations"
        :key="rec.vehicleId"
        class="rec-card"
        role="option"
        :aria-label="rec.vehicleName + ', économie de ' + formatCurrency(rec.annualSavings) + ' par an'"
        @click="emit('load-alternative', rec)"
      >
        <!-- Badge badge-teal en haut -->
        <div class="rec-badge">Catalogue</div>

        <!-- Nom du véhicule -->
        <div class="rec-vehicle-name">{{ rec.vehicleName }}</div>

        <!-- Gain annuel mis en valeur -->
        <div class="rec-savings-block">
          <span class="rec-savings-label">Gain / an</span>
          <span class="rec-savings-value">{{ formatCurrency(rec.annualSavings) }}</span>
        </div>

        <!-- Prix achat/transition en bas -->
        <div class="rec-purchase-row">
          <span class="rec-purchase-label">Achat</span>
          <span class="rec-purchase-value">{{ formatCurrency(rec.switchInvestment) }}</span>
        </div>
      </button>
    </div>
  </div>
</template>

<style scoped>
.recommendations-container {
  background: hsl(var(--bg-card) / 0.4);
}

.rec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* ── Carousel scroll container ── */
.rec-carousel {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding: 6px 4px 12px;
  -webkit-overflow-scrolling: touch;
}

/* Thin, stylised scrollbar */
.rec-carousel::-webkit-scrollbar { height: 4px; }
.rec-carousel::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 2px;
}
.rec-carousel::-webkit-scrollbar-thumb {
  background: hsl(var(--border-glass));
  border-radius: 2px;
}
.rec-carousel::-webkit-scrollbar-thumb:hover {
  background: hsl(var(--accent-teal) / 0.8);
}

/* ── Square card ── */
.rec-card {
  /* Fixed square dimensions */
  flex: 0 0 152px;
  width: 152px;
  height: 152px;
  scroll-snap-align: start;

  /* Stack contents vertically */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  gap: 4px;

  /* Visual style */
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid hsl(var(--border-glass));
  border-radius: 14px;
  cursor: pointer;
  outline: none;

  /* Animation */
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.rec-card:hover,
.rec-card:focus-visible {
  border-color: hsl(var(--accent-cyan));
  box-shadow: 0 0 18px 0 hsl(var(--accent-cyan) / 0.3);
  background: rgba(8, 145, 178, 0.06);
  transform: translateY(-3px);
}

.rec-card:active {
  transform: scale(0.97);
  box-shadow: none;
}

/* Small "Catalogue" badge */
.rec-badge {
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: hsl(var(--accent-teal) / 0.15);
  color: hsl(var(--accent-teal));
  padding: 2px 8px;
  border-radius: 20px;
  width: 100%;
  text-align: center;
}

/* Vehicle name – two-line clamp */
.rec-vehicle-name {
  font-size: 0.78rem;
  font-weight: 600;
  background: linear-gradient(135deg, hsl(var(--text-main)) 30%, hsl(var(--accent-cyan)) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: center;
  width: 100%;
  line-height: 1.3;
}

/* Annual savings block */
.rec-savings-block {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.rec-savings-label {
  font-size: 0.58rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: hsl(var(--text-dimmed));
  font-weight: 600;
}
.rec-savings-value {
  font-size: 0.9rem;
  font-weight: 700;
  color: hsl(var(--accent-teal));
  line-height: 1.2;
}

/* Purchase row at the bottom */
.rec-purchase-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 6px;
  width: 100%;
}
.rec-purchase-label {
  font-size: 0.58rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: hsl(var(--text-dimmed));
  font-weight: 600;
}
.rec-purchase-value {
  font-size: 0.75rem;
  font-weight: 600;
  color: hsl(var(--text-muted));
}

/* ── Desktop carousel nav buttons ── */
.carousel-nav-btns {
  display: flex;
  gap: 4px;
}
.carousel-nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid hsl(var(--border-glass));
  background: hsl(var(--bg-card));
  color: hsl(var(--text-muted));
  cursor: pointer;
  transition: all 0.2s ease;
}
.carousel-nav-btn:hover {
  border-color: hsl(var(--accent-teal) / 0.5);
  color: hsl(var(--accent-teal));
}

/* ── Responsive tweaks ── */
@media (max-width: 480px) {
  .rec-card {
    flex: 0 0 140px;
    width: 140px;
    height: 140px;
  }
  .carousel-nav-btns {
    display: none; /* touch users scroll naturally */
  }
}
</style>
