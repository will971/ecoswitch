package com.example.springbootapp.business.vehicule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.springbootapp.model.dto.BrandDto;
import com.example.springbootapp.model.dto.VehicleModelDto;
import com.example.springbootapp.model.dto.FinitionMotorisationDto;
import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.service.CatalogService;
import com.example.springbootapp.service.UserService;
import com.example.springbootapp.service.VehiculeService;

@Component
public class VehiculeBusiness {

    private final VehiculeService vehiculeService;
    private final UserService userService;
    private final CatalogService catalogService;

    public VehiculeBusiness(VehiculeService vehiculeService, UserService userService, CatalogService catalogService) {
        this.vehiculeService = vehiculeService;
        this.userService = userService;
        this.catalogService = catalogService;
    }

    public Vehicule create(Vehicule vehicule, String email) {
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vous devez être connecté pour ajouter un véhicule.");
        }
        AppUser user = userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
        
        vehicule.setCreatedBy(user.getEmail());
        if (vehicule.getVisibility() == null || vehicule.getVisibility().isBlank()) {
            vehicule.setVisibility("PUBLIC");
        }
        return vehiculeService.create(vehicule);
    }

    public List<Vehicule> findAll(String name, String fuelType, String brand, String model, String version, String principalName) {
        List<Vehicule> all = vehiculeService.findAll(null, null, name, fuelType, brand, model, version);
        
        if (principalName != null) {
            AppUser user = userService.findByEmail(principalName).orElse(null);
            if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
                return all;
            } else {
                final String userEmail = (user != null) ? user.getEmail() : "";
                return all.stream()
                        .filter(v -> "PUBLIC".equalsIgnoreCase(v.getVisibility()) || userEmail.equalsIgnoreCase(v.getCreatedBy()))
                        .toList();
            }
        } else {
            return all.stream()
                    .filter(v -> "PUBLIC".equalsIgnoreCase(v.getVisibility()))
                    .toList();
        }
    }

    public Vehicule findById(Long id, String principalName) {
        Vehicule v = vehiculeService.findById(id);
        if ("PRIVATE".equalsIgnoreCase(v.getVisibility())) {
            if (principalName == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
            }
            AppUser user = userService.findByEmail(principalName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé."));
            if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !user.getEmail().equalsIgnoreCase(v.getCreatedBy())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
            }
        }
        return v;
    }

    public Vehicule update(Long id, Vehicule vehicule, String principalName) {
        if (principalName == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        AppUser user = userService.findByEmail(principalName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
        Vehicule existing = vehiculeService.findById(id);
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !user.getEmail().equalsIgnoreCase(existing.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le créateur ou un administrateur peut modifier ce véhicule.");
        }
        vehicule.setCreatedBy(existing.getCreatedBy());
        if (vehicule.getVisibility() == null || vehicule.getVisibility().isBlank()) {
            vehicule.setVisibility(existing.getVisibility());
        }
        return vehiculeService.update(id, vehicule);
    }

    public void delete(Long id, String principalName) {
        if (principalName == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
        }
        AppUser user = userService.findByEmail(principalName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
        Vehicule existing = vehiculeService.findById(id);
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !user.getEmail().equalsIgnoreCase(existing.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le créateur ou un administrateur peut supprimer ce véhicule.");
        }
        vehiculeService.delete(id);
    }

    public List<String> getCatalogBrands() {
        Set<String> brands = new LinkedHashSet<>();
        // 1. From CatalogService
        for (BrandDto b : catalogService.getAllBrands()) {
            if (b.name() != null && !b.name().isBlank()) {
                brands.add(b.name());
            }
        }
        // 2. From Vehicule table
        for (Vehicule v : vehiculeService.findAll()) {
            if (v.getBrand() != null && !v.getBrand().isBlank()) {
                brands.add(v.getBrand());
            }
        }
        return brands.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public List<String> getCatalogModels(String brand) {
        Set<String> models = new LinkedHashSet<>();
        // 1. From CatalogService
        for (BrandDto b : catalogService.getAllBrands()) {
            if (brand.equalsIgnoreCase(b.name())) {
                for (VehicleModelDto m : catalogService.getModels(b.id())) {
                    models.add(m.name());
                }
            }
        }
        // 2. From Vehicule table
        for (Vehicule v : vehiculeService.findAll()) {
            if (brand.equalsIgnoreCase(v.getBrand()) && v.getModel() != null && !v.getModel().isBlank()) {
                if (v.getGeneration() != null && !v.getGeneration().isBlank()) {
                    models.add(v.getModel() + " (" + v.getGeneration() + ")");
                } else {
                    models.add(v.getModel());
                }
            }
        }
        return models.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public List<Map<String, String>> getCatalogVersions(String brand, String model) {
        String cleanModel = model.split("\\(")[0].trim();
        List<Map<String, String>> versions = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        // 1. From CatalogService variants
        for (BrandDto b : catalogService.getAllBrands()) {
            if (brand.equalsIgnoreCase(b.name())) {
                for (VehicleModelDto m : catalogService.getModels(b.id())) {
                    if (cleanModel.equalsIgnoreCase(m.name())) {
                        for (FinitionMotorisationDto v : catalogService.getVariants(m.id(), null, null)) {
                            String fullVersion = v.motorisationName() + " - " + v.finitionName();
                            if (seen.add(fullVersion)) {
                                versions.add(Map.of("version", fullVersion));
                            }
                        }
                    }
                }
            }
        }

        // 2. From Vehicule table
        for (Vehicule v : vehiculeService.findAll()) {
            if (brand.equalsIgnoreCase(v.getBrand()) && cleanModel.equalsIgnoreCase(v.getModel()) && v.getVersion() != null) {
                if (seen.add(v.getVersion())) {
                    versions.add(Map.of("version", v.getVersion()));
                }
            }
        }

        versions.sort(Comparator.comparing(a -> a.get("version")));
        return versions;
    }
}
