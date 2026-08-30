package com.example.springbootapp.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "finitions")
public class Finition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // ex: "Allure", "GT", "Evolution", "Techno", "Iconic"

    @Column(name = "image_url", length = 2000)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    @JsonIgnoreProperties({"finitions", "motorisations"})
    private VehicleModel model;

    @OneToMany(mappedBy = "finition", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("finition")
    private List<FinitionMotorisation> variantPricings = new ArrayList<>();

    public Finition() {
    }

    public Finition(String name, String imageUrl, VehicleModel model) {
        this.name = name;
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
