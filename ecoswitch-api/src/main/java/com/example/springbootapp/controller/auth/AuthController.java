package com.example.springbootapp.controller.auth;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.service.JwtService;
import com.example.springbootapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Inscription, connexion et SSO Google avec retour de JWT")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final ObjectMapper objectMapper;
    private final HttpClient   httpClient;
    private final JwtService   jwtService;
    private final UserService  userService;

    @Value("${app.security.google.client-id:1047781504975-placeholderclientid.apps.googleusercontent.com}")
    private String expectedClientId;

    public AuthController(ObjectMapper objectMapper, JwtService jwtService, UserService userService) {
        this.objectMapper = objectMapper;
        this.httpClient   = HttpClient.newBuilder().build();
        this.jwtService   = jwtService;
        this.userService  = userService;
    }

    // ── Inscription ───────────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Inscrire un nouvel utilisateur (email + mot de passe)")
    @ApiResponse(responseCode = "201", description = "Compte créé, JWT retourné")
    @ApiResponse(responseCode = "409", description = "Email déjà utilisé")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String email    = payload.get("email");
        String password = payload.get("password");
        String name     = payload.get("name");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email et mot de passe obligatoires."));
        }

        try {
            AppUser user  = userService.register(email.trim().toLowerCase(), name != null ? name : "Utilisateur", password);
            String  token = jwtService.generateToken(user.getEmail());
            logger.info("Inscription réussie pour : {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(user, token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Connexion email/mot de passe ──────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Connexion email + mot de passe, retourne un JWT")
    @ApiResponse(responseCode = "200", description = "Authentification réussie, JWT retourné")
    @ApiResponse(responseCode = "401", description = "Identifiants incorrects")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email    = payload.get("email");
        String password = payload.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email et mot de passe obligatoires."));
        }

        try {
            AppUser user  = userService.login(email.trim().toLowerCase(), password);
            String  token = jwtService.generateToken(user.getEmail());
            logger.info("Connexion réussie pour : {}", user.getEmail());
            return ResponseEntity.ok(buildAuthResponse(user, token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Google SSO ────────────────────────────────────────────────────────

    @PostMapping("/google-login")
    @Operation(summary = "Valider un ID Token Google, créer/mettre à jour l'utilisateur, retourner un JWT")
    @ApiResponse(responseCode = "200", description = "Authentification réussie")
    @ApiResponse(responseCode = "401", description = "Authentification échouée")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("credential");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le jeton Google (credential) est manquant."));
        }

        try {
            // Valider le token via l'API Google
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                    + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpStatus.OK.value()) {
                logger.warn("Token Google invalide. Code: {}", response.statusCode());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Jeton d'authentification Google invalide ou expiré."));
            }

            Map<?, ?> tokenInfo = objectMapper.readValue(response.body(), Map.class);
            String email         = (String) tokenInfo.get("email");
            String emailVerified = (String) tokenInfo.get("email_verified");
            String aud           = (String) tokenInfo.get("aud");
            String iss           = (String) tokenInfo.get("iss");
            String name          = (String) tokenInfo.get("name");

            boolean verified = "true".equals(emailVerified) || Boolean.TRUE.equals(tokenInfo.get("email_verified"));
            if (email == null || !verified) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email Google non vérifié ou absent."));
            }
            String normalizedEmail = email.trim().toLowerCase();
            if (iss == null || !iss.contains("accounts.google.com")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Émetteur du jeton invalide."));
            }
            if (expectedClientId != null && !expectedClientId.contains("placeholderclientid")
                    && !expectedClientId.equals(aud)) {
                logger.warn("Audience mismatch! Attendu: {}, Reçu: {}", expectedClientId, aud);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Client ID du jeton incorrect."));
            }

            // Créer ou retrouver l'utilisateur en base
            AppUser user  = userService.upsertGoogleUser(normalizedEmail, name != null ? name : "Utilisateur Google");
            String  token = jwtService.generateToken(user.getEmail());
            logger.info("Authentification Google réussie pour : {}", normalizedEmail);
            return ResponseEntity.ok(buildAuthResponse(user, token));

        } catch (Exception e) {
            logger.error("Erreur validation SSO Google", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors de la validation du jeton Google."));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Map<String, Object> buildAuthResponse(AppUser user, String token) {
        return Map.of(
                "token",  token,
                "name",   user.getName(),
                "email",  user.getEmail(),
                "plan",   user.getPlan()
        );
    }
}
