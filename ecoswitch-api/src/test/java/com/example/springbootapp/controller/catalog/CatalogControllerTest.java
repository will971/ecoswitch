package com.example.springbootapp.controller.catalog;

import com.example.springbootapp.business.catalog.CatalogBusiness;
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
import com.example.springbootapp.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CatalogControllerTest {

    private CatalogService catalogService;
    private CatalogBusiness catalogBusiness;
    private CatalogController catalogController;

    @BeforeEach
    void setUp() {
        catalogService = Mockito.mock(CatalogService.class);
        catalogBusiness = new CatalogBusiness(catalogService);
        catalogController = new CatalogController(catalogBusiness);
    }

    @Test
    void shouldCreateAndListBrands() {
        Brand brand = new Brand("Tesla", "https://logo.com/tesla.svg");
        brand.setId(1L);

        when(catalogService.createBrand(any(Brand.class))).thenReturn(brand);
        when(catalogService.getAllBrands()).thenReturn(List.of(new BrandDto(1L, "Tesla", "https://logo.com/tesla.svg", 3)));

        ResponseEntity<Brand> createRes = catalogController.createBrand(brand);
        assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        assertNotNull(createRes.getBody());
        assertEquals("Tesla", createRes.getBody().getName());

        ResponseEntity<List<BrandDto>> listRes = catalogController.getAllBrands();
        assertEquals(HttpStatus.OK, listRes.getStatusCode());
        assertEquals(1, listRes.getBody().size());
        assertEquals("Tesla", listRes.getBody().get(0).name());
    }

    @Test
    void shouldHandleModelAndVariantRequests() {
        Brand brand = new Brand("Renault", "https://logo.com/renault.svg");
        brand.setId(1L);

        VehicleModel model = new VehicleModel("Megane E-Tech", brand, "https://car.com/megane.png", "Compacte");
        model.setId(10L);

        Motorisation mot = new Motorisation("EV60 220 ch", FuelType.ELECTRIC, 15.5, 220, 60.0, model);
        mot.setId(100L);

        Finition fin = new Finition("Iconic", "https://car.com/iconic.png", model);
        fin.setId(200L);

        FinitionMotorisation variant = new FinitionMotorisation(fin, mot, 42000.0, 320.0, 300.0, 250.0, 20000.0);
        variant.setId(500L);

        when(catalogService.createModel(eq(1L), any(VehicleModel.class))).thenReturn(model);
        when(catalogService.createMotorisation(eq(10L), any(Motorisation.class))).thenReturn(mot);
        when(catalogService.createFinition(eq(10L), any(Finition.class))).thenReturn(fin);
        when(catalogService.createVariant(eq(200L), eq(100L), any(FinitionMotorisation.class))).thenReturn(variant);

        ResponseEntity<VehicleModel> modelRes = catalogController.createModel(1L, model);
        assertEquals(HttpStatus.CREATED, modelRes.getStatusCode());
        assertEquals("Megane E-Tech", modelRes.getBody().getName());

        ResponseEntity<Motorisation> motRes = catalogController.createMotorisation(10L, mot);
        assertEquals(HttpStatus.CREATED, motRes.getStatusCode());
        assertEquals(15.5, motRes.getBody().getConsumptionWltp());

        ResponseEntity<Finition> finRes = catalogController.createFinition(10L, fin);
        assertEquals(HttpStatus.CREATED, finRes.getStatusCode());
        assertEquals("Iconic", finRes.getBody().getName());

        ResponseEntity<FinitionMotorisation> variantRes = catalogController.createVariant(200L, 100L, variant);
        assertEquals(HttpStatus.CREATED, variantRes.getStatusCode());
        assertEquals(42000.0, variantRes.getBody().getPurchasePrice());
    }

    @Test
    void shouldReturnFullHierarchy() {
        CatalogHierarchyDto hierarchy = new CatalogHierarchyDto(
                1L, "Renault", "https://logo.com/renault.svg", List.of()
        );
        when(catalogService.getFullHierarchy()).thenReturn(List.of(hierarchy));

        ResponseEntity<List<CatalogHierarchyDto>> res = catalogController.getFullHierarchy();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
        assertEquals("Renault", res.getBody().get(0).name());
    }
}
