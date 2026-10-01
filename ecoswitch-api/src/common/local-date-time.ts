/**
 * Reproduit LocalDateTime.toString() de Java pour une date a la milliseconde.
 *
 * `simulation.saved_at` est un `timestamp` SANS fuseau. Le Java y ecrivait
 * l'heure murale du serveur (UTC en conteneur) et la renvoyait sans `Z` :
 * "2026-09-30T14:23:11.123". Prisma lit ce timestamp comme de l'UTC, donc les
 * getters UTC restituent exactement la meme heure murale.
 *
 * Format : secondes omises si nulles ET sans fraction ; fraction en millis.
 */
export function toJavaLocalDateTimeString(d: Date): string {
  const p2 = (n: number) => String(n).padStart(2, '0')
  const base = `${d.getUTCFullYear()}-${p2(d.getUTCMonth() + 1)}-${p2(d.getUTCDate())}T${p2(d.getUTCHours())}:${p2(d.getUTCMinutes())}`
  const s = d.getUTCSeconds()
  const ms = d.getUTCMilliseconds()
  if (s === 0 && ms === 0) return base
  return ms === 0 ? `${base}:${p2(s)}` : `${base}:${p2(s)}.${String(ms).padStart(3, '0')}`
}
