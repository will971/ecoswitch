package com.example.springbootapp.model.dto;

import com.example.springbootapp.model.entity.FuelType;

public record FinitionMotorisationDto(
    Long id,
    Long finitionId,
    String finitionName,
    String finitionImageUrl,
    Long motorisationId,
    String motorisationName,
    FuelType fuelType,
    double consumptionWltp,
    Integer powerHp,
    Double batteryCapacityKwh,
    Long modelId,
    String modelName,
    String modelImageUrl,
    String category,
    Long brandId,
    String brandName,
    String brandLogoUrl,
    double purchasePrice,
    Double monthlyLoa,
    Double monthlyLld,
    Double defaultInsuranceCost,
    Double defaultMaintenanceCost,
    Double estimatedResaleValue
) {}
