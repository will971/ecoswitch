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
  <div class="recommendations-container p-4 border-glass rounded mb-4">
    <div class="rec-header flex-between mb-3">
      <div class="flex items-center gap-2">
        <Sparkles size="18" class="text-teal" />
        <h4 class="text-sm font-heading text-main m-0 font-bold">Recommandations de l'Assistant</h4>
      </div>
      <!-- Boutons de navigation du carrousel -->
      <div class="carousel-nav-btns flex gap-1">
        <button class="carousel-nav-btn" aria-label="Précédent" @click="scrollCarousel(-1)">
          <ChevronLeft size="16" />
        </button>
        <button class="carousel-nav-btn" aria-label="Suivant" @click="scrollCarousel(1)">
          <ChevronRight size="16" />
        </button>
      </div>
    </div>
    <p class="text-xs text-muted mb-4">L'assistant a identifié des alternatives pertinentes du catalogue. Cliquez pour simuler :</p>

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
        <!-- Badge Catalogue -->
        <span class="rec-badge">Catalogue</span>

        <!-- Nom du véhicule -->
        <div class="rec-vehicle-name">{{ rec.vehicleName }}</div>

        <div class="rec-card-divider"></div>

        <!-- Gain annuel mis en valeur -->
        <div class="rec-savings-block">
          <span class="rec-savings-label">Gain / an</span>
          <span class="rec-savings-value text-teal">{{ formatCurrency(rec.annualSavings) }}</span>
        </div>

        <!-- Prix achat/transition en bas -->
        <div class="rec-purchase-row">
          <span class="rec-purchase-label">Prix d'achat</span>
          <span class="rec-purchase-value">{{ formatCurrency(rec.switchInvestment) }}</span>
        </div>
      </button>
    </div>
  </div>
</template>

<style scoped>
.recommendations-container {
  background: hsl(var(--bg-glass));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 20px;
  box-shadow: var(--shadow-card);
}

.rec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.text-main {
  color: hsl(var(--text-main));
}

/* ── Carousel scroll container ── */
.rec-carousel {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding: 4px 2px 14px;
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
  background: hsl(var(--accent-teal));
}

/* ── Square card ── */
.rec-card {
  flex: 0 0 170px;
  width: 170px;
  min-height: 180px;
  scroll-snap-align: start;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 16px 12px;
  background: hsl(var(--bg-card));
  border: 1px solid hsl(var(--border-glass));
  border-radius: 16px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;
}

.rec-card:hover,
.rec-card:focus-visible {
  border-color: hsl(var(--accent-teal));
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}

.rec-card:active {
  transform: scale(0.98);
}

/* Small Catalogue badge */
.rec-badge {
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: hsla(var(--accent-teal) / 0.15);
  color: hsl(var(--accent-teal));
  padding: 3px 10px;
  border-radius: 20px;
  text-align: center;
}

/* Vehicle name */
.rec-vehicle-name {
  font-family: var(--font-sans);
  font-size: 0.8rem;
  font-weight: 700;
  color: hsl(var(--text-main));
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: center;
  width: 100%;
  line-height: 1.3;
}

.rec-card-divider {
  width: 100%;
  height: 1px;
  background: hsl(var(--border-glass));
  margin: 6px 0;
}

/* Annual savings block */
.rec-savings-block {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.rec-savings-label {
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: hsl(var(--text-muted));
  font-weight: 700;
}
.rec-savings-value {
  font-family: var(--font-sans);
  font-size: 1.05rem;
  font-weight: 800;
  margin-top: 2px;
}

/* Purchase price bottom row */
.rec-purchase-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  width: 100%;
}
.rec-purchase-label {
  font-size: 0.65rem;
  text-transform: uppercase;
  color: hsl(var(--text-muted));
  font-weight: 600;
}
.rec-purchase-value {
  font-size: 0.78rem;
  font-weight: 700;
  color: hsl(var(--text-main));
}

/* ── Navigation arrows ── */
.carousel-nav-btns {
  display: flex;
  gap: 4px;
}
.carousel-nav-btn {
  background: hsl(var(--bg-card));
  border: 1px solid hsl(var(--border-glass));
  color: hsl(var(--text-muted));
  border-radius: 8px;
  width: 28px;
  height: 28px;
  color: hsl(var(--text-muted));
  cursor: pointer;
  transition: all 0.2s ease;
}
.carousel-nav-btn:hover {
  border-color: hsl(var(--accent-teal) / 0.5);
  color: hsl(var(--accent-teal));
}

.text-teal { color: hsl(var(--accent-teal)) !important; }

/* ── Responsive tweaks ── */
@media (max-width: 480px) {
  .rec-card {
    flex: 0 0 140px;
    width: 140px;
    height: 160px;
  }
  .carousel-nav-btns {
    display: none;
  }
}
</style>
