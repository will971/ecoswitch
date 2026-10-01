import { Inject, Injectable, NotFoundException } from '@nestjs/common'
import { CACHE_MANAGER } from '@nestjs/cache-manager'
import type { Cache } from 'cache-manager'
import { PrismaService } from '../common/prisma/prisma.service'
import { toNumber } from '../common/bigint'
import { caseInsensitiveCompare } from './catalog.sort'
import type { BrandHierarchy, ModelHierarchy, MotorisationHierarchy, VariantPrice } from './catalog.types'
import type { CatalogVariant } from '../comparison/comparison.types'
import type { FuelType } from '../comparison/fuel-type'

/** Inclusion complete du graphe d'une variante, pour le DTO plat et la vue detaillee. */
const VARIANT_INCLUDE = {
  finition: { include: { model: { include: { brand: true } } } },
  motorisation: { include: { model: { include: { brand: true } } } },
} as const

@Injectable()
export class CatalogService {
  /**
   * Duree du cache catalogue. 10 min en production, comme le Java.
   *
   * En developpement Conductor la base est PARTAGEE entre workspaces : sans TTL
   * court, une ecriture faite depuis un workspace reste invisible des autres
   * jusqu'a expiration. `.conductor/scripts/dev.sh` abaisse donc cette valeur.
   */
  private static readonly CACHE_TTL_MS = Number(process.env.CATALOG_CACHE_TTL_MS ?? 10 * 60 * 1000)
  private static readonly KEY_HIERARCHY = 'catalog:hierarchy'
  private static readonly KEY_VARIANTS = 'catalog:variants'

  constructor(
    private readonly prisma: PrismaService,
    @Inject(CACHE_MANAGER) private readonly cache: Cache,
  ) {}

  // ── Hierarchie ────────────────────────────────────────────────────────

  /**
   * Arbre complet marque > modele > motorisation/finition, en 5 requetes.
   *
   * L'ordre est contractuel : marques, modeles, finitions et motorisations sont
   * tries par nom avec la semantique Java (cf. catalog.sort.ts).
   * `availableFinitions` n'est PAS trie — il suit l'ordre des variantes par id,
   * comme le `findAllWithDetails()` du Java.
   */
  async getFullHierarchy(): Promise<BrandHierarchy[]> {
    const cached = await this.cache.get<BrandHierarchy[]>(CatalogService.KEY_HIERARCHY)
    if (cached) return cached

    const [brands, models, finitions, motorisations, variants] = await Promise.all([
      this.prisma.brand.findMany(),
      this.prisma.vehicleModel.findMany(),
      this.prisma.finition.findMany(),
      this.prisma.motorisation.findMany(),
      this.prisma.finitionMotorisation.findMany({
        include: { finition: true },
        orderBy: { id: 'asc' },
      }),
    ])

    brands.sort((a, b) => caseInsensitiveCompare(a.name, b.name))
    models.sort((a, b) => caseInsensitiveCompare(a.name, b.name))
    finitions.sort((a, b) => caseInsensitiveCompare(a.name, b.name))
    motorisations.sort((a, b) => caseInsensitiveCompare(a.name, b.name))

    const modelsByBrand = groupBy(models, (m) => m.brandId)
    const finitionsByModel = groupBy(finitions, (f) => f.modelId)
    const motorisationsByModel = groupBy(motorisations, (m) => m.modelId)
    const variantsByMotorisation = groupBy(variants, (v) => v.motorisationId)

    const result: BrandHierarchy[] = brands.map((b) => {
      const modelDtos: ModelHierarchy[] = (modelsByBrand.get(b.id) ?? []).map((m) => {
        const motorisationDtos: MotorisationHierarchy[] = (motorisationsByModel.get(m.id) ?? []).map(
          (mot) => {
            const availableFinitions: VariantPrice[] = (variantsByMotorisation.get(mot.id) ?? []).map(
              (v) => ({
                variantId: toNumber(v.id),
                finitionId: toNumber(v.finitionId),
                finitionName: v.finition.name,
                finitionImageUrl: v.finition.imageUrl,
                purchasePrice: v.purchasePrice,
                monthlyLoa: v.monthlyLoa,
                monthlyLld: v.monthlyLld,
                defaultMaintenanceCost: v.defaultMaintenanceCost,
                estimatedResaleValue: v.estimatedResaleValue,
              }),
            )
            return {
              id: toNumber(mot.id),
              name: mot.name,
              fuelType: mot.fuelType as FuelType,
              consumptionWltp: mot.consumptionWltp,
              powerHp: mot.powerHp,
              batteryCapacityKwh: mot.batteryCapacityKwh,
              autonomieWltpKm: mot.autonomieWltpKm,
              consoThermiquePhev: mot.consoThermiquePhev,
              availableFinitions,
            }
          },
        )

        return {
          id: toNumber(m.id),
          name: m.name,
          imageUrl: m.imageUrl,
          category: m.category,
          motorisations: motorisationDtos,
          finitions: (finitionsByModel.get(m.id) ?? []).map((f) => ({
            id: toNumber(f.id),
            name: f.name,
            imageUrl: f.imageUrl,
          })),
        }
      })

      return { id: toNumber(b.id), name: b.name, logoUrl: b.logoUrl, models: modelDtos }
    })

    await this.cache.set(CatalogService.KEY_HIERARCHY, result, CatalogService.CACHE_TTL_MS)
    return result
  }

  // ── Variantes ─────────────────────────────────────────────────────────

  /** DTO plat a 24 champs. Les filtres se substituent dans l'ordre du Java. */
  async getVariants(
    modelId?: number | null,
    motorisationId?: number | null,
    finitionId?: number | null,
  ): Promise<CatalogVariant[]> {
    const noFilter = modelId == null && motorisationId == null && finitionId == null
    if (noFilter) {
      const cached = await this.cache.get<CatalogVariant[]>(CatalogService.KEY_VARIANTS)
      if (cached) return cached
    }

    let where: Record<string, unknown> = {}
    if (motorisationId != null) where = { motorisationId: BigInt(motorisationId) }
    else if (finitionId != null) where = { finitionId: BigInt(finitionId) }
    else if (modelId != null) where = { motorisation: { modelId: BigInt(modelId) } }

    const rows = await this.prisma.finitionMotorisation.findMany({
      where,
      include: VARIANT_INCLUDE,
      orderBy: { id: 'asc' },
    })
    const result = rows.map((r) => this.toVariantDto(r))

    if (noFilter) {
      await this.cache.set(CatalogService.KEY_VARIANTS, result, CatalogService.CACHE_TTL_MS)
    }
    return result
  }

  /**
   * Renvoie le GRAPHE d'entites, pas le DTO plat — `DirectSimulator.vue:324-338`
   * lit `data.finition.model.brand.name`, `data.motorisation.fuelType`, etc.
   * Les collections inverses sont elaguees, comme le font les
   * @JsonIgnoreProperties cote Java.
   */
  async getVariantById(id: number): Promise<Record<string, unknown>> {
    const v = await this.prisma.finitionMotorisation.findUnique({
      where: { id: BigInt(id) },
      include: VARIANT_INCLUDE,
    })
    if (v === null) {
      throw new NotFoundException(`Variante introuvable avec l'id : ${id}`)
    }

    const brand = (b: { id: bigint; name: string; logoUrl: string | null }) => ({
      id: toNumber(b.id),
      name: b.name,
      logoUrl: b.logoUrl,
    })
    const model = (m: {
      id: bigint
      name: string
      imageUrl: string | null
      category: string | null
      brand: { id: bigint; name: string; logoUrl: string | null }
    }) => ({
      id: toNumber(m.id),
      name: m.name,
      imageUrl: m.imageUrl,
      category: m.category,
      brand: brand(m.brand),
    })

    return {
      id: toNumber(v.id),
      purchasePrice: v.purchasePrice,
      monthlyLoa: v.monthlyLoa,
      monthlyLld: v.monthlyLld,
      defaultMaintenanceCost: v.defaultMaintenanceCost,
      estimatedResaleValue: v.estimatedResaleValue,
      finition: {
        id: toNumber(v.finition.id),
        name: v.finition.name,
        imageUrl: v.finition.imageUrl,
        model: model(v.finition.model),
      },
      motorisation: {
        id: toNumber(v.motorisation.id),
        name: v.motorisation.name,
        fuelType: v.motorisation.fuelType,
        consumptionWltp: v.motorisation.consumptionWltp,
        powerHp: v.motorisation.powerHp,
        batteryCapacityKwh: v.motorisation.batteryCapacityKwh,
        autonomieWltpKm: v.motorisation.autonomieWltpKm,
        consoThermiquePhev: v.motorisation.consoThermiquePhev,
        model: model(v.motorisation.model),
      },
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private toVariantDto(r: any): CatalogVariant {
    const mot = r.motorisation
    const fin = r.finition
    const model = mot.model
    const brand = model.brand
    return {
      id: toNumber(r.id),
      finitionId: toNumber(fin.id),
      finitionName: fin.name,
      finitionImageUrl: fin.imageUrl,
      motorisationId: toNumber(mot.id),
      motorisationName: mot.name,
      fuelType: mot.fuelType as FuelType,
      consumptionWltp: mot.consumptionWltp,
      powerHp: mot.powerHp,
      batteryCapacityKwh: mot.batteryCapacityKwh,
      autonomieWltpKm: mot.autonomieWltpKm,
      consoThermiquePhev: mot.consoThermiquePhev,
      modelId: toNumber(model.id),
      modelName: model.name,
      modelImageUrl: model.imageUrl,
      category: model.category,
      brandId: toNumber(brand.id),
      brandName: brand.name,
      brandLogoUrl: brand.logoUrl,
      purchasePrice: r.purchasePrice,
      monthlyLoa: r.monthlyLoa,
      monthlyLld: r.monthlyLld,
      defaultMaintenanceCost: r.defaultMaintenanceCost,
      estimatedResaleValue: r.estimatedResaleValue,
    }
  }

  /** Toute mutation invalide l'ensemble du cache catalogue, comme le @CacheEvict Java. */
  async invalidateCache(): Promise<void> {
    await this.cache.del(CatalogService.KEY_HIERARCHY)
    await this.cache.del(CatalogService.KEY_VARIANTS)
  }
}

function groupBy<T, K>(items: T[], key: (item: T) => K): Map<K, T[]> {
  const map = new Map<K, T[]>()
  for (const item of items) {
    const k = key(item)
    const bucket = map.get(k)
    if (bucket === undefined) map.set(k, [item])
    else bucket.push(item)
  }
  return map
}
