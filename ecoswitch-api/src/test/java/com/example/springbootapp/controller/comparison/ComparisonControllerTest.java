package com.example.springbootapp.controller.comparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.service.CostCalculationService;
import com.example.springbootapp.service.VehiculeService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.springbootapp.business.comparison.ComparisonBusiness;

class ComparisonControllerTest {

	private final VehiculeService vehiculeService = Mockito.mock(VehiculeService.class);
	private final CostCalculationService costCalculationService = new CostCalculationService();
	private final ComparisonBusiness comparisonBusiness = new ComparisonBusiness(vehiculeService, costCalculationService);
	private final ComparisonController comparisonController = new ComparisonController(comparisonBusiness);

	@Test
	void shouldCalculateDirectProfitabilitySuccessfully() {
		Vehicule current = buildVehicule("Current Car", FuelType.PETROL, 18000, 7.5, 850.0, 600.0, 0.0, 12000.0);
		Vehicule target = buildVehicule("Target Electric Car", FuelType.ELECTRIC, 18000, 16.5, 650.0, 350.0, 25000.0, 0.0);

		Map<String, Double> fuelPrices = Map.of(
			"PETROL", 1.9,
			"ELECTRIC", 0.23
		);

		ComparisonController.DirectProfitabilityRequest request = new ComparisonController.DirectProfitabilityRequest(
			current,
			target,
			fuelPrices,
			10,
			0.0,
			1.0,
			25000.0,
			false,
			false,
			null
		);

		ComparisonController.DirectProfitabilityResponse response = comparisonController.compareDirect(request);

		assertNotNull(response);
		assertEquals(4, response.breakEvenYear());
		assertEquals(9000.0, response.switchInvestment());
		assertEquals(4015.0, response.currentAnnualCost());
		assertEquals(1683.1, response.targetAnnualCost());
		assertEquals(2331.9, response.annualSavings(), 0.001);
		assertNotNull(response.recommendations());
	}

	@Test
	void shouldThrowExceptionWhenMissingVehicle() {
		ComparisonController.DirectProfitabilityRequest request = new ComparisonController.DirectProfitabilityRequest(
			null,
			null,
			Map.of(),
			10,
			null,
			null,
			null,
			null,
			null,
			null
		);

		assertThrows(IllegalArgumentException.class, () -> comparisonController.compareDirect(request));
	}

	private Vehicule buildVehicule(
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
