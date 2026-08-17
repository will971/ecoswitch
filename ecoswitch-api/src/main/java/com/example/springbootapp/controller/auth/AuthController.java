package com.example.springbootapp.controller.auth;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootapp.model.entity.AppUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.springbootapp.business.auth.AuthBusiness;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Inscription, connexion et SSO Google avec retour de JWT")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthBusiness authBusiness;

    public AuthController(AuthBusiness authBusiness) {
        this.authBusiness = authBusiness;
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

        try {
            AuthBusiness.AuthResult result = authBusiness.register(email, name, password);
            return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(result.user(), result.token()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("obligatoires")) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
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

        try {
            AuthBusiness.AuthResult result = authBusiness.login(email, password);
            return ResponseEntity.ok(buildAuthResponse(result.user(), result.token()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("obligatoires")) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
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

        try {
            AuthBusiness.AuthResult result = authBusiness.googleLogin(idToken);
            return ResponseEntity.ok(buildAuthResponse(result.user(), result.token()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("manquant")) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur validation SSO Google", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne lors de la validation du jeton Google."));
        }
    }

    // ── Profil utilisateur connecté ────────────────────────────────────────
    
    @GetMapping("/me")
    @Operation(summary = "Récupérer les informations de l'utilisateur connecté via JWT", security = { @SecurityRequirement(name = "bearerAuth") })
    @ApiResponse(responseCode = "200", description = "Profil utilisateur")
    @ApiResponse(responseCode = "401", description = "Non authentifié")
    public ResponseEntity<?> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Non authentifié"));
        }
        String email = authentication.getName();
        return authBusiness.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(Map.of(
                        "name", user.getName() != null ? user.getName() : "Utilisateur",
                        "email", user.getEmail(),
                        "plan", user.getPlan() != null ? user.getPlan() : "Pro",
                        "role", user.getRole() != null ? user.getRole() : "USER"
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Map<String, Object> buildAuthResponse(AppUser user, String token) {
        return Map.of(
                "token",  token,
                "name",   user.getName(),
                "email",  user.getEmail(),
                "plan",   user.getPlan(),
                "role",   user.getRole()
        );
    }
}
