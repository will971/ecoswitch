import { Logger } from '@nestjs/common'
import { optionalEnv } from './env'

const logger = new Logger('Gemini')
const ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/models'
const DEFAULT_MODEL = 'gemini-flash-lite-latest'

export interface GeminiResult {
  text: string
  modelVersion: string
}

/** Vrai si une cle exploitable est configuree (le Java ignorait les "YOUR_..."). */
export function geminiConfigured(): boolean {
  const key = optionalEnv('GEMINI_API_KEY')
  return key !== undefined && !key.startsWith('YOUR_')
}

/**
 * Appelle Gemini en essayant plusieurs modeles, et ne leve JAMAIS : un echec
 * rend null et l'appelant bascule sur son moteur deterministe.
 *
 * La cle passe en en-tete x-goog-api-key plutot qu'en query string (Java) :
 * elle ne fuit ainsi ni dans les journaux d'acces ni dans les traces d'erreur.
 */
export async function geminiGenerate(
  prompt: string,
  options: { models?: string[]; timeoutMs?: number } = {},
): Promise<GeminiResult | null> {
  const key = optionalEnv('GEMINI_API_KEY')
  if (key === undefined || key.startsWith('YOUR_')) return null

  const configured = optionalEnv('GEMINI_MODEL') ?? DEFAULT_MODEL
  const models = [...new Set([configured, ...(options.models ?? [DEFAULT_MODEL])])]
  const body = JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] })

  for (const model of models) {
    try {
      const res = await fetch(`${ENDPOINT}/${model}:generateContent`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-goog-api-key': key },
        body,
        signal: AbortSignal.timeout(options.timeoutMs ?? 10_000),
      })
      if (!res.ok) {
        logger.warn(`Modele ${model} : HTTP ${res.status}, essai du suivant`)
        continue
      }
      const json = (await res.json()) as {
        modelVersion?: string
        candidates?: { content?: { parts?: { text?: string }[] } }[]
      }
      const text = json.candidates?.[0]?.content?.parts?.[0]?.text?.trim()
      if (text) return { text, modelVersion: json.modelVersion ?? model }
    } catch (e) {
      logger.warn(`Modele ${model} : ${e instanceof Error ? e.message : String(e)}`)
    }
  }
  return null
}
