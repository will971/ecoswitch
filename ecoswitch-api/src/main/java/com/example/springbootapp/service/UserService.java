package com.example.springbootapp.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.repository.AppUserRepository;

/**
 * Service métier pour la gestion des utilisateurs (inscription / connexion).
 */
@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Inscription email/mot de passe ────────────────────────────────────

    /**
     * Inscrit un nouvel utilisateur avec email et mot de passe.
     *
     * @throws IllegalArgumentException si l'email est déjà utilisé
     */
    public AppUser register(String email, String name, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }
        AppUser user = new AppUser(
                email,
                name,
                passwordEncoder.encode(rawPassword),
                "email",
                "Pro"
        );
        return userRepository.save(user);
    }

    // ── Connexion email/mot de passe ──────────────────────────────────────

    /**
     * Vérifie les identifiants et retourne l'utilisateur si valides.
     *
     * @throws IllegalArgumentException si email inconnu ou mot de passe incorrect
     */
    public AppUser login(String email, String rawPassword) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect."));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect.");
        }
        return user;
    }

    // ── Upsert Google SSO ─────────────────────────────────────────────────

    /**
     * Crée ou met à jour un utilisateur authentifié via Google SSO.
     * Si l'email existe déjà avec un autre provider, le compte est lié.
     */
    public AppUser upsertGoogleUser(String email, String name) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            AppUser newUser = new AppUser(email, name, null, "google", "Pro");
            return userRepository.save(newUser);
        });
    }

    public java.util.Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}

