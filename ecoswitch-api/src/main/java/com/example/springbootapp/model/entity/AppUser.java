package com.example.springbootapp.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Représente un utilisateur inscrit sur la plateforme EcoSwitch.
 * Supporte deux modes d'authentification : email/mot de passe et Google SSO.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email unique, utilisé comme identifiant JWT */
    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 200)
    private String name;

    /** Hash bcrypt du mot de passe — null pour les comptes Google SSO */
    @Column(name = "password_hash")
    private String passwordHash;

    /** "email" ou "google" */
    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false, length = 50)
    private String plan;

    // ── Constructeurs ──────────────────────────────────────────────────────

    public AppUser() {}

    public AppUser(String email, String name, String passwordHash, String provider, String plan) {
        this.email        = email;
        this.name         = name;
        this.passwordHash = passwordHash;
        this.provider     = provider;
        this.plan         = plan;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId()                { return id; }
    public String getEmail()           { return email; }
    public String getName()            { return name; }
    public String getPasswordHash()    { return passwordHash; }
    public String getProvider()        { return provider; }
    public String getPlan()            { return plan; }

    public void setId(Long id)                     { this.id = id; }
    public void setEmail(String email)             { this.email = email; }
    public void setName(String name)               { this.name = name; }
    public void setPasswordHash(String h)          { this.passwordHash = h; }
    public void setProvider(String provider)       { this.provider = provider; }
    public void setPlan(String plan)               { this.plan = plan; }
}
