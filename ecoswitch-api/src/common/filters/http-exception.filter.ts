import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common'
import type { Request, Response } from 'express'
import { DomainError } from '../domain-error'

/**
 * Uniformise toutes les erreurs vers `{"error": "<message>"}`.
 *
 * C'est la forme que lit `api.js:parseApiResponse` cote Vue — toute autre
 * enveloppe ferait afficher un message generique a la place du vrai motif.
 */
@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  private readonly logger = new Logger(AllExceptionsFilter.name)

  catch(exception: unknown, host: ArgumentsHost): void {
    const ctx = host.switchToHttp()
    const response = ctx.getResponse<Response>()
    const request = ctx.getRequest<Request>()

    let status = HttpStatus.INTERNAL_SERVER_ERROR
    let message = 'Erreur interne du serveur.'

    if (exception instanceof DomainError) {
      status = HttpStatus.BAD_REQUEST
      message = exception.message
    } else if (exception instanceof HttpException) {
      status = exception.getStatus()
      const body = exception.getResponse()
      if (typeof body === 'string') {
        message = body
      } else if (typeof body === 'object' && body !== null) {
        const b = body as Record<string, unknown>
        // Les exceptions Nest portent { message: <motif>, error: <libelle HTTP> } :
        // c'est le motif que le front doit afficher, pas « Conflict ».
        if (typeof b.message === 'string') message = b.message
        else if (Array.isArray(b.message)) message = b.message.join(', ')
        else if (typeof b.error === 'string') message = b.error
      }
    } else if (exception instanceof Error) {
      // Les messages internes ne fuient pas vers le client.
      this.logger.error(`${request.method} ${request.url}`, exception.stack)
    }

    response.status(status).json({ error: message })
  }
}
