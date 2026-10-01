import { Injectable } from '@nestjs/common'
import type { Prisma } from '@prisma/client'
import { toNumber } from '../common/bigint'
import { DomainError } from '../common/domain-error'
import { PrismaService } from '../common/prisma/prisma.service'
import { isFuelType } from '../comparison/fuel-type'
import { CatalogService } from './catalog.service'
import { caseInsensitiveCompare } from './catalog.sort'

type Tx = Prisma.TransactionClient
type JsonBody = Record<string, unknown>

const str = (v: unknown) => (typeof v === 'string' ? v : undefined)
const numOrNull = (v: unknown) => {
  const n = typeof v === 'string' && v.trim() !== '' ? Number(v) : v
  return typeof n === 'number' && Number.isFinite(n) ? n : null
}
const id = (v: bigint) => toNumber(v)

/**
 * Listes et ecritures du catalogue — portage de CatalogService.java.
 *
 * Toutes les erreurs metier sont des DomainError (400), y compris « introuvable » :
 * le CatalogController Java convertissait toute IllegalArgumentException en 400.
 * Chaque ecriture invalide le cache catalogue.
 */
@Injectable()
export class CatalogAdminService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly catalog: CatalogService,
  ) {}

  // ── Listes (consommees par scripts/seed_catalog.py) ─────────────────────

  async listBrands() {
    const [brands, counts] = await Promise.all([
      this.prisma.brand.findMany(),
      this.prisma.vehicleModel.groupBy({ by: ['brandId'], _count: { _all: true } }),
    ])
    const byBrand = new Map(counts.map((c) => [c.brandId, c._count._all]))
    return brands
      .sort((a, b) => caseInsensitiveCompare(a.name, b.name))
      .map((b) => ({ id: id(b.id), name: b.name, logoUrl: b.logoUrl, modelCount: byBrand.get(b.id) ?? 0 }))
  }

  async listModels(brandId: number | null) {
    const models = await this.prisma.vehicleModel.findMany({
      where: brandId != null ? { brandId: BigInt(brandId) } : {},
      include: { brand: true, _count: { select: { motorisations: true, finitions: true } } },
      // Avec filtre : ORDER BY name en base (findByBrandIdOrderByNameAsc).
      orderBy: brandId != null ? { name: 'asc' } : { id: 'asc' },
    })
    if (brandId == null) models.sort((a, b) => caseInsensitiveCompare(a.name, b.name))
    return models.map((m) => ({
      id: id(m.id),
      brandId: id(m.brandId),
      brandName: m.brand.name,
      brandLogoUrl: m.brand.logoUrl,
      name: m.name,
      imageUrl: m.imageUrl,
      category: m.category,
      motorisationCount: m._count.motorisations,
      finitionCount: m._count.finitions,
    }))
  }

  async listMotorisations(modelId: number | null) {
    const rows = await this.prisma.motorisation.findMany({
      where: modelId != null ? { modelId: BigInt(modelId) } : {},
      include: { model: { include: { brand: true } } },
      orderBy: modelId != null ? { name: 'asc' } : { id: 'asc' },
    })
    return rows.map((m) => ({
      id: id(m.id),
      modelId: id(m.modelId),
      modelName: m.model.name,
      brandName: m.model.brand.name,
      name: m.name,
      fuelType: m.fuelType,
      consumptionWltp: m.consumptionWltp,
      powerHp: m.powerHp,
      batteryCapacityKwh: m.batteryCapacityKwh,
      autonomieWltpKm: m.autonomieWltpKm,
      consoThermiquePhev: m.consoThermiquePhev,
    }))
  }

  async listFinitions(modelId: number | null) {
    const rows = await this.prisma.finition.findMany({
      where: modelId != null ? { modelId: BigInt(modelId) } : {},
      include: { model: { include: { brand: true } } },
      orderBy: modelId != null ? { name: 'asc' } : { id: 'asc' },
    })
    return rows.map((f) => ({
      id: id(f.id),
      modelId: id(f.modelId),
      modelName: f.model.name,
      brandName: f.model.brand.name,
      name: f.name,
      imageUrl: f.imageUrl,
    }))
  }

  // ── Marques ─────────────────────────────────────────────────────────────

  async createBrand(body: JsonBody) {
    const name = str(body.name)?.trim()
    if (!name) throw new DomainError('Le nom de la marque est obligatoire.')
    await this.assertBrandNameFree(name)
    const b = await this.prisma.brand.create({ data: { name, logoUrl: str(body.logoUrl) ?? null } })
    return this.done({ id: id(b.id), name: b.name, logoUrl: b.logoUrl })
  }

  async updateBrand(brandId: number, body: JsonBody) {
    const existing = await this.brand(brandId)
    const data: Prisma.BrandUpdateInput = {}
    const name = str(body.name)?.trim()
    if (name) {
      if (name.toLowerCase() !== existing.name.toLowerCase()) await this.assertBrandNameFree(name)
      data.name = name
    }
    if (str(body.logoUrl) !== undefined) data.logoUrl = str(body.logoUrl)
    const b = await this.prisma.brand.update({ where: { id: existing.id }, data })
    return this.done({ id: id(b.id), name: b.name, logoUrl: b.logoUrl })
  }

  /** Cascade JPA : la marque emporte ses modeles et tout ce qui en depend. */
  async deleteBrand(brandId: number) {
    const existing = await this.brand(brandId)
    await this.prisma.$transaction(async (tx) => {
      const models = await tx.vehicleModel.findMany({ where: { brandId: existing.id }, select: { id: true } })
      await deleteModelsCascade(tx, models.map((m) => m.id))
      await tx.brand.delete({ where: { id: existing.id } })
    })
    await this.catalog.invalidateCache()
  }

  // ── Modeles ─────────────────────────────────────────────────────────────

  async createModel(brandId: number | null, body: JsonBody) {
    const name = str(body.name)?.trim()
    if (!name) throw new DomainError('Le nom du modèle est obligatoire.')
    if (brandId == null) throw new DomainError('Le paramètre brandId est obligatoire.')
    const brand = await this.brand(brandId)
    const duplicate = await this.prisma.vehicleModel.findFirst({
      where: { brandId: brand.id, name: { equals: name, mode: 'insensitive' } },
    })
    if (duplicate) throw new DomainError(`Le modèle '${name}' existe déjà pour la marque ${brand.name}`)
    const m = await this.prisma.vehicleModel.create({
      data: { name, brandId: brand.id, imageUrl: str(body.imageUrl) ?? null, category: str(body.category) ?? null },
    })
    return this.done(modelJson(m))
  }

  async updateModel(modelId: number, body: JsonBody) {
    const existing = await this.model(modelId)
    const data: Prisma.VehicleModelUpdateInput = {}
    const name = str(body.name)?.trim()
    if (name) data.name = name
    if (str(body.imageUrl) !== undefined) data.imageUrl = str(body.imageUrl)
    if (str(body.category) !== undefined) data.category = str(body.category)
    const m = await this.prisma.vehicleModel.update({ where: { id: existing.id }, data })
    return this.done(modelJson(m))
  }

  async deleteModel(modelId: number) {
    const existing = await this.model(modelId)
    await this.prisma.$transaction((tx) => deleteModelsCascade(tx, [existing.id]))
    await this.catalog.invalidateCache()
  }

  // ── Motorisations ───────────────────────────────────────────────────────

  async createMotorisation(modelId: number | null, body: JsonBody) {
    const name = str(body.name)?.trim()
    if (!name) throw new DomainError('Le nom de la motorisation est obligatoire.')
    if (!isFuelType(body.fuelType)) throw new DomainError('Le type de carburant/énergie est obligatoire.')
    const consumptionWltp = numOrNull(body.consumptionWltp) ?? 0
    if (consumptionWltp < 0) throw new DomainError('La consommation WLTP ne peut pas être négative.')
    if (modelId == null) throw new DomainError('Le paramètre modelId est obligatoire.')
    const model = await this.model(modelId)
    const m = await this.prisma.motorisation.create({
      data: {
        name,
        fuelType: body.fuelType,
        consumptionWltp,
        powerHp: intOrNull(body.powerHp),
        batteryCapacityKwh: numOrNull(body.batteryCapacityKwh),
        autonomieWltpKm: intOrNull(body.autonomieWltpKm),
        consoThermiquePhev: numOrNull(body.consoThermiquePhev),
        modelId: model.id,
      },
    })
    return this.done(motorisationJson(m))
  }

  /**
   * Ecart volontaire avec le Java : celui-ci remettait la consommation a 0
   * quand le champ etait absent du corps (double primitif). On ne met a jour
   * que les champs effectivement fournis.
   */
  async updateMotorisation(motorisationId: number, body: JsonBody) {
    const existing = await this.motorisation(motorisationId)
    const data: Prisma.MotorisationUpdateInput = {}
    const name = str(body.name)?.trim()
    if (name) data.name = name
    if (isFuelType(body.fuelType)) data.fuelType = body.fuelType
    const conso = numOrNull(body.consumptionWltp)
    if (conso !== null && conso >= 0) data.consumptionWltp = conso
    if (intOrNull(body.powerHp) !== null) data.powerHp = intOrNull(body.powerHp)
    if (numOrNull(body.batteryCapacityKwh) !== null) data.batteryCapacityKwh = numOrNull(body.batteryCapacityKwh)
    if (intOrNull(body.autonomieWltpKm) !== null) data.autonomieWltpKm = intOrNull(body.autonomieWltpKm)
    if (numOrNull(body.consoThermiquePhev) !== null) data.consoThermiquePhev = numOrNull(body.consoThermiquePhev)
    const m = await this.prisma.motorisation.update({ where: { id: existing.id }, data })
    return this.done(motorisationJson(m))
  }

  async deleteMotorisation(motorisationId: number) {
    const existing = await this.motorisation(motorisationId)
    await this.prisma.$transaction([
      this.prisma.finitionMotorisation.deleteMany({ where: { motorisationId: existing.id } }),
      this.prisma.motorisation.delete({ where: { id: existing.id } }),
    ])
    await this.catalog.invalidateCache()
  }

  // ── Finitions ───────────────────────────────────────────────────────────

  async createFinition(modelId: number | null, body: JsonBody) {
    const name = str(body.name)?.trim()
    if (!name) throw new DomainError('Le nom de la finition est obligatoire.')
    if (modelId == null) throw new DomainError('Le paramètre modelId est obligatoire.')
    const model = await this.model(modelId)
    const f = await this.prisma.finition.create({
      data: { name, imageUrl: str(body.imageUrl) ?? null, modelId: model.id },
    })
    return this.done(finitionJson(f))
  }

  async updateFinition(finitionId: number, body: JsonBody) {
    const existing = await this.finition(finitionId)
    const data: Prisma.FinitionUpdateInput = {}
    const name = str(body.name)?.trim()
    if (name) data.name = name
    if (str(body.imageUrl) !== undefined) data.imageUrl = str(body.imageUrl)
    const f = await this.prisma.finition.update({ where: { id: existing.id }, data })
    return this.done(finitionJson(f))
  }

  async deleteFinition(finitionId: number) {
    const existing = await this.finition(finitionId)
    await this.prisma.$transaction([
      this.prisma.finitionMotorisation.deleteMany({ where: { finitionId: existing.id } }),
      this.prisma.finition.delete({ where: { id: existing.id } }),
    ])
    await this.catalog.invalidateCache()
  }

  // ── Variantes ───────────────────────────────────────────────────────────

  /** Upsert sur (finition, motorisation), comme le Java. */
  async createVariant(finitionId: number | null, motorisationId: number | null, body: JsonBody) {
    if (finitionId == null || motorisationId == null) {
      throw new DomainError('Les paramètres finitionId et motorisationId sont obligatoires.')
    }
    const finition = await this.finition(finitionId)
    const motorisation = await this.motorisation(motorisationId)
    if (finition.modelId !== motorisation.modelId) {
      throw new DomainError('La finition et la motorisation doivent appartenir au même modèle.')
    }
    const purchasePrice = numOrNull(body.purchasePrice) ?? 0
    const monthlyLoa = numOrNull(body.monthlyLoa)
    const monthlyLld = numOrNull(body.monthlyLld)
    const maintenance = numOrNull(body.defaultMaintenanceCost)
    const resale = numOrNull(body.estimatedResaleValue)

    const existing = await this.prisma.finitionMotorisation.findUnique({
      where: { finitionId_motorisationId: { finitionId: finition.id, motorisationId: motorisation.id } },
    })
    const v = existing
      ? await this.prisma.finitionMotorisation.update({
          where: { id: existing.id },
          data: {
            purchasePrice,
            monthlyLoa,
            monthlyLld,
            ...(maintenance !== null ? { defaultMaintenanceCost: maintenance } : {}),
            ...(resale !== null ? { estimatedResaleValue: resale } : {}),
          },
        })
      : await this.prisma.finitionMotorisation.create({
          data: {
            finitionId: finition.id,
            motorisationId: motorisation.id,
            purchasePrice,
            monthlyLoa,
            monthlyLld,
            defaultMaintenanceCost: maintenance,
            estimatedResaleValue: resale,
          },
        })
    return this.done(variantJson(v))
  }

  async updateVariant(variantId: number, body: JsonBody) {
    const existing = await this.variant(variantId)
    const data: Prisma.FinitionMotorisationUpdateInput = {}
    const price = numOrNull(body.purchasePrice)
    if (price !== null && price > 0) data.purchasePrice = price
    for (const k of ['monthlyLoa', 'monthlyLld', 'defaultMaintenanceCost', 'estimatedResaleValue'] as const) {
      const n = numOrNull(body[k])
      if (n !== null) data[k] = n
    }
    const v = await this.prisma.finitionMotorisation.update({ where: { id: existing.id }, data })
    return this.done(variantJson(v))
  }

  async deleteVariant(variantId: number) {
    const existing = await this.variant(variantId)
    await this.prisma.finitionMotorisation.delete({ where: { id: existing.id } })
    await this.catalog.invalidateCache()
  }

  // ── Utilitaires ─────────────────────────────────────────────────────────

  private async done<T>(result: T): Promise<T> {
    await this.catalog.invalidateCache()
    return result
  }

  private async assertBrandNameFree(name: string) {
    const clash = await this.prisma.brand.findFirst({ where: { name: { equals: name, mode: 'insensitive' } } })
    if (clash) throw new DomainError(`Une marque avec le nom '${name}' existe déjà.`)
  }

  private async brand(n: number) {
    const b = await this.prisma.brand.findUnique({ where: { id: BigInt(n) } })
    if (!b) throw new DomainError(`Marque introuvable avec l'id : ${n}`)
    return b
  }
  private async model(n: number) {
    const m = await this.prisma.vehicleModel.findUnique({ where: { id: BigInt(n) } })
    if (!m) throw new DomainError(`Modèle introuvable avec l'id : ${n}`)
    return m
  }
  private async motorisation(n: number) {
    const m = await this.prisma.motorisation.findUnique({ where: { id: BigInt(n) } })
    if (!m) throw new DomainError(`Motorisation introuvable avec l'id : ${n}`)
    return m
  }
  private async finition(n: number) {
    const f = await this.prisma.finition.findUnique({ where: { id: BigInt(n) } })
    if (!f) throw new DomainError(`Finition introuvable avec l'id : ${n}`)
    return f
  }
  private async variant(n: number) {
    const v = await this.prisma.finitionMotorisation.findUnique({ where: { id: BigInt(n) } })
    if (!v) throw new DomainError(`Variante introuvable avec l'id : ${n}`)
    return v
  }
}

/** Les FK sont en NO ACTION : on supprime les dependances dans l'ordre. */
async function deleteModelsCascade(tx: Tx, modelIds: bigint[]) {
  if (modelIds.length === 0) return
  await tx.finitionMotorisation.deleteMany({
    where: { OR: [{ finition: { modelId: { in: modelIds } } }, { motorisation: { modelId: { in: modelIds } } }] },
  })
  await tx.finition.deleteMany({ where: { modelId: { in: modelIds } } })
  await tx.motorisation.deleteMany({ where: { modelId: { in: modelIds } } })
  await tx.vehicleModel.deleteMany({ where: { id: { in: modelIds } } })
}

function intOrNull(v: unknown): number | null {
  const n = numOrNull(v)
  return n === null ? null : Math.trunc(n)
}

function modelJson(m: { id: bigint; name: string; imageUrl: string | null; category: string | null; brandId: bigint }) {
  return { id: id(m.id), name: m.name, imageUrl: m.imageUrl, category: m.category, brandId: id(m.brandId) }
}
function motorisationJson(m: Prisma.MotorisationGetPayload<object>) {
  return {
    id: id(m.id), name: m.name, fuelType: m.fuelType, consumptionWltp: m.consumptionWltp, powerHp: m.powerHp,
    batteryCapacityKwh: m.batteryCapacityKwh, autonomieWltpKm: m.autonomieWltpKm,
    consoThermiquePhev: m.consoThermiquePhev, modelId: id(m.modelId),
  }
}
function finitionJson(f: { id: bigint; name: string; imageUrl: string | null; modelId: bigint }) {
  return { id: id(f.id), name: f.name, imageUrl: f.imageUrl, modelId: id(f.modelId) }
}
function variantJson(v: Prisma.FinitionMotorisationGetPayload<object>) {
  return {
    id: id(v.id), finitionId: id(v.finitionId), motorisationId: id(v.motorisationId),
    purchasePrice: v.purchasePrice, monthlyLoa: v.monthlyLoa, monthlyLld: v.monthlyLld,
    defaultMaintenanceCost: v.defaultMaintenanceCost, estimatedResaleValue: v.estimatedResaleValue,
  }
}
