<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import { Car, BarChart3, Settings, HelpCircle, Activity, ExternalLink, ShieldCheck, User, LogOut, Lock, Mail, Key, CreditCard, Sparkles, Check, X, Sun, Moon, Menu } from '@lucide/vue'
import DirectSimulator from './components/DirectSimulator.vue'
import VehicleManager from './components/VehicleManager.vue'
import CatalogComparator from './components/CatalogComparator.vue'
import SavedSimulations from './components/SavedSimulations.vue'
import UserProfileModal from './components/UserProfileModal.vue'
import { apiRegister, apiLogin, apiGoogleLogin, apiGetUserVehicleProfiles } from './utils/api.js'
import { useTheme } from './utils/theme.js'

const activeTab = ref('direct') // direct, compare, catalog, saved, pricing
const currentUser = ref(null)
const userProfiles = ref([]) // Liste des profils véhicules
const activeUserProfile = ref(null) // Profil actif
const showMobileMenu = ref(false)

const { currentTheme, initTheme, toggleTheme } = useTheme()

// Modale Auth
const showAuthModal = ref(false)
const isRegister = ref(false)
const authEmail = ref('williams@saas.com')
const authPassword = ref('password')
const authName = ref('Williams Modeste')

// Modale Profil
const showProfileModal = ref(false)

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
const persistSession = async (userData) => {
  const user = { name: userData.name, email: userData.email, plan: userData.plan, role: userData.role }
  localStorage.setItem('saas_token', userData.token)
  localStorage.setItem('saas_user', JSON.stringify(user))
  currentUser.value = user
  showAuthModal.value = false
  authError.value = ''
  
  // Charger les profils véhicules
  try {
    const profiles = await apiGetUserVehicleProfiles()
    userProfiles.value = profiles
    if (profiles.length > 0) {
      activeUserProfile.value = profiles.find(p => p.default) || profiles[profiles.length - 1]
    }
  } catch (e) {
    console.error("Erreur chargement profil véhicule", e)
  }

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

const handleProfileSaved = async () => {
  try {
    const profiles = await apiGetUserVehicleProfiles()
    userProfiles.value = profiles
    if (profiles.length > 0) {
      activeUserProfile.value = profiles.find(p => p.default) || profiles[profiles.length - 1]
    } else {
      activeUserProfile.value = null
    }
  } catch (e) {
    console.error("Erreur rechargement profils", e)
  }
}

onMounted(async () => {
  initTheme()
  checkSession()
  if (currentUser.value) {
    try {
      const profiles = await apiGetUserVehicleProfiles()
      userProfiles.value = profiles
      if (profiles.length > 0) {
        activeUserProfile.value = profiles.find(p => p.default) || profiles[profiles.length - 1]
      }
    } catch (e) {
      console.error("Erreur chargement profil au montage", e)
    }
  }

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
  <div class="app-shell min-h-screen">

    <!-- Décor de fond : icônes flottantes thématiques -->
    <div class="bg-decor" aria-hidden="true">

      <!-- === VOITURES (10) === -->
      <svg class="bg-icon bg-car" style="--x:3%;--y:8%;--s:5rem;--r:-12deg;--d:0s;--op:0.09" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:85%;--y:5%;--s:4rem;--r:8deg;--d:3s;--op:0.08" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:40%;--y:68%;--s:6rem;--r:5deg;--d:6s;--op:0.07" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:68%;--y:50%;--s:3.5rem;--r:-18deg;--d:9s;--op:0.08" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:18%;--y:82%;--s:4.5rem;--r:10deg;--d:1.5s;--op:0.085" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:52%;--y:20%;--s:3rem;--r:-5deg;--d:4s;--op:0.07" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:90%;--y:78%;--s:5.5rem;--r:15deg;--d:12s;--op:0.075" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:25%;--y:35%;--s:3.8rem;--r:-22deg;--d:7s;--op:0.065" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:75%;--y:88%;--s:4.2rem;--r:3deg;--d:10s;--op:0.08" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>
      <svg class="bg-icon bg-car" style="--x:47%;--y:44%;--s:2.8rem;--r:-8deg;--d:15s;--op:0.06" viewBox="0 0 24 24" fill="currentColor"><path d="M19 10l-1.5-3.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3c-.6 0-1 .4-1 1v4c0 .6.4 1 1 1h1.1c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3h.2c.4 1.7 2 3 3.9 3s3.5-1.3 3.9-3H21c.6 0 1-.4 1-1v-4c0-.6-.4-1-1-1h-2z"/><circle cx="8" cy="16" r="2"/><circle cx="16" cy="16" r="2"/></svg>

      <!-- === RECYCLAGE (8) === -->
      <svg class="bg-icon bg-recycle" style="--x:12%;--y:38%;--s:4.5rem;--r:0deg;--d:2s;--op:0.09" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.5 6H7l2.3 3.9-1.1 1.9L4 5.5 1 11l3.5.5L3 14h6.3L12 19l2.7-5H21l-1.5-3 3.5-.5-3-5.5-4.2 6.3-1.1-1.9L18.9 6H16.5L14 2h-2z"/></svg>
      <svg class="bg-icon bg-recycle" style="--x:78%;--y:28%;--s:3.5rem;--r:30deg;--d:7s;--op:0.085" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.5 6H7l2.3 3.9-1.1 1.9L4 5.5 1 11l3.5.5L3 14h6.3L12 19l2.7-5H21l-1.5-3 3.5-.5-3-5.5-4.2 6.3-1.1-1.9L18.9 6H16.5L14 2h-2z"/></svg>
      <svg class="bg-icon bg-recycle" style="--x:58%;--y:80%;--s:5rem;--r:-10deg;--d:4.5s;--op:0.08" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.5 6H7l2.3 3.9-1.1 1.9L4 5.5 1 11l3.5.5L3 14h6.3L12 19l2.7-5H21l-1.5-3 3.5-.5-3-5.5-4.2 6.3-1.1-1.9L18.9 6H16.5L14 2h-2z"/></svg>
      <svg class="bg-icon bg-recycle" style="--x:33%;--y:18%;--s:3rem;--r:45deg;--d:11s;--op:0.075" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.5 6H7l2.3 3.9-1.1 1.9L4 5.5 1 11l3.5.5L3 14h6.3L12 19l2.7-5H21l-1.5-3 3.5-.5-3-5.5-4.2 6.3-1.1-1.9L18.9 6H16.5L14 2h-2z"/></svg>
      <svg class="bg-icon bg-recycle" style="--x:62%;--y:12%;--s:4rem;--r:-20deg;--d:5s;--op:0.08" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.5 6H7l2.3 3.9-1.1 1.9L4 5.5 1 11l3.5.5L3 14h6.3L12 19l2.7-5H21l-1.5-3 3.5-.5-3-5.5-4.2 6.3-1.1-1.9L18.9 6H16.5L14 2h-2z"/></svg>
      <svg class="bg-icon bg-recycle" style="--x:2%;--y:60%;--s:5.5rem;--r:15deg;--d:8s;--op:0.07" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.5 6H7l2.3 3.9-1.1 1.9L4 5.5 1 11l3.5.5L3 14h6.3L12 19l2.7-5H21l-1.5-3 3.5-.5-3-5.5-4.2 6.3-1.1-1.9L18.9 6H16.5L14 2h-2z"/></svg>
      <svg class="bg-icon bg-recycle" style="--x:88%;--y:45%;--s:3.8rem;--r:-35deg;--d:13s;--op:0.085" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.5 6H7l2.3 3.9-1.1 1.9L4 5.5 1 11l3.5.5L3 14h6.3L12 19l2.7-5H21l-1.5-3 3.5-.5-3-5.5-4.2 6.3-1.1-1.9L18.9 6H16.5L14 2h-2z"/></svg>
      <svg class="bg-icon bg-recycle" style="--x:44%;--y:92%;--s:4.5rem;--r:25deg;--d:16s;--op:0.07" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.5 6H7l2.3 3.9-1.1 1.9L4 5.5 1 11l3.5.5L3 14h6.3L12 19l2.7-5H21l-1.5-3 3.5-.5-3-5.5-4.2 6.3-1.1-1.9L18.9 6H16.5L14 2h-2z"/></svg>

      <!-- === DEVISES (17) === -->
      <span class="bg-icon bg-currency" style="--x:91%;--y:60%;--s:5.5rem;--r:-5deg;--d:0.5s;--op:0.1">€</span>
      <span class="bg-icon bg-currency" style="--x:2%;--y:63%;--s:4.5rem;--r:12deg;--d:5s;--op:0.09">$</span>
      <span class="bg-icon bg-currency" style="--x:49%;--y:3%;--s:3.5rem;--r:-8deg;--d:8s;--op:0.085">€</span>
      <span class="bg-icon bg-currency" style="--x:27%;--y:52%;--s:5rem;--r:20deg;--d:3.5s;--op:0.08">$</span>
      <span class="bg-icon bg-currency" style="--x:69%;--y:16%;--s:4rem;--r:-18deg;--d:10s;--op:0.09">€</span>
      <span class="bg-icon bg-currency" style="--x:7%;--y:88%;--s:5rem;--r:5deg;--d:2.5s;--op:0.08">$</span>
      <span class="bg-icon bg-currency" style="--x:54%;--y:42%;--s:3rem;--r:-25deg;--d:13s;--op:0.075">€</span>
      <span class="bg-icon bg-currency" style="--x:81%;--y:90%;--s:4.5rem;--r:15deg;--d:7.5s;--op:0.085">$</span>
      <span class="bg-icon bg-currency" style="--x:38%;--y:5%;--s:3.5rem;--r:-12deg;--d:6s;--op:0.08">$</span>
      <span class="bg-icon bg-currency" style="--x:15%;--y:18%;--s:4rem;--r:22deg;--d:9s;--op:0.09">€</span>
      <span class="bg-icon bg-currency" style="--x:72%;--y:72%;--s:3.8rem;--r:-8deg;--d:14s;--op:0.08">$</span>
      <span class="bg-icon bg-currency" style="--x:30%;--y:95%;--s:4.5rem;--r:18deg;--d:1s;--op:0.075">€</span>
      <span class="bg-icon bg-currency" style="--x:94%;--y:25%;--s:3.5rem;--r:-15deg;--d:11s;--op:0.085">$</span>
      <span class="bg-icon bg-currency" style="--x:55%;--y:58%;--s:5rem;--r:30deg;--d:17s;--op:0.07">€</span>
      <span class="bg-icon bg-currency" style="--x:20%;--y:65%;--s:3rem;--r:-20deg;--d:4s;--op:0.08">$</span>
      <span class="bg-icon bg-currency" style="--x:83%;--y:12%;--s:4.8rem;--r:10deg;--d:18s;--op:0.075">€</span>
      <span class="bg-icon bg-currency" style="--x:10%;--y:45%;--s:3.5rem;--r:-30deg;--d:20s;--op:0.085">$</span>
    </div>

    <!-- Sidebar Gauche (Navigation) -->
    <aside class="sidebar-left">
      <div class="brand flex gap-2">
        <div class="logo-box flex-center">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M19 10L17.5 6.5C17.2 5.8 16.5 5.3 15.7 5.3H8.3C7.5 5.3 6.8 5.8 6.5 6.5L5 10H3C2.4 10 2 10.4 2 11V15C2 15.6 2.4 16 3 16H4.1C4.5 17.7 6.1 19 8 19C9.9 19 11.5 17.7 11.9 16H12.1C12.5 17.7 14.1 19 16 19C17.9 19 19.5 17.7 19.9 16H21C21.6 16 22 15.6 22 15V11C22 10.4 21.6 10 21 10H19Z" fill="url(#logo-grad-car)" />
            <path d="M12.5 5C14.5 5 18 6.5 19 8.5" stroke="#10b981" stroke-width="1.5" stroke-linecap="round" />
            <!-- Leaf tail representing transition to green -->
            <path d="M21 10C21.5 9 22.5 6.5 20.5 4.5C18.5 2.5 16 3.5 15 4C16.5 5 16.5 7.5 16.5 8.5C16.5 9.5 17 10 17 10H21Z" fill="#10b981" />
            <!-- Wheels -->
            <circle cx="8" cy="16" r="2.5" fill="#0f172a" stroke="#10b981" stroke-width="1.5" />
            <circle cx="16" cy="16" r="2.5" fill="#0f172a" stroke="#10b981" stroke-width="1.5" />
            <defs>
              <linearGradient id="logo-grad-car" x1="2" y1="5.3" x2="22" y2="19" gradientUnits="userSpaceOnUse">
                <stop stop-color="#22d3ee" />
                <stop offset="1" stop-color="#10b981" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="brand-title text-gradient">EcoSwitch</h1>
          <p class="brand-subtitle hide-on-mobile">Simulateur & Calculateur de Rentabilité</p>
        </div>
      </div>

      <!-- Navigation Onglets -->
      <nav class="sidebar-nav flex flex-column gap-2 mt-5">
        <button class="nav-btn justify-start" :class="activeTab === 'direct' ? 'active' : ''" @click="setTab('direct')">
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
        <button v-if="currentUser" class="nav-btn text-teal" @click="showProfileModal = true">
          <Settings size="16" /> Profil Véhicule
        </button>
      </nav>

      <!-- Lien vers Admin H2/Monitoring + Espace User -->
      <div class="sidebar-bottom mt-auto flex flex-column gap-3">
        <a href="http://localhost:8080/admin/index.html" target="_blank" class="admin-link flex-center gap-1 text-xs hide-on-mobile">
          <ShieldCheck size="16" class="text-cyan" />
          <span class="admin-text">Console H2</span>
          <ExternalLink size="12" />
        </a>

        <div class="flex-between w-100 theme-toggle-container theme-toggle-mobile">
          <!-- Bouton bascule de thème -->
          <button class="icon-btn-nav flex-center theme-toggle-btn" @click="toggleTheme" :title="currentTheme === 'light' ? 'Activer le mode sombre' : 'Activer le mode clair'" :aria-label="currentTheme === 'light' ? 'Activer le mode sombre' : 'Activer le mode clair'">
            <Sun v-if="currentTheme === 'light'" size="16" class="text-amber" />
            <Moon v-else size="16" class="text-cyan" />
          </button>
        </div>

        <!-- Authentification Section -->
        <div class="user-auth-section border-t border-glass pt-3 w-100">
          <div v-if="currentUser" class="user-session flex-between w-100 gap-2">
            <div class="flex gap-2 items-center">
              <div class="user-avatar flex-center">{{ currentUser.name.charAt(0) }}</div>
              <div class="user-info-navbar hide-on-mobile">
                <div class="text-xs font-semibold truncate" style="max-width: 120px;">{{ currentUser.name }}</div>
              </div>
            </div>
            <button class="icon-btn-nav hover-text-rose shrink-0" @click="logout" title="Se déconnecter" aria-label="Se déconnecter">
              <LogOut size="14" />
            </button>
          </div>
          <button v-else class="btn btn-secondary w-100 btn-small flex-center gap-1 glow-teal" @click="openAuth">
            <User size="14" />
            <span class="auth-text">Espace Client</span>
          </button>
        </div>
      </div>
    </aside>

    <!-- Zone principale centrale -->
    <main class="main-content-area py-5 px-4 w-100">
      <div class="max-w-7xl mx-auto w-100 h-100 flex flex-column">
        <div class="flex-1">
          <Transition name="fade" mode="out-in">
            <div :key="activeTab">
              <DirectSimulator v-if="activeTab === 'direct'" :loadedSimulation="loadedSimulation" :currentUser="currentUser" :userProfile="activeUserProfile" />
              <CatalogComparator v-else-if="activeTab === 'compare'" :currentUser="currentUser" :userProfiles="userProfiles" :activeUserProfile="activeUserProfile" />
              <VehicleManager v-else-if="activeTab === 'catalog'" :currentUser="currentUser" :userProfile="activeUserProfile" @open-simulator="setTab('direct')" />
              <SavedSimulations v-else-if="activeTab === 'saved' && currentUser" :currentUser="currentUser" @load-simulation="handleLoadSimulation" />
            </div>
          </Transition>
        </div>
        <!-- Footer Premium -->
        <footer class="footer-glass py-4 px-4 text-center text-xs text-dimmed mt-4 rounded-xl" style="border-radius: 12px;">
          <p>&copy; 2026 EcoSwitch. Tous droits réservés.</p>
        </footer>
      </div>
    </main>

    <!-- Sidebar Droite (Context & Value Add) -->
    <aside class="sidebar-right hide-on-mobile">
      <div class="card-glass glow-teal mb-4 p-4">
        <h3 class="text-sm font-bold flex items-center gap-2 mb-3 text-gradient-teal">
          <Sparkles size="16" /> Astuces EcoSwitch
        </h3>
        <ul class="text-xs text-muted list-none flex flex-column gap-3 m-0 p-0">
          <li class="flex gap-2">
            <Check size="14" class="text-teal mt-1 shrink-0" />
            <span style="line-height: 1.4;">Roulez souple : une conduite apaisée économise jusqu'à 20% d'énergie.</span>
          </li>
          <li class="flex gap-2">
            <Check size="14" class="text-teal mt-1 shrink-0" />
            <span style="line-height: 1.4;">La pression des pneus influe directement sur l'autonomie électrique.</span>
          </li>
          <li class="flex gap-2">
            <Check size="14" class="text-teal mt-1 shrink-0" />
            <span style="line-height: 1.4;">Utilisez le freinage régénératif au maximum.</span>
          </li>
        </ul>
      </div>

      <div v-if="currentUser" class="card-glass p-4 mt-auto">
        <h3 class="text-xs text-dimmed mb-2 uppercase font-bold">Votre Impact</h3>
        <div class="flex items-end gap-2 mb-1">
          <span class="text-2xl font-bold text-teal" style="line-height: 1;">12%</span>
          <span class="text-xs text-muted mb-1">d'économie</span>
        </div>
        <p class="text-xxs text-dimmed mt-1">Basé sur vos récentes simulations</p>
      </div>
    </aside>
  </div>

  <!-- Barre de Navigation Basse sur Mobile (Style Snapchat/Instagram) -->
  <nav class="mobile-bottom-nav">
    <button class="mobile-nav-btn" :class="activeTab === 'direct' ? 'active' : ''" @click="setTab('direct')" title="Simulateur direct" aria-label="Simulateur direct">
      <HelpCircle size="22" />
    </button>
    <button class="mobile-nav-btn" :class="activeTab === 'compare' ? 'active' : ''" @click="setTab('compare')" title="Comparateur" aria-label="Comparateur">
      <BarChart3 size="22" />
    </button>
    <button class="mobile-nav-btn" :class="activeTab === 'catalog' ? 'active' : ''" @click="setTab('catalog')" title="Catalogue H2" aria-label="Catalogue H2">
      <Car size="22" />
    </button>
    <template v-if="currentUser">
      <button class="mobile-nav-btn" @click="showProfileModal = true" title="Profil Véhicule" aria-label="Profil Véhicule">
        <Settings size="22" />
      </button>
      <button class="mobile-nav-btn" :class="activeTab === 'saved' ? 'active' : ''" @click="setTab('saved')" title="Mes Simulations" aria-label="Mes Simulations">
        <Sparkles size="22" />
      </button>
    </template>
  </nav>

  <div>
    <!-- Modale d'Authentification (SaaS Sign In/Up) -->
    <div v-if="showAuthModal" class="auth-modal-overlay flex-center">
      <div class="card-glass glow-teal auth-modal-card p-4 relative max-w-md w-100">
        <button class="absolute top-4 right-4 text-dimmed hover-text-main" @click="closeAuth" aria-label="Fermer la fenêtre">
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
            <span>{{ authLoading ? 'Connexion...' : (isRegister ? 'Créer mon compte' : 'Se connecter') }}</span>
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

    <!-- Modale Profil Utilisateur (Garage) -->
    <UserProfileModal
      :show="showProfileModal"
      :profiles="userProfiles"
      @close="showProfileModal = false"
      @profiles-updated="handleProfileSaved"
    />

    <!-- Google Identity Services script manages the real OAuth popup window, no more mock modals -->
  </div>
</template>

<style>
/* =============================================
   BACKGROUND DECORATIVE LAYER
   ============================================= */
.bg-decor {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.bg-icon {
  position: absolute;
  left: var(--x);
  top: var(--y);
  font-size: var(--s);
  width: var(--s);
  height: var(--s);
  transform: rotate(var(--r));
  opacity: var(--op);
  animation: bg-float 20s ease-in-out infinite;
  animation-delay: var(--d);
  user-select: none;
  line-height: 1;
}

/* Voitures : couleur teal (accent principal) */
.bg-car {
  color: hsl(var(--accent-teal));
}

/* Recyclage : couleur cyan */
.bg-recycle {
  color: hsl(var(--accent-cyan));
}

/* Devises : dégradé entre teal et cyan via couleur ambre pour contraste */
.bg-currency {
  color: hsl(var(--accent-teal));
  font-family: var(--font-heading);
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Alternance couleur sur les $ (pairs) */
.bg-currency:nth-child(odd) {
  color: hsl(var(--accent-cyan));
}

@keyframes bg-float {
  0%   { transform: rotate(var(--r)) translateY(0px);   opacity: var(--op); }
  30%  { opacity: calc(var(--op) * 1.5); }
  50%  { transform: rotate(var(--r)) translateY(-18px);  opacity: var(--op); }
  70%  { opacity: calc(var(--op) * 0.6); }
  100% { transform: rotate(var(--r)) translateY(0px);   opacity: var(--op); }
}

/* S'assure que le contenu applicatif est au-dessus du décor */
.app-shell > aside,
.app-shell > main,
.app-shell > nav {
  position: relative;
  z-index: 1;
}

.app-shell {
  display: grid;
  grid-template-columns: 240px 1fr 280px;
  height: 100vh;
  width: 100%;
  overflow: hidden;
}
@media (min-width: 1920px) {
  .app-shell {
    grid-template-columns: 260px 1fr 320px;
  }
}
@media (min-width: 2560px) {
  .app-shell {
    grid-template-columns: 280px 1fr 360px;
  }
}

.sidebar-left {
  background: rgba(var(--bg-nav));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-right: 1px solid hsl(var(--border-glass));
  display: flex;
  flex-direction: column;
  padding: 1.5rem 1rem;
  overflow-y: auto;
  z-index: 10;
}

.sidebar-left .nav-btn {
  justify-content: flex-start;
  width: 100%;
}

.sidebar-right {
  background: rgba(var(--bg-footer));
  border-left: 1px solid hsl(var(--border-glass));
  display: flex;
  flex-direction: column;
  padding: 1.5rem 1rem;
  overflow-y: auto;
  z-index: 10;
}

.main-content-area {
  overflow-y: auto;
  height: 100vh;
}

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
.max-w-7xl { 
  max-width: 80rem; 
  width: 100%;
}
@media (min-width: 1920px) {
  .max-w-7xl {
    max-width: 110rem;
  }
}
@media (min-width: 2560px) {
  .max-w-7xl {
    max-width: 140rem;
  }
}
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

/* Layout adjustments for smaller screens */
@media (max-width: 1200px) {
  .app-shell {
    grid-template-columns: 240px 1fr; /* Hide right sidebar */
  }
  .sidebar-right {
    display: none;
  }
}

@media (max-width: 1024px) {
  .app-shell {
    display: flex;
    flex-direction: column;
    height: auto;
    padding-bottom: 75px !important;
    overflow: visible;
  }
  .sidebar-left {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding: 0.75rem 1rem;
    border-right: none;
    border-bottom: 1px solid hsl(var(--border-glass));
    position: sticky;
    top: 0;
    width: 100%;
    z-index: 50;
  }
  .sidebar-left .sidebar-nav {
    display: none; /* Hidden on mobile, use bottom nav instead */
  }
  .sidebar-left .sidebar-bottom {
    flex-direction: row;
    margin-top: 0;
    gap: 0.5rem;
    align-items: center;
  }
  .sidebar-left .user-auth-section {
    border-top: none;
    padding-top: 0;
    width: auto !important;
  }
  .sidebar-left .theme-toggle-mobile {
    width: auto !important;
  }
  .sidebar-left .user-session {
    width: auto !important;
  }
  .sidebar-left .admin-text, .sidebar-left .auth-text {
    display: none;
  }
  .main-content-area {
    height: auto;
    overflow-y: visible;
  }
  
  .hide-on-mobile {
    display: none !important;
  }
  .hide-on-desktop {
    display: flex !important;
  }
}

.mobile-bottom-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 65px;
  background: hsl(var(--bg-deep) / 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid hsl(var(--border-glass));
  justify-content: space-around;
  align-items: center;
  z-index: 999;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.15);
}

@media (max-width: 1024px) {
  .mobile-bottom-nav {
    display: flex;
  }
}

.mobile-nav-btn {
  background: transparent;
  border: none;
  color: hsl(var(--text-muted));
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.mobile-nav-btn:active {
  transform: scale(0.9);
}

.mobile-nav-btn.active {
  color: hsl(var(--accent-teal));
  background: rgba(20, 184, 166, 0.08);
}
</style>

