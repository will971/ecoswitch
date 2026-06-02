package com.example.springbootapp.service;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Service de gestion des tokens JWT.
 * Utilise HMAC-SHA256 avec une clé configurée dans application.yaml.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.security.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-days:7}")
    private int expirationDays;

    // ── Clé de signature ──────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "La propriété 'app.security.jwt.secret' est manquante. "
                            + "Configurez une clé HMAC en Base64 (ex: 256 bits minimum).");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "La propriété 'app.security.jwt.secret' doit être encodée en Base64 valide.", e);
        }
    }

    // ── Génération ────────────────────────────────────────────────────────

    /**
     * Génère un JWT signé contenant l'email de l'utilisateur comme sujet.
     *
     * @param email identifiant unique de l'utilisateur
     * @return token JWT compact signé
     */
    public String generateToken(String email) {
        Date now        = new Date();
        Date expiration = new Date(now.getTime() + (long) expirationDays * 24 * 60 * 60 * 1000);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    // ── Extraction ────────────────────────────────────────────────────────

    /**
     * Extrait l'email (sujet) d'un token JWT valide.
     *
     * @param token JWT compact
     * @return email de l'utilisateur
     * @throws JwtException si le token est invalide ou expiré
     */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // ── Validation ────────────────────────────────────────────────────────

    /**
     * Vérifie la signature et la date d'expiration du token.
     *
     * @param token JWT compact
     * @return {@code true} si le token est valide et non expiré
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }
}
