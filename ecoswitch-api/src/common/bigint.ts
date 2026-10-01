/**
 * Prisma rend les colonnes `bigint` sous forme de BigInt JS, sur lesquels
 * JSON.stringify leve un TypeError. Tous les identifiants du domaine tiennent
 * tres largement dans Number.MAX_SAFE_INTEGER.
 *
 * Les mappers DTO convertissent explicitement ; ce shim n'est qu'un filet pour
 * les chemins qui auraient ete oublies.
 */
export function installBigIntJsonSupport(): void {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ;(BigInt.prototype as any).toJSON = function (this: bigint): number {
    return Number(this)
  }
}

/** Conversion explicite d'un identifiant Prisma vers un nombre JSON-serialisable. */
export function toNumber(value: bigint | number): number {
  return typeof value === 'bigint' ? Number(value) : value
}
