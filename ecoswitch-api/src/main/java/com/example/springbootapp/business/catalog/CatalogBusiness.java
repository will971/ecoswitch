package com.example.springbootapp.business.catalog;

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
import com.example.springbootapp.service.CatalogService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogBusiness {

    private final CatalogService catalogService;

    public CatalogBusiness(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // Brands
    public List<BrandDto> getAllBrands() {
        return catalogService.getAllBrands();
    }

    public Brand getBrandById(Long id) {
        return catalogService.getBrandById(id);
    }

    public Brand createBrand(Brand brand) {
        return catalogService.createBrand(brand);
    }

    public Brand updateBrand(Long id, Brand brand) {
        return catalogService.updateBrand(id, brand);
    }

    public void deleteBrand(Long id) {
        catalogService.deleteBrand(id);
    }

    // Models
    public List<VehicleModelDto> getModels(Long brandId) {
        return catalogService.getModels(brandId);
    }

    public VehicleModel getModelById(Long id) {
        return catalogService.getModelById(id);
    }

    public VehicleModel createModel(Long brandId, VehicleModel model) {
        return catalogService.createModel(brandId, model);
    }

    public VehicleModel updateModel(Long id, VehicleModel model) {
        return catalogService.updateModel(id, model);
    }

    public void deleteModel(Long id) {
        catalogService.deleteModel(id);
    }

    // Motorisations
    public List<MotorisationDto> getMotorisations(Long modelId) {
        return catalogService.getMotorisations(modelId);
    }

    public Motorisation getMotorisationById(Long id) {
        return catalogService.getMotorisationById(id);
    }

    public Motorisation createMotorisation(Long modelId, Motorisation motorisation) {
        return catalogService.createMotorisation(modelId, motorisation);
    }

    public Motorisation updateMotorisation(Long id, Motorisation motorisation) {
        return catalogService.updateMotorisation(id, motorisation);
    }

    public void deleteMotorisation(Long id) {
        catalogService.deleteMotorisation(id);
    }

    // Finitions
    public List<FinitionDto> getFinitions(Long modelId) {
        return catalogService.getFinitions(modelId);
    }

    public Finition getFinitionById(Long id) {
        return catalogService.getFinitionById(id);
    }

    public Finition createFinition(Long modelId, Finition finition) {
        return catalogService.createFinition(modelId, finition);
    }

    public Finition updateFinition(Long id, Finition finition) {
        return catalogService.updateFinition(id, finition);
    }

    public void deleteFinition(Long id) {
        catalogService.deleteFinition(id);
    }

    // Variants & Pricing
    public List<FinitionMotorisationDto> getVariants(Long modelId, Long motorisationId, Long finitionId) {
        return catalogService.getVariants(modelId, motorisationId, finitionId);
    }

    public FinitionMotorisation getVariantById(Long id) {
        return catalogService.getVariantById(id);
    }

    public FinitionMotorisation createVariant(Long finitionId, Long motorisationId, FinitionMotorisation variant) {
        return catalogService.createVariant(finitionId, motorisationId, variant);
    }

    public FinitionMotorisation updateVariant(Long id, FinitionMotorisation variant) {
        return catalogService.updateVariant(id, variant);
    }

    public void deleteVariant(Long id) {
        catalogService.deleteVariant(id);
    }

    // Hierarchy
    public List<CatalogHierarchyDto> getFullHierarchy() {
        return catalogService.getFullHierarchy();
    }
}
