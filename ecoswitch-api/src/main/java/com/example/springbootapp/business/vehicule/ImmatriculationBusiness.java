package com.example.springbootapp.business.vehicule;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.springbootapp.service.ImmatriculationService;

@Component
public class ImmatriculationBusiness {

    private final ImmatriculationService immatriculationService;

    public ImmatriculationBusiness(ImmatriculationService immatriculationService) {
        this.immatriculationService = immatriculationService;
    }

    public Optional<Map<String, Object>> rechercherPlaque(String plaque) {
        if (plaque == null || plaque.isBlank()) {
            throw new IllegalArgumentException("Le numero de plaque est manquant.");
        }
        return immatriculationService.getVehicleByPlate(plaque);
    }
}
