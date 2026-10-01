import { Module } from '@nestjs/common'
import { FuelPricesService } from './fuel-prices.service'
import { OpenDataFuelClient } from './open-data.client'

@Module({ providers: [FuelPricesService, OpenDataFuelClient], exports: [FuelPricesService] })
export class FuelPricesModule {}
