<template>
  <div class="catalog-cascade-selector">
    <!-- Header -->
    <div class="selector-header">
      <div class="header-title-group">
        <span class="header-label">{{ label || 'Sélection depuis le catalogue automobile' }}</span>
        <span v-if="loading" class="loading-tag">
          <span class="spinner-dot"></span> Chargement du catalogue...
        </span>
      </div>
      <button
        type="button"
        @click="resetSelection"
        v-if="selectedBrand || selectedModel"
        class="btn-reset"
      >
        Réinitialiser
      </button>
    </div>

    <!-- Error state -->
    <div v-if="loadError" class="catalog-alert alert-error">
      <span>{{ loadError }}</span>
      <button type="button" @click="loadHierarchy" class="alert-retry-btn">Réessayer</button>
    </div>

    <!-- 4-STEP SELECTORS GRID -->
    <div class="selectors-grid">
      <!-- 1. MARQUE -->
      <div class="selector-item" ref="brandDropdownRef">
        <label class="item-label">
          <span class="step-num">1</span> Marque
        </label>
        
        <div class="custom-dropdown">
          <button
            type="button"
            class="dropdown-trigger"
            :class="{ 'is-active': activeDropdown === 'brand', 'has-value': !!selectedBrand }"
            @click="toggleDropdown('brand')"
            :disabled="loading || brands.length === 0"
          >
            <div class="trigger-content">
              <div class="trigger-icon-box">
                <img
                  v-if="selectedBrand?.logoUrl"
                  :src="selectedBrand.logoUrl"
                  :alt="selectedBrand.name"
                  class="trigger-img object-contain"
                  @error="(e) => e.target.style.display = 'none'"
                />
                <span v-else class="trigger-fallback-emoji">🚗</span>
              </div>
              <span class="trigger-text">
                {{ selectedBrand ? selectedBrand.name : '-- Choisir une marque --' }}
              </span>
            </div>
            <span class="chevron-icon" :class="{ 'rotate': activeDropdown === 'brand' }">▼</span>
          </button>

          <!-- Dropdown List -->
          <div v-if="activeDropdown === 'brand'" class="dropdown-menu-box animation-dropdown">
            <div class="dropdown-search-box" v-if="brands.length > 5">
              <input
                v-model="brandSearch"
                type="text"
                placeholder="Rechercher une marque..."
                class="dropdown-search-input"
                @click.stop
              />
            </div>
            <div class="dropdown-options-list">
              <div
                v-for="b in filteredBrands"
                :key="b.id"
                class="dropdown-option-row"
                :class="{ 'is-selected': selectedBrandId === b.id }"
                @click="selectBrand(b)"
              >
                <div class="option-left">
                  <div class="option-img-box">
                    <img
                      v-if="b.logoUrl"
                      :src="b.logoUrl"
                      :alt="b.name"
                      class="option-img object-contain"
                      @error="(e) => e.target.style.display = 'none'"
                    />
                    <span v-else class="option-fallback-emoji">🚗</span>
                  </div>
                  <span class="option-title">{{ b.name }}</span>
                </div>
                <div class="option-right">
                  <span class="option-badge">{{ b.models?.length || 0 }} mod.</span>
                  <span v-if="selectedBrandId === b.id" class="check-mark">✓</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. MODÈLE -->
      <div class="selector-item" ref="modelDropdownRef">
        <label class="item-label">
          <span class="step-num">2</span> Modèle
        </label>
        
        <div class="custom-dropdown">
          <button
            type="button"
            class="dropdown-trigger"
            :class="{ 'is-active': activeDropdown === 'model', 'has-value': !!selectedModel }"
            @click="toggleDropdown('model')"
            :disabled="!selectedBrandId || availableModels.length === 0"
          >
            <div class="trigger-content">
              <div class="trigger-icon-box model-icon-box">
                <img
                  v-if="selectedModel?.imageUrl"
                  :src="selectedModel.imageUrl"
                  :alt="selectedModel.name"
                  class="trigger-img object-cover"
                  @error="(e) => e.target.style.display = 'none'"
                />
                <span v-else class="trigger-fallback-emoji">🚘</span>
              </div>
              <span class="trigger-text">
                {{ selectedModel ? `${selectedModel.name} (${selectedModel.category || 'Véhicule'})` : '-- Choisir un modèle --' }}
              </span>
            </div>
            <span class="chevron-icon" :class="{ 'rotate': activeDropdown === 'model' }">▼</span>
          </button>

          <!-- Dropdown List -->
          <div v-if="activeDropdown === 'model'" class="dropdown-menu-box animation-dropdown">
            <div class="dropdown-options-list">
              <div
                v-for="m in availableModels"
                :key="m.id"
                class="dropdown-option-row"
                :class="{ 'is-selected': selectedModelId === m.id }"
                @click="selectModel(m)"
              >
                <div class="option-left">
                  <div class="option-img-box model-img-box">
                    <img
                      v-if="m.imageUrl"
                      :src="m.imageUrl"
                      :alt="m.name"
                      class="option-img object-cover"
                      @error="(e) => e.target.style.display = 'none'"
                    />
                    <span v-else class="option-fallback-emoji">🚘</span>
                  </div>
                  <div class="option-text-stack">
                    <span class="option-title">{{ m.name }}</span>
                    <span class="option-subtitle">{{ m.category || 'Général' }}</span>
                  </div>
                </div>
                <span v-if="selectedModelId === m.id" class="check-mark">✓</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3. MOTORISATION (WLTP) -->
      <div class="selector-item" ref="motorisationDropdownRef">
        <label class="item-label">
          <span class="step-num">3</span> Motorisation & Conso WLTP
        </label>
        
        <div class="custom-dropdown">
          <button
            type="button"
            class="dropdown-trigger"
            :class="{ 'is-active': activeDropdown === 'motorisation', 'has-value': !!selectedMotorisation }"
            @click="toggleDropdown('motorisation')"
            :disabled="!selectedModelId || availableMotorisations.length === 0"
          >
            <div class="trigger-content">
              <span class="fuel-indicator-tag" :class="getFuelBadgeClass(selectedMotorisation?.fuelType)">
                {{ selectedMotorisation ? (selectedMotorisation.fuelType === 'ELECTRIC' ? '⚡ Élec' : '🍃 Hyb') : '⚡' }}
              </span>
              <span class="trigger-text">
                <template v-if="selectedMotorisation">
                  {{ selectedMotorisation.name }} • <strong>{{ selectedMotorisation.consumptionWltp }} {{ getConsumptionUnit(selectedMotorisation.fuelType) }}</strong>
                </template>
                <template v-else>
                  -- Choisir une motorisation --
                </template>
              </span>
            </div>
            <span class="chevron-icon" :class="{ 'rotate': activeDropdown === 'motorisation' }">▼</span>
          </button>

          <!-- Dropdown List -->
          <div v-if="activeDropdown === 'motorisation'" class="dropdown-menu-box animation-dropdown">
            <div class="dropdown-options-list">
              <div
                v-for="mot in availableMotorisations"
                :key="mot.id"
                class="dropdown-option-row"
                :class="{ 'is-selected': selectedMotorisationId === mot.id }"
                @click="selectMotorisation(mot)"
              >
                <div class="option-left">
                  <span class="fuel-indicator-tag" :class="getFuelBadgeClass(mot.fuelType)">
                    {{ mot.fuelType === 'ELECTRIC' ? '⚡ Élec' : (mot.fuelType === 'HYBRID' ? '🍃 Hyb' : '⛽') }}
                  </span>
                  <div class="option-text-stack">
                    <span class="option-title">{{ mot.name }}</span>
                    <span class="option-subtitle">
                      {{ mot.powerHp }} ch {{ mot.batteryCapacityKwh ? `• ${mot.batteryCapacityKwh} kWh` : '' }}
                    </span>
                  </div>
                </div>
                <div class="option-right">
                  <span class="wltp-pill">
                    WLTP: <strong>{{ mot.consumptionWltp }} {{ getConsumptionUnit(mot.fuelType) }}</strong>
                  </span>
                  <span v-if="selectedMotorisationId === mot.id" class="check-mark">✓</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 4. FINITION & TARIFS -->
      <div class="selector-item" ref="finitionDropdownRef">
        <label class="item-label">
          <span class="step-num">4</span> Finition & Tarifs
        </label>
        
        <div class="custom-dropdown">
          <button
            type="button"
            class="dropdown-trigger"
            :class="{ 'is-active': activeDropdown === 'finition', 'has-value': !!selectedVariantPrice }"
            @click="toggleDropdown('finition')"
            :disabled="!selectedMotorisationId || availableFinitionsWithPricing.length === 0"
          >
            <div class="trigger-content">
              <div class="trigger-icon-box finition-icon-box">
                <img
                  v-if="selectedVariantPrice?.finitionImageUrl"
                  :src="selectedVariantPrice.finitionImageUrl"
                  :alt="selectedVariantPrice.finitionName"
                  class="trigger-img object-cover"
                  @error="(e) => e.target.style.display = 'none'"
                />
                <span v-else class="trigger-fallback-emoji">✨</span>
              </div>
              <span class="trigger-text">
                <template v-if="selectedVariantPrice">
                  {{ selectedVariantPrice.finitionName }} — <strong>{{ formatCurrency(selectedVariantPrice.purchasePrice) }}</strong>
                </template>
                <template v-else>
                  -- Choisir une finition --
                </template>
              </span>
            </div>
            <span class="chevron-icon" :class="{ 'rotate': activeDropdown === 'finition' }">▼</span>
          </button>

          <!-- Dropdown List -->
          <div v-if="activeDropdown === 'finition'" class="dropdown-menu-box animation-dropdown">
            <div class="dropdown-options-list">
              <div
                v-for="fin in availableFinitionsWithPricing"
                :key="fin.finitionId"
                class="dropdown-option-row"
                :class="{ 'is-selected': selectedFinitionId === fin.finitionId }"
                @click="selectFinition(fin)"
              >
                <div class="option-left">
                  <div class="option-img-box finition-img-box">
                    <img
                      v-if="fin.finitionImageUrl"
                      :src="fin.finitionImageUrl"
                      :alt="fin.finitionName"
                      class="option-img object-cover"
                      @error="(e) => e.target.style.display = 'none'"
                    />
                    <span v-else class="option-fallback-emoji">✨</span>
                  </div>
                  <div class="option-text-stack">
                    <span class="option-title">{{ fin.finitionName }}</span>
                    <span class="option-subtitle">
                      Achat: <strong>{{ formatCurrency(fin.purchasePrice) }}</strong>
                    </span>
                  </div>
                </div>
                <div class="option-right">
                  <div class="pricing-tags-stack">
                    <span v-if="fin.monthlyLoa" class="tag-loa">LOA: {{ formatCurrency(fin.monthlyLoa) }}/m</span>
                    <span v-if="fin.monthlyLld" class="tag-lld">LLD: {{ formatCurrency(fin.monthlyLld) }}/m</span>
                  </div>
                  <span v-if="selectedFinitionId === fin.finitionId" class="check-mark">✓</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- PREVIEW CARD OF SELECTED VEHICLE (COMPACT & PRECISE) -->
    <div v-if="selectedVariantPrice && selectedModel && selectedMotorisation" class="vehicle-preview-card">
      <div class="preview-card-inner">
        <!-- Photo du modèle avec taille contrainte -->
        <div class="preview-media-box">
          <img
            v-if="selectedVariantPrice.finitionImageUrl || selectedModel.imageUrl"
            :src="selectedVariantPrice.finitionImageUrl || selectedModel.imageUrl"
            :alt="selectedModel.name"
            class="preview-vehicle-img"
          />
          <div v-else class="preview-placeholder">🚘</div>
          <span class="preview-category-badge">{{ selectedModel.category || 'Automobile' }}</span>
        </div>

        <!-- Informations Textuelles & Spécifications -->
        <div class="preview-info-col">
          <div class="preview-header-row">
            <div class="preview-brand-logo-box">
              <img
                v-if="selectedBrand?.logoUrl"
                :src="selectedBrand.logoUrl"
                :alt="selectedBrand.name"
                class="preview-brand-logo"
              />
            </div>
            <h4 class="preview-title">{{ selectedBrand?.name }} {{ selectedModel.name }}</h4>
            <span class="badge" :class="getFuelBadgeClass(selectedMotorisation.fuelType)">
              {{ selectedMotorisation.fuelType }}
            </span>
          </div>

          <div class="preview-subtitle-row">
            <span>Finition <strong>{{ selectedVariantPrice.finitionName }}</strong></span>
            <span class="bullet-sep">•</span>
            <span>{{ selectedMotorisation.name }} ({{ selectedMotorisation.powerHp }} ch)</span>
          </div>

          <!-- Badges de Spécifications & Tarification -->
          <div class="preview-badges-row">
            <div class="stat-pill wltp-pill-main">
              <span class="stat-pill-label">Conso WLTP</span>
              <span class="stat-pill-val">{{ selectedMotorisation.consumptionWltp }} {{ getConsumptionUnit(selectedMotorisation.fuelType) }}</span>
            </div>

            <div class="stat-pill">
              <span class="stat-pill-label">Achat</span>
              <span class="stat-pill-val text-main">{{ formatCurrency(selectedVariantPrice.purchasePrice) }}</span>
            </div>

            <div v-if="selectedVariantPrice.monthlyLoa" class="stat-pill">
              <span class="stat-pill-label">LOA</span>
              <span class="stat-pill-val text-cyan">{{ formatCurrency(selectedVariantPrice.monthlyLoa) }}/m</span>
            </div>

            <div v-if="selectedVariantPrice.monthlyLld" class="stat-pill">
              <span class="stat-pill-label">LLD</span>
              <span class="stat-pill-val text-teal">{{ formatCurrency(selectedVariantPrice.monthlyLld) }}/m</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { apiGetCatalogHierarchy } from '@/utils/api'

const props = defineProps({
  label: {
    type: String,
    default: ''
  },
  initialBrand: {
    type: String,
    default: ''
  },
  initialModel: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select-variant', 'change-brand', 'change-model'])

const brands = ref([])
const loading = ref(false)
const loadError = ref(null)

const selectedBrandId = ref(null)
const selectedModelId = ref(null)
const selectedMotorisationId = ref(null)
const selectedFinitionId = ref(null)

const activeDropdown = ref(null) // 'brand' | 'model' | 'motorisation' | 'finition' | null
const brandSearch = ref('')

const brandDropdownRef = ref(null)
const modelDropdownRef = ref(null)
const motorisationDropdownRef = ref(null)
const finitionDropdownRef = ref(null)

// ── Computed Properties ──────────────────────────────────────────────────

const selectedBrand = computed(() => {
  return brands.value.find(b => b.id === selectedBrandId.value) || null
})

const filteredBrands = computed(() => {
  if (!brandSearch.value.trim()) return brands.value
  const q = brandSearch.value.toLowerCase()
  return brands.value.filter(b => b.name.toLowerCase().includes(q))
})

const availableModels = computed(() => {
  return selectedBrand.value?.models || []
})

const selectedModel = computed(() => {
  return availableModels.value.find(m => m.id === selectedModelId.value) || null
})

const availableMotorisations = computed(() => {
  return selectedModel.value?.motorisations || []
})

const selectedMotorisation = computed(() => {
  return availableMotorisations.value.find(m => m.id === selectedMotorisationId.value) || null
})

const availableFinitionsWithPricing = computed(() => {
  if (!selectedMotorisation.value) return []
  return selectedMotorisation.value.availableFinitions || selectedMotorisation.value.variantPricings || []
})

const selectedVariantPrice = computed(() => {
  if (!selectedFinitionId.value || !availableFinitionsWithPricing.value) return null
  return availableFinitionsWithPricing.value.find(v => v.finitionId === selectedFinitionId.value) || null
})

// ── Dropdown Controls ────────────────────────────────────────────────────

function toggleDropdown(name) {
  if (activeDropdown.value === name) {
    activeDropdown.value = null
  } else {
    activeDropdown.value = name
  }
}

function handleClickOutside(event) {
  const isClickInside = [brandDropdownRef, modelDropdownRef, motorisationDropdownRef, finitionDropdownRef].some(
    refEl => refEl.value && refEl.value.contains(event.target)
  )
  if (!isClickInside) {
    activeDropdown.value = null
  }
}

// ── Selection Methods ────────────────────────────────────────────────────

function selectBrand(b) {
  selectedBrandId.value = b.id
  selectedModelId.value = null
  selectedMotorisationId.value = null
  selectedFinitionId.value = null
  activeDropdown.value = null
  emit('change-brand', b.name)
}

function selectModel(m) {
  selectedModelId.value = m.id
  selectedMotorisationId.value = null
  selectedFinitionId.value = null
  activeDropdown.value = null
  emit('change-model', m.name)

  // Auto-select if only 1 motorisation
  if (m.motorisations && m.motorisations.length === 1) {
    selectMotorisation(m.motorisations[0])
  }
}

function selectMotorisation(mot) {
  selectedMotorisationId.value = mot.id
  selectedFinitionId.value = null
  activeDropdown.value = null

  const finList = mot.availableFinitions || mot.variantPricings || []
  if (finList.length === 1) {
    selectFinition(finList[0])
  }
}

function selectFinition(fin) {
  selectedFinitionId.value = fin.finitionId
  activeDropdown.value = null
  emitSelectedVariant()
}

function emitSelectedVariant() {
  if (!selectedVariantPrice.value || !selectedModel.value || !selectedMotorisation.value) return

  const payload = {
    brand: selectedBrand.value?.name || '',
    model: selectedModel.value.name,
    version: `${selectedMotorisation.value.name} - ${selectedVariantPrice.value.finitionName}`,
    fuelType: selectedMotorisation.value.fuelType,
    consumption: selectedMotorisation.value.consumptionWltp,
    powerHp: selectedMotorisation.value.powerHp,
    batteryCapacityKwh: selectedMotorisation.value.batteryCapacityKwh,
    purchasePrice: selectedVariantPrice.value.purchasePrice,
    monthlyLoa: selectedVariantPrice.value.monthlyLoa,
    monthlyLld: selectedVariantPrice.value.monthlyLld,
    insuranceCost: selectedVariantPrice.value.defaultInsuranceCost || 650.0,
    maintenanceCost: selectedVariantPrice.value.defaultMaintenanceCost || 250.0,
    resaleValue: selectedVariantPrice.value.estimatedResaleValue || 0.0,
    imageUrl: selectedVariantPrice.value.finitionImageUrl || selectedModel.value.imageUrl,
    brandLogoUrl: selectedBrand.value?.logoUrl
  }

  emit('select-variant', payload)
}

function resetSelection() {
  selectedBrandId.value = null
  selectedModelId.value = null
  selectedMotorisationId.value = null
  selectedFinitionId.value = null
  activeDropdown.value = null
}

// ── Lifecycle & Data Loading ─────────────────────────────────────────────

async function loadHierarchy() {
  loading.value = true
  loadError.value = null
  try {
    const data = await apiGetCatalogHierarchy()
    brands.value = data || []
    
    // Auto-match initial values if provided
    if (props.initialBrand) {
      const b = brands.value.find(item => item.name.toLowerCase() === props.initialBrand.toLowerCase())
      if (b) {
        selectedBrandId.value = b.id
        if (props.initialModel && b.models) {
          const cleanInitModel = props.initialModel.split('(')[0].trim().toLowerCase()
          const m = b.models.find(item => item.name.toLowerCase().includes(cleanInitModel) || cleanInitModel.includes(item.name.toLowerCase()))
          if (m) {
            selectedModelId.value = m.id
          }
        }
      }
    }
  } catch (err) {
    loadError.value = err.message || "Erreur lors du chargement du catalogue."
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHierarchy()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// ── Helpers ───────────────────────────────────────────────────────────────

function getConsumptionUnit(fuelType) {
  if (fuelType === 'ELECTRIC') return 'kWh/100km'
  if (fuelType === 'HYDROGEN') return 'kg/100km'
  return 'L/100km'
}

function formatCurrency(val) {
  if (val == null || isNaN(val)) return '0 €'
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val)
}

function getFuelBadgeClass(fuelType) {
  switch (fuelType) {
    case 'ELECTRIC': return 'badge-teal'
    case 'HYBRID': return 'badge-cyan'
    case 'DIESEL': return 'badge-amber'
    default: return 'badge-rose'
  }
}
</script>

<style scoped>
.catalog-cascade-selector {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Header */
.selector-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-glass);
}
.header-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.01em;
}
.loading-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.7rem;
  color: var(--accent-teal);
  font-weight: 600;
}
.spinner-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-teal);
  animation: pulse 1.2s infinite ease-in-out;
}
@keyframes pulse {
  0%, 100% { opacity: 0.2; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

.btn-reset {
  background: transparent;
  border: none;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-dimmed);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.15s ease;
}
.btn-reset:hover {
  color: var(--accent-rose);
}

/* Alert */
.catalog-alert {
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.alert-error {
  background: var(--accent-rose-soft);
  color: var(--accent-rose);
  border: 1px solid rgba(225, 29, 72, 0.2);
}
.alert-retry-btn {
  background: transparent;
  border: none;
  color: var(--accent-rose);
  text-decoration: underline;
  cursor: pointer;
  font-weight: 700;
}

/* 4 Selectors Grid */
.selectors-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
@media (max-width: 640px) {
  .selectors-grid {
    grid-template-columns: 1fr;
  }
}

.selector-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
}
.item-label {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-dimmed);
  display: flex;
  align-items: center;
  gap: 4px;
}
.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
  font-size: 0.62rem;
  font-weight: 800;
}

/* Custom Dropdown Trigger */
.custom-dropdown {
  position: relative;
  width: 100%;
}
.dropdown-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 10px;
  background: var(--bg-input);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  cursor: pointer;
  text-align: left;
  outline: none;
  transition: all 0.15s ease;
  min-height: 38px;
}
.dropdown-trigger:hover:not(:disabled) {
  border-color: var(--border-hover);
}
.dropdown-trigger.is-active {
  border-color: var(--accent-teal);
  box-shadow: 0 0 0 2px var(--accent-teal-soft);
}
.dropdown-trigger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--bg-card-subtle);
}

.trigger-content {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}
.trigger-icon-box {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  padding: 1px;
}
.trigger-icon-box.model-icon-box, .trigger-icon-box.finition-icon-box {
  width: 26px;
  height: 18px;
}
.trigger-img {
  width: 100%;
  height: 100%;
  display: block;
}
.trigger-fallback-emoji {
  font-size: 0.75rem;
}
.trigger-text {
  font-size: 0.78rem;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chevron-icon {
  font-size: 0.55rem;
  color: var(--text-dimmed);
  transition: transform 0.2s ease;
}
.chevron-icon.rotate {
  transform: rotate(180deg);
}

/* Dropdown Menu Popup */
.dropdown-menu-box {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--bg-card);
  border: 1px solid var(--border-hover);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-modal);
  z-index: 100;
  max-height: 240px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.dropdown-search-box {
  padding: 6px;
  border-bottom: 1px solid var(--border-glass);
}
.dropdown-search-input {
  width: 100%;
  padding: 5px 8px;
  border-radius: 6px;
  border: 1px solid var(--border-glass);
  background: var(--bg-input);
  color: var(--text-main);
  font-size: 0.74rem;
  outline: none;
}
.dropdown-search-input:focus {
  border-color: var(--accent-teal);
}

.dropdown-options-list {
  overflow-y: auto;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.dropdown-option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s ease;
}
.dropdown-option-row:hover {
  background: var(--bg-card-hover);
}
.dropdown-option-row.is-selected {
  background: var(--accent-teal-soft);
}

.option-left {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}
.option-img-box {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  padding: 1px;
}
.option-img-box.model-img-box, .option-img-box.finition-img-box {
  width: 32px;
  height: 22px;
}
.option-img {
  width: 100%;
  height: 100%;
  display: block;
}
.option-fallback-emoji {
  font-size: 0.8rem;
}
.option-text-stack {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.option-title {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text-main);
}
.option-subtitle {
  font-size: 0.68rem;
  color: var(--text-dimmed);
}

.option-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.option-badge {
  font-size: 0.65rem;
  color: var(--text-dimmed);
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  padding: 1px 5px;
  border-radius: 4px;
}
.check-mark {
  color: var(--accent-teal);
  font-size: 0.8rem;
  font-weight: 800;
}

.fuel-indicator-tag {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
.wltp-pill {
  font-size: 0.68rem;
  color: var(--text-muted);
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  padding: 2px 6px;
  border-radius: 4px;
}
.pricing-tags-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
}
.tag-loa {
  font-size: 0.65rem;
  color: var(--accent-cyan);
  font-weight: 600;
}
.tag-lld {
  font-size: 0.65rem;
  color: var(--accent-teal);
  font-weight: 600;
}

/* VEHICLE PREVIEW CARD (Strictly Constrained) */
.vehicle-preview-card {
  margin-top: 4px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  box-shadow: var(--shadow-card);
}
.preview-card-inner {
  display: flex;
  align-items: center;
  gap: 14px;
}
@media (max-width: 640px) {
  .preview-card-inner {
    flex-direction: column;
    align-items: flex-start;
  }
}

.preview-media-box {
  width: 120px;
  height: 75px;
  border-radius: var(--radius-sm);
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-vehicle-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.preview-placeholder {
  font-size: 1.8rem;
}
.preview-category-badge {
  position: absolute;
  bottom: 3px;
  left: 4px;
  font-size: 0.58rem;
  font-weight: 700;
  background: rgba(0, 0, 0, 0.65);
  color: white;
  padding: 1px 5px;
  border-radius: 4px;
  backdrop-filter: blur(4px);
}

.preview-info-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.preview-header-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.preview-brand-logo-box {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-brand-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.preview-title {
  font-size: 0.92rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
}

.preview-subtitle-row {
  font-size: 0.72rem;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}
.bullet-sep {
  color: var(--text-dimmed);
}

.preview-badges-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 4px;
}
.stat-pill {
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  border-radius: 6px;
  padding: 3px 7px;
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}
.stat-pill-label {
  font-size: 0.58rem;
  text-transform: uppercase;
  color: var(--text-dimmed);
  font-weight: 700;
}
.stat-pill-val {
  font-size: 0.72rem;
  font-weight: 700;
}
.wltp-pill-main {
  border-color: rgba(16, 124, 65, 0.25);
  background: var(--accent-teal-soft);
}
.wltp-pill-main .stat-pill-label {
  color: var(--accent-teal);
}
.wltp-pill-main .stat-pill-val {
  color: var(--accent-teal);
}

.object-contain {
  object-fit: contain;
}
.object-cover {
  object-fit: cover;
}

.text-cyan {
  color: var(--accent-cyan);
}
.text-teal {
  color: var(--accent-teal);
}
.text-main {
  color: var(--text-main);
}
</style>
