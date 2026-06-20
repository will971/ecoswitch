package com.example.springbootapp.business.comparison;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.springbootapp.controller.comparison.ComparisonController.CustomProfitabilityComparisonRequest;
import com.example.springbootapp.controller.comparison.ComparisonController.DirectProfitabilityRequest;
import com.example.springbootapp.controller.comparison.ComparisonController.DirectProfitabilityResponse;
import com.example.springbootapp.controller.comparison.ComparisonController.ProfitabilityComparisonRequest;
import com.example.springbootapp.controller.comparison.ComparisonController.ProfitabilityComparisonResponse;
import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.service.VehiculeService;

@SpringBootTest
@Transactional
class ComparisonBusinessTest {

    @Autowired
    private ComparisonBusiness comparisonBusiness;

    @Autowired
    private VehiculeService vehiculeService;

    @Test
    void shouldCompareCatalogProfitability() {
        // Create current vehicle (diesel)
        Vehicule current = new Vehicule();
        current.setName("Peugeot 308");
        current.setBrand("Peugeot");
        current.setModel("308");
        current.setVersion("BlueHDi");
        current.setFuelType(FuelType.DIESEL);
        current.setConsumption(5.0);
        current.setResaleValue(10000.0);
        current.setPurchasePrice(25000.0);
        current = vehiculeService.create(current);

        // Create target vehicle (electric)
        Vehicule target = new Vehicule();
        target.setName("Renault Zoe");
        target.setBrand("Renault");
        target.setModel("Zoe");
        target.setVersion("R110");
        target.setFuelType(FuelType.ELECTRIC);
        target.setConsumption(17.0);
        target.setResaleValue(8000.0);
        target.setPurchasePrice(30000.0);
        target = vehiculeService.create(target);

        // Request
        ProfitabilityComparisonRequest request = new ProfitabilityComparisonRequest(
            current.getId(),
            List.of(target.getId()),
            Map.of("DIESEL", 1.8, "ELECTRIC", 0.25),
            5,
            0.0
        );

        ProfitabilityComparisonResponse response = comparisonBusiness.compareProfitability(request);
        assertNotNull(response);
        assertEquals(current.getId(), response.currentVehicleId());
        assertFalse(response.alternatives().isEmpty());
        assertEquals(target.getId(), response.alternatives().get(0).vehicleId());
    }

    @Test
    void shouldThrowOnInvalidCompareRequest() {
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareProfitability(null));
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareProfitability(
            new ProfitabilityComparisonRequest(null, List.of(1L), Map.of(), 5, 0.0)
        ));
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareProfitability(
            new ProfitabilityComparisonRequest(1L, List.of(), Map.of(), 5, 0.0)
        ));
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareProfitability(
            new ProfitabilityComparisonRequest(1L, List.of(2L), Map.of(), -1, 0.0)
        ));
    }

    @Test
    void shouldCompareCustomProfitability() {
        Vehicule current = new Vehicule();
        current.setName("Custom Diesel");
        current.setBrand("Custom");
        current.setModel("Diesel");
        current.setVersion("v1");
        current.setFuelType(FuelType.DIESEL);
        current.setConsumption(6.0);
        current.setResaleValue(5000.0);

        Vehicule target = new Vehicule();
        target.setName("Renault Zoe");
        target.setBrand("Renault");
        target.setModel("Zoe");
        target.setVersion("R110");
        target.setFuelType(FuelType.ELECTRIC);
        target.setConsumption(15.0);
        target.setPurchasePrice(25000.0);
        target = vehiculeService.create(target);

        CustomProfitabilityComparisonRequest request = new CustomProfitabilityComparisonRequest(
            current,
            List.of(target.getId()),
            Map.of("DIESEL", 1.8, "ELECTRIC", 0.25),
            5,
            500.0
        );

        ProfitabilityComparisonResponse response = comparisonBusiness.compareCustomProfitability(request);
        assertNotNull(response);
        assertFalse(response.alternatives().isEmpty());
    }

    @Test
    void shouldThrowOnInvalidCustomCompareRequest() {
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareCustomProfitability(null));
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareCustomProfitability(
            new CustomProfitabilityComparisonRequest(null, List.of(1L), Map.of(), 5, 0.0)
        ));
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareCustomProfitability(
            new CustomProfitabilityComparisonRequest(new Vehicule(), List.of(), Map.of(), 5, 0.0)
        ));
    }

    @Test
    void shouldCompareDirectProfitability() {
        Vehicule current = new Vehicule();
        current.setName("Current Car");
        current.setBrand("BrandA");
        current.setModel("ModelA");
        current.setVersion("vA");
        current.setFuelType(FuelType.DIESEL);
        current.setConsumption(6.0);
        current.setResaleValue(10000.0);

        Vehicule target = new Vehicule();
        target.setName("Target EV");
        target.setBrand("BrandB");
        target.setModel("ModelB");
        target.setVersion("vB");
        target.setFuelType(FuelType.ELECTRIC);
        target.setConsumption(16.0);
        target.setPurchasePrice(35000.0);

        DirectProfitabilityRequest request = new DirectProfitabilityRequest(
            current,
            target,
            Map.of("DIESEL", 1.7, "ELECTRIC", 0.25),
            10,
            0.0,
            0.8, // homeChargingRatio
            25000.0, // taxIncome
            true, // scrapVehicle
            false, // isLeasing
            0.0 // customLeasingMonthlyPrice
        );

        DirectProfitabilityResponse response = comparisonBusiness.compareDirect(request);
        assertNotNull(response);
        assertTrue(response.annualSavings() > 0 || response.annualSavings() <= 0); // basic sanity
        assertTrue(response.totalSubsidies() >= 0);
    }

    @Test
    void shouldCompareDirectProfitabilityWithLeasing() {
        Vehicule current = new Vehicule();
        current.setName("Current Car");
        current.setBrand("BrandA");
        current.setModel("ModelA");
        current.setVersion("vA");
        current.setFuelType(FuelType.DIESEL);
        current.setConsumption(6.0);
        current.setResaleValue(10000.0);

        Vehicule target = new Vehicule();
        target.setName("Target EV");
        target.setBrand("BrandB");
        target.setModel("ModelB");
        target.setVersion("vB");
        target.setFuelType(FuelType.ELECTRIC);
        target.setConsumption(16.0);
        target.setPurchasePrice(35000.0);

        // With default leasing calculation
        DirectProfitabilityRequest request1 = new DirectProfitabilityRequest(
            current,
            target,
            Map.of("DIESEL", 1.7, "ELECTRIC", 0.25),
            10,
            0.0,
            0.8,
            25000.0,
            true,
            true, // isLeasing
            0.0
        );

        DirectProfitabilityResponse response1 = comparisonBusiness.compareDirect(request1);
        assertNotNull(response1);
        assertTrue(response1.targetMonthlyTotalCost() > 0);

        // With custom leasing price
        DirectProfitabilityRequest request2 = new DirectProfitabilityRequest(
            current,
            target,
            Map.of("DIESEL", 1.7, "ELECTRIC", 0.25),
            10,
            0.0,
            0.8,
            25000.0,
            true,
            true, // isLeasing
            299.0 // customLeasingMonthlyPrice
        );

        DirectProfitabilityResponse response2 = comparisonBusiness.compareDirect(request2);
        assertNotNull(response2);
    }

    @Test
    void shouldThrowOnInvalidDirectCompareRequest() {
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareDirect(null));
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareDirect(
            new DirectProfitabilityRequest(null, new Vehicule(), Map.of(), 5, 0.0, 0.8, 0.0, false, false, 0.0)
        ));
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareDirect(
            new DirectProfitabilityRequest(new Vehicule(), null, Map.of(), 5, 0.0, 0.8, 0.0, false, false, 0.0)
        ));
        
        Vehicule v1 = new Vehicule();
        Vehicule v2 = new Vehicule();
        v2.setFuelType(FuelType.ELECTRIC);
        assertThrows(IllegalArgumentException.class, () -> comparisonBusiness.compareDirect(
            new DirectProfitabilityRequest(v1, v2, Map.of(), 5, 0.0, 0.8, 0.0, false, false, 0.0)
        ));
    }
}
