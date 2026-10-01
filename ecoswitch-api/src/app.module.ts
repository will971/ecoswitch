import { CacheModule } from '@nestjs/cache-manager'
import { Module } from '@nestjs/common'
import { ScheduleModule } from '@nestjs/schedule'
import { AuthModule } from './auth/auth.module'
import { CatalogModule } from './catalog/catalog.module'
import { PrismaModule } from './common/prisma/prisma.module'
import { ComparisonModule } from './comparison/comparison.module'
import { HealthController } from './health.controller'
import { ImmatriculationModule } from './immatriculation/immatriculation.module'
import { MediaModule } from './media/media.module'
import { SimulationsModule } from './simulations/simulations.module'
import { UserProfilesModule } from './user-profiles/user-profiles.module'

@Module({
  imports: [
    PrismaModule,
    CacheModule.register({ isGlobal: true, ttl: 10 * 60 * 1000, max: 1000 }),
    ScheduleModule.forRoot(),
    AuthModule,
    CatalogModule,
    ComparisonModule,
    SimulationsModule,
    UserProfilesModule,
    ImmatriculationModule,
    MediaModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
