package com.example.springbootapp.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.security.google.client-id:1047781504975-placeholderclientid.apps.googleusercontent.com}")
    private String expectedClientId;

    public GoogleAuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Map<?, ?> verifyToken(String idToken) throws IOException, InterruptedException {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("Token Google invalide. Code: {}", response.statusCode());
            throw new IllegalArgumentException("Jeton d'authentification Google invalide ou expiré.");
        }

        Map<?, ?> tokenInfo = objectMapper.readValue(response.body(), Map.class);
        String email = (String) tokenInfo.get("email");
        String emailVerified = (String) tokenInfo.get("email_verified");
        String aud = (String) tokenInfo.get("aud");
        String iss = (String) tokenInfo.get("iss");

        boolean verified = "true".equals(emailVerified) || Boolean.TRUE.equals(tokenInfo.get("email_verified"));
        if (email == null || !verified) {
            throw new IllegalArgumentException("Email Google non vérifié ou absent.");
        }
        if (iss == null || !iss.contains("accounts.google.com")) {
            throw new IllegalArgumentException("Émetteur du jeton invalide.");
        }
        if (expectedClientId != null && !expectedClientId.contains("placeholderclientid")
                && !expectedClientId.equals(aud)) {
            logger.warn("Audience mismatch! Attendu: {}, Reçu: {}", expectedClientId, aud);
            throw new IllegalArgumentException("Client ID du jeton incorrect.");
        }

        return tokenInfo;
    }
}
