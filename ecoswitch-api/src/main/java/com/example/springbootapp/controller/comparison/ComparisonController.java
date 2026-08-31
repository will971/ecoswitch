package com.example.springbootapp.controller.comparison;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.business.comparison.ComparisonBusiness;
import com.example.springbootapp.service.AiAdvisorService;
import com.example.springbootapp.service.FuelPriceLiveService;
import org.springframework.web.bind.annotation.GetMapping;

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

	private final ComparisonBusiness comparisonBusiness;
	private final AiAdvisorService aiAdvisorService;
	private final FuelPriceLiveService fuelPriceLiveService;

	public ComparisonController(ComparisonBusiness comparisonBusiness,
			AiAdvisorService aiAdvisorService,
			FuelPriceLiveService fuelPriceLiveService) {
		this.comparisonBusiness = comparisonBusiness;
		this.aiAdvisorService = aiAdvisorService;
		this.fuelPriceLiveService = fuelPriceLiveService;
	}

	@GetMapping("/fuel-prices/live")
	@Operation(summary = "Obtenir les prix réels des carburants en temps réel (Open Data gouv + IA)")
	@ApiResponse(responseCode = "200", description = "Prix des carburants en direct et conseils IA")
	public FuelPriceLiveService.FuelPricesLiveResponse getLiveFuelPrices() {
		return fuelPriceLiveService.getLiveFuelPrices();
	}

	@PostMapping("/ai-advisor")
	@Operation(summary = "Générer une synthèse et un diagnostic IA personnalisé via Gemini")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Synthèse IA générée"),
			@ApiResponse(responseCode = "400", description = "Données invalides")
	})
	public AiAdvisorService.AiAdvisorResponse getAiAdvisorSummary(@RequestBody AiAdvisorService.AiAdvisorRequest request) {
		return aiAdvisorService.generateAdvice(request);
	}

	@PostMapping("/profitability")
	@Operation(summary = "Comparer plusieurs vehicules et calculer le seuil de rentabilite")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Comparaison calculee"),
			@ApiResponse(responseCode = "400", description = "Donnees invalides")
	})
	public ProfitabilityComparisonResponse compareProfitability(@RequestBody ProfitabilityComparisonRequest request) {
		return comparisonBusiness.compareProfitability(request);
	}

	@PostMapping("/profitability/custom")
	@Operation(summary = "Comparer plusieurs véhicules à un véhicule actuel personnalisé (ex: issu du profil)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Comparaison calculée"),
			@ApiResponse(responseCode = "400", description = "Données invalides")
	})
	public ProfitabilityComparisonResponse compareCustomProfitability(@RequestBody CustomProfitabilityComparisonRequest request) {
		return comparisonBusiness.compareCustomProfitability(request);
	}

	@PostMapping("/profitability/direct")
	@Operation(summary = "Calculer la rentabilite en comparant deux vehicules saisis a la volee")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Comparaison calculee"),
			@ApiResponse(responseCode = "400", description = "Donnees invalides")
	})
	public DirectProfitabilityResponse compareDirect(@RequestBody DirectProfitabilityRequest request) {
		return comparisonBusiness.compareDirect(request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ApiResponse(responseCode = "400", description = "Erreur de validation", content = @Content(schema = @Schema(implementation = Map.class)))
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", exception.getMessage()));
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
