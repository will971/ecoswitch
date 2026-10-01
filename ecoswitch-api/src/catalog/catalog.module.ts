import { Module } from '@nestjs/common'
import { CatalogAdminService } from './catalog-admin.service'
import { CatalogController } from './catalog.controller'
import { CatalogService } from './catalog.service'

@Module({
  controllers: [CatalogController],
  providers: [CatalogService, CatalogAdminService],
  exports: [CatalogService],
})
export class CatalogModule {}
