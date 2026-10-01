import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common'
import { toNumber } from '../common/bigint'
import { toJavaLocalDateTimeString } from '../common/local-date-time'
import { PrismaService } from '../common/prisma/prisma.service'

export interface SimulationResponse {
  id: number
  name: string
  /** Format LocalDateTime Java, sans fuseau — cf. local-date-time.ts. */
  savedAt: string
  /** Blob JSON opaque, deja serialise par le front. */
  simulationData: string
}

@Injectable()
export class SimulationsService {
  constructor(private readonly prisma: PrismaService) {}

  async list(email: string): Promise<SimulationResponse[]> {
    const rows = await this.prisma.simulation.findMany({
      where: { userEmail: email },
      orderBy: { savedAt: 'desc' },
    })
    return rows.map(toResponse)
  }

  async save(email: string, name?: string, simulationData?: string): Promise<SimulationResponse> {
    if (!name || name.trim() === '') {
      throw new BadRequestException('Le nom de la simulation est obligatoire.')
    }
    if (!simulationData || simulationData.trim() === '') {
      throw new BadRequestException('Les données de simulation sont obligatoires.')
    }
    const row = await this.prisma.simulation.create({
      data: { userEmail: email, name: name.trim(), savedAt: new Date(), simulationData },
    })
    return toResponse(row)
  }

  /** Ne supprime que les simulations de l'utilisateur ; 404 sinon, sans distinguer. */
  async delete(email: string, id: number): Promise<void> {
    const { count } = await this.prisma.simulation.deleteMany({
      where: { id: BigInt(id), userEmail: email },
    })
    if (count === 0) throw new NotFoundException('Simulation introuvable.')
  }
}

function toResponse(r: { id: bigint; name: string; savedAt: Date; simulationData: string }): SimulationResponse {
  return {
    id: toNumber(r.id),
    name: r.name,
    savedAt: toJavaLocalDateTimeString(r.savedAt),
    simulationData: r.simulationData,
  }
}
