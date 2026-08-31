/**
 * Helper API — EcoSwitch
 * Centralise les appels fetch avec gestion automatique du JWT.
 * Chaque requête vers /api/v1/* inclut automatiquement le header Authorization: Bearer <token>.
 */

function getApiBaseUrl() {
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    if (import.meta.env.VITE_API_URL) return `${import.meta.env.VITE_API_URL.replace(/\/$/, '')}/api/v1`
    if (import.meta.env.VITE_API_BASE_URL) return `${import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')}/api/v1`
  }
  if (typeof window !== 'undefined' && window.location) {
    const host = window.location.hostname || ''
    if (host.includes('railway.app') && !host.includes('api')) {
      return 'https://ecoswitch-api.up.railway.app/api/v1'
    }
  }
  return '/api/v1'
}

const BASE_URL = getApiBaseUrl()

/**
 * Normalise les chemins relatifs d'images (/uploads/...) vers une URL directe absolue en production.
 */
export function formatImageUrl(url) {
  if (!url || typeof url !== 'string') return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    return url
  }
  const cleanUrl = url.startsWith('/') ? url : `/${url}`
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    if (import.meta.env.VITE_API_URL) return `${import.meta.env.VITE_API_URL.replace(/\/$/, '')}${cleanUrl}`
    if (import.meta.env.VITE_API_BASE_URL) return `${import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')}${cleanUrl}`
  }
  if (typeof window !== 'undefined' && window.location) {
    const host = window.location.hostname || ''
    if (host.includes('railway.app') && !host.includes('api')) {
      return `https://ecoswitch-api.up.railway.app${cleanUrl}`
    }
  }
  return cleanUrl
}

/** Récupère le token JWT stocké après connexion */
function getToken() {
  try {
    const t = localStorage.getItem('saas_token')
    if (!t || t === 'undefined' || t === 'null' || typeof t !== 'string') {
      return null
    }
    const clean = t.trim()
    if (!clean || clean.length < 10 || clean.includes('\n') || clean.includes('\r')) {
      return null
    }
    return clean
  } catch (e) {
    return null
  }
}

/**
 * Fetch avec JWT automatique et gestion de timeout.
 * @param {string} path       - chemin relatif ex: '/simulations'
 * @param {RequestInit} options - options fetch standard
 * @returns {Promise<Response>}
 */
async function apiFetch(path, options = {}) {
  const isPublicAuthRoute = path.startsWith('/auth/login') ||
                            path.startsWith('/auth/register') ||
                            path.startsWith('/auth/google-login')

  const token = isPublicAuthRoute ? null : getToken()

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {})
  }

  const timeoutMs = options.timeout || 30000
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers,
      signal: options.signal || controller.signal
    })
    return response
  } catch (err) {
    if (err.name === 'AbortError') {
      throw new Error("Le serveur met trop de temps à répondre (Délai d'attente dépassé).")
    }
    throw err
  } finally {
    clearTimeout(timeoutId)
  }
}

// ── Auth ──────────────────────────────────────────────────────────────────

export async function apiRegister(email, password, name) {
  const res = await apiFetch('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password, name })
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de l'inscription.")
  return data // { token, name, email, plan }
}

export async function apiLogin(email, password) {
  const res = await apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || 'Email ou mot de passe incorrect.')
  return data // { token, name, email, plan }
}

export async function apiGoogleLogin(credential) {
  const res = await apiFetch('/auth/google-login', {
    method: 'POST',
    body: JSON.stringify({ credential })
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Échec de la validation Google.")
  return data // { token, name, email, plan }
}

export async function apiGetMe() {
  const res = await apiFetch('/auth/me')
  if (res.status === 401) throw new Error('SESSION_EXPIRED')
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur récupération profil.")
  return data
}

// ── Simulations ───────────────────────────────────────────────────────────

export async function apiGetSimulations() {
  const res = await apiFetch('/simulations')
  if (res.status === 401) throw new Error('SESSION_EXPIRED')
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || 'Impossible de charger les simulations.')
  return data // SimulationResponse[]
}

export async function apiSaveSimulation(name, simulationData) {
  const res = await apiFetch('/simulations', {
    method: 'POST',
    body: JSON.stringify({ name, simulationData: JSON.stringify(simulationData) })
  })
  if (res.status === 401) throw new Error('SESSION_EXPIRED')
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || 'Impossible de sauvegarder la simulation.')
  return data // SimulationResponse
}

export async function apiDeleteSimulation(id) {
  const res = await apiFetch(`/simulations/${id}`, { method: 'DELETE' })
  if (res.status === 401) throw new Error('SESSION_EXPIRED')
  if (res.status === 404) throw new Error('Simulation introuvable.')
  if (!res.ok) throw new Error('Impossible de supprimer la simulation.')
}

// ── User Vehicle Profile (Garage) ──────────────────────────────────────────

export async function apiGetUserVehicleProfiles() {
  const res = await apiFetch('/users/me/vehicle-profiles')
  if (res.status === 401) throw new Error('SESSION_EXPIRED')
  if (res.status === 204) return [] // No profiles found
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || 'Impossible de charger vos profils véhicules.')
  return data || []
}

export async function apiCreateUserVehicleProfile(profileData) {
  const res = await apiFetch('/users/me/vehicle-profiles', {
    method: 'POST',
    body: JSON.stringify(profileData)
  })
  if (res.status === 401) throw new Error('SESSION_EXPIRED')
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || 'Impossible de créer le profil véhicule.')
  return data
}

export async function apiUpdateUserVehicleProfile(id, profileData) {
  const res = await apiFetch(`/users/me/vehicle-profiles/${id}`, {
    method: 'PUT',
    body: JSON.stringify(profileData)
  })
  if (res.status === 401) throw new Error('SESSION_EXPIRED')
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || 'Impossible de mettre à jour le profil véhicule.')
  return data
}

export async function apiDeleteUserVehicleProfile(id) {
  const res = await apiFetch(`/users/me/vehicle-profiles/${id}`, {
    method: 'DELETE'
  })
  if (res.status === 401) throw new Error('SESSION_EXPIRED')
  if (res.status === 404) throw new Error('Profil introuvable.')
  if (!res.ok) throw new Error('Impossible de supprimer le profil véhicule.')
}

// ── IA Advisor (Gemini) ───────────────────────────────────────────────────

export async function apiGetAiAdvisorSummary(simulationPayload) {
  const res = await apiFetch('/comparisons/ai-advisor', {
    method: 'POST',
    body: JSON.stringify(simulationPayload)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Impossible de générer l'analyse IA.")
  return data
}

// ── Catalog REST Services ──────────────────────────────────────────────────

export async function apiGetCatalogHierarchy() {
  const res = await apiFetch('/catalog/hierarchy')
  if (!res.ok) throw new Error("Impossible de charger l'arborescence du catalogue.")
  const data = await res.json()
  return (data || []).map(b => ({
    ...b,
    logoUrl: formatImageUrl(b.logoUrl),
    models: (b.models || []).map(m => ({
      ...m,
      imageUrl: formatImageUrl(m.imageUrl),
      finitions: (m.finitions || []).map(f => ({
        ...f,
        imageUrl: formatImageUrl(f.imageUrl)
      })),
      motorisations: (m.motorisations || []).map(mot => ({
        ...mot,
        availableFinitions: (mot.availableFinitions || []).map(af => ({
          ...af,
          finitionImageUrl: formatImageUrl(af.finitionImageUrl)
        }))
      }))
    }))
  }))
}

export async function apiGetCatalogBrands() {
  const res = await apiFetch('/catalog/brands')
  if (!res.ok) throw new Error("Impossible de charger les marques.")
  const data = await res.json()
  return (data || []).map(b => ({ ...b, logoUrl: formatImageUrl(b.logoUrl) }))
}

export async function apiCreateBrand(brand) {
  const res = await apiFetch('/catalog/brands', {
    method: 'POST',
    body: JSON.stringify(brand)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la création de la marque.")
  return data
}

export async function apiUpdateBrand(id, brand) {
  const res = await apiFetch(`/catalog/brands/${id}`, {
    method: 'PUT',
    body: JSON.stringify(brand)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la modification de la marque.")
  return data
}

export async function apiDeleteBrand(id) {
  const res = await apiFetch(`/catalog/brands/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error("Impossible de supprimer la marque.")
}

export async function apiGetCatalogModels(brandId = null) {
  const q = brandId ? `?brandId=${brandId}` : ''
  const res = await apiFetch(`/catalog/models${q}`)
  if (!res.ok) throw new Error("Impossible de charger les modèles.")
  const data = await res.json()
  return (data || []).map(m => ({
    ...m,
    imageUrl: formatImageUrl(m.imageUrl),
    brandLogoUrl: formatImageUrl(m.brandLogoUrl)
  }))
}

export async function apiCreateModel(brandId, model) {
  const res = await apiFetch(`/catalog/models?brandId=${brandId}`, {
    method: 'POST',
    body: JSON.stringify(model)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la création du modèle.")
  return data
}

export async function apiUpdateModel(id, model) {
  const res = await apiFetch(`/catalog/models/${id}`, {
    method: 'PUT',
    body: JSON.stringify(model)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la modification du modèle.")
  return data
}

export async function apiDeleteModel(id) {
  const res = await apiFetch(`/catalog/models/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error("Impossible de supprimer le modèle.")
}

export async function apiGetCatalogMotorisations(modelId = null) {
  const q = modelId ? `?modelId=${modelId}` : ''
  const res = await apiFetch(`/catalog/motorisations${q}`)
  if (!res.ok) throw new Error("Impossible de charger les motorisations.")
  return await res.json()
}

export async function apiCreateMotorisation(modelId, motorisation) {
  const res = await apiFetch(`/catalog/motorisations?modelId=${modelId}`, {
    method: 'POST',
    body: JSON.stringify(motorisation)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la création de la motorisation.")
  return data
}

export async function apiUpdateMotorisation(id, motorisation) {
  const res = await apiFetch(`/catalog/motorisations/${id}`, {
    method: 'PUT',
    body: JSON.stringify(motorisation)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la modification de la motorisation.")
  return data
}

export async function apiDeleteMotorisation(id) {
  const res = await apiFetch(`/catalog/motorisations/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error("Impossible de supprimer la motorisation.")
}

export async function apiGetCatalogFinitions(modelId = null) {
  const q = modelId ? `?modelId=${modelId}` : ''
  const res = await apiFetch(`/catalog/finitions${q}`)
  if (!res.ok) throw new Error("Impossible de charger les finitions.")
  const data = await res.json()
  return (data || []).map(f => ({ ...f, imageUrl: formatImageUrl(f.imageUrl) }))
}

export async function apiCreateFinition(modelId, finition) {
  const res = await apiFetch(`/catalog/finitions?modelId=${modelId}`, {
    method: 'POST',
    body: JSON.stringify(finition)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la création de la finition.")
  return data
}

export async function apiUpdateFinition(id, finition) {
  const res = await apiFetch(`/catalog/finitions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(finition)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la modification de la finition.")
  return data
}

export async function apiDeleteFinition(id) {
  const res = await apiFetch(`/catalog/finitions/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error("Impossible de supprimer la finition.")
}

export async function apiGetCatalogVariants(modelId = null, motorisationId = null, finitionId = null) {
  const params = new URLSearchParams()
  if (modelId) params.append('modelId', modelId)
  if (motorisationId) params.append('motorisationId', motorisationId)
  if (finitionId) params.append('finitionId', finitionId)
  const q = params.toString() ? `?${params.toString()}` : ''
  const res = await apiFetch(`/catalog/variants${q}`)
  if (!res.ok) throw new Error("Impossible de charger les tarifs des variantes.")
  const data = await res.json()
  return (data || []).map(v => ({
    ...v,
    brandLogoUrl: formatImageUrl(v.brandLogoUrl),
    imageUrl: formatImageUrl(v.imageUrl),
    finitionImageUrl: formatImageUrl(v.finitionImageUrl)
  }))
}

export async function apiCreateVariant(finitionId, motorisationId, variant) {
  const res = await apiFetch(`/catalog/variants?finitionId=${finitionId}&motorisationId=${motorisationId}`, {
    method: 'POST',
    body: JSON.stringify(variant)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de l'association tarifaire.")
  return data
}

export async function apiUpdateVariant(id, variant) {
  const res = await apiFetch(`/catalog/variants/${id}`, {
    method: 'PUT',
    body: JSON.stringify(variant)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors de la mise à jour tarifaire.")
  return data
}

export async function apiDeleteVariant(id) {
  const res = await apiFetch(`/catalog/variants/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error("Impossible de supprimer la variante.")
}

// ── Upload Image ───────────────────────────────────────────────────────────

export async function apiUploadImage(file, folder = 'general') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('folder', folder)

  const token = getToken()
  const headers = token ? { 'Authorization': `Bearer ${token}` } : {}

  const res = await fetch(`${BASE_URL}/uploads/image`, {
    method: 'POST',
    headers,
    body: formData
  })

  const data = await res.json()
  if (!res.ok) throw new Error(data.error || "Erreur lors du téléversement de l'image.")
  return data.url // e.g. "/uploads/brands/..."
}

export async function apiCompareCustomProfitability(payload) {
  const res = await apiFetch('/comparisons/profitability/custom', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || 'Erreur lors du calcul de rentabilité.')
  return data
}

// ── Prix Carburants Live (Open Data + IA) ──────────────────────────────────

export async function apiGetLiveFuelPrices() {
  const res = await apiFetch('/comparisons/fuel-prices/live')
  if (!res.ok) throw new Error('Impossible de récupérer les prix des carburants en direct.')
  return await res.json()
}
