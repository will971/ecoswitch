import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common'
import type { UserVehicleProfile as Row } from '@prisma/client'
import { toNumber } from '../common/bigint'
import { PrismaService } from '../common/prisma/prisma.service'
import { isFuelType } from '../comparison/fuel-type'

/**
 * Forme sur le fil. ATTENTION : le drapeau s'appelle `default`, pas
 * `isDefault` — c'est ce qu'emettait Jackson (getter `isDefault()`), et le
 * front lit `p.default` dans App.vue et UserProfileModal.vue.
 */
export interface VehicleProfileDto {
  id: number
  userEmail: string
  default: boolean
  name: string
  fuelType: string
  consumption: number
  annualMileage: number
  maintenanceCost: number
  resaleValue: number
  petrolPrice: number
  dieselPrice: number
  electricPrice: number
}

export type VehicleProfileInput = Partial<Omit<VehicleProfileDto, 'id' | 'userEmail'>>

/**
 * Valeurs appliquees quand un champ est absent du corps. Elles reproduisent
 * Jackson : 0 pour un primitif non fourni, l'initialiseur de champ pour les prix.
 */
const DEFAULTS = {
  consumption: 0,
  annualMileage: 0,
  maintenanceCost: 0,
  resaleValue: 0,
  petrolPrice: 1.88,
  dieselPrice: 1.74,
  electricPrice: 0.25,
} as const

@Injectable()
export class UserProfilesService {
  constructor(private readonly prisma: PrismaService) {}

  async list(email: string): Promise<VehicleProfileDto[]> {
    const rows = await this.prisma.userVehicleProfile.findMany({
      where: { userEmail: email },
      orderBy: { id: 'asc' },
    })
    return rows.map(toDto)
  }

  /** Le premier profil est toujours favori ; un nouveau favori detrone l'ancien. */
  async create(email: string, input: VehicleProfileInput): Promise<VehicleProfileDto> {
    const fields = this.validate(input)
    return this.prisma.$transaction(async (tx) => {
      const existing = await tx.userVehicleProfile.count({ where: { userEmail: email } })
      const wantsDefault = input.default === true
      const isDefault = existing === 0 || wantsDefault
      if (wantsDefault) {
        await tx.userVehicleProfile.updateMany({
          where: { userEmail: email, isDefault: true },
          data: { isDefault: false },
        })
      }
      const row = await tx.userVehicleProfile.create({
        data: { userEmail: email, isDefault, ...fields },
      })
      return toDto(row)
    })
  }

  async update(email: string, id: number, input: VehicleProfileInput): Promise<VehicleProfileDto> {
    const fields = this.validate(input)
    return this.prisma.$transaction(async (tx) => {
      const current = await tx.userVehicleProfile.findUnique({ where: { id: BigInt(id) } })
      if (current === null || current.userEmail !== email) {
        throw new NotFoundException('Profil introuvable.')
      }
      let isDefault = current.isDefault
      if (input.default === true && !current.isDefault) {
        await tx.userVehicleProfile.updateMany({
          where: { userEmail: email, isDefault: true, NOT: { id: current.id } },
          data: { isDefault: false },
        })
        isDefault = true
      } else if (input.default !== true && current.isDefault) {
        // Comportement Java : decocher le favori le retire, sans en designer
        // un autre. Le front retombe alors sur le dernier profil de la liste.
        isDefault = false
      }
      const row = await tx.userVehicleProfile.update({
        where: { id: current.id },
        data: { isDefault, ...fields },
      })
      return toDto(row)
    })
  }

  /** Supprimer le favori promeut le plus ancien profil restant. */
  async delete(email: string, id: number): Promise<void> {
    await this.prisma.$transaction(async (tx) => {
      const current = await tx.userVehicleProfile.findUnique({ where: { id: BigInt(id) } })
      if (current === null || current.userEmail !== email) {
        throw new NotFoundException('Profil introuvable.')
      }
      await tx.userVehicleProfile.delete({ where: { id: current.id } })
      if (current.isDefault) {
        const next = await tx.userVehicleProfile.findFirst({
          where: { userEmail: email },
          orderBy: { id: 'asc' },
        })
        if (next !== null) {
          await tx.userVehicleProfile.update({ where: { id: next.id }, data: { isDefault: true } })
        }
      }
    })
  }

  /**
   * Le Java laissait la base rejeter un nom ou un carburant manquant (500).
   * On renvoie un 400 explicite a la place.
   */
  private validate(input: VehicleProfileInput) {
    if (!input || typeof input.name !== 'string' || input.name.trim() === '') {
      throw new BadRequestException('Le nom du véhicule est obligatoire.')
    }
    if (!isFuelType(input.fuelType)) {
      throw new BadRequestException('Type de carburant invalide.')
    }
    return {
      name: input.name,
      fuelType: input.fuelType,
      consumption: num(input.consumption, DEFAULTS.consumption),
      annualMileage: Math.trunc(num(input.annualMileage, DEFAULTS.annualMileage)),
      maintenanceCost: num(input.maintenanceCost, DEFAULTS.maintenanceCost),
      resaleValue: num(input.resaleValue, DEFAULTS.resaleValue),
      petrolPrice: num(input.petrolPrice, DEFAULTS.petrolPrice),
      dieselPrice: num(input.dieselPrice, DEFAULTS.dieselPrice),
      electricPrice: num(input.electricPrice, DEFAULTS.electricPrice),
    }
  }
}

function num(value: unknown, fallback: number): number {
  if (value === null || value === undefined || value === '') return fallback
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

export function toDto(r: Row): VehicleProfileDto {
  return {
    id: toNumber(r.id),
    userEmail: r.userEmail,
    default: r.isDefault,
    name: r.name,
    fuelType: r.fuelType,
    consumption: r.consumption ?? DEFAULTS.consumption,
    annualMileage: r.annualMileage ?? DEFAULTS.annualMileage,
    maintenanceCost: r.maintenanceCost ?? DEFAULTS.maintenanceCost,
    resaleValue: r.resaleValue ?? DEFAULTS.resaleValue,
    petrolPrice: r.petrolPrice ?? DEFAULTS.petrolPrice,
    dieselPrice: r.dieselPrice ?? DEFAULTS.dieselPrice,
    electricPrice: r.electricPrice ?? DEFAULTS.electricPrice,
  }
}
