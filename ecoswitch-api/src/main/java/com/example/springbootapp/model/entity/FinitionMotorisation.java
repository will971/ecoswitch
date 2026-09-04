package com.example.springbootapp.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "finition_motorisations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"finition_id", "motorisation_id"})
    }
)
public class FinitionMotorisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "finition_id", nullable = false)
    @JsonIgnoreProperties("variantPricings")
    private Finition finition;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "motorisation_id", nullable = false)
    @JsonIgnoreProperties("variantPricings")
    private Motorisation motorisation;

    @Column(name = "purchase_price", nullable = false)
    private double purchasePrice; // Prix d'achat comptant TTC €

    @Column(name = "monthly_loa")
    private Double monthlyLoa; // Loyer mensuel indicatif LOA €/mois

    @Column(name = "monthly_lld")
    private Double monthlyLld; // Loyer mensuel indicatif LLD €/mois

    @Column(name = "default_maintenance_cost")
    private Double defaultMaintenanceCost; // Entretien indicatif €/an

    @Column(name = "estimated_resale_value")
    private Double estimatedResaleValue; // Valeur de revente estimée €

    public FinitionMotorisation() {
    }

    public FinitionMotorisation(
            Finition finition,
            Motorisation motorisation,
            double purchasePrice,
            Double monthlyLoa,
            Double monthlyLld,
            Double defaultMaintenanceCost,
            Double estimatedResaleValue
    ) {
        this.finition = finition;
        this.motorisation = motorisation;
        this.purchasePrice = purchasePrice;
        this.monthlyLoa = monthlyLoa;
        this.monthlyLld = monthlyLld;
        this.defaultMaintenanceCost = defaultMaintenanceCost;
        this.estimatedResaleValue = estimatedResaleValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Finition getFinition() {
        return finition;
    }

    public void setFinition(Finition finition) {
        this.finition = finition;
    }

    public Motorisation getMotorisation() {
        return motorisation;
    }

    public void setMotorisation(Motorisation motorisation) {
        this.motorisation = motorisation;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Double getMonthlyLoa() {
        return monthlyLoa;
    }

    public void setMonthlyLoa(Double monthlyLoa) {
        this.monthlyLoa = monthlyLoa;
    }

    public Double getMonthlyLld() {
        return monthlyLld;
    }

    public void setMonthlyLld(Double monthlyLld) {
        this.monthlyLld = monthlyLld;
    }


    public Double getDefaultMaintenanceCost() {
        return defaultMaintenanceCost;
    }

    public void setDefaultMaintenanceCost(Double defaultMaintenanceCost) {
        this.defaultMaintenanceCost = defaultMaintenanceCost;
    }

    public Double getEstimatedResaleValue() {
        return estimatedResaleValue;
    }

    public void setEstimatedResaleValue(Double estimatedResaleValue) {
        this.estimatedResaleValue = estimatedResaleValue;
    }
}
