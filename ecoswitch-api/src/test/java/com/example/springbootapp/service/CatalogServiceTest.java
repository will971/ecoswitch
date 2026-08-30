package com.example.springbootapp.service;

import com.example.springbootapp.model.dto.BrandDto;
import com.example.springbootapp.model.dto.CatalogHierarchyDto;
import com.example.springbootapp.model.dto.FinitionMotorisationDto;
import com.example.springbootapp.model.dto.MotorisationDto;
import com.example.springbootapp.model.dto.VehicleModelDto;
import com.example.springbootapp.model.entity.Brand;
import com.example.springbootapp.model.entity.Finition;
import com.example.springbootapp.model.entity.FinitionMotorisation;
import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Motorisation;
import com.example.springbootapp.model.entity.VehicleModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CatalogServiceTest {

    @Autowired
    private CatalogService catalogService;

    @Test
    void shouldPerformCompleteCatalogCrudWorkflow() {
        // 1. Create Brand
        Brand brand = catalogService.createBrand(new Brand("Peugeot", "/uploads/brands/peugeot.svg"));
        assertNotNull(brand.getId());
        assertEquals("Peugeot", brand.getName());

        // 2. Create Model
        VehicleModel model = catalogService.createModel(
                brand.getId(),
                new VehicleModel("208", null, "/uploads/models/208.png", "Citadine")
        );
        assertNotNull(model.getId());
        assertEquals("208", model.getName());
        assertEquals("Peugeot", model.getBrand().getName());

        // 3. Create Motorisations
        Motorisation elec = catalogService.createMotorisation(
                model.getId(),
                new Motorisation("Électrique 156 ch (54 kWh)", FuelType.ELECTRIC, 14.5, 156, 54.0, null)
        );
        assertNotNull(elec.getId());
        assertEquals(14.5, elec.getConsumptionWltp());

        Motorisation hybrid = catalogService.createMotorisation(
                model.getId(),
                new Motorisation("Hybrid 136 e-DCS6", FuelType.HYBRID, 4.5, 136, null, null)
        );
        assertNotNull(hybrid.getId());
        assertEquals(4.5, hybrid.getConsumptionWltp());

        // 4. Create Finitions
        Finition allure = catalogService.createFinition(
                model.getId(),
                new Finition("Allure", "/uploads/finitions/allure.png", null)
        );
        assertNotNull(allure.getId());

        Finition gt = catalogService.createFinition(
                model.getId(),
                new Finition("GT", "/uploads/finitions/gt.png", null)
        );
        assertNotNull(gt.getId());

        // 5. Create Many-to-Many Variant Pricings (Finition x Motorisation)
        // 208 Allure Électrique
        FinitionMotorisation v1 = catalogService.createVariant(
                allure.getId(),
                elec.getId(),
                new FinitionMotorisation(null, null, 34800.0, 270.0, 250.0, 600.0, 240.0, 16000.0)
        );
        assertNotNull(v1.getId());
        assertEquals(34800.0, v1.getPurchasePrice());
        assertEquals(270.0, v1.getMonthlyLoa());

        // 208 GT Électrique
        FinitionMotorisation v2 = catalogService.createVariant(
                gt.getId(),
                elec.getId(),
                new FinitionMotorisation(null, null, 37100.0, 310.0, 290.0, 640.0, 250.0, 17500.0)
        );
        assertNotNull(v2.getId());

        // 208 GT Hybrid
        FinitionMotorisation v3 = catalogService.createVariant(
                gt.getId(),
                hybrid.getId(),
                new FinitionMotorisation(null, null, 28500.0, 230.0, 210.0, 580.0, 360.0, 13000.0)
        );
        assertNotNull(v3.getId());

        // 6. Test querying DTOs
        List<BrandDto> brands = catalogService.getAllBrands();
        assertTrue(brands.stream().anyMatch(b -> "Peugeot".equals(b.name())));

        List<VehicleModelDto> models = catalogService.getModels(brand.getId());
        assertEquals(1, models.size());
        assertEquals("208", models.get(0).name());

        List<MotorisationDto> motorisations = catalogService.getMotorisations(model.getId());
        assertEquals(2, motorisations.size());

        List<FinitionMotorisationDto> elecVariants = catalogService.getVariants(model.getId(), elec.getId(), null);
        assertEquals(2, elecVariants.size());

        List<FinitionMotorisationDto> gtVariants = catalogService.getVariants(model.getId(), null, gt.getId());
        assertEquals(2, gtVariants.size()); // GT is available with Elec and Hybrid!

        // 7. Test Full Hierarchy
        List<CatalogHierarchyDto> hierarchy = catalogService.getFullHierarchy();
        assertFalse(hierarchy.isEmpty());
        CatalogHierarchyDto peugeotTree = hierarchy.stream().filter(h -> "Peugeot".equals(h.name())).findFirst().orElseThrow();
        assertEquals(1, peugeotTree.models().size());
        assertEquals(2, peugeotTree.models().get(0).motorisations().size());
        assertEquals(2, peugeotTree.models().get(0).finitions().size());
    }
}
