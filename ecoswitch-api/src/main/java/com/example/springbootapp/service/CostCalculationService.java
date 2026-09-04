package com.example.springbootapp.service;

import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CostCalculationService {

	/**
	 * Calcule le coût annuel d'énergie/carburant.
	 * Prise en compte de la dissociation PHEV (trajets quotidiens en électrique vs longs trajets en thermique).
	 */
	public double calculateAnnualFuelCost(Vehicule vehicle, double fuelPrice) {
		return calculateAnnualFuelCost(vehicle, fuelPrice, null, null, null);
	}

	public double calculateAnnualFuelCost(Vehicule vehicle, double fuelPrice, Map<String, Double> fuelPricesByType, Double homeChargingRatio, Integer autonomieWltpKm) {
		if (vehicle.getFuelType() == FuelType.PLUGIN_HYBRID) {
			return calculatePhevAnnualCost(vehicle, fuelPricesByType, homeChargingRatio, autonomieWltpKm);
		}

		return (vehicle.getAnnualMileage() / 100.0)
			* vehicle.getConsumption()
			* fuelPrice;
	}

	/**
	 * Calcul précis pour les Hybrides Rechargeables (PHEV) :
	 * - Distance journalière = Kilométrage annuel / 250 jours
	 * - Part électrique = min(1.0, Autonomie WLTP / Distance journalière) * Taux de recharge
	 * - Part thermique = 1.0 - Part électrique
	 */
	public double calculatePhevAnnualCost(Vehicule vehicle, Map<String, Double> fuelPricesByType, Double homeChargingRatio, Integer autonomieWltpKm) {
		int annualMileage = vehicle.getAnnualMileage() > 0 ? vehicle.getAnnualMileage() : 15000;
		double dailyDistance = annualMileage / 250.0; // km/jour ouvrable moyen

		int electricRange = (autonomieWltpKm != null && autonomieWltpKm > 0) ? autonomieWltpKm : 55; // 55 km WLTP moyen par défaut
		double chargeRate = homeChargingRatio != null ? Math.max(0.2, homeChargingRatio) : 0.85;

		double electricShare = Math.min(1.0, (double) electricRange / Math.max(1.0, dailyDistance)) * chargeRate;
		double thermalShare = Math.max(0.0, 1.0 - electricShare);

		double electricMileage = annualMileage * electricShare;
		double thermalMileage = annualMileage * thermalShare;

		// Conso électrique moyenne PHEV ~ 18 kWh/100km
		double electricConso = 18.0;
		// Conso thermique batterie vide ~ 6.5 L/100km ou valeur véhicule
		double thermalConso = vehicle.getConsumption() > 0 ? vehicle.getConsumption() : 6.5;

		double electricPrice = (fuelPricesByType != null && fuelPricesByType.containsKey("ELECTRIC")) 
				? fuelPricesByType.get("ELECTRIC") : 0.2516;
		double petrolPrice = (fuelPricesByType != null && fuelPricesByType.containsKey("PETROL")) 
				? fuelPricesByType.get("PETROL") : 2.04;

		double electricCost = (electricMileage / 100.0) * electricConso * electricPrice;
		double thermalCost = (thermalMileage / 100.0) * thermalConso * petrolPrice;

		return electricCost + thermalCost;
	}

	public double calculateAnnualCost(Vehicule vehicle, double fuelPrice) {
		return calculateAnnualCost(vehicle, fuelPrice, null, null, null);
	}

	public double calculateAnnualCost(Vehicule vehicle, double fuelPrice, Map<String, Double> fuelPricesByType, Double homeChargingRatio, Integer autonomieWltpKm) {
		double fuelCost = calculateAnnualFuelCost(vehicle, fuelPrice, fuelPricesByType, homeChargingRatio, autonomieWltpKm);

		return fuelCost
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
		if (("HYBRID".equals(fuelType) || "PLUGIN_HYBRID".equals(fuelType)) && !fuelPricesByType.containsKey(fuelType)) {
			Double petrolPrice = fuelPricesByType.get("PETROL");
			if (petrolPrice == null) {
				throw new IllegalArgumentException("Prix d'energie (Essence) manquant pour le calcul Hybride.");
			}
			return petrolPrice;
		}
		Double fuelPrice = fuelPricesByType.get(fuelType);
		if (fuelPrice == null) {
			Double petrolPrice = fuelPricesByType.get("PETROL");
			if (petrolPrice != null) return petrolPrice;
			throw new IllegalArgumentException("Prix d'energie manquant pour: " + fuelType);
		}
		return fuelPrice;
	}

	public double resolveWeightedFuelPrice(Vehicule vehicle, double baseFuelPrice, Double homeChargingRatio) {
		if (vehicle.getFuelType() == FuelType.ELECTRIC && homeChargingRatio != null) {
			double homeRatio = Math.max(0.0, Math.min(1.0, homeChargingRatio));
			double fastRatio = 1.0 - homeRatio;
			double fastPrice = 0.55; // tarif moyen borne rapide publique
			return (homeRatio * baseFuelPrice) + (fastRatio * fastPrice);
		}
		return baseFuelPrice;
	}

	public double calculateBonusEcologique(Vehicule targetVehicle, Double taxIncome) {
		if (targetVehicle.getFuelType() != FuelType.ELECTRIC) {
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
		if (targetVehicle.getFuelType() != FuelType.ELECTRIC && 
			targetVehicle.getFuelType() != FuelType.HYBRID &&
			targetVehicle.getFuelType() != FuelType.PLUGIN_HYBRID) {
			return 0.0;
		}
		if (currentVehicle.getFuelType() != FuelType.PETROL && 
			currentVehicle.getFuelType() != FuelType.DIESEL) {
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
			case PLUGIN_HYBRID:
				return vehicle.getConsumption() * 9.5;
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
