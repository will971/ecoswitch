package com.example.springbootapp.dao;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.example.springbootapp.model.dto.FuelPriceOpenDataDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class FuelPriceOpenDataDao {

    private static final Logger logger = LoggerFactory.getLogger(FuelPriceOpenDataDao.class);

    private final String openDataApiUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public FuelPriceOpenDataDao(
            @Value("${app.fuel-prices.open-data-url:https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records}") String openDataApiUrl,
            ObjectMapper objectMapper) {
        this.openDataApiUrl = openDataApiUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public FuelPriceOpenDataDto fetchNationalAverages() {
        try {
            String queryParams = "?where=gazole_prix%20%3E%201.0%20AND%20gazole_prix%20%3C%202.8%20AND%20e10_prix%20%3E%201.0%20AND%20e10_prix%20%3C%202.8"
                    + "&select=avg(gazole_prix)%20as%20avg_gazole,%20avg(e10_prix)%20as%20avg_e10,%20avg(sp95_prix)%20as%20avg_sp95,%20avg(sp98_prix)%20as%20avg_sp98,%20avg(e85_prix)%20as%20avg_e85,%20count(id)%20as%20total_stations"
                    + "&limit=1";

            String fullUrl = openDataApiUrl + queryParams;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode results = root.path("results");

                if (results.isArray() && !results.isEmpty()) {
                    JsonNode record = results.get(0);
                    Map<String, Double> prices = new HashMap<>();

                    double avgE10 = record.path("avg_e10").asDouble(2.04);
                    double avgGazole = record.path("avg_gazole").asDouble(2.21);
                    double avgSp95 = record.path("avg_sp95").asDouble(avgE10 + 0.05);
                    double avgSp98 = record.path("avg_sp98").asDouble(avgE10 + 0.12);
                    double avgE85 = record.path("avg_e85").asDouble(0.88);
                    int totalStations = record.path("total_stations").asInt(7000);

                    prices.put("PETROL", Math.round(avgE10 * 100.0) / 100.0);
                    prices.put("DIESEL", Math.round(avgGazole * 100.0) / 100.0);
                    prices.put("SP95", Math.round(avgSp95 * 100.0) / 100.0);
                    prices.put("SP98", Math.round(avgSp98 * 100.0) / 100.0);
                    prices.put("E85", Math.round(avgE85 * 100.0) / 100.0);

                    logger.info("Open Data DAO : {} stations sondées (SP95-E10={}€, Gazole={}€)", totalStations, prices.get("PETROL"), prices.get("DIESEL"));
                    return new FuelPriceOpenDataDto(prices, totalStations, true);
                }
            } else {
                logger.warn("Open Data DAO : code HTTP {} reçu de {}", response.statusCode(), openDataApiUrl);
            }
        } catch (Exception e) {
            logger.warn("Open Data DAO : échec de récupération des prix ({}) : {}", e.getClass().getSimpleName(), e.getMessage());
        }

        return FuelPriceOpenDataDto.failed();
    }
}
