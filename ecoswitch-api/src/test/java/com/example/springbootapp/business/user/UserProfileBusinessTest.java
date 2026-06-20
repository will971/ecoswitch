package com.example.springbootapp.business.user;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.UserVehicleProfile;
import com.example.springbootapp.service.UserVehicleProfileService;

@SpringBootTest
@Transactional
class UserProfileBusinessTest {

    @Autowired
    private UserProfileBusiness userProfileBusiness;

    @Autowired
    private UserVehicleProfileService profileService;

    @Test
    void shouldCreateAndManageProfiles() {
        String email = "profile-owner@test.com";

        // 1. Initially no profiles
        List<UserVehicleProfile> profiles = userProfileBusiness.getProfiles(email);
        assertTrue(profiles.isEmpty());

        // 2. Create first profile (should automatically become default)
        UserVehicleProfile input1 = new UserVehicleProfile();
        input1.setName("First Profile");
        input1.setFuelType(FuelType.DIESEL);
        input1.setConsumption(6.0);
        input1.setAnnualMileage(12000);
        input1.setDefault(false); // even if input is false, it should become true since it's the first one

        UserVehicleProfile created1 = userProfileBusiness.createProfile(input1, email);
        assertNotNull(created1.getId());
        assertEquals("First Profile", created1.getName());
        assertTrue(created1.isDefault());
        assertEquals(email, created1.getUserEmail());

        // 3. Create second profile as default
        UserVehicleProfile input2 = new UserVehicleProfile();
        input2.setName("Second Profile");
        input2.setFuelType(FuelType.ELECTRIC);
        input2.setConsumption(16.0);
        input2.setAnnualMileage(15000);
        input2.setDefault(true);

        UserVehicleProfile created2 = userProfileBusiness.createProfile(input2, email);
        assertNotNull(created2.getId());
        assertTrue(created2.isDefault());

        // 4. Verify first profile is no longer default
        UserVehicleProfile updatedCreated1 = profileService.findById(created1.getId()).orElseThrow();
        assertFalse(updatedCreated1.isDefault());

        // 5. Update first profile to be default
        UserVehicleProfile updateInput = new UserVehicleProfile();
        updateInput.setName("First Profile Updated");
        updateInput.setFuelType(FuelType.DIESEL);
        updateInput.setConsumption(5.8);
        updateInput.setAnnualMileage(13000);
        updateInput.setDefault(true);

        Optional<UserVehicleProfile> updatedOpt = userProfileBusiness.updateProfile(created1.getId(), updateInput, email);
        assertTrue(updatedOpt.isPresent());
        assertTrue(updatedOpt.get().isDefault());
        assertEquals("First Profile Updated", updatedOpt.get().getName());

        // Second profile should no longer be default
        UserVehicleProfile updatedCreated2 = profileService.findById(created2.getId()).orElseThrow();
        assertFalse(updatedCreated2.isDefault());

        // 6. Delete default profile (should promote the remaining profile to default)
        boolean deleted = userProfileBusiness.deleteProfile(created1.getId(), email);
        assertTrue(deleted);

        // Verification of promotion
        UserVehicleProfile remaining = profileService.findById(created2.getId()).orElseThrow();
        assertTrue(remaining.isDefault());

        // Clean deletion
        boolean deleted2 = userProfileBusiness.deleteProfile(created2.getId(), email);
        assertTrue(deleted2);
        assertTrue(profileService.findByUserEmail(email).isEmpty());
    }

    @Test
    void shouldReturnEmptyOrFalseOnMismatchingUserOrMissingProfile() {
        String email = "profile-owner@test.com";

        // Create a profile for owner
        UserVehicleProfile input = new UserVehicleProfile();
        input.setName("Zoe");
        input.setFuelType(FuelType.ELECTRIC);
        input.setConsumption(15.0);
        input.setDefault(true);
        UserVehicleProfile created = userProfileBusiness.createProfile(input, email);

        // Try updating with different email -> returns empty
        Optional<UserVehicleProfile> updated = userProfileBusiness.updateProfile(created.getId(), input, "wrong-owner@test.com");
        assertTrue(updated.isEmpty());

        // Try deleting with different email -> returns false
        boolean deleted = userProfileBusiness.deleteProfile(created.getId(), "wrong-owner@test.com");
        assertFalse(deleted);

        // Try deleting non-existent id -> returns false
        boolean deletedNonExistent = userProfileBusiness.deleteProfile(99999L, email);
        assertFalse(deletedNonExistent);
    }
}
