/**
 * Erreur de validation metier, rendue en 400 avec son message.
 *
 * Equivalent des IllegalArgumentException du Java, que le ComparisonController
 * convertissait en `400 {"error": message}`. Le service de calcul reste ainsi
 * independant de Nest tout en produisant la bonne reponse HTTP.
 */
export class DomainError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'DomainError'
  }
}
