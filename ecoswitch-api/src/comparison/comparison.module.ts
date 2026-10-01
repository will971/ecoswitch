import { Module } from '@nestjs/common'
import { AiAdvisorModule } from '../ai-advisor/ai-advisor.module'
import { CatalogModule } from '../catalog/catalog.module'
import { FuelPricesModule } from '../fuel-prices/fuel-prices.module'
import { ComparisonController } from './comparison.controller'
import { CatalogService } from '../catalog/catalog.service'
import { ComparisonService, CatalogVariantProvider } from './comparison.service'
import { CostCalculationService } from './cost-calculation.service'
import type { CatalogVariant } from './comparison.types'

/** Branche le moteur de recommandation sur le catalogue reel. */
class PrismaCatalogVariantProvider extends CatalogVariantProvider {
  constructor(private readonly catalog: CatalogService) {
    super()
  }

  async getVariants(): Promise<CatalogVariant[]> {
    return this.catalog.getVariants()
  }
}

@Module({
  imports: [CatalogModule, FuelPricesModule, AiAdvisorModule],
  controllers: [ComparisonController],
  providers: [
    CostCalculationService,
    ComparisonService,
    {
      provide: CatalogVariantProvider,
      useFactory: (catalog: CatalogService) => new PrismaCatalogVariantProvider(catalog),
      inject: [CatalogService],
    },
  ],
  exports: [ComparisonService, CostCalculationService],
})
export class ComparisonModule {}
