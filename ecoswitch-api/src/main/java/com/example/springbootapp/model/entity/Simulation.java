package com.example.springbootapp.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Simulation de rentabilité sauvegardée par un utilisateur.
 * Les données complètes sont sérialisées en JSON dans {@code simulationData}.
 */
@Entity
@Table(
    name = "simulation",
    indexes = @Index(name = "idx_simulation_user_email", columnList = "user_email")
)
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 320)
    private String userEmail;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    /** Payload JSON complet de la simulation (véhicules, résultats, prix carburant…) */
    @Column(name = "simulation_data", columnDefinition = "TEXT", nullable = false)
    private String simulationData;

    // ── Constructeurs ──────────────────────────────────────────────────────

    public Simulation() {}

    public Simulation(String userEmail, String name, LocalDateTime savedAt, String simulationData) {
        this.userEmail       = userEmail;
        this.name            = name;
        this.savedAt         = savedAt;
        this.simulationData  = simulationData;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getUserEmail()         { return userEmail; }
    public String getName()              { return name; }
    public LocalDateTime getSavedAt()    { return savedAt; }
    public String getSimulationData()    { return simulationData; }

    public void setId(Long id)                         { this.id = id; }
    public void setUserEmail(String userEmail)         { this.userEmail = userEmail; }
    public void setName(String name)                   { this.name = name; }
    public void setSavedAt(LocalDateTime savedAt)      { this.savedAt = savedAt; }
    public void setSimulationData(String data)         { this.simulationData = data; }
}
