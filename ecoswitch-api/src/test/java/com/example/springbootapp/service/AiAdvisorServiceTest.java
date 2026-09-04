package com.example.springbootapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiAdvisorServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiAdvisorService aiAdvisorService = new AiAdvisorService(objectMapper);

    @Test
    void shouldGenerateDeterministicAdviceWhenApiKeyNotConfigured() {
        Vehicule current = new Vehicule();
        current.setName("Renault Clio 1.2");
        current.setFuelType(FuelType.PETROL);
        current.setConsumption(6.5);
        current.setAnnualMileage(15000);
        current.setResaleValue(5000.0);

        Vehicule target = new Vehicule();
        target.setName("Renault Zoe EV50");
        target.setFuelType(FuelType.ELECTRIC);
        target.setConsumption(16.0);
        target.setPurchasePrice(24000.0);

        AiAdvisorService.AiAdvisorRequest request = new AiAdvisorService.AiAdvisorRequest(
                current,
                target,
                15000.0,
                0.85,
                22000.0,
                false,
                false,
                110.0,
                1320.0,
                4000.0,
                4,
                1500.0
        );

        AiAdvisorService.AiAdvisorResponse response = aiAdvisorService.generateAdvice(request);

        assertNotNull(response);
        assertEquals("POSITIVE", response.status());
        assertNotNull(response.verdict());
        assertNotNull(response.financialAdvice());
        assertNotNull(response.chargingAdvice());
        assertNotNull(response.ecologicalImpact());
        assertTrue(response.keyRecommendations().size() >= 2);
    }
}
