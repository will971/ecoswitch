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

	@Test
	void shouldCalculateWeightedFuelPriceForElectric() {
		Vehicule electric = buildVehicule(1L, "Tesla", FuelType.ELECTRIC, 18000, 15.0, 600, 300, 40000, 0);
		double basePrice = 0.25; // 0.25 €/kWh
		double weightedPrice = costCalculationService.resolveWeightedFuelPrice(electric, basePrice, 0.8); // 80% home, 20% highway
		
		// 80% * 0.25 + 20% * 0.65 = 0.20 + 0.13 = 0.33
		assertEquals(0.33, weightedPrice, 0.001);
	}

	@Test
	void shouldCalculateBonusEcologique() {
		Vehicule electricCheap = buildVehicule(1L, "Zoe", FuelType.ELECTRIC, 15000, 17.2, 500, 200, 35000, 0);
		Vehicule electricExpensive = buildVehicule(2L, "Tesla S", FuelType.ELECTRIC, 15000, 19.0, 900, 400, 85000, 0);
		Vehicule petrol = buildVehicule(3L, "Clio", FuelType.PETROL, 15000, 5.5, 500, 300, 20000, 0);

		// Zoe with low income <= 15400
		assertEquals(7000.0, costCalculationService.calculateBonusEcologique(electricCheap, 12000.0));
		// Zoe with standard income
		assertEquals(4000.0, costCalculationService.calculateBonusEcologique(electricCheap, 25000.0));
		// Tesla (too expensive > 47000)
		assertEquals(0.0, costCalculationService.calculateBonusEcologique(electricExpensive, 12000.0));
		// Petrol (not electric)
		assertEquals(0.0, costCalculationService.calculateBonusEcologique(petrol, 12000.0));
	}

	@Test
	void shouldCalculateCO2Emissions() {
		Vehicule petrol = buildVehicule(1L, "Clio", FuelType.PETROL, 10000, 6.0, 0, 0, 0, 0);
		Vehicule electric = buildVehicule(2L, "Zoe", FuelType.ELECTRIC, 10000, 15.0, 0, 0, 0, 0);

		// 6.0 * 23.0 = 138.0 g/km
		assertEquals(138.0, costCalculationService.calculateCO2EmissionsGPerKm(petrol), 0.001);
		// 15.0 * 0.5 = 7.5 g/km
		assertEquals(7.5, costCalculationService.calculateCO2EmissionsGPerKm(electric), 0.001);

		// Annual CO2: 138g * 10000km / 1000 = 1380 kg
		assertEquals(1380.0, costCalculationService.calculateAnnualCO2Kg(petrol), 0.001);
	}

	@Test
	void shouldFallbackToPetrolForHybridWhenHybridPriceMissing() {
		Vehicule hybrid = buildVehicule(4L, "Prius", FuelType.HYBRID, 12000, 4.2, 550, 350, 20000, 0);
		Map<String, Double> prices = Map.of("PETROL", 1.9);
		double price = costCalculationService.resolveFuelPrice(hybrid, prices);
		assertEquals(1.9, price, 0.001);
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
