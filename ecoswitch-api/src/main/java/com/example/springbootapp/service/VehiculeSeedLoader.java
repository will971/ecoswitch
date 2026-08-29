package com.example.springbootapp.service;

import com.example.springbootapp.config.AppSeedProperties;
import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
public class VehiculeSeedLoader implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(VehiculeSeedLoader.class);

	private final VehiculeService vehiculeService;
	private final ResourceLoader resourceLoader;
	private final AppSeedProperties seedProperties;
	private final ObjectMapper objectMapper;

	public VehiculeSeedLoader(
		VehiculeService vehiculeService,
		ResourceLoader resourceLoader,
		AppSeedProperties seedProperties,
		ObjectMapper objectMapper
	) {
		this.vehiculeService = vehiculeService;
		this.resourceLoader = resourceLoader;
		this.seedProperties = seedProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!vehiculeService.findAll().isEmpty()) {
			logger.info("La base contient deja des vehicules, seed ignore.");
			return;
		}

		String seedUrl = seedProperties.getVehiclesUrl();
		boolean loadedFromApi = false;

		// 1. Tenter le chargement via l'API publique (JSON)
		if (seedUrl != null && !seedUrl.isBlank()) {
			logger.info("Tentative de chargement des vehicules depuis l'API publique: {}", seedUrl);
			try {
				List<VehiculeSeedItem> seedItems = fetchVehiclesFromApi(seedUrl);
				for (VehiculeSeedItem item : seedItems) {
					vehiculeService.create(toVehicule(item));
				}
				logger.info("{} vehicules inseres avec succes depuis l'API publique JSON.", seedItems.size());
				loadedFromApi = true;
			}
			catch (Exception exception) {
				logger.warn("Echec du chargement depuis l'API publique ({}), repli sur le fichier local. Erreur: {}", 
					seedUrl, exception.getMessage());
			}
		}

		// 2. Repli sur le fichier CSV local si l'API publique n'a pas chargé les données
		if (!loadedFromApi) {
			String seedFileLocation = seedProperties.getVehiclesFile();
			if (seedFileLocation == null || seedFileLocation.isBlank()) {
				logger.info("Aucun fichier de seed local configure, demarrage avec base vide.");
				return;
			}

			logger.info("Chargement des vehicules depuis le fichier local: {}", seedFileLocation);
			Resource resource = resourceLoader.getResource(seedFileLocation);
			if (!resource.exists()) {
				logger.info("Fichier de seed local absent ({}), demarrage avec base vide.", seedFileLocation);
				return;
			}

			try {
				List<VehiculeSeedItem> seedItems = readSeedItems(resource);
				for (VehiculeSeedItem item : seedItems) {
					vehiculeService.create(toVehicule(item));
				}
				logger.info("{} vehicules inseres depuis le fichier local {}.", seedItems.size(), seedFileLocation);
			}
			catch (IOException exception) {
				logger.warn("Impossible de lire le fichier de seed local {}: {}", seedFileLocation, exception.getMessage());
			}
			catch (IllegalArgumentException exception) {
				logger.warn("Donnees de seed locales invalides dans {}: {}", seedFileLocation, exception.getMessage());
			}
		}
	}

	private List<VehiculeSeedItem> fetchVehiclesFromApi(String url) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(Duration.ofSeconds(5))
			.header("Accept", "application/json")
			.GET()
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IOException("HTTP status " + response.statusCode() + " lors de la requete API.");
		}

		return objectMapper.readValue(response.body(), new TypeReference<List<VehiculeSeedItem>>() {});
	}

	private List<VehiculeSeedItem> readSeedItems(Resource resource) throws IOException {
		try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(resource.getInputStream()))) {
			return reader.lines()
				.map(String::trim)
				.filter(line -> !line.isBlank() && !line.startsWith("#"))
				.filter(line -> !line.toLowerCase().startsWith("name, brand,") && !line.toLowerCase().startsWith("name,"))
				.map(this::parseCsvLine)
				.collect(Collectors.toList());
		}
	}

	private VehiculeSeedItem parseCsvLine(String line) {
		String[] parts = line.split(",", -1);
		if (parts.length != 12) {
			throw new IllegalArgumentException("Ligne CSV invalide: " + line);
		}
		return new VehiculeSeedItem(
			parts[0].trim(),
			parts[1].trim(),
			parts[2].trim(),
			parts[3].trim(),
			parts[4].trim(),
			Double.parseDouble(parts[5].trim()),
			FuelType.valueOf(parts[6].trim().toUpperCase()),
			Double.parseDouble(parts[7].trim()),
			Integer.parseInt(parts[8].trim()),
			Double.parseDouble(parts[9].trim()),
			Double.parseDouble(parts[10].trim()),
			Double.parseDouble(parts[11].trim())
		);
	}

	private Vehicule toVehicule(VehiculeSeedItem item) {
		Vehicule vehicule = new Vehicule();
		vehicule.setName(item.name());
		vehicule.setBrand(item.brand());
		vehicule.setModel(item.model());
		vehicule.setGeneration(item.generation().isEmpty() ? null : item.generation());
		vehicule.setVersion(item.version());
		vehicule.setPurchasePrice(item.purchasePrice());
		vehicule.setFuelType(item.fuelType());
		vehicule.setConsumption(item.consumption());
		vehicule.setAnnualMileage(item.annualMileage());
		vehicule.setInsuranceCost(item.insuranceCost());
		vehicule.setMaintenanceCost(item.maintenanceCost());
		vehicule.setResaleValue(item.resaleValue());
		return vehicule;
	}

	public record VehiculeSeedItem(
		String name,
		String brand,
		String model,
		String generation,
		String version,
		double purchasePrice,
		FuelType fuelType,
		double consumption,
		int annualMileage,
		double insuranceCost,
		double maintenanceCost,
		double resaleValue
	) {
	}
}
