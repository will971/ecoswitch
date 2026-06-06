package com.example.springbootapp.controller.vehicule;

import com.example.springbootapp.service.AdemeService.AdemeVehicle;
import com.example.springbootapp.business.vehicule.AdemeBusiness;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ademe")
@Tag(name = "ADEME", description = "Recherche de vehicules par marque, modele et version via le dump ADEME")
public class AdemeController {

	private final AdemeBusiness ademeBusiness;

	public AdemeController(AdemeBusiness ademeBusiness) {
		this.ademeBusiness = ademeBusiness;
	}

	@GetMapping("/brands")
	@Operation(summary = "Lister toutes les marques uniques de véhicules")
	public ResponseEntity<List<String>> getBrands() {
		return ResponseEntity.ok(ademeBusiness.getBrands());
	}

	@GetMapping("/models")
	@Operation(summary = "Lister tous les modèles pour une marque")
	public ResponseEntity<List<String>> getModels(@RequestParam String brand) {
		return ResponseEntity.ok(ademeBusiness.getModels(brand));
	}

	@GetMapping("/versions")
	@Operation(summary = "Lister toutes les versions pour un couple marque et modèle")
	public ResponseEntity<List<AdemeVehicle>> getVersions(@RequestParam String brand, @RequestParam String model) {
		return ResponseEntity.ok(ademeBusiness.getVersions(brand, model));
	}

	@GetMapping("/vehicle")
	@Operation(summary = "Récupérer un véhicule spécifique par marque, modèle et version")
	public ResponseEntity<?> getVehicle(
			@RequestParam String brand,
			@RequestParam String model,
			@RequestParam String version) {
		return ademeBusiness.getVehicle(brand, model, version)
				.map(v -> ResponseEntity.ok(Map.of(
						"name", v.getFullName(),
						"fuelType", v.fuelType().name(),
						"consumption", v.consumption(),
						"annualMileage", v.annualMileage(),
						"insuranceCost", v.insuranceCost(),
						"maintenanceCost", v.maintenanceCost(),
						"resaleValue", v.resaleValue(),
						"purchasePrice", v.purchasePrice(),
						"source", "ADEME_DUMP"
				)))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
