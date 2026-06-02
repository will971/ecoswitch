package com.example.springbootapp.service;

import com.example.springbootapp.model.entity.Vehicule;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CostCalculationService {

	public double calculateAnnualFuelCost(Vehicule vehicle, double fuelPrice) {
		return (vehicle.getAnnualMileage() / 100.0)
			* vehicle.getConsumption()
			* fuelPrice;
	}

	public double calculateAnnualCost(Vehicule vehicle, double fuelPrice) {
		double fuelCost = calculateAnnualFuelCost(vehicle, fuelPrice);

		return fuelCost
			+ vehicle.getInsuranceCost()
			+ vehicle.getMaintenanceCost();
	}

	public double calculateTotalCost(Vehicule vehicle, int years, double fuelPrice) {
		double annualCost = calculateAnnualCost(vehicle, fuelPrice);

		return vehicle.getPurchasePrice()
			+ (annualCost * years);
	}

	public double resolveFuelPrice(Vehicule vehicle, Map<String, Double> fuelPricesByType) {
		if (fuelPricesByType == null) {
			throw new IllegalArgumentException("Les prix d'energie sont obligatoires.");
		}
		String fuelType = vehicle.getFuelType().name();
		Double fuelPrice = fuelPricesByType.get(fuelType);
		if (fuelPrice == null) {
			throw new IllegalArgumentException("Prix d'energie manquant pour: " + fuelType);
		}
		return fuelPrice;
	}

	public double calculateSwitchCostAtYear(
		Vehicule currentVehicle,
		Vehicule targetVehicle,
		int year,
		Map<String, Double> fuelPricesByType
	) {
		return calculateSwitchCostAtYear(currentVehicle, targetVehicle, year, fuelPricesByType, 0.0);
	}

	public double calculateSwitchCostAtYear(
		Vehicule currentVehicle,
		Vehicule targetVehicle,
		int year,
		Map<String, Double> fuelPricesByType,
		double immediateRepairCost
	) {
		double currentAnnualCost = calculateAnnualCost(currentVehicle, resolveFuelPrice(currentVehicle, fuelPricesByType));
		double targetAnnualCost = calculateAnnualCost(targetVehicle, resolveFuelPrice(targetVehicle, fuelPricesByType));
		double switchInvestment = Math.max(0.0, targetVehicle.getPurchasePrice() - currentVehicle.getResaleValue());

		double currentTotalFromNow = immediateRepairCost + (currentAnnualCost * year);
		double targetTotalFromNow = switchInvestment + (targetAnnualCost * year);
		return targetTotalFromNow - currentTotalFromNow;
	}

	public Integer calculateBreakEvenYear(
		Vehicule currentVehicle,
		Vehicule targetVehicle,
		int maxYears,
		Map<String, Double> fuelPricesByType
	) {
		return calculateBreakEvenYear(currentVehicle, targetVehicle, maxYears, fuelPricesByType, 0.0);
	}

	public Integer calculateBreakEvenYear(
		Vehicule currentVehicle,
		Vehicule targetVehicle,
		int maxYears,
		Map<String, Double> fuelPricesByType,
		double immediateRepairCost
	) {
		for (int year = 1; year <= maxYears; year++) {
			double costDelta = calculateSwitchCostAtYear(currentVehicle, targetVehicle, year, fuelPricesByType, immediateRepairCost);
			if (costDelta <= 0.0) {
				return year;
			}
		}
		return null;
	}
}