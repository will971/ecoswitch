package com.example.springbootapp.business.auth;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.service.GoogleAuthService;
import com.example.springbootapp.service.JwtService;
import com.example.springbootapp.service.UserService;

@Component
public class AuthBusiness {

    private static final Logger logger = LoggerFactory.getLogger(AuthBusiness.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final GoogleAuthService googleAuthService;

    public AuthBusiness(UserService userService, JwtService jwtService, GoogleAuthService googleAuthService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.googleAuthService = googleAuthService;
    }

    public AuthResult register(String email, String name, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email et mot de passe obligatoires.");
        }
        AppUser user = userService.register(email.trim().toLowerCase(), name != null ? name : "Utilisateur", password);
        String token = jwtService.generateToken(user.getEmail());
        logger.info("Inscription réussie pour : {}", user.getEmail());
        return new AuthResult(user, token);
    }

    public AuthResult login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email et mot de passe obligatoires.");
        }
        AppUser user = userService.login(email.trim().toLowerCase(), password);
        String token = jwtService.generateToken(user.getEmail());
        logger.info("Connexion réussie pour : {}", user.getEmail());
        return new AuthResult(user, token);
    }

    public AuthResult googleLogin(String idToken) throws Exception {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Le jeton Google (credential) est manquant.");
        }

        // Valider le token via le service Google
        Map<?, ?> tokenInfo = googleAuthService.verifyToken(idToken);
        String email = (String) tokenInfo.get("email");
        String name = (String) tokenInfo.get("name");
        String normalizedEmail = email.trim().toLowerCase();

        // Créer ou retrouver l'utilisateur
        AppUser user = userService.upsertGoogleUser(normalizedEmail, name != null ? name : "Utilisateur Google");
        String token = jwtService.generateToken(user.getEmail());
        logger.info("Authentification Google réussie pour : {}", normalizedEmail);

        return new AuthResult(user, token);
    }

    public record AuthResult(AppUser user, String token) {}
}
