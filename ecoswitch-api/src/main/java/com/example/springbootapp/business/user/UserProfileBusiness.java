package com.example.springbootapp.business.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.springbootapp.model.entity.UserVehicleProfile;
import com.example.springbootapp.service.UserVehicleProfileService;

@Component
public class UserProfileBusiness {

    private final UserVehicleProfileService profileService;

    public UserProfileBusiness(UserVehicleProfileService profileService) {
        this.profileService = profileService;
    }

    public List<UserVehicleProfile> getProfiles(String email) {
        return profileService.findByUserEmail(email);
    }

    public UserVehicleProfile createProfile(UserVehicleProfile profileInput, String email) {
        List<UserVehicleProfile> existingProfiles = profileService.findByUserEmail(email);
        
        UserVehicleProfile profile = new UserVehicleProfile();
        profile.setUserEmail(email);
        
        if (existingProfiles.isEmpty() || profileInput.isDefault()) {
            profile.setDefault(true);
            if (profileInput.isDefault()) {
                clearOtherDefaults(existingProfiles);
            }
        } else {
            profile.setDefault(false);
        }

        updateProfileFields(profile, profileInput);
        return profileService.save(profile);
    }

    public Optional<UserVehicleProfile> updateProfile(Long id, UserVehicleProfile profileInput, String email) {
        Optional<UserVehicleProfile> optionalProfile = profileService.findById(id);
        if (optionalProfile.isEmpty() || !optionalProfile.get().getUserEmail().equals(email)) {
            return Optional.empty();
        }

        UserVehicleProfile profile = optionalProfile.get();
        List<UserVehicleProfile> existingProfiles = profileService.findByUserEmail(email);

        if (profileInput.isDefault() && !profile.isDefault()) {
            profile.setDefault(true);
            clearOtherDefaults(existingProfiles);
        } else if (!profileInput.isDefault() && profile.isDefault()) {
            profile.setDefault(false);
        }

        updateProfileFields(profile, profileInput);
        return Optional.of(profileService.save(profile));
    }

    public boolean deleteProfile(Long id, String email) {
        Optional<UserVehicleProfile> optionalProfile = profileService.findById(id);
        if (optionalProfile.isEmpty() || !optionalProfile.get().getUserEmail().equals(email)) {
            return false;
        }

        UserVehicleProfile profileToDelete = optionalProfile.get();
        profileService.delete(profileToDelete);

        if (profileToDelete.isDefault()) {
            List<UserVehicleProfile> remaining = profileService.findByUserEmail(email);
            if (!remaining.isEmpty()) {
                UserVehicleProfile newDefault = remaining.get(0);
                newDefault.setDefault(true);
                profileService.save(newDefault);
            }
        }

        return true;
    }

    private void clearOtherDefaults(List<UserVehicleProfile> profiles) {
        for (UserVehicleProfile p : profiles) {
            if (p.isDefault()) {
                p.setDefault(false);
                profileService.save(p);
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
