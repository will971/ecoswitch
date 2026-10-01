import { Body, Controller, Delete, Get, HttpCode, Param, ParseIntPipe, Post } from '@nestjs/common'
import type { AuthUser } from '../auth/auth.types'
import { CurrentUser } from '../auth/decorators'
import { SimulationsService } from './simulations.service'

/** Protege par defaut (AuthGuard global) : chaque utilisateur ne voit que les siennes. */
@Controller('api/v1/simulations')
export class SimulationsController {
  constructor(private readonly simulations: SimulationsService) {}

  @Get()
  list(@CurrentUser() user: AuthUser) {
    return this.simulations.list(user.email)
  }

  @Post()
  @HttpCode(201)
  save(@CurrentUser() user: AuthUser, @Body() body: { name?: string; simulationData?: string }) {
    return this.simulations.save(user.email, body?.name, body?.simulationData)
  }

  @Delete(':id')
  @HttpCode(204)
  async delete(@CurrentUser() user: AuthUser, @Param('id', ParseIntPipe) id: number): Promise<void> {
    await this.simulations.delete(user.email, id)
  }
}
