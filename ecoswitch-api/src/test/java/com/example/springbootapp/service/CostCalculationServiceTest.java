package com.example.springbootapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CostCalculationServiceTest {

	private final CostCalculationService costCalculationService = new CostCalculationService();

	@Test
	void shouldCalculateBreakEvenYearWhenTargetBecomesCheaper() {
		Vehicule current = buildVehicule(1L, "Current", FuelType.PETROL, 18_000, 7.5, 850, 600, 0, 12_000);
		Vehicule target = buildVehicule(2L, "Target", FuelType.ELECTRIC, 18_000, 16.5, 650, 350, 25_000, 0);

		Map<String, Double> prices = Map.of(
			"PETROL",
			1.9,
			"ELECTRIC",
			0.23
		);

		Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(current, target, 10, prices);

		assertEquals(6, breakEvenYear);
	}

	@Test
	void shouldReturnNullWhenTargetNeverBecomesCheaper() {
		Vehicule current = buildVehicule(1L, "Current", FuelType.PETROL, 12_000, 5.8, 650, 450, 0, 8_000);
		Vehicule target = buildVehicule(2L, "Target", FuelType.PETROL, 12_000, 5.6, 620, 430, 30_000, 0);

		Map<String, Double> prices = Map.of("PETROL", 1.9);

		Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(current, target, 10, prices);

		assertNull(breakEvenYear);
	}

	@Test
	void shouldCalculateBreakEvenYearWithImmediateRepairs() {
		Vehicule current = buildVehicule(1L, "BMW 114i", FuelType.PETROL, 12_000, 6.5, 650, 500, 0, 4_500);
		Vehicule target = buildVehicule(2L, "New Hybrid Car", FuelType.HYBRID, 12_000, 4.2, 550, 350, 20_000, 0);

		Map<String, Double> prices = Map.of(
			"PETROL", 1.9,
			"HYBRID", 1.85
		);

		Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(current, target, 20, prices, 3000.0);

		assertEquals(16, breakEvenYear);
	}

	private Vehicule buildVehicule(
		Long id,
		String name,
		FuelType fuelType,
		int annualMileage,
		double consumption,
		double insuranceCost,
		double maintenanceCost,
		double purchasePrice,
		double resaleValue
	) {
		Vehicule vehicule = new Vehicule();
		vehicule.setId(id);
		vehicule.setName(name);
		vehicule.setFuelType(fuelType);
		vehicule.setAnnualMileage(annualMileage);
		vehicule.setConsumption(consumption);
		vehicule.setInsuranceCost(insuranceCost);
		vehicule.setMaintenanceCost(maintenanceCost);
		vehicule.setPurchasePrice(purchasePrice);
		vehicule.setResaleValue(resaleValue);
		return vehicule;
	}
}
