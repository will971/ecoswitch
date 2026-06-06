package com.example.springbootapp.controller.user;

import com.example.springbootapp.model.entity.UserVehicleProfile;
import com.example.springbootapp.business.user.UserProfileBusiness;
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

    private final UserProfileBusiness userProfileBusiness;

    public UserProfileController(UserProfileBusiness userProfileBusiness) {
        this.userProfileBusiness = userProfileBusiness;
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les profils véhicules", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<List<UserVehicleProfile>> getProfiles(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        List<UserVehicleProfile> profiles = userProfileBusiness.getProfiles(email);
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
        UserVehicleProfile saved = userProfileBusiness.createProfile(profileInput, email);
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
        Optional<UserVehicleProfile> saved = userProfileBusiness.updateProfile(id, profileInput, email);
        return saved.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un profil véhicule", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        boolean deleted = userProfileBusiness.deleteProfile(id, email);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
