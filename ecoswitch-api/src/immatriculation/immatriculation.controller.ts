import { Controller, Get, Param } from '@nestjs/common'
import { Throttle } from '@nestjs/throttler'
import { Public } from '../auth/decorators'
import { ImmatriculationService } from './immatriculation.service'

/**
 * Public (utilise dans le wizard avant toute connexion), mais limite a
 * 20 requetes/minute/IP : chaque appel peut declencher une requete sortante
 * vers un tiers. Le Java n'avait aucune limite.
 */
@Public()
@Controller('api/v1/immatriculation')
export class ImmatriculationController {
  constructor(private readonly service: ImmatriculationService) {}

  @Get(':plaque')
  @Throttle({ default: { limit: 20, ttl: 60_000 } })
  lookup(@Param('plaque') plaque: string) {
    return this.service.lookup(plaque)
  }
}
