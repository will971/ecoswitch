package com.example.springbootapp.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "motorisations")
public class Motorisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name; // ex: "Électrique 156 ch (54 kWh)", "E-Tech Full Hybrid 145", "PureTech 100 S&S"

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 30)
    private FuelType fuelType;

    @Column(name = "consumption_wltp", nullable = false)
    private double consumptionWltp; // kWh/100km pour électrique, L/100km pour thermique/hybride

    @Column(name = "power_hp")
    private Integer powerHp; // Puissance en chevaux (ex: 156)

    @Column(name = "battery_capacity_kwh")
    private Double batteryCapacityKwh; // Capacité batterie utile/brute en kWh

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    @JsonIgnoreProperties({"motorisations", "finitions"})
    private VehicleModel model;

    @OneToMany(mappedBy = "motorisation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("motorisation")
    private List<FinitionMotorisation> variantPricings = new ArrayList<>();

    public Motorisation() {
    }

    public Motorisation(String name, FuelType fuelType, double consumptionWltp, Integer powerHp, Double batteryCapacityKwh, VehicleModel model) {
        this.name = name;
        this.fuelType = fuelType;
        this.consumptionWltp = consumptionWltp;
        this.powerHp = powerHp;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.model = model;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public double getConsumptionWltp() {
        return consumptionWltp;
    }

    public void setConsumptionWltp(double consumptionWltp) {
        this.consumptionWltp = consumptionWltp;
    }

    public Integer getPowerHp() {
        return powerHp;
    }

    public void setPowerHp(Integer powerHp) {
        this.powerHp = powerHp;
    }

    public Double getBatteryCapacityKwh() {
        return batteryCapacityKwh;
    }

    public void setBatteryCapacityKwh(Double batteryCapacityKwh) {
        this.batteryCapacityKwh = batteryCapacityKwh;
    }

    public VehicleModel getModel() {
        return model;
    }

    public void setModel(VehicleModel model) {
        this.model = model;
    }

    public List<FinitionMotorisation> getVariantPricings() {
        return variantPricings;
    }

    public void setVariantPricings(List<FinitionMotorisation> variantPricings) {
        this.variantPricings = variantPricings;
    }
}
