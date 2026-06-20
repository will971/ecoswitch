package com.example.springbootapp.service;

import com.example.springbootapp.model.entity.FuelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdemeService {

	private static final Logger logger = LoggerFactory.getLogger(AdemeService.class);
	private final ResourceLoader resourceLoader;
	private final ObjectMapper objectMapper;
	private final List<AdemeVehicle> ademeVehicles = new ArrayList<>();

	private Map<String, String> brandNormalizations = new HashMap<>();
	private Map<String, Map<String, ModelRule>> modelRules = new HashMap<>();

	public AdemeService(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
		this.resourceLoader = resourceLoader;
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	public void init() {
		loadMappings();
		loadHistoricalSeedData();
		loadAdemeData();
	}

	private void loadMappings() {
		Resource resource = resourceLoader.getResource("classpath:data/model-mappings.json");
		if (!resource.exists()) {
			logger.warn("Model mappings file not found, skipping mappings load.");
			return;
		}
		try {
			MappingsConfig config = objectMapper.readValue(resource.getInputStream(), MappingsConfig.class);
			if (config.brandNormalizations() != null) {
				this.brandNormalizations = config.brandNormalizations();
			}
			if (config.modelRules() != null) {
				this.modelRules = config.modelRules();
			}
			logger.info("Loaded {} brand normalizations and {} model rules.", brandNormalizations.size(), modelRules.size());
		} catch (Exception e) {
			logger.error("Failed to load model mappings config", e);
		}
	}

	/**
	 * Charge les véhicules historiques du fichier de seed (permettant d'avoir les anciens modèles 2000-2015
	 * avec leurs codes de génération précis comme E46, F30, F20, etc.).
	 */
	private void loadHistoricalSeedData() {
		Resource resource = resourceLoader.getResource("classpath:data/vehicules-seed.csv");
		if (!resource.exists()) {
			logger.warn("Historical seed file not found, skipping historical load.");
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
			String line;
			boolean isHeader = true;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (isHeader) {
					isHeader = false;
					continue;
				}
				try {
					String[] parts = line.split(",");
					if (parts.length < 12) continue;
					
					String brand = cleanValue(parts[1]);
					String model = cleanValue(parts[2]);
					String generation = cleanValue(parts[3]);
					String version = cleanValue(parts[4]);
					
					double purchasePrice = Double.parseDouble(parts[5].trim());
					FuelType fuelType = FuelType.valueOf(parts[6].trim().toUpperCase());
					double consumption = Double.parseDouble(parts[7].trim());
					int annualMileage = Integer.parseInt(parts[8].trim());
					double insuranceCost = Double.parseDouble(parts[9].trim());
					double maintenanceCost = Double.parseDouble(parts[10].trim());
					double resaleValue = Double.parseDouble(parts[11].trim());

					brand = normalizeBrand(brand);

					String formattedModel = model;
					if (!generation.isEmpty()) {
						if (brand.equalsIgnoreCase("BMW")) {
							formattedModel = model + " (" + generation + ")";
						} else {
							formattedModel = model + " " + generation;
						}
					}

					ademeVehicles.add(new AdemeVehicle(
							brand,
							formattedModel,
							version,
							fuelType,
							consumption,
							annualMileage,
							insuranceCost,
							maintenanceCost,
							resaleValue,
							purchasePrice
					));
				} catch (Exception e) {
					// Ignore line error
				}
			}
			logger.info("Loaded {} historical vehicles.", ademeVehicles.size());
		} catch (Exception e) {
			logger.error("Failed to load historical seed data", e);
		}
	}

	private void loadAdemeData() {
		Resource resource = resourceLoader.getResource("classpath:data/ademe-car-labelling.csv");
		if (!resource.exists()) {
			logger.error("ADEME CSV file not found!");
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
			String line;
			boolean isHeader = true;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				if (isHeader) {
					isHeader = false;
					continue;
				}
				try {
					String[] parts = line.split(";");
					if (parts.length < 25) continue;

					String brand = cleanValue(parts[0]);
					String model = cleanValue(parts[1]);
					String rawVersion = cleanValue(parts[4]);
					String energy = cleanValue(parts[5]).toUpperCase();
					
					if (brand.isEmpty() || model.isEmpty() || rawVersion.isEmpty()) {
						continue;
					}

					// Determine fuel type
					FuelType fuelType = FuelType.PETROL;
					if (energy.contains("ELECTRIC") || energy.contains("ELECTRIQUE")) {
						fuelType = FuelType.ELECTRIC;
					} else if (energy.contains("GAZOLE") || energy.contains("DIESEL")) {
						fuelType = FuelType.DIESEL;
					} else if (energy.contains("ELEC") || energy.contains("HYBR")) {
						fuelType = FuelType.HYBRID;
					}

					// Enrich version with technical details (Hp, Fiscal power, Gearbox, Displacement)
					String engineCc = cleanValue(parts[7]);
					String gearbox = cleanValue(parts[14]);
					String ratios = cleanValue(parts[15]);
					String fiscalHp = cleanValue(parts[9]);
					
					String hpStr = "";
					try {
						double kw = Double.parseDouble(cleanValue(parts[10]).replace(",", "."));
						if (kw > 0) {
							hpStr = Math.round(kw * 1.36) + "ch";
						}
					} catch (Exception e) {}

					// Clean names
					brand = normalizeBrand(brand);
					model = capitalize(model);

					// Apply model rule mapping from configuration
					if (modelRules.containsKey(brand)) {
						Map<String, ModelRule> brandRules = modelRules.get(brand);
						if (brandRules != null) {
							ModelRule rule = findModelRule(brandRules, model);
							if (rule != null) {
								if (rule.versionContains() != null && !rule.versionContains().isEmpty()) {
									boolean matchedVersion = false;
									for (Map.Entry<String, String> entry : rule.versionContains().entrySet()) {
										if (rawVersion.toUpperCase().contains(entry.getKey().toUpperCase())) {
											model = entry.getValue();
											matchedVersion = true;
											break;
										}
									}
									if (!matchedVersion && rule.replacement() != null) {
										model = rule.replacement();
									}
								} else if (rule.replacement() != null) {
									model = rule.replacement();
								}
							}
						}
					}

					StringBuilder versionBuilder = new StringBuilder(rawVersion);
					List<String> specs = new ArrayList<>();
					specs.add("2022/2023"); // Indique que c'est la génération neuve du dump ADEME
					if (!hpStr.isEmpty()) specs.add(hpStr);
					if (!fiscalHp.isEmpty()) specs.add(fiscalHp + " CV");
					if (!gearbox.isEmpty()) {
						String shortBox = gearbox.equalsIgnoreCase("MECANIQUE") ? "BVM" : (gearbox.equalsIgnoreCase("AUTOMATIQUE") ? "BVA" : gearbox);
						if (!ratios.isEmpty() && !ratios.equals("0")) {
							specs.add(shortBox + ratios);
						} else {
							specs.add(shortBox);
						}
					}
					if (!engineCc.isEmpty() && !engineCc.equals("0")) {
						specs.add(engineCc + " cm³");
					}

					if (!specs.isEmpty()) {
						versionBuilder.append(" (").append(String.join(" - ", specs)).append(")");
					}
					String version = versionBuilder.toString();

					// Parse consumption
					double consumption = 6.0;
					if (fuelType == FuelType.ELECTRIC) {
						String elecConsoStr = parts.length > 26 ? parts[26] : "";
						if (!elecConsoStr.isEmpty()) {
							double val = Double.parseDouble(elecConsoStr.replace(",", ".").replace("\"", "").trim());
							if (val > 100) {
								consumption = val / 10.0;
							} else {
								consumption = val;
							}
						} else {
							consumption = 16.0;
						}
					} else {
						String mixConsoMinStr = parts[24];
						String mixConsoMaxStr = parts.length > 25 ? parts[25] : "";
						double minConso = 0;
						double maxConso = 0;
						if (!mixConsoMinStr.isEmpty()) {
							minConso = Double.parseDouble(mixConsoMinStr.replace(",", ".").replace("\"", "").trim());
						}
						if (!mixConsoMaxStr.isEmpty()) {
							maxConso = Double.parseDouble(mixConsoMaxStr.replace(",", ".").replace("\"", "").trim());
						}
						if (minConso > 0 && maxConso > 0) {
							consumption = (minConso + maxConso) / 2.0;
						} else if (minConso > 0) {
							consumption = minConso;
						} else if (maxConso > 0) {
							consumption = maxConso;
						}
					}
					consumption = Math.round(consumption * 100.0) / 100.0;

					// Parse price
					double purchasePrice = 25000.0;
					if (parts.length > 51) {
						String priceStr = parts[51];
						if (!priceStr.isEmpty()) {
							purchasePrice = Double.parseDouble(priceStr.replace(",", ".").replace("\"", "").trim());
						}
					}
					
					// Generate budgets (More realistic resale retention of 60% to 75% for 2022/2023 models)
					int annualMileage = 15000;
					double insuranceCost = 700.0;
					double maintenanceCost = 400.0;
					double resaleValue = purchasePrice * 0.65;

					switch (fuelType) {
						case ELECTRIC -> {
							annualMileage = 15000;
							insuranceCost = 800.0;
							maintenanceCost = 250.0;
							resaleValue = purchasePrice * 0.75;
						}
						case HYBRID -> {
							annualMileage = 15000;
							insuranceCost = 700.0;
							maintenanceCost = 350.0;
							resaleValue = purchasePrice * 0.70;
						}
						case DIESEL -> {
							annualMileage = 18000;
							insuranceCost = 750.0;
							maintenanceCost = 450.0;
							resaleValue = purchasePrice * 0.60;
						}
						default -> {
							annualMileage = 12000;
							insuranceCost = 650.0;
							maintenanceCost = 400.0;
							resaleValue = purchasePrice * 0.65;
						}
					}

					ademeVehicles.add(new AdemeVehicle(
							brand,
							model,
							version,
							fuelType,
							consumption,
							annualMileage,
							insuranceCost,
							maintenanceCost,
							resaleValue,
							purchasePrice
					));
				} catch (Exception e) {
					// Ignore parsing errors
				}
			}
			logger.info("Loaded {} real ADEME vehicles from ademe-car-labelling.csv.", ademeVehicles.size() - 97);
		} catch (Exception e) {
			logger.error("Failed to load ADEME data", e);
		}
	}

	private String normalizeBrand(String brand) {
		if (brand == null) return "";
		String clean = brand.trim().toLowerCase();
		if (brandNormalizations.containsKey(clean)) {
			return brandNormalizations.get(clean);
		}
		return capitalize(brand);
	}

	private ModelRule findModelRule(Map<String, ModelRule> brandRules, String model) {
		for (Map.Entry<String, ModelRule> entry : brandRules.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(model)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private String cleanValue(String val) {
		if (val == null) return "";
		val = val.trim();
		if (val.startsWith("\"") && val.endsWith("\"")) {
			val = val.substring(1, val.length() - 1);
		}
		return val.trim();
	}

	private String capitalize(String str) {
		if (str == null || str.isEmpty()) return "";
		String[] words = str.split(" ");
		return Arrays.stream(words)
				.map(w -> w.isEmpty() ? "" : w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	public record ModelRule(
		String replacement,
		Map<String, String> versionContains
	) {}

	public record MappingsConfig(
		Map<String, String> brandNormalizations,
		Map<String, Map<String, ModelRule>> modelRules
	) {}

	public List<String> getBrands() {
		return ademeVehicles.stream()
				.map(AdemeVehicle::brand)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	public List<String> getModels(String brand) {
		if (brand == null) return Collections.emptyList();
		String cleanBrand = brand.trim().toLowerCase();
		return ademeVehicles.stream()
				.filter(v -> v.brand().toLowerCase().equals(cleanBrand))
				.map(AdemeVehicle::model)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	public List<AdemeVehicle> getVersions(String brand, String model) {
		if (brand == null || model == null) return Collections.emptyList();
		String cleanBrand = brand.trim().toLowerCase();
		String cleanModel = model.trim().toLowerCase();
		
		Map<String, AdemeVehicle> uniqueVersions = new LinkedHashMap<>();
		ademeVehicles.stream()
				.filter(v -> v.brand().toLowerCase().equals(cleanBrand) && v.model().toLowerCase().equals(cleanModel))
				.forEach(v -> uniqueVersions.putIfAbsent(v.version(), v));

		return uniqueVersions.values().stream()
				.sorted(Comparator.comparing(AdemeVehicle::version))
				.collect(Collectors.toList());
	}

	public Optional<AdemeVehicle> getVehicle(String brand, String model, String version) {
		if (brand == null || model == null || version == null) return Optional.empty();
		String cleanBrand = brand.trim().toLowerCase();
		String cleanModel = model.trim().toLowerCase();
		String cleanVersion = version.trim().toLowerCase();
		return ademeVehicles.stream()
				.filter(v -> v.brand().toLowerCase().equals(cleanBrand) && 
						v.model().toLowerCase().equals(cleanModel) && 
						v.version().toLowerCase().equals(cleanVersion))
				.findFirst();
	}

	public record AdemeVehicle(
			String brand,
			String model,
			String version,
			FuelType fuelType,
			double consumption,
			int annualMileage,
			double insuranceCost,
			double maintenanceCost,
			double resaleValue,
			double purchasePrice
	) {
		public String getFullName() {
			return String.format("%s %s %s", brand, model, version).trim().replaceAll(" +", " ");
		}
	}
}
