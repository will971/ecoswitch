import { Controller, Get } from '@nestjs/common'
import { Public } from './auth/decorators'
import { PrismaService } from './common/prisma/prisma.service'

@Public()
@Controller('health')
export class HealthController {
  constructor(private readonly prisma: PrismaService) {}

  /** Sonde du HEALTHCHECK Docker : verifie aussi l'acces base, pas seulement le process. */
  @Get()
  async check(): Promise<{ status: string; database: string }> {
    try {
      await this.prisma.$queryRaw`SELECT 1`
      return { status: 'ok', database: 'up' }
    } catch {
      return { status: 'degraded', database: 'down' }
    }
  }
}
