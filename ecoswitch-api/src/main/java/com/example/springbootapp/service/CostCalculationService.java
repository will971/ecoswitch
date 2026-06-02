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
		if ("HYBRID".equals(fuelType) && !fuelPricesByType.containsKey("HYBRID")) {
			Double petrolPrice = fuelPricesByType.get("PETROL");
			if (petrolPrice == null) {
				throw new IllegalArgumentException("Prix d'energie (Essence) manquant pour le calcul Hybride.");
			}
			return petrolPrice;
		}
		Double fuelPrice = fuelPricesByType.get(fuelType);
		if (fuelPrice == null) {
			throw new IllegalArgumentException("Prix d'energie manquant pour: " + fuelType);
		}
		return fuelPrice;
	}

	public double resolveWeightedFuelPrice(Vehicule vehicle, double baseFuelPrice, Double homeChargingRatio) {
		if (vehicle.getFuelType() == com.example.springbootapp.model.entity.FuelType.ELECTRIC && homeChargingRatio != null) {
			double homeRatio = Math.max(0.0, Math.min(1.0, homeChargingRatio));
			double fastRatio = 1.0 - homeRatio;
			double fastPrice = 0.65; // tarif moyen borne rapide publique
			return (homeRatio * baseFuelPrice) + (fastRatio * fastPrice);
		}
		return baseFuelPrice;
	}

	public double calculateBonusEcologique(Vehicule targetVehicle, Double taxIncome) {
		if (targetVehicle.getFuelType() != com.example.springbootapp.model.entity.FuelType.ELECTRIC) {
			return 0.0;
		}
		if (targetVehicle.getPurchasePrice() > 47000.0) {
			return 0.0;
		}
		// RFR/part <= 15 400 € -> 7000 €, sinon 4000 €
		if (taxIncome != null && taxIncome <= 15400.0) {
			return 7000.0;
		}
		return 4000.0;
	}

	public double calculatePrimeConversion(Vehicule currentVehicle, Vehicule targetVehicle, Boolean scrapVehicle, Double taxIncome) {
		if (scrapVehicle == null || !scrapVehicle) {
			return 0.0;
		}
		if (targetVehicle.getFuelType() != com.example.springbootapp.model.entity.FuelType.ELECTRIC && 
			targetVehicle.getFuelType() != com.example.springbootapp.model.entity.FuelType.HYBRID) {
			return 0.0;
		}
		if (currentVehicle.getFuelType() != com.example.springbootapp.model.entity.FuelType.PETROL && 
			currentVehicle.getFuelType() != com.example.springbootapp.model.entity.FuelType.DIESEL) {
			return 0.0;
		}
		if (taxIncome != null && taxIncome <= 15400.0) {
			return 3000.0;
		}
		return 1500.0;
	}

	public double calculateCO2EmissionsGPerKm(Vehicule vehicle) {
		if (vehicle.getConsumption() <= 0) {
			return 0.0;
		}
		switch (vehicle.getFuelType()) {
			case PETROL:
				return vehicle.getConsumption() * 23.0;
			case DIESEL:
				return vehicle.getConsumption() * 26.4;
			case HYBRID:
				return vehicle.getConsumption() * 20.0;
			case ELECTRIC:
				return vehicle.getConsumption() * 0.5;
			default:
				return 0.0;
		}
	}

	public double calculateAnnualCO2Kg(Vehicule vehicle) {
		double gPerKm = calculateCO2EmissionsGPerKm(vehicle);
		return (gPerKm * vehicle.getAnnualMileage()) / 1000.0;
	}

	public double calculateSwitchCostAtYear(
		Vehicule currentVehicle,
		Vehicule targetVehicle,
		int year,
		double currentFuelPrice,
		double targetFuelPrice,
		double switchInvestment,
		double immediateRepairCost
	) {
		double currentAnnualCost = calculateAnnualCost(currentVehicle, currentFuelPrice);
		double targetAnnualCost = calculateAnnualCost(targetVehicle, targetFuelPrice);
		double currentTotalFromNow = immediateRepairCost + (currentAnnualCost * year);
		double targetTotalFromNow = switchInvestment + (targetAnnualCost * year);
		return targetTotalFromNow - currentTotalFromNow;
	}

	public Integer calculateBreakEvenYear(
		Vehicule currentVehicle,
		Vehicule targetVehicle,
		int maxYears,
		double currentFuelPrice,
		double targetFuelPrice,
		double switchInvestment,
		double immediateRepairCost
	) {
		for (int year = 1; year <= maxYears; year++) {
			double costDelta = calculateSwitchCostAtYear(
				currentVehicle, targetVehicle, year, 
				currentFuelPrice, targetFuelPrice, 
				switchInvestment, immediateRepairCost);
			if (costDelta <= 0.0) {
				return year;
			}
		}
		return null;
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