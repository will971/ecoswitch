package com.example.springbootapp.controller.vehicule;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/immatriculation")
@Tag(name = "Immatriculation", description = "Recherche de vehicule par plaque d'immatriculation via Oscaro + Base locale de secours")
public class ImmatriculationController {

	private static final Logger logger = LoggerFactory.getLogger(ImmatriculationController.class);
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final ResourceLoader resourceLoader;
	private final Map<String, Map<String, Object>> fallbackDatabase = new HashMap<>();

	public ImmatriculationController(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
		this.objectMapper = objectMapper;
		this.resourceLoader = resourceLoader;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(4))
				.build();
		loadFallbackDatabase();
	}

	private void loadFallbackDatabase() {
		try {
			Resource resource = resourceLoader.getResource("classpath:data/fallback-plates.json");
			if (resource.exists()) {
				Map<String, Map<String, Object>> data = objectMapper.readValue(
					resource.getInputStream(),
					new TypeReference<Map<String, Map<String, Object>>>() {}
				);
				data.forEach((key, value) -> {
					String cleanKey = key.replace("-", "").replace(" ", "").toUpperCase().trim();
					fallbackDatabase.put(cleanKey, value);
				});
				logger.info("Loaded {} fallback vehicles from external JSON configuration.", fallbackDatabase.size());
			} else {
				logger.warn("Fallback database file not found at classpath:data/fallback-plates.json");
			}
		} catch (IOException e) {
			logger.error("Failed to load fallback database from JSON", e);
		}
	}

	@GetMapping("/{plaque}")
	@Operation(summary = "Rechercher un vehicule par plaque d'immatriculation")
	@ApiResponse(responseCode = "200", description = "Vehicule trouve")
	@ApiResponse(responseCode = "404", description = "Vehicule introuvable")
	public ResponseEntity<?> rechercherPlaque(@PathVariable String plaque) {
		if (plaque == null || plaque.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Le numero de plaque est manquant."));
		}

		// Nettoyer la plaque (enlever tirets, espaces, mettre en majuscule)
		String cleanPlaque = plaque.replace("-", "").replace(" ", "").toUpperCase().trim();
		logger.info("Recherche de plaque d'immatriculation pour : {} (nettoyee: {})", plaque, cleanPlaque);

		// 1. Tenter d'interroger en direct l'API Oscaro
		try {
			String url = "https://www.oscaro.com/catalog/vehicles/by_registration?registration=" + URLEncoder.encode(cleanPlaque, StandardCharsets.UTF_8);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.header("Accept", "application/json")
					.header("Referer", "https://www.oscaro.com/")
					.header("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
					.timeout(Duration.ofSeconds(3))
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				List<?> vehicles = objectMapper.readValue(response.body(), List.class);
				if (vehicles != null && !vehicles.isEmpty()) {
					Map<?, ?> vehicle = (Map<?, ?>) vehicles.get(0);
					
					// Extraction et normalisation des donnees
					String make = (String) vehicle.get("make");
					String model = (String) vehicle.get("model");
					String version = (String) vehicle.get("version");
					String fuel = (String) vehicle.get("fuel");
					
					String brandName = make != null ? make : "Marque inconnue";
					String modelName = model != null ? model : "Modele inconnu";
					String versionName = version != null ? version : "";
					
					String formattedName = String.format("%s %s %s", brandName, modelName, versionName).trim().replaceAll(" +", " ");
					
					// Mappage du type de carburant et de la consommation par defaut
					String fuelType = "PETROL";
					double consumption = 6.2;
					double insurance = 600.0;
					double maintenance = 400.0;
					double resale = 6000.0;

					if (fuel != null) {
						String cleanFuel = fuel.toLowerCase();
						if (cleanFuel.contains("elec") || cleanFuel.contains("volt")) {
							fuelType = "ELECTRIC";
							consumption = 16.5;
							insurance = 550.0;
							maintenance = 200.0;
							resale = 15000.0;
						} else if (cleanFuel.contains("hybr")) {
							fuelType = "HYBRID";
							consumption = 4.4;
							insurance = 580.0;
							maintenance = 320.0;
							resale = 12000.0;
						} else if (cleanFuel.contains("dies") || cleanFuel.contains("gazo")) {
							fuelType = "DIESEL";
							consumption = 4.9;
							insurance = 620.0;
							maintenance = 450.0;
							resale = 8000.0;
						}
					}

					logger.info("Vehicule identifie via Oscaro : {} ({})", formattedName, fuelType);

					return ResponseEntity.ok(Map.of(
							"name", formattedName,
							"fuelType", fuelType,
							"consumption", consumption,
							"annualMileage", 15000,
							"insuranceCost", insurance,
							"maintenanceCost", maintenance,
							"resaleValue", resale,
							"source", "OSCARO"
					));
				}
			} else {
				logger.warn("L'API Oscaro a repondu avec le code : {}", response.statusCode());
			}

		} catch (IOException | InterruptedException e) {
			logger.warn("Echec de l'interrogation en direct d'Oscaro (reseau ou blocage), bascule sur le dictionnaire local. Message: {}", e.getMessage());
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
		}

		// 2. Bascule sur la base locale en cas d'erreur ou d'absence de reponse d'Oscaro
		if (fallbackDatabase.containsKey(cleanPlaque)) {
			Map<String, Object> localCar = fallbackDatabase.get(cleanPlaque);
			logger.info("Vehicule identifie via le dictionnaire de secours local : {} (Plaque: {})", localCar.get("name"), cleanPlaque);
			
			// Retourner une copie avec la source explicite
			Map<String, Object> responseMap = new HashMap<>(localCar);
			responseMap.put("source", "LOCAL_FALLBACK");
			return ResponseEntity.ok(responseMap);
		}

		logger.warn("Aucun vehicule trouve pour la plaque : {}", cleanPlaque);
		return ResponseEntity.status(404).body(Map.of(
				"error", "Plaque d'immatriculation introuvable. Veuillez saisir manuellement les informations."
		));
	}
}
