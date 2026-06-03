package com.example.springbootapp.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_vehicle_profile")
public class UserVehicleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 320)
    private String userEmail;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(name = "consumption")
    private double consumption;

    @Column(name = "annual_mileage")
    private int annualMileage;

    @Column(name = "insurance_cost")
    private double insuranceCost;

    @Column(name = "maintenance_cost")
    private double maintenanceCost;

    @Column(name = "resale_value")
    private double resaleValue;

    // Prix des énergies personnalisés
    @Column(name = "petrol_price")
    private double petrolPrice = 1.88;

    @Column(name = "diesel_price")
    private double dieselPrice = 1.74;

    @Column(name = "electric_price")
    private double electricPrice = 0.25;

    public UserVehicleProfile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }

    public double getConsumption() { return consumption; }
    public void setConsumption(double consumption) { this.consumption = consumption; }

    public int getAnnualMileage() { return annualMileage; }
    public void setAnnualMileage(int annualMileage) { this.annualMileage = annualMileage; }

    public double getInsuranceCost() { return insuranceCost; }
    public void setInsuranceCost(double insuranceCost) { this.insuranceCost = insuranceCost; }

    public double getMaintenanceCost() { return maintenanceCost; }
    public void setMaintenanceCost(double maintenanceCost) { this.maintenanceCost = maintenanceCost; }

    public double getResaleValue() { return resaleValue; }
    public void setResaleValue(double resaleValue) { this.resaleValue = resaleValue; }

    public double getPetrolPrice() { return petrolPrice; }
    public void setPetrolPrice(double petrolPrice) { this.petrolPrice = petrolPrice; }

    public double getDieselPrice() { return dieselPrice; }
    public void setDieselPrice(double dieselPrice) { this.dieselPrice = dieselPrice; }

    public double getElectricPrice() { return electricPrice; }
    public void setElectricPrice(double electricPrice) { this.electricPrice = electricPrice; }
}
