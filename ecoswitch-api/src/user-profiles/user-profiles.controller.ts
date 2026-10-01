import { Body, Controller, Delete, Get, HttpCode, Param, ParseIntPipe, Post, Put } from '@nestjs/common'
import type { AuthUser } from '../auth/auth.types'
import { CurrentUser } from '../auth/decorators'
import { UserProfilesService, VehicleProfileInput } from './user-profiles.service'

/**
 * Garage virtuel. Protege par l'AuthGuard global — cote Java, ces routes
 * etaient accessibles anonymement sous l'identite "anonymousUser".
 */
@Controller('api/v1/users/me/vehicle-profiles')
export class UserProfilesController {
  constructor(private readonly profiles: UserProfilesService) {}

  @Get()
  list(@CurrentUser() user: AuthUser) {
    return this.profiles.list(user.email)
  }

  /** 200 et non 201 : c'est ce que renvoyait le Java, et ce qu'attend le front. */
  @Post()
  @HttpCode(200)
  create(@CurrentUser() user: AuthUser, @Body() body: VehicleProfileInput) {
    return this.profiles.create(user.email, body)
  }

  @Put(':id')
  update(@CurrentUser() user: AuthUser, @Param('id', ParseIntPipe) id: number, @Body() body: VehicleProfileInput) {
    return this.profiles.update(user.email, id, body)
  }

  @Delete(':id')
  @HttpCode(204)
  async delete(@CurrentUser() user: AuthUser, @Param('id', ParseIntPipe) id: number): Promise<void> {
    await this.profiles.delete(user.email, id)
  }
}
