package com.example.springbootapp.model.dto;

public record VehicleModelDto(
    Long id,
    Long brandId,
    String brandName,
    String brandLogoUrl,
    String name,
    String imageUrl,
    String category,
    int motorisationCount,
    int finitionCount
) {}
