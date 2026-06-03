package com.example.springbootapp.controller.user;

import com.example.springbootapp.model.entity.UserVehicleProfile;
import com.example.springbootapp.repository.UserVehicleProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users/me/vehicle-profiles")
@Tag(name = "User Profile", description = "Gestion du profil véhicule de l'utilisateur")
public class UserProfileController {

    private final UserVehicleProfileRepository userVehicleProfileRepository;

    public UserProfileController(UserVehicleProfileRepository userVehicleProfileRepository) {
        this.userVehicleProfileRepository = userVehicleProfileRepository;
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les profils véhicules", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<List<UserVehicleProfile>> getProfiles(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        List<UserVehicleProfile> profiles = userVehicleProfileRepository.findByUserEmail(email);
        return ResponseEntity.ok(profiles);
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau profil véhicule", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<UserVehicleProfile> createProfile(
            @RequestBody UserVehicleProfile profileInput,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        List<UserVehicleProfile> existingProfiles = userVehicleProfileRepository.findByUserEmail(email);
        
        UserVehicleProfile profile = new UserVehicleProfile();
        profile.setUserEmail(email);
        
        // Si c'est le premier profil ou s'il est explicitement marqué par défaut
        if (existingProfiles.isEmpty() || profileInput.isDefault()) {
            profile.setDefault(true);
            if (profileInput.isDefault()) {
                clearOtherDefaults(existingProfiles);
            }
        } else {
            profile.setDefault(false);
        }

        updateProfileFields(profile, profileInput);
        UserVehicleProfile saved = userVehicleProfileRepository.save(profile);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un profil véhicule existant", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<UserVehicleProfile> updateProfile(
            @PathVariable Long id,
            @RequestBody UserVehicleProfile profileInput,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();

        Optional<UserVehicleProfile> optionalProfile = userVehicleProfileRepository.findById(id);
        if (optionalProfile.isEmpty() || !optionalProfile.get().getUserEmail().equals(email)) {
            return ResponseEntity.notFound().build();
        }

        UserVehicleProfile profile = optionalProfile.get();
        List<UserVehicleProfile> existingProfiles = userVehicleProfileRepository.findByUserEmail(email);

        if (profileInput.isDefault() && !profile.isDefault()) {
            profile.setDefault(true);
            clearOtherDefaults(existingProfiles);
        } else if (!profileInput.isDefault() && profile.isDefault()) {
            // Si l'utilisateur essaie d'enlever le statut par défaut, on l'autorise mais un autre devrait le devenir ?
            // On laisse l'utilisateur gérer
            profile.setDefault(false);
        }

        updateProfileFields(profile, profileInput);
        UserVehicleProfile saved = userVehicleProfileRepository.save(profile);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un profil véhicule", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        Optional<UserVehicleProfile> optionalProfile = userVehicleProfileRepository.findById(id);
        if (optionalProfile.isEmpty() || !optionalProfile.get().getUserEmail().equals(email)) {
            return ResponseEntity.notFound().build();
        }

        UserVehicleProfile profileToDelete = optionalProfile.get();
        userVehicleProfileRepository.delete(profileToDelete);

        // Si le profil supprimé était par défaut, on assigne un nouveau par défaut si possible
        if (profileToDelete.isDefault()) {
            List<UserVehicleProfile> remaining = userVehicleProfileRepository.findByUserEmail(email);
            if (!remaining.isEmpty()) {
                UserVehicleProfile newDefault = remaining.get(0);
                newDefault.setDefault(true);
                userVehicleProfileRepository.save(newDefault);
            }
        }

        return ResponseEntity.noContent().build();
    }

    private void clearOtherDefaults(List<UserVehicleProfile> profiles) {
        for (UserVehicleProfile p : profiles) {
            if (p.isDefault()) {
                p.setDefault(false);
                userVehicleProfileRepository.save(p);
            }
        }
    }

    private void updateProfileFields(UserVehicleProfile profile, UserVehicleProfile input) {
        profile.setName(input.getName());
        profile.setFuelType(input.getFuelType());
        profile.setConsumption(input.getConsumption());
        profile.setAnnualMileage(input.getAnnualMileage());
        profile.setInsuranceCost(input.getInsuranceCost());
        profile.setMaintenanceCost(input.getMaintenanceCost());
        profile.setResaleValue(input.getResaleValue());
        profile.setPetrolPrice(input.getPetrolPrice());
        profile.setDieselPrice(input.getDieselPrice());
        profile.setElectricPrice(input.getElectricPrice());
    }
}
