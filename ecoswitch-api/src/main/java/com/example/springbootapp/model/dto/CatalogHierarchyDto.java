package com.example.springbootapp.model.dto;

import com.example.springbootapp.model.entity.FuelType;
import java.util.List;

public record CatalogHierarchyDto(
    Long id,
    String name,
    String logoUrl,
    List<ModelHierarchyDto> models
) {
    public record ModelHierarchyDto(
        Long id,
        String name,
        String imageUrl,
        String category,
        List<MotorisationHierarchyDto> motorisations,
        List<FinitionHierarchyDto> finitions
    ) {}

    public record MotorisationHierarchyDto(
        Long id,
        String name,
        FuelType fuelType,
        double consumptionWltp,
        Integer powerHp,
        Double batteryCapacityKwh,
        List<VariantPriceDto> availableFinitions
    ) {}

    public record FinitionHierarchyDto(
        Long id,
        String name,
        String imageUrl
    ) {}

    public record VariantPriceDto(
        Long variantId,
        Long finitionId,
        String finitionName,
        String finitionImageUrl,
        double purchasePrice,
        Double monthlyLoa,
        Double monthlyLld,
        Double defaultInsuranceCost,
        Double defaultMaintenanceCost,
        Double estimatedResaleValue
    ) {}
}
