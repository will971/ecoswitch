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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FuelPriceLiveService {

    private static final Logger logger = LoggerFactory.getLogger(FuelPriceLiveService.class);

    private static final String OPEN_DATA_URL =
            "https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records"
            + "?where=gazole_prix%20%3E%201.0%20AND%20gazole_prix%20%3C%202.8%20AND%20e10_prix%20%3E%201.0%20AND%20e10_prix%20%3C%202.8"
            + "&select=avg(gazole_prix)%20as%20avg_gazole,%20avg(e10_prix)%20as%20avg_e10,%20avg(sp95_prix)%20as%20avg_sp95,%20avg(sp98_prix)%20as%20avg_sp98,%20avg(e85_prix)%20as%20avg_e85,%20count(id)%20as%20total_stations"
            + "&limit=1";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    // Cache des prix en direct
    private final Map<String, Double> cachedPrices = new ConcurrentHashMap<>();
    private String lastUpdatedIso = null;
    private String aiSummary = null;
    private int totalStationsSurveyed = 0;

    public FuelPriceLiveService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();

        // Valeurs par défaut initiales robustes
        cachedPrices.put("PETROL", 1.88);
        cachedPrices.put("DIESEL", 1.76);
        cachedPrices.put("ELECTRIC", 0.2516);
        cachedPrices.put("HYBRID", 1.88);
        cachedPrices.put("E85", 0.88);
        cachedPrices.put("SP98", 1.96);
        this.lastUpdatedIso = Instant.now().toString();
        this.aiSummary = "Tarifs moyens nationaux indicatifs en vigueur en France.";

        // Charger en tâche de fond dès le démarrage
        new Thread(this::fetchLivePrices).start();
    }

    public record FuelPricesLiveResponse(
            Map<String, Double> prices,
            String lastUpdated,
            int stationsCount,
            String source,
            String aiInsights
    ) {}

    public FuelPricesLiveResponse getLiveFuelPrices() {
        return new FuelPricesLiveResponse(
                Map.copyOf(cachedPrices),
                lastUpdatedIso,
                totalStationsSurveyed,
                "Ministère de l'Économie & CRE / RTE (Open Data temps réel)",
                aiSummary
        );
    }

    /**
     * Mise à jour automatique toutes les 3 heures.
     */
    @Scheduled(fixedRate = 3 * 60 * 60 * 1000)
    public void scheduledUpdate() {
        fetchLivePrices();
    }

    public synchronized void fetchLivePrices() {
        logger.info("Synchronisation des prix des carburants en temps réel via Open Data...");
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OPEN_DATA_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "EcoSwitchFuelSync/2.0 (contact@ecoswitch.fr)")
                    .GET()
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(res.body());
                JsonNode results = root.path("results");
                if (results.isArray() && !results.isEmpty()) {
                    JsonNode agg = results.get(0);
                    double avgGazole = agg.path("avg_gazole").asDouble(1.76);
                    double avgE10 = agg.path("avg_e10").asDouble(1.88);
                    double avgSp98 = agg.path("avg_sp98").asDouble(1.96);
                    double avgE85 = agg.path("avg_e85").asDouble(0.88);
                    int stations = agg.path("total_stations").asInt(9000);

                    double roundedGazole = Math.round(avgGazole * 100.0) / 100.0;
                    double roundedE10 = Math.round(avgE10 * 100.0) / 100.0;
                    double roundedSp98 = Math.round(avgSp98 * 100.0) / 100.0;
                    double roundedE85 = Math.round(avgE85 * 100.0) / 100.0;

                    cachedPrices.put("PETROL", roundedE10);
                    cachedPrices.put("DIESEL", roundedGazole);
                    cachedPrices.put("HYBRID", roundedE10);
                    cachedPrices.put("SP98", roundedSp98);
                    cachedPrices.put("E85", roundedE85);
                    cachedPrices.put("ELECTRIC", 0.2516);

                    this.totalStationsSurveyed = stations;
                    this.lastUpdatedIso = Instant.now().toString();

                    logger.info("Prix réels synchronisés : SP95-E10={}€, Gazole={}€ (sur {} stations)",
                            roundedE10, roundedGazole, stations);

                    enrichWithAiInsight(roundedE10, roundedGazole);
                }
            } else {
                logger.warn("Open Data fuel API status code {}", res.statusCode());
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des prix des carburants : {}", e.getMessage());
        }
    }

    private void enrichWithAiInsight(double sp95Price, double dieselPrice) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            this.aiSummary = String.format(
                    "Moyennes nationales observées en direct sur %d stations : SP95-E10 à %.2f €/L, Gazole à %.2f €/L.",
                    totalStationsSurveyed, sp95Price, dieselPrice);
            return;
        }

        try {
            String prompt = String.format(
                    "Tu es l'économiste en transition énergétique de EcoSwitch. En une phrase courte et percutante (max 120 caractères), donne un conseil sur le coût à l'usage entre thermique (Essence: %.2f €/L, Diesel: %.2f €/L) et électricité (0.25 €/kWh).",
                    sp95Price, dieselPrice
            );

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{
                                    Map.of("text", prompt)
                            })
                    }
            ));

            String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=" + apiKey;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(geminiUrl))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(res.body());
                String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
                if (text != null && !text.trim().isEmpty()) {
                    this.aiSummary = text.trim().replace("\n", " ");
                }
            }
        } catch (Exception ex) {
            this.aiSummary = String.format("Moyennes en direct : Essence à %.2f €/L, Gazole à %.2f €/L.", sp95Price, dieselPrice);
        }
    }
}
