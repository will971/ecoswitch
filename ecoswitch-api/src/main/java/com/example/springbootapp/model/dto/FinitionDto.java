package com.example.springbootapp.model.dto;

public record FinitionDto(
    Long id,
    Long modelId,
    String modelName,
    String brandName,
    String name,
    String imageUrl
) {}
