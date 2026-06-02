package com.example.springbootapp.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "vehicule")
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // ex: "Mazda 3 2.0 Skyactiv-G"

    @Column(name = "purchase_price")
    private double purchasePrice; // prix d'achat €

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType; // essence, diesel, etc.

    @Column(name = "consumption")
    private double consumption;
    // L/100km ou kWh/100km

    @Column(name = "annual_mileage")
    private int annualMileage;
    // km/an

    @Column(name = "insurance_cost")
    private double insuranceCost;
    // €/an

    @Column(name = "maintenance_cost")
    private double maintenanceCost;
    // €/an

    @Column(name = "resale_value")
    private double resaleValue;
    // valeur estimée après X années (optionnel au début)

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public double getConsumption() {
        return consumption;
    }

    public int getAnnualMileage() {
        return annualMileage;
    }

    public double getInsuranceCost() {
        return insuranceCost;
    }

    public double getMaintenanceCost() {
        return maintenanceCost;
    }

    public double getResaleValue() {
        return resaleValue;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public void setConsumption(double consumption) {
        this.consumption = consumption;
    }

    public void setAnnualMileage(int annualMileage) {
        this.annualMileage = annualMileage;
    }

    public void setInsuranceCost(double insuranceCost) {
        this.insuranceCost = insuranceCost;
    }

    public void setMaintenanceCost(double maintenanceCost) {
        this.maintenanceCost = maintenanceCost;
    }

    public void setResaleValue(double resaleValue) {
        this.resaleValue = resaleValue;
    }

    public record VehicleCost(
            double annualFuelCost,
            double annualCost,
            double totalCost) {
    }
}
