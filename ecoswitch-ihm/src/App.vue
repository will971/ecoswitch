<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import { Car, BarChart3, Settings, HelpCircle, Activity, ExternalLink, ShieldCheck, User, LogOut, Lock, Mail, Key, CreditCard, Sparkles, Check, X, Sun, Moon } from '@lucide/vue'
import DirectSimulator from './components/DirectSimulator.vue'
import VehicleManager from './components/VehicleManager.vue'
import CatalogComparator from './components/CatalogComparator.vue'
import SavedSimulations from './components/SavedSimulations.vue'
import { apiRegister, apiLogin, apiGoogleLogin } from './utils/api.js'
import { useTheme } from './utils/theme.js'

const activeTab = ref('direct') // direct, compare, catalog, saved, pricing
const currentUser = ref(null)

const { currentTheme, initTheme, toggleTheme } = useTheme()

// Modale Auth
const showAuthModal = ref(false)
const isRegister = ref(false)
const authEmail = ref('williams@saas.com')
const authPassword = ref('password')
const authName = ref('Williams Modeste')

// Simulation chargée depuis l'historique
const loadedSimulation = ref(null)

const setTab = (tab) => {
  activeTab.value = tab
}

const authError = ref('')
const authLoading = ref(false)

/** Restaure la session depuis le localStorage si le token JWT est encore présent */
const checkSession = () => {
  const user  = localStorage.getItem('saas_user')
  const token = localStorage.getItem('saas_token')
  if (user && token) {
    currentUser.value = JSON.parse(user)
  }
}

/** Persiste la session après auth réussie */
const persistSession = (userData) => {
  const user = { name: userData.name, email: userData.email, plan: userData.plan }
  localStorage.setItem('saas_token', userData.token)
  localStorage.setItem('saas_user', JSON.stringify(user))
  currentUser.value = user
  showAuthModal.value = false
  authError.value = ''
  if (activeTab.value === 'saved') activeTab.value = 'direct'
}

/** Déconnexion forcée (expiration de session) */
const forceLogout = () => {
  localStorage.removeItem('saas_user')
  localStorage.removeItem('saas_token')
  currentUser.value = null
  if (activeTab.value === 'saved') activeTab.value = 'direct'
  showAuthModal.value = true
  authError.value = 'Votre session a expiré. Veuillez vous reconnecter.'
}

const openAuth = () => {
  showAuthModal.value = true
  authError.value = ''
}

const closeAuth = () => {
  showAuthModal.value = false
  authError.value = ''
}

const handleAuth = async () => {
  authLoading.value = true
  authError.value = ''
  try {
    let userData
    if (isRegister.value) {
      userData = await apiRegister(
        authEmail.value.trim().toLowerCase(),
        authPassword.value,
        authName.value || 'Utilisateur'
      )
    } else {
      userData = await apiLogin(
        authEmail.value.trim().toLowerCase(),
        authPassword.value
      )
    }
    persistSession(userData)
  } catch (err) {
    authError.value = err.message
  } finally {
    authLoading.value = false
  }
}

const initGoogleSignIn = () => {
  if (typeof google === 'undefined') return
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || '1047781504975-placeholderclientid.apps.googleusercontent.com'
  google.accounts.id.initialize({
    client_id: clientId,
    callback: handleGoogleCredentialResponse,
    auto_select: false,
    cancel_on_tap_outside: true
  })
}

const renderGoogleButton = () => {
  if (typeof google === 'undefined') return
  const btnEl = document.getElementById('google-signin-btn')
  if (!btnEl) return
  google.accounts.id.renderButton(
    btnEl,
    { 
      theme: 'dark', 
      size: 'large', 
      width: 320,
      text: 'continue_with',
      shape: 'rectangular'
    }
  )
}

const handleGoogleCredentialResponse = async (response) => {
  authLoading.value = true
  authError.value = ''
  try {
    const userData = await apiGoogleLogin(response.credential)
    persistSession(userData)
  } catch (err) {
    console.error("Erreur d'authentification Google SSO", err)
    authError.value = `Google SSO : ${err.message}`
  } finally {
    authLoading.value = false
  }
}

watch(showAuthModal, (newVal) => {
  if (newVal) {
    nextTick(() => {
      renderGoogleButton()
    })
  }
})

const logout = () => {
  localStorage.removeItem('saas_user')
  localStorage.removeItem('saas_token')
  currentUser.value = null
  if (activeTab.value === 'saved') {
    activeTab.value = 'direct'
  }
}

const handleLoadSimulation = (sim) => {
  loadedSimulation.value = sim
  activeTab.value = 'direct'
}

onMounted(() => {
  initTheme()
  checkSession()

  // Google Identity Services — certaines versions du parser Safari (JavaScriptCore)
  // génèrent une SyntaxError interne au script GSI (via eval/new Function côté Google).
  // On charge le script avec onerror pour éviter une erreur non interceptée dans la console.
  const script = document.createElement('script')
  script.src = 'https://accounts.google.com/gsi/client'
  script.async = true
  script.defer = true
  script.onload = () => {
    try {
      initGoogleSignIn()
    } catch (e) {
      // Google SSO non disponible dans ce navigateur — l'auth email reste fonctionnelle
      console.warn('Google SSO non disponible:', e.message)
    }
  }
  script.onerror = () => {
    console.warn('Google SSO : impossible de charger le script (réseau ou navigateur incompatible)')
  }
  document.head.appendChild(script)
})
</script>

<template>
  <div class="app-root flex flex-column min-h-screen">
    <!-- Navbar Premium -->
    <header class="navbar-glass py-3 px-4 flex-between sticky-top">
      <div class="brand flex-center gap-2">
        <div class="logo-box flex-center">
          <Activity class="text-teal spinner-slow" size="22" />
        </div>
        <div>
          <h1 class="brand-title text-gradient">EcoSwitch</h1>
          <p class="brand-subtitle">Simulateur & Calculateur de Rentabilité</p>
        </div>
      </div>

      <!-- Navigation Onglets -->
      <nav class="flex gap-2">
        <button class="nav-btn" :class="activeTab === 'direct' ? 'active' : ''" @click="setTab('direct')">
          <HelpCircle size="16" /> Simulateur direct
        </button>
        <button class="nav-btn" :class="activeTab === 'compare' ? 'active' : ''" @click="setTab('compare')">
          <BarChart3 size="16" /> Comparateur
        </button>
        <button class="nav-btn" :class="activeTab === 'catalog' ? 'active' : ''" @click="setTab('catalog')">
          <Car size="16" /> Catalogue H2
        </button>
        <button v-if="currentUser" class="nav-btn" :class="activeTab === 'saved' ? 'active' : ''" @click="setTab('saved')">
          <Sparkles size="16" class="text-cyan" /> Mes Simulations
        </button>
      </nav>

      <!-- Lien vers Admin H2/Monitoring + Espace User -->
      <div class="flex-center gap-3">
        <a href="http://localhost:8080/admin/index.html" target="_blank" class="admin-link flex-center gap-1 text-xs">
          <ShieldCheck size="16" class="text-cyan" />
          <span>Console H2</span>
          <ExternalLink size="12" />
        </a>

        <!-- Bouton bascule de thème -->
        <button class="icon-btn-nav flex-center theme-toggle-btn" @click="toggleTheme" :title="currentTheme === 'light' ? 'Activer le mode sombre' : 'Activer le mode clair'">
          <Sun v-if="currentTheme === 'light'" size="16" class="text-amber" />
          <Moon v-else size="16" class="text-cyan" />
        </button>

        <!-- Authentification Section -->
        <div class="border-l border-glass pl-3 flex-center">
          <div v-if="currentUser" class="user-session flex-center gap-2">
            <div class="user-avatar flex-center">{{ currentUser.name.charAt(0) }}</div>
            <div class="user-info-navbar">
              <div class="text-xs font-semibold">{{ currentUser.name }}</div>
              <div class="badge badge-teal text-xxs font-bold uppercase">{{ currentUser.plan }}</div>
            </div>
            <button class="icon-btn-nav hover-text-rose" @click="logout" title="Se déconnecter">
              <LogOut size="14" />
            </button>
          </div>
          <button v-else class="btn btn-secondary btn-small flex-center gap-1 glow-teal" @click="openAuth">
            <User size="14" />
            <span>Espace Client</span>
          </button>
        </div>
      </div>
    </header>

    <!-- Zone principale -->
    <main class="main-content-area flex-1 py-5 px-4 max-w-7xl mx-auto w-100">
      <Transition name="fade" mode="out-in">
        <div :key="activeTab">
          <DirectSimulator v-if="activeTab === 'direct'" :loadedSimulation="loadedSimulation" :currentUser="currentUser" />
          <CatalogComparator v-else-if="activeTab === 'compare'" />
          <VehicleManager v-else-if="activeTab === 'catalog'" />
          <SavedSimulations v-else-if="activeTab === 'saved' && currentUser" :currentUser="currentUser" @load-simulation="handleLoadSimulation" />
          
        </div>
      </Transition>
    </main>

    <!-- Footer Premium -->
    <footer class="footer-glass py-4 px-4 text-center text-xs text-dimmed mt-auto">
      <p>&copy; 2026 EcoSwitch. Tous droits réservés. Propulsé par Spring Boot 4 et Vue.js 3 (Vite).</p>
      <p class="mt-1">Interface SaaS Premium développée selon les standards esthétiques mode sombre.</p>
    </footer>

    <!-- Modale d'Authentification (SaaS Sign In/Up) -->
    <div v-if="showAuthModal" class="auth-modal-overlay flex-center">
      <div class="card-glass glow-teal auth-modal-card p-4 relative max-w-md w-100">
        <button class="absolute top-4 right-4 text-dimmed hover-text-main" @click="closeAuth">
          <X size="20" />
        </button>

        <h3 class="text-gradient mb-3">{{ isRegister ? 'Créer un compte client' : 'Espace Client Connexion' }}</h3>
        <p class="text-xs text-muted mb-4">
          {{ isRegister ? 'Inscrivez-vous pour débloquer la sauvegarde et l\'export CSV.' : 'Entrez vos identifiants pour accéder à vos simulations sauvegardées.' }}
        </p>

        <div class="auth-form mt-2">
          <div v-if="isRegister" class="form-group mb-3">
            <label class="form-label">Nom complet</label>
            <input v-model="authName" type="text" class="form-control" placeholder="Williams Modeste" required />
          </div>
          <div class="form-group mb-3">
            <label class="form-label">Adresse Email</label>
            <div class="input-with-icon">
              <input v-model="authEmail" type="email" class="form-control" placeholder="williams@saas.com" required />
            </div>
          </div>
          <div class="form-group mb-4">
            <label class="form-label">Mot de passe</label>
            <div class="input-with-icon">
              <input v-model="authPassword" type="password" class="form-control" placeholder="••••••••" required />
            </div>
          </div>

          <!-- Message d'erreur -->
          <div v-if="authError" class="auth-error-msg mb-2">{{ authError }}</div>

          <button class="btn btn-primary w-100 mb-2" @click="handleAuth" :disabled="authLoading">
            <span>{{ authLoading ? 'Connexion...' : (isRegister ? 'Créer mon compte Pro' : 'Se connecter au compte Pro') }}</span>
            <Lock size="16" />
          </button>


          <!-- Séparateur "OU" -->
          <div class="flex-center gap-3 my-3 text-xxs text-dimmed uppercase">
            <span class="border-b border-glass flex-1" style="height: 1px;"></span>
            <span>ou</span>
            <span class="border-b border-glass flex-1" style="height: 1px;"></span>
          </div>

          <!-- Véritable Bouton Google SSO (Identity Services) -->
          <div id="google-signin-btn" class="flex-center w-100 mb-3" style="min-height: 40px;"></div>

          <div class="text-center text-xs text-dimmed">
            <span v-if="isRegister">Déjà client ? </span>
            <span v-else>Pas encore de compte ? </span>
            <button class="btn-link text-cyan pointer-events-auto" @click="isRegister = !isRegister">
              {{ isRegister ? 'Se connecter' : 'Créer un compte' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Google Identity Services script manages the real OAuth popup window, no more mock modals -->
  </div>
</template>

<style>
/* App Root variables and resets */
.min-h-screen { min-height: 100vh; }
.flex { display: flex; }
.flex-column { flex-direction: column; }
.flex-1 { flex: 1; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.list-none { list-style: none; }
.items-center { align-items: center; }
.max-w-7xl { max-width: 80rem; }
.mx-auto { margin-left: auto; margin-right: auto; }
.w-100 { width: 100%; }
.mt-auto { margin-top: auto; }
.mt-4 { margin-top: 1rem; }
.my-3 { margin-top: 0.75rem; margin-bottom: 0.75rem; }
.px-4 { padding-left: 1rem; padding-right: 1rem; }
.py-3 { padding-top: 0.75rem; padding-bottom: 0.75rem; }
.py-4 { padding-top: 1rem; padding-bottom: 1rem; }
.py-5 { padding-top: 2.5rem; padding-bottom: 2.5rem; }
.pl-3 { padding-left: 0.75rem; }
.relative { position: relative; }
.overflow-hidden { overflow: hidden; }

/* Sticky Navbar & Footer */
.navbar-glass {
  background: rgba(var(--bg-nav));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid hsl(var(--border-glass));
}
.footer-glass {
  background: rgba(var(--bg-footer));
  border-top: 1px solid hsl(var(--border-glass));
}
.logo-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(20, 184, 166, 0.1);
  border: 1px solid rgba(20, 184, 166, 0.3);
}
.brand-title {
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.2;
}
.brand-subtitle {
  font-size: 0.65rem;
  color: hsl(var(--text-muted));
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Navbar Buttons */
.nav-btn {
  background: transparent;
  border: none;
  outline: none;
  font-family: var(--font-heading);
  font-size: 0.88rem;
  font-weight: 500;
  color: hsl(var(--text-muted));
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
}
.nav-btn:hover {
  color: hsl(var(--text-main));
  background: rgba(var(--bg-hover));
}
.nav-btn.active {
  color: hsl(var(--accent-teal));
  background: rgba(20, 184, 166, 0.08);
  border: 1px solid rgba(20, 184, 166, 0.2);
}
.admin-link {
  color: hsl(var(--text-muted));
  text-decoration: none;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(8, 145, 178, 0.06);
  border: 1px solid rgba(8, 145, 178, 0.2);
  transition: all 0.2s ease;
}
.admin-link:hover {
  color: hsl(var(--text-main));
  background: rgba(8, 145, 178, 0.12);
  border-color: hsl(var(--accent-cyan) / 0.4);
}

/* User avatar and profile section */
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, hsl(var(--accent-teal)) 0%, hsl(var(--accent-cyan)) 100%);
  color: hsl(var(--bg-deep));
  font-family: var(--font-heading);
  font-weight: bold;
  font-size: 0.9rem;
}
.user-info-navbar {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}
.icon-btn-nav {
  background: transparent;
  border: none;
  color: hsl(var(--text-muted));
  cursor: pointer;
  padding: 4px;
  transition: color 0.2s ease;
}
.theme-toggle-btn {
  border-radius: 8px;
  padding: 8px;
  background: rgba(var(--bg-hover));
  border: 1px solid hsl(var(--border-glass));
  color: hsl(var(--text-muted));
  transition: all 0.2s ease;
}
.theme-toggle-btn:hover {
  color: hsl(var(--text-main));
  background: hsl(var(--border-glass) / 0.4);
  border-color: hsl(var(--text-muted) / 0.3);
}
.hover-text-rose:hover {
  color: hsl(var(--accent-rose));
}
.border-l { border-left: 1px solid; }

/* Pricing SaaS Grid */
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  max-width: 60rem;
  margin: 0 auto;
}
@media (max-width: 900px) {
  .pricing-grid {
    grid-template-columns: 1fr;
    max-width: 25rem;
  }
}
.border-teal-active {
  border-color: hsl(var(--accent-teal) / 0.5) !important;
}
.popular-tag {
  background: hsl(var(--accent-teal));
  color: hsl(var(--bg-deep));
}
.text-teal-muted {
  color: hsl(var(--accent-teal) / 0.7);
}
.text-dimmed { color: hsl(var(--text-dimmed)); }
.text-xs { font-size: 0.75rem; }
.text-xxs { font-size: 0.65rem; }
.text-3xl { font-size: 2rem; font-weight: 700; }
.text-lg { font-size: 1.15rem; }
.font-bold { font-weight: 700; }
.uppercase { text-transform: uppercase; }

/* Auth Modale Overlay */
.auth-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(var(--bg-overlay));
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 1000;
}
.auth-modal-card {
  z-index: 1001;
  background: hsl(var(--bg-auth-card));
}
.hover-text-main:hover {
  color: hsl(var(--text-main));
}
.btn-link {
  background: transparent;
  border: none;
  font-weight: 500;
  cursor: pointer;
  text-decoration: underline;
}
.btn-link:hover {
  color: hsl(var(--text-main));
}
.pointer-events-auto { pointer-events: auto; }
.absolute { position: absolute; }
.top-4 { top: 1rem; }
.right-4 { right: 1rem; }
.max-w-md { max-width: 28rem; }

/* Slow spinner micro-animation */
@keyframes spin-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.spinner-slow {
  animation: spin-slow 8s linear infinite;
}

/* Page transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Message d'erreur dans la modale auth */
.auth-error-msg {
  background: rgba(225, 29, 72, 0.12);
  border: 1px solid rgba(225, 29, 72, 0.35);
  border-radius: 8px;
  color: hsl(355, 80%, 72%);
  font-size: 0.8rem;
  padding: 8px 12px;
  text-align: center;
}
</style>

