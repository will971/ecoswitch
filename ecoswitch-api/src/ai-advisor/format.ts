/**
 * Equivalents des formats Java utilises dans les textes du conseiller.
 * Uniquement de l'affichage : aucun calcul ne transite par ici.
 */

/** String.format("%.Nf") en locale racine (point decimal). */
export const fixed = (v: number, digits: number) => v.toFixed(digits)

/** String.format("%,.0f", v).replace(',', ' ') : "15 000". */
export function grouped(v: number): string {
  const rounded = Number(v.toFixed(0))
  const sign = rounded < 0 ? '-' : ''
  return sign + String(Math.abs(rounded)).replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
}
