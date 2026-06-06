package com.example.springbootapp.business.vehicule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.service.UserService;
import com.example.springbootapp.service.VehiculeService;

@Component
public class VehiculeBusiness {

    private final VehiculeService vehiculeService;
    private final UserService userService;

    public VehiculeBusiness(VehiculeService vehiculeService, UserService userService) {
        this.vehiculeService = vehiculeService;
        this.userService = userService;
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
        return vehiculeService.findAll().stream()
                .map(Vehicule::getBrand)
                .filter(b -> b != null && !b.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getCatalogModels(String brand) {
        return vehiculeService.findAll().stream()
                .filter(v -> brand.equalsIgnoreCase(v.getBrand()))
                .map(v -> {
                    if (v.getGeneration() != null && !v.getGeneration().isBlank()) {
                        return v.getModel() + " (" + v.getGeneration() + ")";
                    }
                    return v.getModel();
                })
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<Map<String, String>> getCatalogVersions(String brand, String model) {
        String cleanModel = model.split("\\(")[0].trim();
        return vehiculeService.findAll().stream()
                .filter(v -> brand.equalsIgnoreCase(v.getBrand()) && cleanModel.equalsIgnoreCase(v.getModel()))
                .map(v -> Map.of("version", v.getVersion()))
                .distinct()
                .sorted((a, b) -> a.get("version").compareTo(b.get("version")))
                .toList();
    }
}
