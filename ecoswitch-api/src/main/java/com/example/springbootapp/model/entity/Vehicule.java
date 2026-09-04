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

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "generation", length = 100)
    private String generation;

    @Column(name = "version", length = 300)
    private String version;

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


    @Column(name = "maintenance_cost")
    private double maintenanceCost;
    // €/an

    @Column(name = "resale_value")
    private double resaleValue;
    // valeur estimée après X années (optionnel au début)

    @Column(name = "url", length = 2000)
    private String url;

    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility = "PUBLIC";

    @Column(name = "created_by", length = 320)
    private String createdBy;

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


    public double getMaintenanceCost() {
        return maintenanceCost;
    }

    public double getResaleValue() {
        return resaleValue;
    }

    public String getUrl() {
        return url;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getCreatedBy() {
        return createdBy;
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


    public void setMaintenanceCost(double maintenanceCost) {
        this.maintenanceCost = maintenanceCost;
    }

    public void setResaleValue(double resaleValue) {
        this.resaleValue = resaleValue;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getGeneration() { return generation; }
    public void setGeneration(String generation) { this.generation = generation; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public record VehicleCost(
            double annualFuelCost,
            double annualCost,
            double totalCost) {
    }
}
