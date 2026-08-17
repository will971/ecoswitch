<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import {
  Car,
  BarChart3,
  Settings,
  Zap,
  Activity,
  ExternalLink,
  ShieldCheck,
  User,
  LogOut,
  Lock,
  Mail,
  Key,
  CreditCard,
  Sparkles,
  Check,
  Sun,
  Moon,
  Bookmark,
  TrendingUp,
  Fuel
} from '@lucide/vue'

import DirectSimulator from './components/DirectSimulator.vue'
import CatalogComparator from './components/CatalogComparator.vue'
import VehicleManager from './components/VehicleManager.vue'
import SavedSimulations from './components/SavedSimulations.vue'
import UserProfileModal from './components/UserProfileModal.vue'
import { apiLogin, apiRegister, apiGoogleLogin, apiGetMe, apiGetUserVehicleProfiles } from './utils/api.js'

// Vue active
const activeTab = ref('direct-sim')

// Theme (Default Light Studio)
const theme = ref(localStorage.getItem('eco_theme') || 'light')

const applyTheme = (t) => {
  theme.value = t
  localStorage.setItem('eco_theme', t)
  document.documentElement.setAttribute('data-theme', t)
}

const toggleTheme = () => {
  applyTheme(theme.value === 'dark' ? 'light' : 'dark')
}

// État Authentification
const currentUser = ref(null)
const authModalOpen = ref(false)
const authMode = ref('login') // 'login' | 'register'
const authEmail = ref('')
const authPassword = ref('')
const authError = ref('')
const authLoading = ref(false)

// Garage & Profils utilisateur
const userProfiles = ref([])
const activeUserProfile = ref(null)
const profileModalOpen = ref(false)

// Données transmises au simulateur lors d'un rechargement
const simulationToLoad = ref(null)

const checkCurrentUser = async () => {
  const token = localStorage.getItem('saas_token')
  const savedUser = localStorage.getItem('saas_user')
  if (!token || token === 'undefined' || token === 'null' || token.length < 10) {
    handleLogout()
    return
  }

  if (savedUser && savedUser !== 'undefined' && savedUser !== 'null') {
    try {
      currentUser.value = JSON.parse(savedUser)
    } catch (e) {}
  }

  try {
    const user = await apiGetMe()
    currentUser.value = user
    localStorage.setItem('saas_user', JSON.stringify(user))
    await loadUserProfiles()
  } catch (err) {
    console.warn("Session expirée ou invalide :", err)
    handleLogout()
  }
}

const loadUserProfiles = async () => {
  if (!currentUser.value) return
  try {
    const profiles = await apiGetUserVehicleProfiles()
    userProfiles.value = profiles
    if (profiles && profiles.length > 0) {
      const defaultP = profiles.find(p => p.default) || profiles[0]
      activeUserProfile.value = defaultP
    } else {
      activeUserProfile.value = null
    }
  } catch (err) {
    console.error("Erreur chargement garage :", err)
  }
}

const persistSession = (data) => {
  if (!data?.token) return
  localStorage.setItem('saas_token', data.token)
  const user = {
    email: data.email || authEmail.value,
    name: data.name || (data.email ? data.email.split('@')[0] : 'Utilisateur'),
    plan: data.plan || 'Pro',
    role: data.role || 'USER'
  }
  localStorage.setItem('saas_user', JSON.stringify(user))
  currentUser.value = user
  authModalOpen.value = false
  authEmail.value = ''
  authPassword.value = ''
}

const handleLogin = async () => {
  authError.value = ''
  authLoading.value = true
  try {
    const data = await apiLogin(authEmail.value, authPassword.value)
    persistSession(data)
    await loadUserProfiles()
  } catch (err) {
    authError.value = err.message || 'Email ou mot de passe incorrect.'
  } finally {
    authLoading.value = false
  }
}

const handleRegister = async () => {
  authError.value = ''
  authLoading.value = true
  try {
    const name = authEmail.value.split('@')[0]
    const data = await apiRegister(authEmail.value, authPassword.value, name)
    persistSession(data)
    await loadUserProfiles()
  } catch (err) {
    authError.value = err.message || "Erreur lors de l'inscription."
  } finally {
    authLoading.value = false
  }
}

// ── Google SSO Integration (Identity Services) ──────────────────────────────
const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''

const initGoogleSignIn = () => {
  if (typeof google === 'undefined' || !google.accounts?.id) return
  try {
    google.accounts.id.initialize({
      client_id: clientId,
      callback: handleGoogleCredentialResponse,
      auto_select: false,
      cancel_on_tap_outside: true
    })
  } catch (e) {
    console.warn("Initialisation Google Sign-In:", e)
  }
}

const renderGoogleButton = () => {
  if (typeof google === 'undefined' || !google.accounts?.id) return
  const btnEl = document.getElementById('google-signin-btn')
  if (!btnEl) return
  
  btnEl.innerHTML = ''
  try {
    google.accounts.id.renderButton(
      btnEl,
      { 
        theme: theme.value === 'dark' ? 'filled_black' : 'outline', 
        size: 'large', 
        width: 300,
        text: 'continue_with',
        shape: 'rectangular',
        logo_alignment: 'left'
      }
    )
  } catch (e) {
    console.warn("Erreur renderButton Google:", e)
  }
}

const handleGoogleCredentialResponse = async (response) => {
  if (!response?.credential) return
  authLoading.value = true
  authError.value = ''
  try {
    const data = await apiGoogleLogin(response.credential)
    persistSession(data)
    await loadUserProfiles()
  } catch (err) {
    console.error("Erreur d'authentification Google SSO", err)
    authError.value = `Google SSO : ${err.message}`
  } finally {
    authLoading.value = false
  }
}

watch(authModalOpen, (newVal) => {
  if (newVal) {
    nextTick(() => {
      initGoogleSignIn()
      setTimeout(() => {
        renderGoogleButton()
      }, 50)
    })
  }
})

const handleLogout = () => {
  localStorage.removeItem('saas_token')
  localStorage.removeItem('saas_user')
  currentUser.value = null
  userProfiles.value = []
  activeUserProfile.value = null
}

const openLoginModal = () => {
  authMode.value = 'login'
  authError.value = ''
  authModalOpen.value = true
}

const handleLoadSimulation = (simData) => {
  simulationToLoad.value = { ...simData, _loadTimestamp: Date.now() }
  activeTab.value = 'direct-sim'
}

onMounted(() => {
  applyTheme(theme.value)
  checkCurrentUser()

  // Chargement propre du script Google Identity Services
  if (!document.getElementById('google-gsi-script')) {
    const script = document.createElement('script')
    script.id = 'google-gsi-script'
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.defer = true
    script.onload = () => {
      try {
        initGoogleSignIn()
        if (authModalOpen.value) {
          renderGoogleButton()
        }
      } catch (e) {
        console.warn('Google SSO non disponible:', e.message)
      }
    }
    script.onerror = () => {
      console.warn('Google SSO : impossible de charger le script GSI')
    }
    document.head.appendChild(script)
  } else {
    initGoogleSignIn()
  }
})
</script>

<template>
  <div class="app-layout">
    
    <!-- Sidebar de Navigation Gauche (Apple Style Minimal) -->
    <aside class="app-sidebar">
      
      <!-- Brand Header -->
      <div class="sidebar-header">
        <div class="brand-badge flex-center">
          <Zap size="18" class="text-teal" />
        </div>
        <div class="brand-text">
          <span class="brand-name">EcoSwitch</span>
          <span class="brand-tagline">Optimisation Automobile</span>
        </div>
      </div>

      <!-- Navigation Links -->
      <nav class="sidebar-nav">
        <button
          class="nav-link"
          :class="{ active: activeTab === 'direct-sim' }"
          @click="activeTab = 'direct-sim'"
        >
          <Zap size="17" />
          <span>Simulateur Express</span>
        </button>

        <button
          class="nav-link"
          :class="{ active: activeTab === 'comparator' }"
          @click="activeTab = 'comparator'"
        >
          <BarChart3 size="17" />
          <span>Comparateur Flotte</span>
        </button>

        <button
          class="nav-link"
          :class="{ active: activeTab === 'vehicles' }"
          @click="activeTab = 'vehicles'"
        >
          <Car size="17" />
          <span>Catalogue Véhicules</span>
        </button>

        <button
          v-if="currentUser"
          class="nav-link"
          :class="{ active: activeTab === 'saved-sims' }"
          @click="activeTab = 'saved-sims'"
        >
          <Bookmark size="17" />
          <span>Mes Simulations</span>
        </button>

        <button
          v-if="currentUser"
          class="nav-link"
          @click="profileModalOpen = true"
        >
          <Settings size="17" />
          <span>Mon Garage & Profil</span>
        </button>
      </nav>

      <!-- Sidebar Footer (Theme, Admin, Auth) -->
      <div class="sidebar-footer">
        <!-- Energy Ticker Mini -->
        <div class="energy-mini-card mb-3 p-2.5 rounded-xl border-glass bg-card-subtle">
          <div class="flex-between items-center mb-1.5">
            <span class="text-xxs uppercase font-bold text-dimmed flex items-center gap-1">
              <Fuel size="11" class="text-teal" /> Énergies (FR)
            </span>
            <span class="badge badge-teal badge-small">2026</span>
          </div>
          <div class="flex-between text-xxs py-0.5">
            <span class="text-muted">SP95-E10</span>
            <span class="font-mono font-bold text-main">1,88 €/L</span>
          </div>
          <div class="flex-between text-xxs py-0.5">
            <span class="text-muted">Gazole B7</span>
            <span class="font-mono font-bold text-main">1,74 €/L</span>
          </div>
          <div class="flex-between text-xxs py-0.5">
            <span class="text-muted">Électricité</span>
            <span class="font-mono font-bold text-teal">0,25 €/kWh</span>
          </div>
        </div>

        <!-- Theme Toggle & Admin -->
        <div class="flex-between items-center mb-3">
          <a
            href="http://localhost:8080/admin"
            target="_blank"
            class="btn-admin-link flex items-center gap-1.5 text-xxs text-muted"
          >
            <ShieldCheck size="13" />
            <span>Console Admin</span>
            <ExternalLink size="10" />
          </a>

          <button
            class="btn-theme-toggle flex-center"
            @click="toggleTheme"
            :title="theme === 'dark' ? 'Passer en thème clair' : 'Passer en thème sombre'"
          >
            <Sun v-if="theme === 'dark'" size="14" class="text-amber" />
            <Moon v-else size="14" class="text-dimmed" />
          </button>
        </div>

        <!-- User Capsule -->
        <div v-if="currentUser" class="user-capsule p-2 rounded-xl border-glass flex-between">
          <div class="flex items-center gap-2 truncate">
            <div class="user-avatar flex-center">
              {{ currentUser.email.charAt(0).toUpperCase() }}
            </div>
            <div class="truncate">
              <div class="user-email truncate text-xs font-bold">{{ currentUser.email }}</div>
              <div class="text-xxs text-teal font-semibold">Compte Vérifié</div>
            </div>
          </div>
          <button class="btn-logout flex-center" @click="handleLogout" title="Se déconnecter">
            <LogOut size="14" />
          </button>
        </div>

        <button
          v-else
          class="btn btn-primary w-100 text-xs font-bold flex items-center justify-center gap-2"
          @click="openLoginModal"
        >
          <User size="14" />
          <span>Espace Client</span>
        </button>
      </div>
    </aside>

    <!-- Canvas Principal Central (Largeur Maîtrisée, Aérée et Responsive) -->
    <main class="app-main">
      <div class="main-container">
        
        <!-- Simulateur Direct & Express -->
        <DirectSimulator
          v-if="activeTab === 'direct-sim'"
          :currentUser="currentUser"
          :userProfile="activeUserProfile"
          :loadedSimulation="simulationToLoad"
          @open-garage="profileModalOpen = true"
          @open-auth="openLoginModal"
        />

        <!-- Comparateur de Catalogue -->
        <CatalogComparator
          v-else-if="activeTab === 'comparator'"
          :currentUser="currentUser"
          :userProfiles="userProfiles"
          :activeUserProfile="activeUserProfile"
        />

        <!-- Catalogue Véhicules -->
        <VehicleManager
          v-else-if="activeTab === 'vehicles'"
          :currentUser="currentUser"
          :userProfile="activeUserProfile"
          @open-simulator="activeTab = 'direct-sim'"
        />

        <!-- Simulations Sauvegardées -->
        <SavedSimulations
          v-else-if="activeTab === 'saved-sims' && currentUser"
          :currentUser="currentUser"
          @load-simulation="handleLoadSimulation"
        />

      </div>
    </main>

    <!-- Modal Mon Garage / Profils -->
    <UserProfileModal
      :show="profileModalOpen"
      :profiles="userProfiles"
      @close="profileModalOpen = false"
      @profiles-updated="loadUserProfiles"
    />

    <!-- Modal Authentification Apple Style -->
    <div v-if="authModalOpen" class="auth-modal-overlay flex-center">
      <div class="card-glass auth-modal-card p-5 relative max-w-sm w-100 animation-fadeIn">
        <button class="icon-btn-close absolute top-4 right-4" @click="authModalOpen = false">
          ✕
        </button>

        <div class="text-center mb-4">
          <div class="auth-icon-badge mx-auto mb-2 flex-center">
            <User size="20" class="text-teal" />
          </div>
          <h3 class="text-main font-heading text-lg font-bold m-0">
            {{ authMode === 'login' ? 'Connexion Espace Client' : 'Création de Compte' }}
          </h3>
          <p class="text-xs text-muted mt-1 m-0">
            {{ authMode === 'login' ? 'Accédez à votre garage et simulations enregistrées' : 'Enregistrez vos véhicules et comparez en 1 clic' }}
          </p>
        </div>

        <div v-if="authError" class="p-2.5 rounded-xl border-glass bg-card text-rose text-xs mb-3 text-center">
          {{ authError }}
        </div>

        <!-- Google SSO Action (Bouton Officiel Google Identity Services) -->
        <div class="google-auth-section mb-2">
          <div id="google-signin-btn" class="flex-center w-100" style="min-height: 44px;"></div>
        </div>

        <div class="auth-divider flex-center my-3">
          <span class="divider-line"></span>
          <span class="divider-text text-xxs text-dimmed uppercase px-2">ou avec adresse email</span>
          <span class="divider-line"></span>
        </div>

        <form @submit.prevent="authMode === 'login' ? handleLogin() : handleRegister()">
          <div class="form-group mb-3">
            <label class="form-label text-xxs">Adresse Email</label>
            <input v-model="authEmail" type="email" class="form-control text-xs" placeholder="nom@exemple.com" required />
          </div>

          <div class="form-group mb-4">
            <label class="form-label text-xxs">Mot de passe</label>
            <input v-model="authPassword" type="password" class="form-control text-xs" placeholder="••••••••" required />
          </div>

          <button type="submit" class="btn btn-primary w-100 py-2.5 text-xs font-bold mb-3" :disabled="authLoading">
            <span v-if="authLoading" class="spinner mr-2"></span>
            <span>{{ authMode === 'login' ? 'Se connecter' : 'Créer mon compte' }}</span>
          </button>
        </form>

        <div class="text-center pt-3 border-t border-glass text-xs">
          <button
            type="button"
            class="btn-text-toggle text-dimmed hover:text-main cursor-pointer"
            @click="authMode = authMode === 'login' ? 'register' : 'login'; authError = ''"
          >
            {{ authMode === 'login' ? "Pas encore de compte ? S'inscrire" : 'Déjà un compte ? Se connecter' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background-color: var(--bg-app);
}

/* Sidebar */
.app-sidebar {
  width: 250px;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-glass);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20px 16px;
  position: sticky;
  top: 0;
  height: 100vh;
  flex-shrink: 0;
  z-index: 100;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-subtle);
}

.brand-badge {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--accent-teal-soft);
  border: 1px solid rgba(16, 124, 65, 0.15);
}

.brand-name {
  display: block;
  font-size: 1.05rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text-main);
  line-height: 1.1;
}

.brand-tagline {
  font-size: 0.65rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-dimmed);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 16px;
  flex: 1;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  background: transparent;
  border: none;
  font-family: var(--font-sans);
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  width: 100%;
}

.nav-link:hover {
  background: var(--bg-card-subtle);
  color: var(--text-main);
}

.nav-link.active {
  background: var(--accent-teal-soft);
  color: var(--accent-teal);
  font-weight: 700;
}

.btn-admin-link {
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s ease;
}
.btn-admin-link:hover {
  background: var(--bg-card-subtle);
  color: var(--text-main);
}

.btn-theme-toggle {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--bg-card-subtle);
  border: 1px solid var(--border-glass);
  cursor: pointer;
}

.user-capsule {
  background: var(--bg-card-subtle);
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent-teal);
  color: #FFFFFF;
  font-size: 0.75rem;
  font-weight: 800;
}

.btn-logout {
  background: transparent;
  border: none;
  color: var(--text-dimmed);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
}
.btn-logout:hover {
  color: var(--accent-rose);
}

/* Canvas Principal */
.app-main {
  flex: 1;
  min-width: 0;
  padding: 32px 40px;
  overflow-y: auto;
}

.main-container {
  max-width: 1080px;
  margin: 0 auto;
}

.auth-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.auth-modal-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  position: relative;
  box-sizing: border-box;
}

.icon-btn-close {
  background: transparent;
  border: none;
  color: var(--text-dimmed);
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.15s ease;
}
.icon-btn-close:hover {
  color: var(--text-main);
  background: var(--bg-card-subtle);
}

.auth-icon-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--accent-teal-soft);
  border: 1px solid rgba(16, 124, 65, 0.2);
}

.btn-google-sso {
  background: var(--bg-card);
  border: 1px solid var(--border-glass);
  border-radius: var(--radius-sm);
  color: var(--text-main);
  transition: all 0.15s ease;
  box-shadow: var(--shadow-sm);
}
.btn-google-sso:hover {
  background: var(--bg-card-subtle);
  border-color: var(--border-hover);
  transform: translateY(-1px);
}

.auth-divider {
  display: flex;
  align-items: center;
  margin: 1rem 0;
}
.divider-line {
  flex: 1;
  height: 1px;
  background: var(--border-glass);
}
.divider-text {
  font-weight: 700;
  letter-spacing: 0.04em;
}

.btn-text-toggle {
  background: transparent;
  border: none;
  font-family: var(--font-sans);
}

@media (max-width: 900px) {
  .app-layout {
    flex-direction: column;
  }
  .app-sidebar {
    width: 100%;
    height: auto;
    position: relative;
    border-right: none;
    border-bottom: 1px solid var(--border-glass);
  }
  .app-main {
    padding: 20px 16px;
  }
}
</style>
