package com.example.springbootapp.controller.comparison;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.service.CostCalculationService;
import com.example.springbootapp.service.VehiculeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/comparisons")
@Tag(name = "Comparaison", description = "Comparer les vehicules et identifier la rentabilite d'un changement")
public class ComparisonController {

	private static final int DEFAULT_MAX_YEARS = 15;

	private final VehiculeService vehiculeService;
	private final CostCalculationService costCalculationService;

	public ComparisonController(VehiculeService vehiculeService, CostCalculationService costCalculationService) {
		this.vehiculeService = vehiculeService;
		this.costCalculationService = costCalculationService;
	}

	@PostMapping("/profitability")
	@Operation(summary = "Comparer plusieurs vehicules et calculer le seuil de rentabilite")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Comparaison calculee"),
			@ApiResponse(responseCode = "400", description = "Donnees invalides")
	})
	public ProfitabilityComparisonResponse compareProfitability(@RequestBody ProfitabilityComparisonRequest request) {
		validateRequest(request);

		Vehicule currentVehicle = vehiculeService.findById(request.currentVehicleId());
		Integer requestedMaxYears = request.maxYears();
		int maxYears = requestedMaxYears == null ? DEFAULT_MAX_YEARS : requestedMaxYears;
		List<VehicleProfitability> alternatives = new ArrayList<>();

		for (Long targetVehicleId : request.targetVehicleIds()) {
			if (targetVehicleId.equals(request.currentVehicleId())) {
				continue;
			}
			Vehicule targetVehicle = vehiculeService.findById(targetVehicleId);
			double currentAnnualCost = costCalculationService.calculateAnnualCost(
					currentVehicle,
					costCalculationService.resolveFuelPrice(currentVehicle, request.fuelPricesByType()));
			double targetAnnualCost = costCalculationService.calculateAnnualCost(
					targetVehicle,
					costCalculationService.resolveFuelPrice(targetVehicle, request.fuelPricesByType()));
			double annualSavings = currentAnnualCost - targetAnnualCost;
			double switchInvestment = Math.max(0.0, targetVehicle.getPurchasePrice() - currentVehicle.getResaleValue());
			double immediateRepairCost = request.immediateRepairCost() == null ? 0.0 : request.immediateRepairCost();
			double totalCostDeltaAtHorizon = costCalculationService.calculateSwitchCostAtYear(
					currentVehicle,
					targetVehicle,
					maxYears,
					request.fuelPricesByType(),
					immediateRepairCost);
			Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(
					currentVehicle,
					targetVehicle,
					maxYears,
					request.fuelPricesByType(),
					immediateRepairCost);

			alternatives.add(
					new VehicleProfitability(
							targetVehicle.getId(),
							targetVehicle.getName(),
							switchInvestment,
							currentAnnualCost,
							targetAnnualCost,
							annualSavings,
							breakEvenYear,
							totalCostDeltaAtHorizon));
		}

		alternatives.sort(
				Comparator
						.comparing((VehicleProfitability item) -> item.breakEvenYear() == null)
						.thenComparing(this::breakEvenSortingValue)
						.thenComparing(VehicleProfitability::totalCostDeltaAtHorizon));

		return new ProfitabilityComparisonResponse(
				currentVehicle.getId(),
				currentVehicle.getName(),
				maxYears,
				alternatives);
	}

	@PostMapping("/profitability/custom")
	@Operation(summary = "Comparer plusieurs véhicules à un véhicule actuel personnalisé (ex: issu du profil)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Comparaison calculée"),
			@ApiResponse(responseCode = "400", description = "Données invalides")
	})
	public ProfitabilityComparisonResponse compareCustomProfitability(@RequestBody CustomProfitabilityComparisonRequest request) {
		if (request == null || request.currentVehicle() == null || request.targetVehicleIds() == null || request.targetVehicleIds().isEmpty()) {
			throw new IllegalArgumentException("Requête invalide ou incomplète.");
		}

		Vehicule currentVehicle = request.currentVehicle();
		Integer requestedMaxYears = request.maxYears();
		int maxYears = requestedMaxYears == null ? DEFAULT_MAX_YEARS : requestedMaxYears;
		List<VehicleProfitability> alternatives = new ArrayList<>();

		for (Long targetVehicleId : request.targetVehicleIds()) {
			Vehicule targetVehicle = vehiculeService.findById(targetVehicleId);
			double currentAnnualCost = costCalculationService.calculateAnnualCost(
					currentVehicle,
					costCalculationService.resolveFuelPrice(currentVehicle, request.fuelPricesByType()));
			double targetAnnualCost = costCalculationService.calculateAnnualCost(
					targetVehicle,
					costCalculationService.resolveFuelPrice(targetVehicle, request.fuelPricesByType()));
			double annualSavings = currentAnnualCost - targetAnnualCost;
			double switchInvestment = Math.max(0.0, targetVehicle.getPurchasePrice() - currentVehicle.getResaleValue());
			double immediateRepairCost = request.immediateRepairCost() == null ? 0.0 : request.immediateRepairCost();
			double totalCostDeltaAtHorizon = costCalculationService.calculateSwitchCostAtYear(
					currentVehicle,
					targetVehicle,
					maxYears,
					request.fuelPricesByType(),
					immediateRepairCost);
			Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(
					currentVehicle,
					targetVehicle,
					maxYears,
					request.fuelPricesByType(),
					immediateRepairCost);

			alternatives.add(
					new VehicleProfitability(
							targetVehicle.getId(),
							targetVehicle.getName(),
							switchInvestment,
							currentAnnualCost,
							targetAnnualCost,
							annualSavings,
							breakEvenYear,
							totalCostDeltaAtHorizon));
		}

		alternatives.sort(
				Comparator
						.comparing((VehicleProfitability item) -> item.breakEvenYear() == null)
						.thenComparing(this::breakEvenSortingValue)
						.thenComparing(VehicleProfitability::totalCostDeltaAtHorizon));

		return new ProfitabilityComparisonResponse(
				currentVehicle.getId() != null ? currentVehicle.getId() : -1L,
				currentVehicle.getName(),
				maxYears,
				alternatives);
	}

	@PostMapping("/profitability/direct")
	@Operation(summary = "Calculer la rentabilite en comparant deux vehicules saisis a la volee")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Comparaison calculee"),
			@ApiResponse(responseCode = "400", description = "Donnees invalides")
	})
	public DirectProfitabilityResponse compareDirect(@RequestBody DirectProfitabilityRequest request) {
		validateDirectRequest(request);

		Vehicule current = request.currentVehicle();
		Vehicule target = request.targetVehicle();
		Integer requestedMaxYears = request.maxYears();
		int maxYears = requestedMaxYears == null ? DEFAULT_MAX_YEARS : requestedMaxYears;
		double immediateRepairCost = request.immediateRepairCost() == null ? 0.0 : request.immediateRepairCost();

		double rawCurrentPrice = costCalculationService.resolveFuelPrice(current, request.fuelPricesByType());
		double currentPrice = costCalculationService.resolveWeightedFuelPrice(current, rawCurrentPrice, request.homeChargingRatio());

		double rawTargetPrice = costCalculationService.resolveFuelPrice(target, request.fuelPricesByType());
		double targetPrice = costCalculationService.resolveWeightedFuelPrice(target, rawTargetPrice, request.homeChargingRatio());

		double currentAnnualCost = costCalculationService.calculateAnnualCost(current, currentPrice);
		double targetAnnualCost = costCalculationService.calculateAnnualCost(target, targetPrice);
		double annualSavings = currentAnnualCost - targetAnnualCost;

		double bonusEcologique = costCalculationService.calculateBonusEcologique(target, request.taxIncome());
		double primeConversion = costCalculationService.calculatePrimeConversion(current, target, request.scrapVehicle(), request.taxIncome());
		double totalSubsidies = bonusEcologique + primeConversion;

		double rawSwitchInvestment = Math.max(0.0, target.getPurchasePrice() - current.getResaleValue());
		double switchInvestment = Math.max(0.0, rawSwitchInvestment - totalSubsidies);

		double totalCostDeltaAtHorizon = costCalculationService.calculateSwitchCostAtYear(
				current,
				target,
				maxYears,
				currentPrice,
				targetPrice,
				switchInvestment,
				immediateRepairCost);

		Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(
				current,
				target,
				maxYears,
				currentPrice,
				targetPrice,
				switchInvestment,
				immediateRepairCost);

		// CO2 calculations
		double currentAnnualCO2 = costCalculationService.calculateAnnualCO2Kg(current);
		double targetAnnualCO2 = costCalculationService.calculateAnnualCO2Kg(target);
		double annualCO2Savings = currentAnnualCO2 - targetAnnualCO2;

		// Monthly / Leasing calculations
		double leasingMonthlyPrice = 0.0;
		if (request.isLeasing() != null && request.isLeasing()) {
			if (request.customLeasingMonthlyPrice() != null && request.customLeasingMonthlyPrice() > 0) {
				leasingMonthlyPrice = request.customLeasingMonthlyPrice();
			} else {
				leasingMonthlyPrice = target.getPurchasePrice() * 0.0125; // 1.25% estimation standard LOA
			}
		}
		double currentMonthlyTotalCost = currentAnnualCost / 12.0;
		double targetMonthlyTotalCost = leasingMonthlyPrice + (targetAnnualCost / 12.0);
		double monthlySavings = currentMonthlyTotalCost - targetMonthlyTotalCost;

		// 2. Moteur de recommandation intelligente (SaaS Advisor)
		List<Vehicule> catalog = vehiculeService.findAll();
		List<VehicleProfitability> recommendations = new ArrayList<>();

		for (Vehicule catalogVehicle : catalog) {
			// On ignore le véhicule actuel ou la cible elle-même
			if (catalogVehicle.getName().equalsIgnoreCase(current.getName()) || 
				catalogVehicle.getName().equalsIgnoreCase(target.getName())) {
				continue;
			}

			try {
				double catFuelPrice = costCalculationService.resolveFuelPrice(catalogVehicle, request.fuelPricesByType());
				double weightedCatPrice = costCalculationService.resolveWeightedFuelPrice(catalogVehicle, catFuelPrice, request.homeChargingRatio());

				double catAnnualCost = costCalculationService.calculateAnnualCost(catalogVehicle, weightedCatPrice);
				double catSavings = currentAnnualCost - catAnnualCost;

				double catBonus = costCalculationService.calculateBonusEcologique(catalogVehicle, request.taxIncome());
				double catPrime = costCalculationService.calculatePrimeConversion(current, catalogVehicle, request.scrapVehicle(), request.taxIncome());
				double catTotalSubsidies = catBonus + catPrime;

				double catRawSwitch = Math.max(0.0, catalogVehicle.getPurchasePrice() - current.getResaleValue());
				double catSwitchInvestment = Math.max(0.0, catRawSwitch - catTotalSubsidies);

				double catTotalCostDelta = costCalculationService.calculateSwitchCostAtYear(
						current,
						catalogVehicle,
						maxYears,
						currentPrice,
						weightedCatPrice,
						catSwitchInvestment,
						immediateRepairCost);

				Integer catBreakEvenYear = costCalculationService.calculateBreakEvenYear(
						current,
						catalogVehicle,
						maxYears,
						currentPrice,
						weightedCatPrice,
						catSwitchInvestment,
						immediateRepairCost);

				// On ne suggère que les véhicules du catalogue qui sont rentables OU offrent des économies
				if (catBreakEvenYear != null || catSavings > 0) {
					recommendations.add(new VehicleProfitability(
							catalogVehicle.getId(),
							catalogVehicle.getName(),
							catSwitchInvestment,
							currentAnnualCost,
							catAnnualCost,
							catSavings,
							catBreakEvenYear,
							catTotalCostDelta));
				}
			} catch (Exception ignored) {
				// Ignorer les erreurs d'énergie manquante dans la map pour certains véhicules
			}
		}

		// Trier par rentabilité
		recommendations.sort(
				Comparator
						.comparing((VehicleProfitability item) -> item.breakEvenYear() == null)
						.thenComparing(this::breakEvenSortingValue)
						.thenComparing(VehicleProfitability::totalCostDeltaAtHorizon));

		List<VehicleProfitability> topRecommendations = recommendations.stream()
				.limit(3)
				.collect(Collectors.toList());

		return new DirectProfitabilityResponse(
				currentAnnualCost,
				targetAnnualCost,
				annualSavings,
				switchInvestment,
				breakEvenYear,
				totalCostDeltaAtHorizon,
				topRecommendations,
				bonusEcologique,
				primeConversion,
				totalSubsidies,
				currentAnnualCO2,
				targetAnnualCO2,
				annualCO2Savings,
				currentMonthlyTotalCost,
				targetMonthlyTotalCost,
				monthlySavings);
	}

	private void validateDirectRequest(DirectProfitabilityRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("La requete est obligatoire.");
		}
		if (request.currentVehicle() == null) {
			throw new IllegalArgumentException("Le vehicule actuel est obligatoire.");
		}
		if (request.targetVehicle() == null) {
			throw new IllegalArgumentException("Le vehicule cible est obligatoire.");
		}
		if (request.currentVehicle().getFuelType() == null) {
			throw new IllegalArgumentException("Le type de carburant du vehicule actuel est obligatoire.");
		}
		if (request.targetVehicle().getFuelType() == null) {
			throw new IllegalArgumentException("Le type de carburant du vehicule cible est obligatoire.");
		}
		if (request.maxYears() != null && request.maxYears() <= 0) {
			throw new IllegalArgumentException("maxYears doit etre superieur a 0.");
		}
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ApiResponse(responseCode = "400", description = "Erreur de validation", content = @Content(schema = @Schema(implementation = Map.class)))
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", exception.getMessage()));
	}

	private void validateRequest(ProfitabilityComparisonRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("La requete est obligatoire.");
		}
		if (request.currentVehicleId() == null) {
			throw new IllegalArgumentException("L'id du vehicule actuel est obligatoire.");
		}
		if (request.targetVehicleIds() == null || request.targetVehicleIds().isEmpty()) {
			throw new IllegalArgumentException("Au moins un vehicule cible est obligatoire.");
		}
		if (request.maxYears() != null && request.maxYears() <= 0) {
			throw new IllegalArgumentException("maxYears doit etre superieur a 0.");
		}
	}

	private Integer breakEvenSortingValue(VehicleProfitability item) {
		Integer breakEvenYear = item.breakEvenYear();
		return breakEvenYear == null ? Integer.MAX_VALUE : breakEvenYear;
	}

	public record ProfitabilityComparisonRequest(
			Long currentVehicleId,
			List<Long> targetVehicleIds,
			Map<String, Double> fuelPricesByType,
			Integer maxYears,
			Double immediateRepairCost) {
	}

	public record CustomProfitabilityComparisonRequest(
			Vehicule currentVehicle,
			List<Long> targetVehicleIds,
			Map<String, Double> fuelPricesByType,
			Integer maxYears,
			Double immediateRepairCost) {
	}

	public record ProfitabilityComparisonResponse(
			Long currentVehicleId,
			String currentVehicleName,
			int maxYears,
			List<VehicleProfitability> alternatives) {
	}

	public record VehicleProfitability(
			Long vehicleId,
			String vehicleName,
			double switchInvestment,
			double currentAnnualCost,
			double targetAnnualCost,
			double annualSavings,
			Integer breakEvenYear,
			double totalCostDeltaAtHorizon) {
	}

	public record DirectProfitabilityRequest(
			Vehicule currentVehicle,
			Vehicule targetVehicle,
			Map<String, Double> fuelPricesByType,
			Integer maxYears,
			Double immediateRepairCost,
			Double homeChargingRatio,
			Double taxIncome,
			Boolean scrapVehicle,
			Boolean isLeasing,
			Double customLeasingMonthlyPrice) {
	}

	public record DirectProfitabilityResponse(
			double currentAnnualCost,
			double targetAnnualCost,
			double annualSavings,
			double switchInvestment,
			Integer breakEvenYear,
			double totalCostDeltaAtHorizon,
			List<VehicleProfitability> recommendations,
			double bonusEcologique,
			double primeConversion,
			double totalSubsidies,
			double currentAnnualCO2,
			double targetAnnualCO2,
			double annualCO2Savings,
			double currentMonthlyTotalCost,
			double targetMonthlyTotalCost,
			double monthlySavings) {
	}
}
