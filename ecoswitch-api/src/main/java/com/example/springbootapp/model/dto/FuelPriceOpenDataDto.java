package com.example.springbootapp.model.dto;

import java.util.Map;

public record FuelPriceOpenDataDto(
        Map<String, Double> prices,
        int totalStationsSurveyed,
        boolean success
) {
    public static FuelPriceOpenDataDto failed() {
        return new FuelPriceOpenDataDto(Map.of(), 0, false);
    }
}
