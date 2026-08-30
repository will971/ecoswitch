package com.example.springbootapp.model.dto;

import com.example.springbootapp.model.entity.FuelType;

public record MotorisationDto(
    Long id,
    Long modelId,
    String modelName,
    String brandName,
    String name,
    FuelType fuelType,
    double consumptionWltp,
    Integer powerHp,
    Double batteryCapacityKwh
) {}
