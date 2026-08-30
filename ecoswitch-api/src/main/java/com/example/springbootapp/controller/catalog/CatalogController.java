package com.example.springbootapp.controller.catalog;

import com.example.springbootapp.business.catalog.CatalogBusiness;
import com.example.springbootapp.model.dto.BrandDto;
import com.example.springbootapp.model.dto.CatalogHierarchyDto;
import com.example.springbootapp.model.dto.FinitionDto;
import com.example.springbootapp.model.dto.FinitionMotorisationDto;
import com.example.springbootapp.model.dto.MotorisationDto;
import com.example.springbootapp.model.dto.VehicleModelDto;
import com.example.springbootapp.model.entity.Brand;
import com.example.springbootapp.model.entity.Finition;
import com.example.springbootapp.model.entity.FinitionMotorisation;
import com.example.springbootapp.model.entity.Motorisation;
import com.example.springbootapp.model.entity.VehicleModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalogue Relationnel", description = "Services REST CRUD pour les Marques, Modèles, Motorisations, Finitions et Tarifs")
public class CatalogController {

    private final CatalogBusiness catalogBusiness;

    public CatalogController(CatalogBusiness catalogBusiness) {
        this.catalogBusiness = catalogBusiness;
    }

    // ==========================================
    // 1. MARQUES (BRANDS)
    // ==========================================

    @GetMapping("/brands")
    @Operation(summary = "Lister toutes les marques avec leurs logos")
    public ResponseEntity<List<BrandDto>> getAllBrands() {
        return ResponseEntity.ok(catalogBusiness.getAllBrands());
    }

    @GetMapping("/brands/{id}")
    @Operation(summary = "Obtenir une marque par son id")
    public ResponseEntity<Brand> getBrandById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogBusiness.getBrandById(id));
    }

    @PostMapping("/brands")
    @Operation(summary = "Créer une nouvelle marque")
    public ResponseEntity<Brand> createBrand(@RequestBody Brand brand) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogBusiness.createBrand(brand));
    }

    @PutMapping("/brands/{id}")
    @Operation(summary = "Mettre à jour une marque")
    public ResponseEntity<Brand> updateBrand(@PathVariable Long id, @RequestBody Brand brand) {
        return ResponseEntity.ok(catalogBusiness.updateBrand(id, brand));
    }

    @DeleteMapping("/brands/{id}")
    @Operation(summary = "Supprimer une marque")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        catalogBusiness.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 2. MODÈLES (VEHICLE MODELS)
    // ==========================================

    @GetMapping("/models")
    @Operation(summary = "Lister les modèles (filtrables par brandId)")
    public ResponseEntity<List<VehicleModelDto>> getModels(@RequestParam(required = false) Long brandId) {
        return ResponseEntity.ok(catalogBusiness.getModels(brandId));
    }

    @GetMapping("/models/{id}")
    @Operation(summary = "Obtenir un modèle par son id")
    public ResponseEntity<VehicleModel> getModelById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogBusiness.getModelById(id));
    }

    @PostMapping("/models")
    @Operation(summary = "Créer un modèle rattaché à une marque")
    public ResponseEntity<VehicleModel> createModel(@RequestParam Long brandId, @RequestBody VehicleModel model) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogBusiness.createModel(brandId, model));
    }

    @PutMapping("/models/{id}")
    @Operation(summary = "Mettre à jour un modèle")
    public ResponseEntity<VehicleModel> updateModel(@PathVariable Long id, @RequestBody VehicleModel model) {
        return ResponseEntity.ok(catalogBusiness.updateModel(id, model));
    }

    @DeleteMapping("/models/{id}")
    @Operation(summary = "Supprimer un modèle")
    public ResponseEntity<Void> deleteModel(@PathVariable Long id) {
        catalogBusiness.deleteModel(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 3. MOTORISATIONS
    // ==========================================

    @GetMapping("/motorisations")
    @Operation(summary = "Lister les motorisations (filtrables par modelId)")
    public ResponseEntity<List<MotorisationDto>> getMotorisations(@RequestParam(required = false) Long modelId) {
        return ResponseEntity.ok(catalogBusiness.getMotorisations(modelId));
    }

    @GetMapping("/motorisations/{id}")
    @Operation(summary = "Obtenir une motorisation par son id")
    public ResponseEntity<Motorisation> getMotorisationById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogBusiness.getMotorisationById(id));
    }

    @PostMapping("/motorisations")
    @Operation(summary = "Créer une motorisation pour un modèle")
    public ResponseEntity<Motorisation> createMotorisation(@RequestParam Long modelId, @RequestBody Motorisation motorisation) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogBusiness.createMotorisation(modelId, motorisation));
    }

    @PutMapping("/motorisations/{id}")
    @Operation(summary = "Mettre à jour une motorisation")
    public ResponseEntity<Motorisation> updateMotorisation(@PathVariable Long id, @RequestBody Motorisation motorisation) {
        return ResponseEntity.ok(catalogBusiness.updateMotorisation(id, motorisation));
    }

    @DeleteMapping("/motorisations/{id}")
    @Operation(summary = "Supprimer une motorisation")
    public ResponseEntity<Void> deleteMotorisation(@PathVariable Long id) {
        catalogBusiness.deleteMotorisation(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 4. FINITIONS
    // ==========================================

    @GetMapping("/finitions")
    @Operation(summary = "Lister les finitions (filtrables par modelId)")
    public ResponseEntity<List<FinitionDto>> getFinitions(@RequestParam(required = false) Long modelId) {
        return ResponseEntity.ok(catalogBusiness.getFinitions(modelId));
    }

    @GetMapping("/finitions/{id}")
    @Operation(summary = "Obtenir une finition par son id")
    public ResponseEntity<Finition> getFinitionById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogBusiness.getFinitionById(id));
    }

    @PostMapping("/finitions")
    @Operation(summary = "Créer une finition pour un modèle")
    public ResponseEntity<Finition> createFinition(@RequestParam Long modelId, @RequestBody Finition finition) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogBusiness.createFinition(modelId, finition));
    }

    @PutMapping("/finitions/{id}")
    @Operation(summary = "Mettre à jour une finition")
    public ResponseEntity<Finition> updateFinition(@PathVariable Long id, @RequestBody Finition finition) {
        return ResponseEntity.ok(catalogBusiness.updateFinition(id, finition));
    }

    @DeleteMapping("/finitions/{id}")
    @Operation(summary = "Supprimer une finition")
    public ResponseEntity<Void> deleteFinition(@PathVariable Long id) {
        catalogBusiness.deleteFinition(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 5. VARIANTES & TARIFS (FINITION x MOTORISATION)
    // ==========================================

    @GetMapping("/variants")
    @Operation(summary = "Lister les variantes tarifées (filtrables par modelId, motorisationId, finitionId)")
    public ResponseEntity<List<FinitionMotorisationDto>> getVariants(
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) Long motorisationId,
            @RequestParam(required = false) Long finitionId
    ) {
        return ResponseEntity.ok(catalogBusiness.getVariants(modelId, motorisationId, finitionId));
    }

    @GetMapping("/variants/{id}")
    @Operation(summary = "Obtenir une variante tarifée par son id")
    public ResponseEntity<FinitionMotorisation> getVariantById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogBusiness.getVariantById(id));
    }

    @PostMapping("/variants")
    @Operation(summary = "Associer une finition et une motorisation avec leurs tarifs (Comptant, LOA, LLD)")
    public ResponseEntity<FinitionMotorisation> createVariant(
            @RequestParam Long finitionId,
            @RequestParam Long motorisationId,
            @RequestBody FinitionMotorisation variant
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogBusiness.createVariant(finitionId, motorisationId, variant));
    }

    @PutMapping("/variants/{id}")
    @Operation(summary = "Mettre à jour les tarifs d'une variante")
    public ResponseEntity<FinitionMotorisation> updateVariant(@PathVariable Long id, @RequestBody FinitionMotorisation variant) {
        return ResponseEntity.ok(catalogBusiness.updateVariant(id, variant));
    }

    @DeleteMapping("/variants/{id}")
    @Operation(summary = "Supprimer une variante")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long id) {
        catalogBusiness.deleteVariant(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 6. ARBORESCENCE HIÉRARCHIQUE COMPLÈTE
    // ==========================================

    @GetMapping("/hierarchy")
    @Operation(summary = "Récupérer l'arborescence complète du catalogue (Marque > Modèle > Motorisation & Finitions tarifées)")
    public ResponseEntity<List<CatalogHierarchyDto>> getFullHierarchy() {
        return ResponseEntity.ok(catalogBusiness.getFullHierarchy());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
