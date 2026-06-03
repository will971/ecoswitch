/**
 * Helper API — EcoSwitch
 * Centralise les appels fetch avec gestion automatique du JWT.
 * Chaque requête vers /api/v1/* inclut automatiquement le header Authorization: Bearer <token>.
 */

const BASE_URL = '/api/v1'

/** Récupère le token JWT stocké après connexion */
function getToken() {
  return localStorage.getItem('saas_token')
}

/**
 * Fetch avec JWT automatique.
 * @param {string} path       - chemin relatif ex: '/simulations'
 * @param {RequestInit} options - options fetch standard
 * @returns {Promise<Response>}
 */
async function apiFetch(path, options = {}) {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {})
  }
  return fetch(`${BASE_URL}${path}`, { ...options, headers })
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
