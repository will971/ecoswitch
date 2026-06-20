package com.example.springbootapp.business.vehicule;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.repository.AppUserRepository;
import com.example.springbootapp.service.VehiculeService;

@SpringBootTest
@Transactional
class VehiculeBusinessTest {

    @Autowired
    private VehiculeBusiness vehiculeBusiness;

    @Autowired
    private VehiculeService vehiculeService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldCreateVehiculeForUser() {
        AppUser user = new AppUser("user@test.com", "User Test", "hash", "email", "Pro");
        appUserRepository.save(user);

        Vehicule v = new Vehicule();
        v.setName("User Car");
        v.setBrand("Tesla");
        v.setModel("Model 3");
        v.setVersion("Standard");
        v.setFuelType(FuelType.ELECTRIC);

        Vehicule created = vehiculeBusiness.create(v, "user@test.com");
        assertNotNull(created.getId());
        assertEquals("user@test.com", created.getCreatedBy());
        assertEquals("PUBLIC", created.getVisibility());
    }

    @Test
    void shouldThrowWhenCreatingWithoutEmail() {
        Vehicule v = new Vehicule();
        v.setName("No Owner Car");
        v.setBrand("Tesla");
        v.setModel("Model 3");
        v.setVersion("Standard");
        v.setFuelType(FuelType.ELECTRIC);

        assertThrows(ResponseStatusException.class, () -> vehiculeBusiness.create(v, null));
    }

    @Test
    void shouldThrowWhenCreatingWithUnknownEmail() {
        Vehicule v = new Vehicule();
        v.setName("No Owner Car");
        v.setBrand("Tesla");
        v.setModel("Model 3");
        v.setVersion("Standard");
        v.setFuelType(FuelType.ELECTRIC);

        assertThrows(ResponseStatusException.class, () -> vehiculeBusiness.create(v, "unknown@test.com"));
    }

    @Test
    void shouldFilterVehiculesByVisibility() {
        // Create an admin
        AppUser admin = new AppUser("admin@test.com", "Admin Test", "hash", "email", "Pro", "ADMIN");
        appUserRepository.save(admin);

        // Create a user
        AppUser user = new AppUser("owner@test.com", "Owner Test", "hash", "email", "Pro", "USER");
        appUserRepository.save(user);

        // Public vehicle
        Vehicule pub = new Vehicule();
        pub.setName("Public Tesla");
        pub.setBrand("Tesla");
        pub.setModel("Model S");
        pub.setVersion("P100D");
        pub.setFuelType(FuelType.ELECTRIC);
        pub.setVisibility("PUBLIC");
        pub.setCreatedBy("owner@test.com");
        vehiculeService.create(pub);

        // Private vehicle
        Vehicule priv = new Vehicule();
        priv.setName("Private Zoe");
        priv.setBrand("Renault");
        priv.setModel("Zoe");
        priv.setVersion("R110");
        priv.setFuelType(FuelType.ELECTRIC);
        priv.setVisibility("PRIVATE");
        priv.setCreatedBy("owner@test.com");
        vehiculeService.create(priv);

        // Guest user (null principal) should only see public
        List<Vehicule> guestList = vehiculeBusiness.findAll(null, null, null, null, null, null);
        assertTrue(guestList.stream().anyMatch(v -> "Public Tesla".equals(v.getName())));
        assertFalse(guestList.stream().anyMatch(v -> "Private Zoe".equals(v.getName())));

        // Owner should see both public and their own private
        List<Vehicule> ownerList = vehiculeBusiness.findAll(null, null, null, null, null, "owner@test.com");
        assertTrue(ownerList.stream().anyMatch(v -> "Public Tesla".equals(v.getName())));
        assertTrue(ownerList.stream().anyMatch(v -> "Private Zoe".equals(v.getName())));

        // Another user should only see public
        List<Vehicule> otherList = vehiculeBusiness.findAll(null, null, null, null, null, "other@test.com");
        assertTrue(otherList.stream().anyMatch(v -> "Public Tesla".equals(v.getName())));
        assertFalse(otherList.stream().anyMatch(v -> "Private Zoe".equals(v.getName())));

        // Admin should see both public and private
        List<Vehicule> adminList = vehiculeBusiness.findAll(null, null, null, null, null, "admin@test.com");
        assertTrue(adminList.stream().anyMatch(v -> "Public Tesla".equals(v.getName())));
        assertTrue(adminList.stream().anyMatch(v -> "Private Zoe".equals(v.getName())));
    }

    @Test
    void shouldFindByIdRespectingVisibility() {
        AppUser owner = new AppUser("owner@test.com", "Owner Test", "hash", "email", "Pro", "USER");
        appUserRepository.save(owner);
        AppUser other = new AppUser("other@test.com", "Other Test", "hash", "email", "Pro", "USER");
        appUserRepository.save(other);

        Vehicule priv = new Vehicule();
        priv.setName("Private Zoe");
        priv.setBrand("Renault");
        priv.setModel("Zoe");
        priv.setVersion("R110");
        priv.setFuelType(FuelType.ELECTRIC);
        priv.setVisibility("PRIVATE");
        priv.setCreatedBy("owner@test.com");
        vehiculeService.create(priv);

        // Guest denied
        assertThrows(ResponseStatusException.class, () -> vehiculeBusiness.findById(priv.getId(), null));
        // Other user denied
        assertThrows(ResponseStatusException.class, () -> vehiculeBusiness.findById(priv.getId(), "other@test.com"));

        // Owner allowed
        Vehicule foundOwner = vehiculeBusiness.findById(priv.getId(), "owner@test.com");
        assertEquals("Private Zoe", foundOwner.getName());
    }

    @Test
    void shouldUpdateVehiculeWhenOwnerOrAdmin() {
        AppUser owner = new AppUser("owner@test.com", "Owner Test", "hash", "email", "Pro", "USER");
        appUserRepository.save(owner);

        Vehicule v = new Vehicule();
        v.setName("My Zoe");
        v.setBrand("Renault");
        v.setModel("Zoe");
        v.setVersion("R110");
        v.setFuelType(FuelType.ELECTRIC);
        v.setCreatedBy("owner@test.com");
        v = vehiculeService.create(v);

        Vehicule updateInput = new Vehicule();
        updateInput.setName("My Zoe Upd");
        updateInput.setBrand("Renault");
        updateInput.setModel("Zoe");
        updateInput.setVersion("R110");
        updateInput.setFuelType(FuelType.ELECTRIC);

        // Guest update throws
        final Long id = v.getId();
        assertThrows(ResponseStatusException.class, () -> vehiculeBusiness.update(id, updateInput, null));

        // Other user update throws
        assertThrows(ResponseStatusException.class, () -> vehiculeBusiness.update(id, updateInput, "other@test.com"));

        // Owner update succeeds
        Vehicule updated = vehiculeBusiness.update(id, updateInput, "owner@test.com");
        assertEquals("My Zoe Upd", updated.getName());
    }

    @Test
    void shouldDeleteVehiculeWhenOwnerOrAdmin() {
        AppUser owner = new AppUser("owner@test.com", "Owner Test", "hash", "email", "Pro", "USER");
        appUserRepository.save(owner);

        Vehicule v = new Vehicule();
        v.setName("To Delete");
        v.setBrand("Renault");
        v.setModel("Zoe");
        v.setVersion("R110");
        v.setFuelType(FuelType.ELECTRIC);
        v.setCreatedBy("owner@test.com");
        v = vehiculeService.create(v);

        final Long id = v.getId();
        // Guest delete throws
        assertThrows(ResponseStatusException.class, () -> vehiculeBusiness.delete(id, null));

        // Other user delete throws
        assertThrows(ResponseStatusException.class, () -> vehiculeBusiness.delete(id, "other@test.com"));

        // Owner delete succeeds
        vehiculeBusiness.delete(id, "owner@test.com");
        assertThrows(IllegalArgumentException.class, () -> vehiculeService.findById(id));
    }

    @Test
    void shouldGetCatalogBrandsModelsAndVersions() {
        Vehicule v1 = new Vehicule();
        v1.setName("Tesla 3");
        v1.setBrand("Tesla");
        v1.setModel("Model 3");
        v1.setGeneration("2024");
        v1.setVersion("Long Range");
        v1.setFuelType(FuelType.ELECTRIC);
        vehiculeService.create(v1);

        Vehicule v2 = new Vehicule();
        v2.setName("Tesla Y");
        v2.setBrand("Tesla");
        v2.setModel("Model Y");
        v2.setVersion("Standard");
        v2.setFuelType(FuelType.ELECTRIC);
        vehiculeService.create(v2);

        List<String> brands = vehiculeBusiness.getCatalogBrands();
        assertTrue(brands.contains("Tesla"));

        List<String> models = vehiculeBusiness.getCatalogModels("Tesla");
        assertTrue(models.contains("Model 3 (2024)"));
        assertTrue(models.contains("Model Y"));

        List<Map<String, String>> versions = vehiculeBusiness.getCatalogVersions("Tesla", "Model 3 (2024)");
        assertFalse(versions.isEmpty());
        assertTrue(versions.stream().anyMatch(m -> "Long Range".equals(m.get("version"))));
    }
}
