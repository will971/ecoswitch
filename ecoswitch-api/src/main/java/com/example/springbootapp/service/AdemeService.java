package com.example.springbootapp.service;

import com.example.springbootapp.model.entity.FuelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdemeService {

	private static final Logger logger = LoggerFactory.getLogger(AdemeService.class);
	private final ResourceLoader resourceLoader;
	private final List<AdemeVehicle> ademeVehicles = new ArrayList<>();

	public AdemeService(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@PostConstruct
	public void init() {
		loadHistoricalSeedData();
		loadAdemeData();
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
					if (parts.length < 8) continue;
					
					String fullName = cleanValue(parts[0]);
					double purchasePrice = Double.parseDouble(parts[1].trim());
					FuelType fuelType = FuelType.valueOf(parts[2].trim().toUpperCase());
					double consumption = Double.parseDouble(parts[3].trim());
					int annualMileage = Integer.parseInt(parts[4].trim());
					double insuranceCost = Double.parseDouble(parts[5].trim());
					double maintenanceCost = Double.parseDouble(parts[6].trim());
					double resaleValue = Double.parseDouble(parts[7].trim());

					// Parser intelligent du nom pour extraire la génération
					ParsedHistorical parsed = parseHistoricalName(fullName);

					ademeVehicles.add(new AdemeVehicle(
							parsed.brand,
							parsed.model,
							parsed.version,
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
					brand = capitalize(brand);
					model = capitalize(model);

					// BMW model mapping to make generation explicit (ex: Serie 1 (F40) pour le dump 2022+)
					if (brand.equalsIgnoreCase("BMW")) {
						if (model.equalsIgnoreCase("Serie 1")) {
							model = "Série 1 (F40)";
						} else if (model.equalsIgnoreCase("Serie 2")) {
							model = "Série 2 (F44/U06)";
						} else if (model.equalsIgnoreCase("Serie 3")) {
							model = "Série 3 (G20)";
						} else if (model.equalsIgnoreCase("Serie 4")) {
							model = "Série 4 (G22)";
						} else if (model.equalsIgnoreCase("Serie 5")) {
							model = "Série 5 (G30)";
						} else if (model.equalsIgnoreCase("Serie X")) {
							if (rawVersion.toUpperCase().contains("X1")) model = "Série X (X1 - U11)";
							else if (rawVersion.toUpperCase().contains("X2")) model = "Série X (X2 - F39)";
							else if (rawVersion.toUpperCase().contains("X3")) model = "Série X (X3 - G01)";
							else if (rawVersion.toUpperCase().contains("X4")) model = "Série X (X4 - G02)";
							else model = "Série X (2022+)";
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
							// For electric, 179.000 Wh/km corresponds to 17.9 kWh/100km. So dividing by 10 is correct if the string is e.g. "179,000". Let's parse it safely.
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
						// For combustion / hybrids, WLTP mixed consumption is often parts[24] (Min) and parts[25] (Max). Let's take the average or the Min. Let's use parts[24], if empty parts[25], or average them.
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
		if (str.equalsIgnoreCase("b.m.w.")) return "BMW";
		String[] words = str.split(" ");
		return Arrays.stream(words)
				.map(w -> w.isEmpty() ? "" : w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	/**
	 * Parse les anciens noms de véhicules pour en extraire des marques, modèles et versions élégants.
	 */
	private ParsedHistorical parseHistoricalName(String fullName) {
		int firstSpace = fullName.indexOf(' ');
		if (firstSpace == -1) {
			return new ParsedHistorical(fullName, "Autre", "Standard");
		}
		
		String brand = capitalize(fullName.substring(0, firstSpace));
		String remaining = fullName.substring(firstSpace + 1);

		String model = "Autre";
		String version = remaining;

		if (brand.equalsIgnoreCase("BMW")) {
			brand = "BMW"; // Uniformise en BMW
			// Parser spécifique BMW historique (ex: BMW 114i F20 (2013) -> brand: BMW, model: Série 1 (F20), version: 114i (2013))
			if (remaining.contains("114i") || remaining.contains("116i") || remaining.contains("118i") || remaining.contains("120d") || remaining.contains("118d")) {
				if (remaining.contains("F20")) {
					model = "Série 1 (F20)";
					version = remaining.replace("F20", "").replaceAll(" +", " ").trim();
				} else {
					model = "Série 1 (E81/E87)";
					version = remaining;
				}
			} else if (remaining.contains("320d") || remaining.contains("325i") || remaining.contains("330d")) {
				if (remaining.contains("E36")) {
					model = "Série 3 (E36)";
					version = remaining.replace("E36", "").replaceAll(" +", " ").trim();
				} else if (remaining.contains("E46")) {
					model = "Série 3 (E46)";
					version = remaining.replace("E46", "").replaceAll(" +", " ").trim();
				} else if (remaining.contains("F30")) {
					model = "Série 3 (F30)";
					version = remaining.replace("F30", "").replaceAll(" +", " ").trim();
				} else {
					model = "Série 3";
					version = remaining;
				}
			} else {
				model = "Autres Modèles";
				version = remaining;
			}
		} else if (brand.equalsIgnoreCase("Peugeot")) {
			brand = "Peugeot";
			if (remaining.contains("208 II")) {
				model = "208 II";
				version = remaining.replace("208 II", "").trim();
			} else if (remaining.contains("208 I")) {
				model = "208 I";
				version = remaining.replace("208 I", "").trim();
			} else if (remaining.contains("206")) {
				model = "206";
				version = remaining.replace("206", "").trim();
			} else if (remaining.contains("207")) {
				model = "207";
				version = remaining.replace("207", "").trim();
			} else if (remaining.contains("308 III")) {
				model = "308 III";
				version = remaining.replace("308 III", "").trim();
			} else if (remaining.contains("308 II")) {
				model = "308 II";
				version = remaining.replace("308 II", "").trim();
			} else if (remaining.contains("3008 II")) {
				model = "3008 II";
				version = remaining.replace("3008 II", "").trim();
			} else if (remaining.contains("3008 I")) {
				model = "3008 I";
				version = remaining.replace("3008 I", "").trim();
			} else {
				// Extraction simple
				int nextSpace = remaining.indexOf(' ');
				if (nextSpace != -1) {
					model = remaining.substring(0, nextSpace);
					version = remaining.substring(nextSpace + 1).trim();
				} else {
					model = remaining;
					version = "Standard";
				}
			}
		} else if (brand.equalsIgnoreCase("Renault")) {
			brand = "Renault";
			if (remaining.contains("Clio V")) {
				model = "Clio V";
				version = remaining.replace("Clio V", "").trim();
			} else if (remaining.contains("Clio IV")) {
				model = "Clio IV";
				version = remaining.replace("Clio IV", "").trim();
			} else if (remaining.contains("Clio III")) {
				model = "Clio III";
				version = remaining.replace("Clio III", "").trim();
			} else if (remaining.contains("Clio II")) {
				model = "Clio II";
				version = remaining.replace("Clio II", "").trim();
			} else if (remaining.contains("Clio I")) {
				model = "Clio I";
				version = remaining.replace("Clio I", "").trim();
			} else if (remaining.contains("Megane IV")) {
				model = "Mégane IV";
				version = remaining.replace("Megane IV", "").trim();
			} else if (remaining.contains("Megane III")) {
				model = "Mégane III";
				version = remaining.replace("Megane III", "").trim();
			} else if (remaining.contains("Megane E-Tech")) {
				model = "Mégane E-Tech";
				version = remaining.replace("Megane E-Tech", "").trim();
			} else {
				int nextSpace = remaining.indexOf(' ');
				if (nextSpace != -1) {
					model = remaining.substring(0, nextSpace);
					version = remaining.substring(nextSpace + 1).trim();
				} else {
					model = remaining;
					version = "Standard";
				}
			}
		} else {
			brand = capitalize(brand);
			int nextSpace = remaining.indexOf(' ');
			if (nextSpace != -1) {
				model = remaining.substring(0, nextSpace);
				version = remaining.substring(nextSpace + 1).trim();
			} else {
				model = remaining;
				version = "Standard";
			}
		}

		return new ParsedHistorical(brand, model, version);
	}

	private static class ParsedHistorical {
		String brand;
		String model;
		String version;

		ParsedHistorical(String brand, String model, String version) {
			this.brand = brand;
			this.model = model;
			this.version = version;
		}
	}

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
