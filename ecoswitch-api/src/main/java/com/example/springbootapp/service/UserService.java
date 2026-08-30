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
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }
        String role = isAdminEmail(normalizedEmail) ? "ADMIN" : "USER";
        AppUser user = new AppUser(
                normalizedEmail,
                name,
                passwordEncoder.encode(rawPassword),
                "email",
                "Pro",
                role
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
        String normalizedEmail = email.trim().toLowerCase();
        AppUser user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect."));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect.");
        }
        if (isAdminEmail(normalizedEmail) && !"ADMIN".equals(user.getRole())) {
            user.setRole("ADMIN");
            user = userRepository.save(user);
        }
        return user;
    }

    // ── Upsert Google SSO ─────────────────────────────────────────────────

    /**
     * Crée ou met à jour un utilisateur authentifié via Google SSO.
     * Si l'email existe déjà avec un autre provider, le compte est lié.
     */
    public AppUser upsertGoogleUser(String email, String name) {
        String normalizedEmail = email.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail).map(existing -> {
            if (isAdminEmail(normalizedEmail) && !"ADMIN".equals(existing.getRole())) {
                existing.setRole("ADMIN");
                return userRepository.save(existing);
            }
            return existing;
        }).orElseGet(() -> {
            String role = isAdminEmail(normalizedEmail) ? "ADMIN" : "USER";
            AppUser newUser = new AppUser(normalizedEmail, name, null, "google", "Pro", role);
            return userRepository.save(newUser);
        });
    }

    public java.util.Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean isAdminEmail(String email) {
        if (email == null) return false;
        String e = email.trim().toLowerCase();
        return "modeste.william.s@gmail.com".equals(e) || "admin".equals(e) || "admin@ecoswitch.com".equals(e);
    }
}

