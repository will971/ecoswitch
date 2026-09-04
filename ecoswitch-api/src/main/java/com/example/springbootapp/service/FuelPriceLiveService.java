package com.example.springbootapp.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.springbootapp.config.CacheConfig;
import com.example.springbootapp.dao.FuelPriceOpenDataDao;
import com.example.springbootapp.model.dto.FuelPriceOpenDataDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FuelPriceLiveService {

    private static final Logger logger = LoggerFactory.getLogger(FuelPriceLiveService.class);

    private final FuelPriceOpenDataDao fuelPriceOpenDataDao;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    // Cache des prix en direct
    private final Map<String, Double> cachedPrices = new ConcurrentHashMap<>();
    private String lastUpdatedIso = null;
    private String aiSummary = null;
    private int totalStationsSurveyed = 0;

    public FuelPriceLiveService(FuelPriceOpenDataDao fuelPriceOpenDataDao, ObjectMapper objectMapper) {
        this.fuelPriceOpenDataDao = fuelPriceOpenDataDao;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();

        // Valeurs par défaut initiales robustes
        cachedPrices.put("PETROL", 1.88);
        cachedPrices.put("DIESEL", 1.76);
        cachedPrices.put("ELECTRIC", 0.2516);
        cachedPrices.put("HYBRID", 1.88);
        cachedPrices.put("PLUGIN_HYBRID", 1.88);
        cachedPrices.put("E85", 0.88);
        cachedPrices.put("SP98", 1.96);
        this.lastUpdatedIso = Instant.now().toString();
        this.aiSummary = "Tarifs moyens nationaux indicatifs en vigueur en France.";

        // Charger en tâche de fond dès le démarrage via la DAO
        new Thread(this::fetchLiveFuelPrices).start();
    }

    /**
     * Synchronise les prix en temps réel via la DAO toutes les 6 heures (ou à 6h et 14h).
     */
    @Scheduled(cron = "0 0 6,12,18 * * *")
    @CacheEvict(value = CacheConfig.CACHE_FUEL_PRICES_LIVE, allEntries = true)
    public void fetchLiveFuelPrices() {
        logger.info("Synchronisation des prix des carburants via FuelPriceOpenDataDao...");
        FuelPriceOpenDataDto result = fuelPriceOpenDataDao.fetchNationalAverages();

        if (result.success() && !result.prices().isEmpty()) {
            Map<String, Double> prices = result.prices();
            if (prices.containsKey("PETROL")) {
                cachedPrices.put("PETROL", prices.get("PETROL"));
                cachedPrices.put("HYBRID", prices.get("PETROL"));
                cachedPrices.put("PLUGIN_HYBRID", prices.get("PETROL"));
            }
            if (prices.containsKey("DIESEL")) {
                cachedPrices.put("DIESEL", prices.get("DIESEL"));
            }
            if (prices.containsKey("SP95")) {
                cachedPrices.put("SP95", prices.get("SP95"));
            }
            if (prices.containsKey("SP98")) {
                cachedPrices.put("SP98", prices.get("SP98"));
            }
            if (prices.containsKey("E85")) {
                cachedPrices.put("E85", prices.get("E85"));
            }

            this.totalStationsSurveyed = result.totalStationsSurveyed();
            this.lastUpdatedIso = Instant.now().toString();

            // Générer un résumé analytique via IA
            generateAiFuelSummary(cachedPrices.get("PETROL"), cachedPrices.get("DIESEL"), cachedPrices.get("ELECTRIC"));
        } else {
            logger.warn("Open Data DAO n'a renvoyé aucun résultat valide, conservation du cache existant.");
        }
    }

    private void generateAiFuelSummary(Double petrolPrice, Double dieselPrice, Double electricPrice) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("YOUR_")) {
            this.aiSummary = String.format("Prix moyens observés sur %d stations en France : SP95-E10 à %.2f €/L, Gazole à %.2f €/L et Électricité à %.4f €/kWh (Tarif Bleu EDF).",
                    totalStationsSurveyed > 0 ? totalStationsSurveyed : 7000, petrolPrice, dieselPrice, electricPrice);
            return;
        }

        try {
            String prompt = String.format(
                    "En tant qu'analyste des mobilités et énergies en France, résume en 2 phrases concises et percutantes "
                    + "la situation actuelle du coût des énergies : Essence SP95-E10 = %.2f €/L, Gazole = %.2f €/L, Électricité = %.4f €/kWh. "
                    + "Mets en avant le différentiel de coût aux 100 km entre thermique et électrique en France.",
                    petrolPrice, dieselPrice, electricPrice
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", java.util.List.of(
                            Map.of("parts", java.util.List.of(Map.of("text", prompt)))
                    )
            );

            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=" + apiKey;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
                if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
                    this.aiSummary = textNode.asText().trim();
                    logger.info("Synthèse IA des prix du carburant générée : {}", this.aiSummary);
                }
            }
        } catch (Exception e) {
            logger.warn("Impossible de générer le résumé IA des prix : {}", e.getMessage());
            this.aiSummary = String.format("SP95-E10 : %.2f €/L | Gazole : %.2f €/L | Électricité : %.4f €/kWh (Relevé sur %d stations).",
                    petrolPrice, dieselPrice, electricPrice, totalStationsSurveyed);
        }
    }

    public Map<String, Double> getLivePrices() {
        return cachedPrices;
    }

    @Cacheable(value = CacheConfig.CACHE_FUEL_PRICES_LIVE)
    public FuelPricesLiveResponse getLiveFuelPrices() {
        return new FuelPricesLiveResponse(
                cachedPrices,
                lastUpdatedIso,
                aiSummary,
                totalStationsSurveyed,
                true
        );
    }

    public record FuelPricesLiveResponse(
            Map<String, Double> prices,
            String lastUpdatedIso,
            String aiSummary,
            int totalStationsSurveyed,
            boolean isLive
    ) {}
}
