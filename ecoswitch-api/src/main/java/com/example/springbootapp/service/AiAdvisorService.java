package com.example.springbootapp.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiAdvisorService {

    private static final Logger logger = LoggerFactory.getLogger(AiAdvisorService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-flash-lite-latest}")
    private String modelName;

    public AiAdvisorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public record AiAdvisorRequest(
            Vehicule currentVehicle,
            Vehicule targetVehicle,
            Double annualMileage,
            Double homeChargingRatio,
            Double taxIncome,
            Boolean scrapVehicle,
            Boolean isLeasing,
            Double monthlySavings,
            Double annualSavings,
            Double totalSubsidies,
            Integer breakEvenYear,
            Double annualCO2Savings
    ) {}

    public record AiAdvisorResponse(
            String verdict,
            String status, // "POSITIVE", "MODERATE", "CAUTION"
            String financialAdvice,
            String chargingAdvice,
            String ecologicalImpact,
            List<String> keyRecommendations,
            int confidenceScore,
            String aiEngine
    ) {}

    public AiAdvisorResponse generateAdvice(AiAdvisorRequest request) {
        if (request == null || request.currentVehicle() == null || request.targetVehicle() == null) {
            throw new IllegalArgumentException("Données de simulation incomplètes pour générer le conseil IA.");
        }

        if (apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("YOUR_")) {
            try {
                AiAdvisorResponse geminiResponse = callGeminiApi(request);
                if (geminiResponse != null) {
                    return geminiResponse;
                }
            } catch (Exception e) {
                logger.warn("Échec de l'appel Gemini API ({}), bascule sur le moteur de règles intelligent : {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }

        return generateDeterministicAdvice(request);
    }

    private AiAdvisorResponse callGeminiApi(AiAdvisorRequest req) throws Exception {
        String prompt = buildPrompt(req);
        List<String> candidateModels = List.of(modelName, "gemini-flash-lite-latest", "gemini-3.5-flash-lite", "gemini-flash-latest");

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        for (String targetModel : candidateModels) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":generateContent?key=" + apiKey;

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode rootNode = objectMapper.readTree(response.body());
                    JsonNode textNode = rootNode.at("/candidates/0/content/parts/0/text");
                    if (!textNode.isMissingNode()) {
                        String text = textNode.asText().trim();
                        if (text.startsWith("```json")) {
                            text = text.substring(7);
                        } else if (text.startsWith("```")) {
                            text = text.substring(3);
                        }
                        if (text.endsWith("```")) {
                            text = text.substring(0, text.length() - 3);
                        }
                        text = text.trim();
                        JsonNode parsed = objectMapper.readTree(text);
                        List<String> recs = new ArrayList<>();
                        if (parsed.has("keyRecommendations") && parsed.get("keyRecommendations").isArray()) {
                            for (JsonNode item : parsed.get("keyRecommendations")) {
                                recs.add(item.asText());
                            }
                        }
                        String modelVersion = rootNode.path("modelVersion").asText(targetModel);
                        return new AiAdvisorResponse(
                                parsed.path("verdict").asText("Transition avantageuse."),
                                parsed.path("status").asText("RECOMMENDED"),
                                parsed.path("financialAdvice").asText(""),
                                parsed.path("chargingAdvice").asText(""),
                                parsed.path("ecologicalImpact").asText(""),
                                recs,
                                parsed.path("confidenceScore").asInt(94),
                                "Google Gemini AI (" + modelVersion + ")"
                        );
                    }
                } else {
                    logger.warn("Modèle {} non-200 : {} (essai du modèle suivant)", targetModel, response.statusCode());
                }
            } catch (Exception modelErr) {
                logger.warn("Erreur sur le modèle {} : {}", targetModel, modelErr.getMessage());
            }
        }

        return null;
    }

    private String buildPrompt(AiAdvisorRequest req) {
        Vehicule cur = req.currentVehicle();
        Vehicule tgt = req.targetVehicle();
        double mileage = req.annualMileage() != null ? req.annualMileage() : 15000.0;
        double subsidies = req.totalSubsidies() != null ? req.totalSubsidies() : 0.0;
        double savings = req.annualSavings() != null ? req.annualSavings() : 0.0;
        double monthly = req.monthlySavings() != null ? req.monthlySavings() : (savings / 12.0);
        double homeRatio = req.homeChargingRatio() != null ? req.homeChargingRatio() * 100.0 : 85.0;
        String breakEven = req.breakEvenYear() != null ? req.breakEvenYear() + " ans" : "> 10 ans";
        double co2 = req.annualCO2Savings() != null ? req.annualCO2Savings() : 0.0;
        boolean isLeasing = req.isLeasing() != null && req.isLeasing();

        return """
                Tu es l'expert automobile et conseiller en transition écologique d'EcoSwitch.
                Analyse cette simulation réelle de changement de véhicule pour un particulier français et produis une synthèse percutante, personnalisée, ultra-claire et bienveillante en français.

                DONNÉES DE LA SIMULATION :
                - Type d'acquisition : %s
                - Véhicule actuel : %s (%s, consommation : %.1f L/100km ou kWh/100km)
                - Véhicule cible : %s (%s, consommation : %.1f L/100km ou kWh/100km, prix : %.0f €)
                - Kilométrage annuel : %.0f km/an (~%.0f km/jour)
                - Profil de recharge domicile : %.0f%% à domicile
                - Aides de l'État déduites : %.0f €
                - Économie annuelle de carburant/énergie : %.0f €/an
                - Bilan trésorerie mensuelle nette : %.0f €/mois %s
                - Réduction de CO2 : %.0f kg/an

                CONSIGNE SPÉCIALE LEASING / LOA / LLD :
                Si l'acquisition est en leasing (LOA/LLD), ne parle JAMAIS d'amortissement sur 10 ans ni du prix total d'achat, car le contrat dure entre 3 et 5 ans (36 à 60 mois). Focalise ton conseil financier sur l'effort de loyer mensuel et la manière dont les économies de carburant viennent compenser tout ou partie du loyer.

                RÉPONDS UNIQUEMENT AU FORMAT JSON STRICT avec la structure suivante :
                {
                  "verdict": "Une phrase de verdict percutante et personnalisée qui résume la pertinence de ce choix pour son profil.",
                  "status": "POSITIVE" (si gain net ou loyer bien absorbé) ou "MODERATE" (si équilibré) ou "CAUTION" (si surcoût mensuel élevé),
                  "financialAdvice": "Explication claire du gain financier mensuel ou de l'effort de trésorerie net par mois.",
                  "chargingAdvice": "Conseil pratique et chiffré sur sa routine de recharge et le coût du plein.",
                  "ecologicalImpact": "Vulgarisation concrète de son impact environnemental (arbres ou trajets).",
                  "keyRecommendations": [
                    "Recommandation pratique 1",
                    "Recommandation pratique 2",
                    "Recommandation pratique 3"
                  ],
                  "confidenceScore": 95
                }
                """.formatted(
                isLeasing ? "Location avec Option d'Achat (LOA) / LLD (Contrat 3 à 5 ans)" : "Achat Comptant / Crédit classique",
                cur.getName(), cur.getFuelType(), cur.getConsumption(),
                tgt.getName(), tgt.getFuelType(), tgt.getConsumption(), tgt.getPurchasePrice(),
                mileage, mileage / 365.0,
                homeRatio,
                subsidies,
                savings, monthly, isLeasing ? "(Loyer déduit des économies)" : "(Gain net d'usage)",
                co2
        );
    }

    public AiAdvisorResponse generateDeterministicAdvice(AiAdvisorRequest req) {
        Vehicule cur = req.currentVehicle();
        Vehicule tgt = req.targetVehicle();
        double mileage = req.annualMileage() != null ? req.annualMileage() : 15000.0;
        double subsidies = req.totalSubsidies() != null ? req.totalSubsidies() : 0.0;
        double annualSavings = req.annualSavings() != null ? req.annualSavings() : 0.0;
        double monthlySavings = req.monthlySavings() != null ? req.monthlySavings() : (annualSavings / 12.0);
        double homeRatio = req.homeChargingRatio() != null ? req.homeChargingRatio() : 0.85;
        Integer breakEven = req.breakEvenYear();
        double co2 = req.annualCO2Savings() != null ? req.annualCO2Savings() : 0.0;
        boolean isElectric = tgt.getFuelType() == FuelType.ELECTRIC;
        boolean isHybrid = tgt.getFuelType() == FuelType.HYBRID || tgt.getFuelType() == FuelType.PLUGIN_HYBRID;
        boolean isLeasing = req.isLeasing() != null && req.isLeasing();

        String verdict;
        String status;
        List<String> recs = new ArrayList<>();

        if (isLeasing) {
            if (monthlySavings >= 0) {
                status = "POSITIVE";
                verdict = String.format("Opération financièrement gagnante : vos économies de carburant absorbent entièrement votre loyer de leasing avec +%.0f €/mois de gain net.", monthlySavings);
            } else if (monthlySavings > -150) {
                status = "MODERATE";
                verdict = String.format("Contrat LOA/LLD très compétitif : pour un effort net de seulement %.0f €/mois, vous roulez dans un véhicule moderne et garanti.", Math.abs(monthlySavings));
            } else {
                status = "CAUTION";
                verdict = String.format("Projet de leasing haut de gamme : le loyer implique un surcoût mensuel net de %.0f € après déduction des économies d'énergie.", Math.abs(monthlySavings));
            }
        } else {
            if (breakEven != null && breakEven <= 5) {
                status = "POSITIVE";
                verdict = String.format("Excellente opportunité : votre passage à la %s est rentabilisé en seulement %d ans grâce à vos %s km annuels.",
                        tgt.getName(), breakEven, formatNumber(mileage));
            } else if (breakEven != null && breakEven <= 8) {
                status = "POSITIVE";
                verdict = String.format("Projet équilibré : votre investissement sur la %s s'amortit en %d ans avec un gain net immédiat sur vos pleins d'énergie.",
                        tgt.getName(), breakEven);
            } else if (annualSavings > 600) {
                status = "MODERATE";
                verdict = String.format("Investissement axé sur le confort et les économies d'usage : vous gagnez %.0f € par an sur votre carburant.", annualSavings);
            } else {
                status = "CAUTION";
                verdict = String.format("Changement axé sur le renouvellement de véhicule : l'écart de prix initial nécessite un horizon plus long pour être amorti.");
            }
        }

        // Conseil financier
        String financialAdvice;
        if (isLeasing) {
            if (monthlySavings >= 0) {
                financialAdvice = String.format("En leasing, vous réalisez une économie d'énergie de %.0f €/an, ce qui compense l'intégralité du loyer et dégage un surplus de trésorerie de %.0f €/mois.", annualSavings, monthlySavings);
            } else {
                financialAdvice = String.format("Sur vos %s km/an, vous économisez %.0f €/mois de carburant, ce qui allège considérablement la charge de votre loyer de leasing.", formatNumber(mileage), annualSavings / 12.0);
            }
        } else {
            if (monthlySavings > 0) {
                financialAdvice = String.format("Sur vos %s km/an, vous économisez environ %.0f € chaque mois sur vos dépenses énergétiques (soit %.0f €/an).",
                        formatNumber(mileage), monthlySavings, annualSavings);
            } else {
                financialAdvice = String.format("Vos dépenses mensuelles d'énergie restent stables (écart de %.0f €/mois). L'avantage principal réside dans la fiabilité et la valeur de revente.",
                        Math.abs(monthlySavings));
            }
        }

        // Conseil recharge & routine
        String chargingAdvice;
        if (isElectric) {
            if (homeRatio >= 0.7) {
                double weeklyCost = (mileage / 52.0 / 100.0) * tgt.getConsumption() * 0.25;
                chargingAdvice = String.format("Avec %.0f%% de recharge à domicile, un plein complet nocturne en heures creuses vous revient à seulement ~%.1f € pour couvrir toute votre semaine (~%.0f km).",
                        homeRatio * 100.0, weeklyCost, mileage / 52.0);
            } else {
                chargingAdvice = "Pour vos recharges en extérieur, utilisez les bornes en voirie pendant vos courses ou activités pour bénéficier de tarifs préférentiels.";
            }
        } else if (isHybrid) {
            chargingAdvice = "Ce modèle hybride s'auto-recharge au freinage sans nécessiter de branchement : idéal pour réduire de 30% à 40% votre consommation en ville.";
        } else {
            chargingAdvice = String.format("Consommation modérée de %.1f L/100km adaptée aux longs trajets réguliers.", tgt.getConsumption());
        }

        // Impact écologique
        String ecologicalImpact;
        if (co2 > 500) {
            int trees = (int) Math.round(co2 / 25.0);
            int flights = (int) Math.round(co2 / 200.0);
            ecologicalImpact = String.format("Vous évitez le rejet de %s kg de CO₂ par an, ce qui équivaut à la captation de %d arbres adultes ou %d allers-retours Paris-Nice évités.",
                    formatNumber(co2), trees, flights);
        } else {
            ecologicalImpact = "Bilan carbone stable et aligné avec les standards d'émissions récents.";
        }

        // Recommandations clés
        if (subsidies > 0) {
            recs.add(String.format("Bénéficiez de %.0f € d'aides gouvernementales (Bonus / Prime) déduites directement par le concessionnaire.", subsidies));
        }
        if (isElectric && homeRatio >= 0.7) {
            recs.add("Installez une prise renforcée 3.7 kW (Green'Up) pour ~350 € ou une Wallbox bénéficiant de 500 € de crédit d'impôt.");
            recs.add("Programmez vos sessions de charge entre 22h et 6h du matin pour profiter du tarif Heures Creuses d'EDF.");
        }
        if (cur.getResaleValue() > 0) {
            recs.add(String.format("Votre véhicule actuel (%s) apporte un apport estimé à %.0f € pour réduire la mensualité.",
                    cur.getName(), cur.getResaleValue()));
        }
        if (recs.size() < 3) {
            recs.add("Réservez un essai routier de 48h en concession pour tester l'autonomie sur vos trajets du quotidien.");
        }

        return new AiAdvisorResponse(
                verdict,
                status,
                financialAdvice,
                chargingAdvice,
                ecologicalImpact,
                recs,
                94,
                "Moteur Expert Local"
        );
    }

    private String formatNumber(double val) {
        return String.format("%,.0f", val).replace(',', ' ');
    }
}
