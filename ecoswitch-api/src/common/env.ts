/** Lecture centralisee de la configuration, avec echec explicite au demarrage. */

export function requireEnv(name: string): string {
  const value = process.env[name]
  if (value === undefined || value.trim() === '') {
    throw new Error(`Variable d'environnement obligatoire manquante : ${name}`)
  }
  return value
}

export function optionalEnv(name: string): string | undefined {
  const value = process.env[name]
  return value === undefined || value.trim() === '' ? undefined : value
}

/** Liste separee par des virgules, normalisee en minuscules. */
export function emailListEnv(name: string): string[] {
  return (optionalEnv(name) ?? '')
    .split(',')
    .map((e) => e.trim().toLowerCase())
    .filter((e) => e.length > 0)
}
