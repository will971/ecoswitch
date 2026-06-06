package com.example.springbootapp.business.vehicule;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.springbootapp.service.AdemeService;
import com.example.springbootapp.service.AdemeService.AdemeVehicle;

@Component
public class AdemeBusiness {

    private final AdemeService ademeService;

    public AdemeBusiness(AdemeService ademeService) {
        this.ademeService = ademeService;
    }

    public List<String> getBrands() {
        return ademeService.getBrands();
    }

    public List<String> getModels(String brand) {
        return ademeService.getModels(brand);
    }

    public List<AdemeVehicle> getVersions(String brand, String model) {
        return ademeService.getVersions(brand, model);
    }

    public Optional<AdemeVehicle> getVehicle(String brand, String model, String version) {
        return ademeService.getVehicle(brand, model, version);
    }
}
