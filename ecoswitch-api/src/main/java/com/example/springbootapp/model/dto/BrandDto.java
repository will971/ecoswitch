package com.example.springbootapp.model.dto;

public record BrandDto(
    Long id,
    String name,
    String logoUrl,
    int modelCount
) {}
