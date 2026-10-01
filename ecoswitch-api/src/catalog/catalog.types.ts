import type { FuelType } from '../comparison/fuel-type'

/**
 * Miroir exact de CatalogHierarchyDto.java. Les noms de champs sont
 * CONTRACTUELS : `api.js:apiGetCatalogHierarchy` reecrit precisement
 * `b.logoUrl`, `m.imageUrl`, `f.imageUrl` et
 * `mot.availableFinitions[].finitionImageUrl`. Un champ renomme casse
 * silencieusement l'affichage des images.
 */
export interface BrandHierarchy {
  id: number
  name: string
  logoUrl: string | null
  models: ModelHierarchy[]
}

export interface ModelHierarchy {
  id: number
  name: string
  imageUrl: string | null
  category: string | null
  motorisations: MotorisationHierarchy[]
  finitions: FinitionHierarchy[]
}

export interface MotorisationHierarchy {
  id: number
  name: string
  fuelType: FuelType
  consumptionWltp: number
  powerHp: number | null
  batteryCapacityKwh: number | null
  autonomieWltpKm: number | null
  consoThermiquePhev: number | null
  availableFinitions: VariantPrice[]
}

export interface FinitionHierarchy {
  id: number
  name: string
  imageUrl: string | null
}

export interface VariantPrice {
  variantId: number
  finitionId: number
  finitionName: string
  finitionImageUrl: string | null
  purchasePrice: number
  monthlyLoa: number | null
  monthlyLld: number | null
  defaultMaintenanceCost: number | null
  estimatedResaleValue: number | null
}
