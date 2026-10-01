/**
 * Reproduit String.CASE_INSENSITIVE_ORDER de Java, unite de code par unite de
 * code : majuscule d'abord, puis minuscule si les majuscules different, sinon
 * departage par la longueur.
 *
 * NE PAS remplacer par localeCompare : celui-ci applique une collation
 * linguistique et reordonnerait les marques accentuees. L'ordre du catalogue
 * est contractuel — le front affiche la hierarchie telle qu'elle arrive.
 *
 * Verifie par catalog.sort.spec.ts contre les vecteurs extraits du Java.
 */
export function caseInsensitiveCompare(s1: string, s2: string): number {
  const n1 = s1.length
  const n2 = s2.length
  const n = Math.min(n1, n2)

  for (let i = 0; i < n; i++) {
    let c1 = s1.charCodeAt(i)
    let c2 = s2.charCodeAt(i)
    if (c1 !== c2) {
      c1 = toUpperCodeUnit(c1)
      c2 = toUpperCodeUnit(c2)
      if (c1 !== c2) {
        c1 = toLowerCodeUnit(c1)
        c2 = toLowerCodeUnit(c2)
        if (c1 !== c2) return c1 - c2
      }
    }
  }
  return n1 - n2
}

/**
 * Equivalent de Character.toUpperCase(char) : une unite de code en entree, une
 * en sortie. String.prototype.toUpperCase peut en produire plusieurs
 * ('ß' -> 'SS'), la ou Java laisse le caractere inchange faute de place.
 */
function toUpperCodeUnit(code: number): number {
  const upper = String.fromCharCode(code).toUpperCase()
  return upper.length === 1 ? upper.charCodeAt(0) : code
}

function toLowerCodeUnit(code: number): number {
  const lower = String.fromCharCode(code).toLowerCase()
  return lower.length === 1 ? lower.charCodeAt(0) : code
}

export function byNameCaseInsensitive<T extends { name: string }>(a: T, b: T): number {
  return caseInsensitiveCompare(a.name, b.name)
}
