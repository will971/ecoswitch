export type Role = 'USER' | 'ADMIN'

/** Utilisateur attache a la requete par l'AuthGuard. */
export interface AuthUser {
  id: string
  email: string
  name: string
  plan: string
  role: Role
}

/** Forme renvoyee au front par register / login / google-login. */
export interface AuthResponse {
  token: string
  name: string
  email: string
  plan: string
  role: Role
}
