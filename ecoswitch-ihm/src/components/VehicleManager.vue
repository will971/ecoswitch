<template>
  <div class="catalog-manager-app">
    <!-- Top Bar Épurée -->
    <div class="manager-header">
      <div class="header-titles">
        <h2 class="manager-main-title">
          <span>Catalogue Constructeurs & Véhicules</span>
          <span v-if="isAdmin" class="badge badge-teal badge-small">
            <ShieldCheck size="12" class="mr-1 inline" /> Mode Gestionnaire
          </span>
        </h2>
        <p class="manager-subtitle hide-on-mobile">
          Base de données des constructeurs, modèles, motorisations WLTP, finitions et barèmes de prix.
        </p>
      </div>

      <div class="header-actions">
        <button
          v-if="isAdmin"
          class="btn btn-primary btn-small flex items-center gap-1.5 font-semibold"
          @click="openAddBrandModal"
        >
          <Plus size="14" />
          <span>Nouvelle Marque</span>
        </button>
      </div>
    </div>

    <!-- Alert Notifications -->
    <div v-if="successMsg" class="catalog-toast toast-success animation-fadeIn">
      <Check size="15" />
      <span>{{ successMsg }}</span>
    </div>
    <div v-if="error" class="catalog-toast toast-error animation-fadeIn">
      <AlertTriangle size="15" />
      <span>{{ error }}</span>
      <button @click="error = null" class="toast-close">✕</button>
    </div>

    <!-- MASTER-DETAIL 2-COLUMN CATALOG LAYOUT -->
    <div class="catalog-main-layout">
      <!-- PANE 1 : LISTE DES CONSTRUCTEURS / MARQUES (Gauche) -->
      <aside class="brands-sidebar-card card-glass">
        <div class="sidebar-header">
          <div class="flex items-center gap-2">
            <span class="sidebar-title">Constructeurs ({{ hierarchy.length }})</span>
          </div>
          <button
            v-if="isAdmin"
            class="link-action-btn flex items-center gap-1"
            @click="openAddBrandModal"
          >
            <Plus size="12" /> Ajouter
          </button>
        </div>

        <!-- Search brand -->
        <div class="sidebar-search-box" v-if="hierarchy.length > 5">
          <div class="search-input-wrapper">
            <Search size="13" class="search-icon" />
            <input
              v-model="brandSearch"
              type="text"
              placeholder="Filtrer une marque..."
              class="sidebar-search-input"
            />
          </div>
        </div>

        <div v-if="loading && hierarchy.length === 0" class="sidebar-loading">
          <span class="spinner-inline"></span> Chargement...
        </div>

        <div v-else class="brands-items-list">
          <div
            v-for="b in filteredHierarchy"
            :key="b.id"
            @click="selectActiveBrand(b)"
            class="brand-row-item"
            :class="{ 'is-selected': activeBrand?.id === b.id }"
          >
            <div class="brand-row-left">
              <div class="brand-logo-box">
                <img
                  v-if="b.logoUrl"
                  :src="b.logoUrl"
                  :alt="b.name"
                  class="brand-logo-img"
                  @error="(e) => e.target.style.display = 'none'"
                />
                <Car v-else size="14" class="text-dimmed" />
              </div>
              <span class="brand-name-text">{{ b.name }}</span>
            </div>

            <div class="brand-row-right">
              <span class="models-count-pill font-mono">{{ b.models?.length || 0 }}</span>
              <div v-if="isAdmin" class="row-hover-actions" @click.stop>
                <button class="action-btn-mini" @click="openEditBrandModal(b)" title="Modifier la marque">
                  <Edit2 size="11" />
                </button>
                <button class="action-btn-mini btn-danger" @click="deleteBrand(b)" title="Supprimer la marque">
                  <Trash2 size="11" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- PANE 2 : DÉTAIL DE LA MARQUE & MODÈLES (Droite) -->
      <main class="brand-detail-main">
        <div v-if="!activeBrand" class="card-glass empty-state-card">
          <Car size="36" class="text-dimmed mb-2" />
          <h3 class="text-sm font-bold text-main">Sélectionnez un constructeur</h3>
          <p class="text-xs text-muted">Choisissez une marque dans la colonne de gauche pour explorer sa gamme de modèles et motorisations.</p>
        </div>

        <div v-else class="space-y-main">
          <!-- BARRE DES MODÈLES DU CONSTRUCTEUR -->
          <div class="card-glass models-selector-card">
            <div class="models-bar-header">
              <div class="brand-badge-info">
                <div class="header-logo-box">
                  <img
                    v-if="activeBrand.logoUrl"
                    :src="activeBrand.logoUrl"
                    :alt="activeBrand.name"
                    class="header-logo-img"
                  />
                </div>
                <h3 class="brand-heading">{{ activeBrand.name }} — Gamme de véhicules</h3>
              </div>
              <button
                v-if="isAdmin"
                class="btn btn-primary btn-small"
                @click="openAddModelModal"
              >
                + Ajouter un modèle
              </button>
            </div>

            <div v-if="!activeBrand.models || activeBrand.models.length === 0" class="empty-models-notice">
              Aucun modèle enregistré pour {{ activeBrand.name }}.
            </div>

            <div v-else class="models-tabs-grid">
              <button
                v-for="m in activeBrand.models"
                :key="m.id"
                @click="activeModel = m"
                class="model-tab-btn"
                :class="{ 'is-active': activeModel?.id === m.id }"
              >
                <div class="tab-img-box">
                  <img
                    v-if="m.imageUrl"
                    :src="m.imageUrl"
                    :alt="m.name"
                    class="tab-img"
                    @error="(e) => e.target.style.display = 'none'"
                  />
                  <Car v-else size="14" class="text-dimmed" />
                </div>
                <div class="tab-text-stack">
                  <span class="tab-model-name">{{ m.name }}</span>
                  <span class="tab-category">{{ m.category || 'Général' }}</span>
                </div>
              </button>
            </div>
          </div>

          <!-- DÉTAIL COMPLET DU MODÈLE SÉLECTIONNÉ -->
          <div v-if="activeModel" class="card-glass model-detail-card">
            <!-- Hero Header du Modèle -->
            <div class="model-hero-banner">
              <div class="hero-left">
                <div class="hero-vehicle-media">
                  <img
                    v-if="activeModel.imageUrl"
                    :src="activeModel.imageUrl"
                    :alt="activeModel.name"
                    class="hero-img"
                  />
                  <Car v-else size="28" class="text-dimmed" />
                  <span class="hero-category-tag">{{ activeModel.category || 'Automobile' }}</span>
                </div>

                <div class="hero-info">
                  <div class="hero-brand-line">
                    <img v-if="activeBrand.logoUrl" :src="activeBrand.logoUrl" class="hero-mini-logo" />
                    <span class="hero-brand-name">{{ activeBrand.name }}</span>
                  </div>
                  <h3 class="hero-model-title">{{ activeModel.name }}</h3>
                  <div class="hero-badges">
                    <span class="badge badge-teal badge-small">{{ activeModel.motorisations?.length || 0 }} motorisations</span>
                    <span class="badge badge-cyan badge-small">{{ activeModel.finitions?.length || 0 }} finitions</span>
                  </div>
                </div>
              </div>

              <div v-if="isAdmin" class="hero-actions">
                <button class="btn btn-secondary btn-small flex items-center gap-1" @click="openEditModelModal(activeModel)">
                  <Edit2 size="12" /> <span>Modifier</span>
                </button>
                <button class="btn btn-secondary btn-small btn-danger" @click="deleteModel(activeModel)" title="Supprimer">
                  <Trash2 size="12" />
                </button>
              </div>
            </div>

            <!-- SECTION : FINITIONS DU MODÈLE -->
            <div class="finitions-section-block">
              <div class="section-title-bar">
                <h4 class="section-title flex items-center gap-1.5">
                  <Layers size="14" class="text-teal" />
                  <span>Finitions disponibles ({{ activeModel.finitions?.length || 0 }})</span>
                </h4>
                <button
                  v-if="isAdmin"
                  class="link-action-btn flex items-center gap-1"
                  @click="openAddFinitionModal"
                >
                  <Plus size="12" /> Ajouter
                </button>
              </div>

              <div v-if="!activeModel.finitions || activeModel.finitions.length === 0" class="empty-sub-notice">
                Aucune finition enregistrée pour ce modèle.
              </div>

              <div v-else class="finitions-pill-grid">
                <div
                  v-for="fin in activeModel.finitions"
                  :key="fin.id"
                  class="finition-card-pill"
                >
                  <div class="finition-pill-left">
                    <div class="finition-media-box">
                      <img
                        v-if="fin.imageUrl"
                        :src="fin.imageUrl"
                        :alt="fin.name"
                        class="finition-img"
                      />
                      <Sparkles v-else size="12" class="text-teal" />
                    </div>
                    <span class="finition-name">{{ fin.name }}</span>
                  </div>

                  <div v-if="isAdmin" class="finition-actions">
                    <button class="action-btn-mini" @click="openEditFinitionModal(fin)">
                      <Edit2 size="10" />
                    </button>
                    <button class="action-btn-mini btn-danger" @click="deleteFinition(fin)">
                      <Trash2 size="10" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- SECTION : MOTORISATIONS & GRILLES TARIFAIRES -->
            <div class="motorisations-section-block">
              <div class="section-title-bar">
                <h4 class="section-title flex items-center gap-1.5">
                  <Zap size="14" class="text-teal" />
                  <span>Motorisations & Grilles Tarifaires</span>
                </h4>
                <button
                  v-if="isAdmin"
                  class="btn btn-primary btn-small flex items-center gap-1"
                  @click="openAddMotorisationModal"
                >
                  <Plus size="12" /> Nouvelle Motorisation
                </button>
              </div>

              <div v-if="!activeModel.motorisations || activeModel.motorisations.length === 0" class="empty-sub-notice">
                Aucune motorisation configurée pour ce modèle.
              </div>

              <!-- Cartes des Motorisations -->
              <div v-else class="motorisations-cards-stack">
                <div
                  v-for="mot in activeModel.motorisations"
                  :key="mot.id"
                  class="motorisation-item-card"
                >
                  <!-- En-tête Motorisation -->
                  <div class="mot-header-bar">
                    <div class="mot-header-left">
                      <span class="badge badge-small" :class="mot.fuelType === 'ELECTRIC' ? 'badge-teal' : 'badge-cyan'">
                        {{ mot.fuelType === 'ELECTRIC' ? '100% Électrique' : (mot.fuelType === 'HYBRID' ? 'Hybride' : mot.fuelType) }}
                      </span>
                      <strong class="mot-name-label text-xs font-bold text-main">{{ mot.name }}</strong>
                      <span class="mot-wltp-badge font-mono text-xxs">
                        WLTP : <strong>{{ mot.consumptionWltp }} {{ mot.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km</strong>
                      </span>
                      <span class="mot-specs-pill font-mono text-xxs">
                        {{ mot.powerHp }} ch {{ mot.batteryCapacityKwh ? `· ${mot.batteryCapacityKwh} kWh` : '' }} {{ mot.autonomieWltpKm ? `· ${mot.autonomieWltpKm} km WLTP` : '' }}
                      </span>
                    </div>

                    <div class="mot-header-right">
                      <button
                        v-if="isAdmin"
                        class="btn-associate-pricing"
                        @click="openAddVariantModal(mot.id)"
                      >
                        + Associer Finition & Prix
                      </button>
                      <div v-if="isAdmin" class="mot-action-btns">
                        <button class="action-btn-mini" @click="openEditMotorisationModal(mot)">
                          <Edit2 size="10" />
                        </button>
                        <button class="action-btn-mini btn-danger" @click="deleteMotorisation(mot)">
                          <Trash2 size="10" />
                        </button>
                      </div>
                    </div>
                  </div>

                  <!-- Grille des Variantes / Finitions tarifées pour ce Moteur -->
                  <div class="mot-variants-container">
                    <div
                      v-if="!getVariantsForMot(mot) || getVariantsForMot(mot).length === 0"
                      class="no-variants-label"
                    >
                      Aucune finition tarifée associée à cette motorisation.
                    </div>

                    <div v-else class="variants-cards-grid">
                      <div
                        v-for="p in getVariantsForMot(mot)"
                        :key="p.variantId"
                        class="variant-price-card"
                      >
                        <div class="variant-top-bar">
                          <div class="variant-finition-name">
                            <span class="text-xs font-bold text-main">{{ p.finitionName }}</span>
                          </div>
                          <div v-if="isAdmin" class="variant-admin-btns">
                            <button class="action-btn-mini" @click="openEditVariantModal(mot.id, p)">
                              <Edit2 size="10" />
                            </button>
                            <button class="action-btn-mini btn-danger" @click="deleteVariant(p.variantId)">
                              <Trash2 size="10" />
                            </button>
                          </div>
                        </div>

                        <!-- Grille de Tarifs -->
                        <div class="variant-pricing-lines">
                          <div class="price-line">
                            <span class="price-label">Achat</span>
                            <strong class="price-value text-main font-mono">{{ formatCurrency(p.purchasePrice) }}</strong>
                          </div>
                          <div v-if="p.monthlyLoa" class="price-line">
                            <span class="price-label">LOA</span>
                            <strong class="price-value text-cyan font-mono">{{ formatCurrency(p.monthlyLoa) }}/m</strong>
                          </div>
                          <div v-if="p.monthlyLld" class="price-line">
                            <span class="price-label">LLD</span>
                            <strong class="price-value text-teal font-mono">{{ formatCurrency(p.monthlyLld) }}/m</strong>
                          </div>
                        </div>

                        <!-- Bouton Simulation Express -->
                        <button
                          type="button"
                          class="btn-simulate-variant flex items-center justify-center gap-1.5"
                          @click="openSimulatorWithVariant(activeBrand, activeModel, mot, p)"
                        >
                          <span>Simuler cette configuration</span>
                          <ArrowRight size="13" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- MODAL 1 : AJOUT / ÉDITION MARQUE -->
    <div v-if="brandModalOpen" class="auth-modal-overlay">
      <div class="auth-modal-card p-4 relative modal-box animation-fadeIn">
        <button class="icon-btn-close absolute top-4 right-4" @click="brandModalOpen = false">✕</button>

        <h3 class="modal-title">
          {{ brandEditMode ? 'Modifier le constructeur' : 'Ajouter un constructeur automobile' }}
        </h3>

        <div class="modal-form-stack">
          <div class="form-group mb-0">
            <label class="form-label">Nom de la marque</label>
            <input v-model="brandForm.name" type="text" class="form-control" placeholder="ex: Tesla, Renault, Fiat..." required />
          </div>

          <div class="form-group mb-0">
            <label class="form-label">Logo officiel (Upload image ou URL)</label>
            <div class="upload-input-group">
              <input v-model="brandForm.logoUrl" type="text" class="form-control" placeholder="/uploads/brands/... ou https://..." />
              <label class="btn btn-secondary btn-small cursor-pointer flex items-center gap-1">
                <UploadCloud size="12" />
                <span>Uploader</span>
                <input type="file" accept="image/*" class="hidden-file-input" @change="(e) => handleFileUpload(e, 'brand', 'brands')" />
              </label>
            </div>
          </div>
        </div>

        <div class="modal-footer-bar">
          <button class="btn btn-secondary" @click="brandModalOpen = false">Annuler</button>
          <button :disabled="loading || uploadingImage" class="btn btn-primary font-bold" @click="saveBrand">
            Enregistrer
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL 2 : AJOUT / ÉDITION MODÈLE -->
    <div v-if="modelModalOpen" class="auth-modal-overlay">
      <div class="auth-modal-card p-4 relative modal-box animation-fadeIn">
        <button class="icon-btn-close absolute top-4 right-4" @click="modelModalOpen = false">✕</button>

        <h3 class="modal-title">
          {{ modelEditMode ? 'Modifier le modèle' : `Ajouter un modèle (${activeBrand?.name})` }}
        </h3>

        <div class="modal-form-stack">
          <div class="form-group mb-0">
            <label class="form-label">Nom du modèle</label>
            <input v-model="modelForm.name" type="text" class="form-control" placeholder="ex: 500e, Megane E-Tech, Model 3..." required />
          </div>

          <div class="form-group mb-0">
            <label class="form-label">Catégorie de carrosserie</label>
            <select v-model="modelForm.category" class="form-control form-select">
              <option value="Citadine">Citadine</option>
              <option value="Compacte">Compacte</option>
              <option value="Berline">Berline</option>
              <option value="SUV">SUV</option>
              <option value="Crossover">Crossover</option>
              <option value="Break">Break</option>
              <option value="Coupé">Coupé</option>
              <option value="Utilitaire">Utilitaire</option>
            </select>
          </div>

          <div class="form-group mb-0">
            <label class="form-label">Photo / Visuel du véhicule (Upload ou URL)</label>
            <div class="upload-input-group">
              <input v-model="modelForm.imageUrl" type="text" class="form-control" placeholder="/uploads/models/... ou https://..." />
              <label class="btn btn-secondary btn-small cursor-pointer flex items-center gap-1">
                <UploadCloud size="12" />
                <span>Uploader</span>
                <input type="file" accept="image/*" class="hidden-file-input" @change="(e) => handleFileUpload(e, 'model', 'models')" />
              </label>
            </div>
          </div>
        </div>

        <div class="modal-footer-bar">
          <button class="btn btn-secondary" @click="modelModalOpen = false">Annuler</button>
          <button :disabled="loading || uploadingImage" class="btn btn-primary font-bold" @click="saveModel">
            Enregistrer
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL 3 : AJOUT / ÉDITION MOTORISATION -->
    <div v-if="motorisationModalOpen" class="auth-modal-overlay">
      <div class="auth-modal-card p-4 relative modal-box animation-fadeIn">
        <button class="icon-btn-close absolute top-4 right-4" @click="motorisationModalOpen = false">✕</button>

        <h3 class="modal-title">
          {{ motorisationEditMode ? 'Modifier la motorisation' : `Ajouter une motorisation (${activeModel?.name})` }}
        </h3>

        <div class="modal-form-stack">
          <div class="form-group mb-0">
            <label class="form-label">Nom du groupe motopropulseur</label>
            <input v-model="motorisationForm.name" type="text" class="form-control" placeholder="ex: Électrique 156 ch (54 kWh)" required />
          </div>

          <div class="grid-2-modal">
            <div class="form-group mb-0">
              <label class="form-label">Énergie</label>
              <select v-model="motorisationForm.fuelType" class="form-control form-select">
                <option value="ELECTRIC">Électrique</option>
                <option value="HYBRID">Hybride</option>
                <option value="PETROL">Essence</option>
                <option value="DIESEL">Diesel</option>
              </select>
            </div>

            <div class="form-group mb-0">
              <label class="form-label">Conso WLTP ({{ motorisationForm.fuelType === 'ELECTRIC' ? 'kWh' : 'L' }}/100km)</label>
              <input v-model.number="motorisationForm.consumptionWltp" type="number" step="0.1" class="form-control font-bold" required />
            </div>
          </div>

          <div class="grid-2-modal">
            <div class="form-group mb-0">
              <label class="form-label">Puissance (ch)</label>
              <input v-model.number="motorisationForm.powerHp" type="number" class="form-control" />
            </div>

            <div class="form-group mb-0">
              <label class="form-label">Capacité Batterie (kWh)</label>
              <input v-model.number="motorisationForm.batteryCapacityKwh" type="number" step="0.1" class="form-control" />
            </div>
          </div>
        </div>

        <div class="modal-footer-bar">
          <button class="btn btn-secondary" @click="motorisationModalOpen = false">Annuler</button>
          <button :disabled="loading" class="btn btn-primary font-bold" @click="saveMotorisation">
            Enregistrer
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL 4 : AJOUT / ÉDITION FINITION -->
    <div v-if="finitionModalOpen" class="auth-modal-overlay">
      <div class="auth-modal-card p-4 relative modal-box animation-fadeIn">
        <button class="icon-btn-close absolute top-4 right-4" @click="finitionModalOpen = false">✕</button>

        <h3 class="modal-title">
          {{ finitionEditMode ? 'Modifier la finition' : `Ajouter une finition (${activeModel?.name})` }}
        </h3>

        <div class="modal-form-stack">
          <div class="form-group mb-0">
            <label class="form-label">Nom de la finition</label>
            <input v-model="finitionForm.name" type="text" class="form-control" placeholder="ex: Techno, GT, Long Range, La Prima..." required />
          </div>

          <div class="form-group mb-0">
            <label class="form-label">Visuel de la finition (Upload ou URL)</label>
            <div class="upload-input-group">
              <input v-model="finitionForm.imageUrl" type="text" class="form-control" placeholder="/uploads/finitions/... ou https://..." />
              <label class="btn btn-secondary btn-small cursor-pointer flex items-center gap-1">
                <UploadCloud size="12" />
                <span>Uploader</span>
                <input type="file" accept="image/*" class="hidden-file-input" @change="(e) => handleFileUpload(e, 'finition', 'finitions')" />
              </label>
            </div>
          </div>
        </div>

        <div class="modal-footer-bar">
          <button class="btn btn-secondary" @click="finitionModalOpen = false">Annuler</button>
          <button :disabled="loading || uploadingImage" class="btn btn-primary font-bold" @click="saveFinition">
            Enregistrer
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL 5 : TARIFICATION VARIANTE (FINITION x MOTEUR) -->
    <div v-if="variantModalOpen" class="auth-modal-overlay">
      <div class="auth-modal-card p-4 relative modal-box-lg animation-fadeIn">
        <button class="icon-btn-close absolute top-4 right-4" @click="variantModalOpen = false">✕</button>

        <h3 class="modal-title">
          {{ variantEditMode ? 'Modifier la tarification' : 'Associer Finition & Moteur avec Tarifs' }}
        </h3>

        <div class="modal-form-stack">
          <div class="grid-2-modal">
            <div class="form-group mb-0">
              <label class="form-label">Finition</label>
              <select v-model="variantForm.finitionId" :disabled="variantEditMode" class="form-control form-select">
                <option v-for="f in activeModel?.finitions" :key="f.id" :value="f.id">{{ f.name }}</option>
              </select>
            </div>

            <div class="form-group mb-0">
              <label class="form-label">Motorisation</label>
              <select v-model="variantForm.motorisationId" :disabled="variantEditMode" class="form-control form-select">
                <option v-for="m in activeModel?.motorisations" :key="m.id" :value="m.id">{{ m.name }}</option>
              </select>
            </div>
          </div>

          <div class="grid-3-modal">
            <div class="form-group mb-0">
              <label class="form-label">Prix Achat (€)</label>
              <input v-model.number="variantForm.purchasePrice" type="number" class="form-control font-bold" required />
            </div>
            <div class="form-group mb-0">
              <label class="form-label">Loyer LOA (€/m)</label>
              <input v-model.number="variantForm.monthlyLoa" type="number" class="form-control text-cyan font-bold" />
            </div>
            <div class="form-group mb-0">
              <label class="form-label">Loyer LLD (€/m)</label>
              <input v-model.number="variantForm.monthlyLld" type="number" class="form-control text-teal font-bold" />
            </div>
          </div>

          <div class="grid-3-modal">
            <div class="form-group mb-0">
              <label class="form-label">Assurance (€/an)</label>
              <input v-model.number="variantForm.defaultInsuranceCost" type="number" class="form-control" />
            </div>
            <div class="form-group mb-0">
              <label class="form-label">Entretien (€/an)</label>
              <input v-model.number="variantForm.defaultMaintenanceCost" type="number" class="form-control" />
            </div>
            <div class="form-group mb-0">
              <label class="form-label">Revente (€)</label>
              <input v-model.number="variantForm.estimatedResaleValue" type="number" class="form-control" />
            </div>
          </div>
        </div>

        <div class="modal-footer-bar">
          <button class="btn btn-secondary" @click="variantModalOpen = false">Annuler</button>
          <button :disabled="loading" class="btn btn-primary font-bold" @click="saveVariant">
            Enregistrer
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Car,
  Layers,
  Sparkles,
  Zap,
  Leaf,
  Plus,
  Edit2,
  Trash2,
  ArrowRight,
  ShieldCheck,
  Eye,
  Check,
  AlertTriangle,
  UploadCloud,
  Search
} from '@lucide/vue'
import {
  apiGetCatalogHierarchy,
  apiCreateBrand,
  apiUpdateBrand,
  apiDeleteBrand,
  apiCreateModel,
  apiUpdateModel,
  apiDeleteModel,
  apiCreateMotorisation,
  apiUpdateMotorisation,
  apiDeleteMotorisation,
  apiCreateFinition,
  apiUpdateFinition,
  apiDeleteFinition,
  apiCreateVariant,
  apiUpdateVariant,
  apiDeleteVariant,
  apiUploadImage
} from '@/utils/api.js'

const props = defineProps({
  currentUser: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['open-simulator'])

const hierarchy = ref([])
const loading = ref(false)
const error = ref(null)
const successMsg = ref(null)
const brandSearch = ref('')

const activeBrand = ref(null)
const activeModel = ref(null)

// Modals State
const brandModalOpen = ref(false)
const brandEditMode = ref(false)
const brandForm = ref({ id: null, name: '', logoUrl: '' })

const modelModalOpen = ref(false)
const modelEditMode = ref(false)
const modelForm = ref({ id: null, name: '', imageUrl: '', category: 'Berline' })

const motorisationModalOpen = ref(false)
const motorisationEditMode = ref(false)
const motorisationForm = ref({
  id: null,
  name: '',
  fuelType: 'ELECTRIC',
  consumptionWltp: 15.0,
  powerHp: 150,
  batteryCapacityKwh: 50.0
})

const finitionModalOpen = ref(false)
const finitionEditMode = ref(false)
const finitionForm = ref({ id: null, name: '', imageUrl: '' })

const variantModalOpen = ref(false)
const variantEditMode = ref(false)
const variantForm = ref({
  id: null,
  finitionId: null,
  motorisationId: null,
  purchasePrice: 35000,
  monthlyLoa: 290,
  monthlyLld: 270,
  defaultInsuranceCost: 650,
  defaultMaintenanceCost: 250,
  estimatedResaleValue: 18000
})

const uploadingImage = ref(false)

// ── Computed ──────────────────────────────────────────────────────────────

const isAdmin = computed(() => {
  if (!props.currentUser) return false
  if (props.currentUser.role === 'ADMIN') return true
  const email = (props.currentUser.email || '').trim().toLowerCase()
  return email === 'modeste.william.s@gmail.com' || email === 'admin' || email === 'admin@ecoswitch.com'
})

const filteredHierarchy = computed(() => {
  if (!brandSearch.value.trim()) return hierarchy.value
  const q = brandSearch.value.toLowerCase()
  return hierarchy.value.filter(b => b.name.toLowerCase().includes(q))
})

// ── Helpers ───────────────────────────────────────────────────────────────

const getVariantsForMot = (mot) => {
  return mot.availableFinitions || mot.variantPricings || []
}

const selectActiveBrand = (b) => {
  activeBrand.value = b
  if (b.models && b.models.length > 0) {
    activeModel.value = b.models[0]
  } else {
    activeModel.value = null
  }
}

const loadCatalogHierarchy = async () => {
  loading.value = true
  error.value = null
  try {
    const data = await apiGetCatalogHierarchy()
    hierarchy.value = data || []
    
    if (activeBrand.value) {
      activeBrand.value = hierarchy.value.find(b => b.id === activeBrand.value.id) || hierarchy.value[0] || null
    } else if (hierarchy.value.length > 0) {
      activeBrand.value = hierarchy.value[0]
    }
    
    if (activeBrand.value && activeBrand.value.models?.length > 0) {
      if (activeModel.value) {
        activeModel.value = activeBrand.value.models.find(m => m.id === activeModel.value.id) || activeBrand.value.models[0]
      } else {
        activeModel.value = activeBrand.value.models[0]
      }
    } else {
      activeModel.value = null
    }
  } catch (err) {
    error.value = err.message || "Erreur lors du chargement du catalogue."
  } finally {
    loading.value = false
  }
}

const handleFileUpload = async (event, targetField, folder) => {
  const file = event.target.files?.[0]
  if (!file) return

  uploadingImage.value = true
  error.value = null
  try {
    const url = await apiUploadImage(file, folder)
    if (targetField === 'brand') brandForm.value.logoUrl = url
    else if (targetField === 'model') modelForm.value.imageUrl = url
    else if (targetField === 'finition') finitionForm.value.imageUrl = url
    showSuccess("Image téléversée avec succès !")
  } catch (err) {
    error.value = err.message || "Échec du téléversement de l'image."
  } finally {
    uploadingImage.value = false
  }
}

// ── CRUD Handlers ─────────────────────────────────────────────────────────

const openAddBrandModal = () => {
  brandEditMode.value = false
  brandForm.value = { id: null, name: '', logoUrl: '' }
  brandModalOpen.value = true
}

const openEditBrandModal = (brand) => {
  brandEditMode.value = true
  brandForm.value = { id: brand.id, name: brand.name, logoUrl: brand.logoUrl || '' }
  brandModalOpen.value = true
}

const saveBrand = async () => {
  if (!brandForm.value.name.trim()) return
  loading.value = true
  try {
    if (brandEditMode.value) {
      await apiUpdateBrand(brandForm.value.id, { name: brandForm.value.name, logoUrl: brandForm.value.logoUrl })
      showSuccess("Marque mise à jour.")
    } else {
      await apiCreateBrand({ name: brandForm.value.name, logoUrl: brandForm.value.logoUrl })
      showSuccess("Marque ajoutée.")
    }
    brandModalOpen.value = false
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const deleteBrand = async (brand) => {
  if (!confirm(`Supprimer la marque ${brand.name} et tous ses modèles ?`)) return
  loading.value = true
  try {
    await apiDeleteBrand(brand.id)
    showSuccess("Marque supprimée.")
    if (activeBrand.value?.id === brand.id) activeBrand.value = null
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const openAddModelModal = () => {
  if (!activeBrand.value) return
  modelEditMode.value = false
  modelForm.value = { id: null, name: '', imageUrl: '', category: 'Berline' }
  modelModalOpen.value = true
}

const openEditModelModal = (model) => {
  modelEditMode.value = true
  modelForm.value = { id: model.id, name: model.name, imageUrl: model.imageUrl || '', category: model.category || 'Berline' }
  modelModalOpen.value = true
}

const saveModel = async () => {
  if (!modelForm.value.name.trim() || !activeBrand.value) return
  loading.value = true
  try {
    if (modelEditMode.value) {
      await apiUpdateModel(modelForm.value.id, modelForm.value)
      showSuccess("Modèle mis à jour.")
    } else {
      await apiCreateModel(activeBrand.value.id, modelForm.value)
      showSuccess("Modèle ajouté.")
    }
    modelModalOpen.value = false
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const deleteModel = async (model) => {
  if (!confirm(`Supprimer le modèle ${model.name} ?`)) return
  loading.value = true
  try {
    await apiDeleteModel(model.id)
    showSuccess("Modèle supprimé.")
    if (activeModel.value?.id === model.id) activeModel.value = null
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const openAddMotorisationModal = () => {
  if (!activeModel.value) return
  motorisationEditMode.value = false
  motorisationForm.value = {
    id: null,
    name: '',
    fuelType: 'ELECTRIC',
    consumptionWltp: 15.0,
    powerHp: 150,
    batteryCapacityKwh: 50.0
  }
  motorisationModalOpen.value = true
}

const openEditMotorisationModal = (mot) => {
  motorisationEditMode.value = true
  motorisationForm.value = { ...mot }
  motorisationModalOpen.value = true
}

const saveMotorisation = async () => {
  if (!motorisationForm.value.name.trim() || !activeModel.value) return
  loading.value = true
  try {
    if (motorisationEditMode.value) {
      await apiUpdateMotorisation(motorisationForm.value.id, motorisationForm.value)
      showSuccess("Motorisation mise à jour.")
    } else {
      await apiCreateMotorisation(activeModel.value.id, motorisationForm.value)
      showSuccess("Motorisation ajoutée.")
    }
    motorisationModalOpen.value = false
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const deleteMotorisation = async (mot) => {
  if (!confirm(`Supprimer la motorisation ${mot.name} ?`)) return
  loading.value = true
  try {
    await apiDeleteMotorisation(mot.id)
    showSuccess("Motorisation supprimée.")
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const openAddFinitionModal = () => {
  if (!activeModel.value) return
  finitionEditMode.value = false
  finitionForm.value = { id: null, name: '', imageUrl: '' }
  finitionModalOpen.value = true
}

const openEditFinitionModal = (fin) => {
  finitionEditMode.value = true
  finitionForm.value = { id: fin.id, name: fin.name, imageUrl: fin.imageUrl || '' }
  finitionModalOpen.value = true
}

const saveFinition = async () => {
  if (!finitionForm.value.name.trim() || !activeModel.value) return
  loading.value = true
  try {
    if (finitionEditMode.value) {
      await apiUpdateFinition(finitionForm.value.id, finitionForm.value)
      showSuccess("Finition mise à jour.")
    } else {
      await apiCreateFinition(activeModel.value.id, finitionForm.value)
      showSuccess("Finition ajoutée.")
    }
    finitionModalOpen.value = false
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const deleteFinition = async (fin) => {
  if (!confirm(`Supprimer la finition ${fin.name} ?`)) return
  loading.value = true
  try {
    await apiDeleteFinition(fin.id)
    showSuccess("Finition supprimée.")
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const openAddVariantModal = (motId = null) => {
  if (!activeModel.value) return
  variantEditMode.value = false
  variantForm.value = {
    id: null,
    finitionId: activeModel.value.finitions?.[0]?.id || null,
    motorisationId: motId || activeModel.value.motorisations?.[0]?.id || null,
    purchasePrice: 35000,
    monthlyLoa: 290,
    monthlyLld: 270,
    defaultInsuranceCost: 650,
    defaultMaintenanceCost: 250,
    estimatedResaleValue: 18000
  }
  variantModalOpen.value = true
}

const openEditVariantModal = (motId, price) => {
  variantEditMode.value = true
  variantForm.value = {
    id: price.variantId,
    finitionId: price.finitionId,
    motorisationId: motId,
    purchasePrice: price.purchasePrice,
    monthlyLoa: price.monthlyLoa,
    monthlyLld: price.monthlyLld,
    defaultInsuranceCost: price.defaultInsuranceCost || 650,
    defaultMaintenanceCost: price.defaultMaintenanceCost || 250,
    estimatedResaleValue: price.estimatedResaleValue || 18000
  }
  variantModalOpen.value = true
}

const saveVariant = async () => {
  if (!variantForm.value.finitionId || !variantForm.value.motorisationId) return
  loading.value = true
  try {
    if (variantEditMode.value) {
      await apiUpdateVariant(variantForm.value.id, variantForm.value)
      showSuccess("Tarif mis à jour.")
    } else {
      await apiCreateVariant(variantForm.value.finitionId, variantForm.value.motorisationId, variantForm.value)
      showSuccess("Tarification enregistrée.")
    }
    variantModalOpen.value = false
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const deleteVariant = async (variantId) => {
  if (!confirm("Supprimer cette variante tarifaire ?")) return
  loading.value = true
  try {
    await apiDeleteVariant(variantId)
    showSuccess("Tarif supprimé.")
    await loadCatalogHierarchy()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

const showSuccess = (msg) => {
  successMsg.value = msg
  setTimeout(() => {
    successMsg.value = null
  }, 3500)
}

const formatCurrency = (val) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(val || 0)
}

const getFuelBadgeClass = (type) => {
  switch (type) {
    case 'ELECTRIC': return 'badge-teal'
    case 'HYBRID': return 'badge-cyan'
    case 'DIESEL': return 'badge-amber'
    default: return 'badge-rose'
  }
}

const openSimulatorWithVariant = (brand, model, mot, price) => {
  const simVehicle = {
    name: `${brand.name} ${model.name} ${mot.name} ${price.finitionName}`,
    brand: brand.name,
    model: model.name,
    version: `${mot.name} - ${price.finitionName}`,
    fuelType: mot.fuelType,
    consumption: mot.consumptionWltp,
    purchasePrice: price.purchasePrice,
    monthlyLoa: price.monthlyLoa,
    monthlyLld: price.monthlyLld,
    insuranceCost: price.defaultInsuranceCost || 650,
    maintenanceCost: price.defaultMaintenanceCost || 250,
    resaleValue: price.estimatedResaleValue || 0,
    imageUrl: price.finitionImageUrl || model.imageUrl,
    brandLogoUrl: brand.logoUrl,
    annualMileage: 15000
  }
  localStorage.setItem('eco_custom_target_vehicle', JSON.stringify(simVehicle))
  emit('open-simulator')
}

onMounted(() => {
  loadCatalogHierarchy()
})
</script>

<style scoped>
.catalog-manager-app {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

/* Header */
.manager-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-glass);
}
.catalog-icon-badge {
  font-size: 1.2rem;
}
.manager-main-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 2px 0;
  flex-wrap: wrap;
}
.admin-status-badge {
  font-size: 0.68rem;
  font-weight: 700;
  background: rgba(16, 124, 65, 0.15);
  color: var(--accent-teal);
  border: 1px solid rgba(16, 124, 65, 0.3);
  padding: 3px 8px;
  border-radius: 9999px;
}
.consultation-status-badge {
  font-size: 0.68rem;
  font-weight: 600;
  background: var(--bg-card-subtle);
  color: var(--text-dimmed);
  border: 1px solid var(--border-glass);
  padding: 3px 8px;
  border-radius: 9999px;
}
.manager-subtitle {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin: 0;
}

/* Toast Alerts */
.catalog-toast {
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 0.78rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.toast-success {
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
  border: 1px solid rgba(16, 124, 65, 0.2);
}
.toast-error {
  background: var(--accent-rose-soft);
  color: var(--accent-rose);
  border: 1px solid rgba(225, 29, 72, 0.2);
}
.toast-close {
  margin-left: auto;
  background: transparent;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 0.85rem;
}

/* Master-Detail Layout */
.catalog-main-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  align-items: start;
}
@media (max-width: 1024px) {
  .catalog-main-layout {
    grid-template-columns: 1fr;
  }
}

/* PANE 1 : Sidebar Marques */
.brands-sidebar-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 24px;
  height: calc(100vh - 120px);
  min-height: 500px;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-glass);
}
.sidebar-title {
  font-size: 0.72rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-dimmed);
}
.link-action-btn {
  background: transparent;
  border: none;
  color: var(--accent-teal);
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
}
.link-action-btn:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.sidebar-search-box {
  width: 100%;
}
.sidebar-search-input {
  width: 100%;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-glass);
  background: var(--bg-input);
  color: var(--text-main);
  font-size: 0.75rem;
  outline: none;
}
.sidebar-search-input:focus {
  border-color: var(--accent-teal);
}

.brands-items-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 4px;
}
.brand-row-item {
  padding: 8px 10px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.15s ease;
}
.brand-row-item:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-hover);
}
.brand-row-item.is-selected {
  background: var(--accent-teal-soft);
  border-color: var(--accent-teal);
}

.brand-row-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-logo-box {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  flex-shrink: 0;
}
.brand-logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.fallback-emoji {
  font-size: 0.85rem;
}
.brand-name-text {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--text-main);
}

.brand-row-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.models-count-pill {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--text-dimmed);
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  padding: 2px 6px;
  border-radius: 9999px;
}
.row-hover-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.action-btn-mini {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  color: var(--text-dimmed);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.65rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.action-btn-mini:hover {
  color: var(--text-main);
  border-color: var(--border-hover);
}
.action-btn-mini.btn-danger:hover {
  color: var(--accent-rose);
  border-color: var(--accent-rose);
}

/* PANE 2 : Modèles & Motorisations */
.brand-detail-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.space-y-main {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.empty-state-card {
  padding: 40px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.empty-icon {
  font-size: 2.5rem;
}

.models-selector-card {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.models-bar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-glass);
}
.brand-badge-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-logo-box {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.header-logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.brand-heading {
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
}

.models-tabs-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.model-tab-btn {
  padding: 6px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.model-tab-btn:hover {
  background: var(--bg-card-hover);
}
.model-tab-btn.is-active {
  background: #1D1D1F;
  border-color: #1D1D1F;
  color: #FFFFFF;
}
[data-theme="dark"] .model-tab-btn.is-active {
  background: #FFFFFF;
  border-color: #FFFFFF;
  color: #000000;
}
.tab-img-box {
  width: 28px;
  height: 18px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tab-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.tab-text-stack {
  display: flex;
  flex-direction: column;
  text-align: left;
  line-height: 1.15;
}
.tab-model-name {
  font-size: 0.78rem;
  font-weight: 700;
}
.tab-category {
  font-size: 0.65rem;
  opacity: 0.75;
}

/* Modèle Detail Hero */
.model-detail-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.model-hero-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-glass);
  gap: 12px;
}
.hero-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.hero-vehicle-media {
  width: 130px;
  height: 80px;
  border-radius: var(--radius-md);
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hero-placeholder-icon {
  font-size: 2rem;
}
.hero-category-tag {
  position: absolute;
  bottom: 4px;
  left: 4px;
  font-size: 0.58rem;
  font-weight: 700;
  background: rgba(0, 0, 0, 0.7);
  color: #FFFFFF;
  padding: 1px 5px;
  border-radius: 4px;
}

.hero-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.hero-brand-line {
  display: flex;
  align-items: center;
  gap: 5px;
}
.hero-mini-logo {
  width: 14px;
  height: 14px;
  object-fit: contain;
}
.hero-brand-name {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--text-dimmed);
}
.hero-model-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
}
.hero-badges {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}
.hero-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Sections */
.section-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.section-title {
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}

/* Finitions Pill Grid */
.finitions-pill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
}
.finition-card-pill {
  padding: 6px 10px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.finition-pill-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.finition-media-box {
  width: 26px;
  height: 18px;
  border-radius: 4px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.finition-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.finition-name {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-main);
}
.finition-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* Motorisations & Tarifs */
.motorisations-cards-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.motorisation-item-card {
  padding: 12px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
  background: var(--bg-card-subtle);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.mot-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-glass);
  gap: 10px;
  flex-wrap: wrap;
}
.mot-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.fuel-badge-tag {
  font-size: 0.7rem;
  font-weight: 800;
  padding: 2px 7px;
  border-radius: 4px;
}
.mot-name-label {
  font-size: 0.82rem;
  color: var(--text-main);
}
.mot-wltp-badge {
  font-size: 0.7rem;
  color: var(--accent-teal);
  background: var(--accent-teal-soft);
  border: 1px solid rgba(16, 124, 65, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
}
.mot-specs-pill {
  font-size: 0.7rem;
  color: var(--text-dimmed);
}

.mot-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-associate-pricing {
  background: transparent;
  border: 1px solid var(--accent-teal);
  color: var(--accent-teal);
  font-size: 0.72rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-associate-pricing:hover {
  background: var(--accent-teal-soft);
}
.mot-action-btns {
  display: flex;
  align-items: center;
  gap: 2px;
}

.no-variants-label {
  font-size: 0.74rem;
  color: var(--text-dimmed);
  font-style: italic;
  padding: 4px 0;
}

/* Grille des Variantes / Finitions Tarifées */
.variants-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 10px;
}
.variant-price-card {
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-glass);
  background: var(--bg-card);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  box-shadow: var(--shadow-sm);
}
.variant-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.variant-finition-name {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.8rem;
  color: var(--text-main);
}
.sparkle-icon {
  font-size: 0.8rem;
}
.variant-admin-btns {
  display: flex;
  align-items: center;
  gap: 2px;
}

.variant-pricing-lines {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.price-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.72rem;
}
.price-label {
  color: var(--text-dimmed);
}
.price-value {
  font-weight: 700;
}

.btn-simulate-variant {
  width: 100%;
  padding: 5px 8px;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  border-radius: 6px;
  color: var(--accent-teal);
  font-size: 0.72rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-simulate-variant:hover {
  background: var(--accent-teal-soft);
  border-color: var(--accent-teal);
}

/* Modals */
.modal-box {
  width: 100%;
  max-width: 440px;
}
.modal-box-lg {
  width: 100%;
  max-width: 520px;
}
.modal-title {
  font-size: 0.95rem;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 12px;
}
.modal-form-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.upload-input-group {
  display: flex;
  gap: 6px;
}
.hidden-file-input {
  display: none;
}
.grid-2-modal {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.grid-3-modal {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.modal-footer-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-glass);
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
.empty-sub-notice {
  font-size: 0.74rem;
  color: var(--text-dimmed);
  font-style: italic;
  padding: 6px 0;
}

/* ── Mobile & Tablet Optimizations (< 1024px) ───────────────────────── */
@media (max-width: 1024px) {
  .catalog-manager-app {
    gap: 12px;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow-x: hidden;
    box-sizing: border-box;
  }

  .manager-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 4px;
    width: 100%;
    box-sizing: border-box;
  }
  .manager-main-title {
    font-size: 1.05rem;
    flex-wrap: wrap;
    gap: 6px;
  }
  .hide-on-mobile {
    display: none !important;
  }
  .header-actions {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }

  .catalog-main-layout {
    grid-template-columns: 1fr;
    gap: 12px;
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }

  /* PANE 1 : Sélecteur de Marques Horizontal */
  .brands-sidebar-card {
    padding: 10px 12px;
    max-height: none;
    gap: 8px;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
    overflow: hidden;
  }
  .sidebar-header {
    padding-bottom: 4px;
  }
  .sidebar-search-box {
    margin-bottom: 2px;
    width: 100%;
    box-sizing: border-box;
  }
  .sidebar-search-input {
    padding: 6px 10px;
    font-size: 0.76rem;
    width: 100%;
    box-sizing: border-box;
  }
  .scroll-hint-pill {
    font-size: 0.62rem;
    font-weight: 700;
    color: var(--accent-teal);
    background: var(--accent-teal-soft);
    padding: 1px 6px;
    border-radius: 9999px;
    border: 1px solid rgba(16, 124, 65, 0.25);
    white-space: nowrap;
    animation: pulseHint 2s infinite ease-in-out;
  }
  @keyframes pulseHint {
    0%, 100% { opacity: 0.85; transform: translateX(0); }
    50% { opacity: 1; transform: translateX(2px); }
  }

  .brands-items-list {
    display: flex;
    flex-direction: row;
    overflow-x: auto;
    scrollbar-width: none;
    -webkit-overflow-scrolling: touch;
    gap: 5px;
    padding: 2px 0 6px 0;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
  }
  .brands-items-list::-webkit-scrollbar {
    display: none;
  }
  .brand-row-item {
    flex-shrink: 0;
    padding: 4px 8px;
    border-radius: 9999px;
    gap: 5px;
  }
  .brand-logo-box {
    width: 18px;
    height: 18px;
    border-radius: 3px;
    padding: 1px;
  }
  .brand-name-text {
    font-size: 0.72rem;
    font-weight: 700;
    white-space: nowrap;
  }
  .models-count-pill {
    font-size: 0.58rem;
    padding: 1px 4px;
    border-radius: 9999px;
  }

  /* PANE 2 : Sélecteur de Modèles Horizontal */
  .brand-detail-main,
  .space-y-main {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
  }

  .models-selector-card {
    padding: 10px 12px;
    gap: 8px;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
    overflow: hidden;
  }
  .models-bar-header {
    padding-bottom: 6px;
    flex-wrap: wrap;
    gap: 8px;
    width: 100%;
    box-sizing: border-box;
  }
  .brand-heading {
    font-size: 0.88rem;
  }
  .models-tabs-grid {
    display: flex;
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
    -webkit-overflow-scrolling: touch;
    gap: 6px;
    padding: 2px 0 6px 0;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
  }
  .models-tabs-grid::-webkit-scrollbar {
    display: none;
  }
  .model-tab-btn {
    flex-shrink: 0;
    padding: 6px 10px;
    border-radius: 8px;
    gap: 6px;
  }
  .tab-img-box {
    width: 24px;
    height: 16px;
  }
  .tab-model-name {
    font-size: 0.72rem;
    white-space: nowrap;
  }
  .tab-category {
    font-size: 0.6rem;
  }

  /* Détail Modèle */
  .model-detail-card {
    padding: 12px 10px;
    gap: 12px;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
    overflow: hidden;
  }
  .model-hero-banner {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    padding-bottom: 10px;
    width: 100%;
    box-sizing: border-box;
  }
  .hero-left {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 8px;
    width: 100%;
    box-sizing: border-box;
  }
  .hero-vehicle-media {
    width: 100%;
    max-width: 240px;
    height: 110px;
    margin: 0 auto;
  }
  .hero-info {
    align-items: center;
    text-align: center;
    width: 100%;
    box-sizing: border-box;
  }
  .hero-model-title {
    font-size: 1.05rem;
  }
  .hero-badges {
    justify-content: center;
  }
  .hero-actions {
    width: 100%;
    justify-content: center;
  }
  .hero-actions button {
    flex: 1;
  }

  /* Finitions */
  .finitions-pill-grid {
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    gap: 6px;
  }
  .finition-card-pill {
    padding: 4px 8px;
  }
  .finition-name {
    font-size: 0.72rem;
  }

  /* Motorisations & Variantes */
  .motorisation-item-card {
    padding: 10px 8px;
    gap: 8px;
  }
  .mot-header-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 6px;
    padding-bottom: 6px;
  }
  .mot-header-left {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 5px;
  }
  .mot-header-right {
    width: 100%;
    justify-content: space-between;
  }
  .btn-associate-pricing {
    flex: 1;
    text-align: center;
    padding: 5px 8px;
  }

  .variants-cards-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .variant-price-card {
    padding: 8px 10px;
    gap: 6px;
  }

  /* Modals */
  .modal-box,
  .modal-box-lg {
    max-width: 95vw;
    padding: 16px 12px;
  }
  .grid-2-modal,
  .grid-3-modal {
    grid-template-columns: 1fr;
  }
}
</style>
