import { Body, Controller, Delete, Get, HttpCode, Param, ParseIntPipe, Post, Put, Query } from '@nestjs/common'
import { Public, Roles } from '../auth/decorators'
import { CatalogAdminService } from './catalog-admin.service'
import { CatalogService } from './catalog.service'

type JsonBody = Record<string, unknown>

/**
 * Lectures publiques ; toute ecriture est reservee aux ADMIN. Cote Java, ces
 * ecritures etaient en permitAll() : n'importe qui pouvait modifier ou vider le
 * catalogue de production.
 */
@Controller('api/v1/catalog')
export class CatalogController {
  constructor(
    private readonly catalog: CatalogService,
    private readonly admin: CatalogAdminService,
  ) {}

  // ── Lectures ──────────────────────────────────────────────────────────

  @Public() @Get('hierarchy')
  hierarchy() { return this.catalog.getFullHierarchy() }

  @Public() @Get('variants')
  variants(@Query('modelId') m?: string, @Query('motorisationId') mo?: string, @Query('finitionId') f?: string) {
    return this.catalog.getVariants(optId(m), optId(mo), optId(f))
  }

  @Public() @Get('variants/:id')
  variant(@Param('id', ParseIntPipe) id: number) { return this.catalog.getVariantById(id) }

  // Consommees par scripts/seed_catalog.py (le front ne les appelle pas).
  @Public() @Get('brands')
  brands() { return this.admin.listBrands() }

  @Public() @Get('models')
  models(@Query('brandId') brandId?: string) { return this.admin.listModels(optId(brandId)) }

  @Public() @Get('motorisations')
  motorisations(@Query('modelId') modelId?: string) { return this.admin.listMotorisations(optId(modelId)) }

  @Public() @Get('finitions')
  finitions(@Query('modelId') modelId?: string) { return this.admin.listFinitions(optId(modelId)) }

  // ── Ecritures (ADMIN) ─────────────────────────────────────────────────

  @Roles('ADMIN') @Post('brands') @HttpCode(201)
  createBrand(@Body() b: JsonBody) { return this.admin.createBrand(b) }

  @Roles('ADMIN') @Put('brands/:id')
  updateBrand(@Param('id', ParseIntPipe) id: number, @Body() b: JsonBody) { return this.admin.updateBrand(id, b) }

  @Roles('ADMIN') @Delete('brands/:id') @HttpCode(204)
  async deleteBrand(@Param('id', ParseIntPipe) id: number) { await this.admin.deleteBrand(id) }

  @Roles('ADMIN') @Post('models') @HttpCode(201)
  createModel(@Query('brandId') brandId: string, @Body() b: JsonBody) { return this.admin.createModel(optId(brandId), b) }

  @Roles('ADMIN') @Put('models/:id')
  updateModel(@Param('id', ParseIntPipe) id: number, @Body() b: JsonBody) { return this.admin.updateModel(id, b) }

  @Roles('ADMIN') @Delete('models/:id') @HttpCode(204)
  async deleteModel(@Param('id', ParseIntPipe) id: number) { await this.admin.deleteModel(id) }

  @Roles('ADMIN') @Post('motorisations') @HttpCode(201)
  createMotorisation(@Query('modelId') modelId: string, @Body() b: JsonBody) {
    return this.admin.createMotorisation(optId(modelId), b)
  }

  @Roles('ADMIN') @Put('motorisations/:id')
  updateMotorisation(@Param('id', ParseIntPipe) id: number, @Body() b: JsonBody) {
    return this.admin.updateMotorisation(id, b)
  }

  @Roles('ADMIN') @Delete('motorisations/:id') @HttpCode(204)
  async deleteMotorisation(@Param('id', ParseIntPipe) id: number) { await this.admin.deleteMotorisation(id) }

  @Roles('ADMIN') @Post('finitions') @HttpCode(201)
  createFinition(@Query('modelId') modelId: string, @Body() b: JsonBody) { return this.admin.createFinition(optId(modelId), b) }

  @Roles('ADMIN') @Put('finitions/:id')
  updateFinition(@Param('id', ParseIntPipe) id: number, @Body() b: JsonBody) { return this.admin.updateFinition(id, b) }

  @Roles('ADMIN') @Delete('finitions/:id') @HttpCode(204)
  async deleteFinition(@Param('id', ParseIntPipe) id: number) { await this.admin.deleteFinition(id) }

  @Roles('ADMIN') @Post('variants') @HttpCode(201)
  createVariant(@Query('finitionId') f: string, @Query('motorisationId') m: string, @Body() b: JsonBody) {
    return this.admin.createVariant(optId(f), optId(m), b)
  }

  @Roles('ADMIN') @Put('variants/:id')
  updateVariant(@Param('id', ParseIntPipe) id: number, @Body() b: JsonBody) { return this.admin.updateVariant(id, b) }

  @Roles('ADMIN') @Delete('variants/:id') @HttpCode(204)
  async deleteVariant(@Param('id', ParseIntPipe) id: number) { await this.admin.deleteVariant(id) }
}

function optId(raw?: string): number | null {
  if (raw === undefined || raw === '' || raw === 'null') return null
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}
