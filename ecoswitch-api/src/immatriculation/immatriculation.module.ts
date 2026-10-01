import { Module } from '@nestjs/common'
import { ImmatriculationController } from './immatriculation.controller'
import { ImmatriculationService } from './immatriculation.service'

@Module({ controllers: [ImmatriculationController], providers: [ImmatriculationService] })
export class ImmatriculationModule {}
