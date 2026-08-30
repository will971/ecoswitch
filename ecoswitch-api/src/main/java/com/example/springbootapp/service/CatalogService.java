package com.example.springbootapp.service;

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
import com.example.springbootapp.repository.BrandRepository;
import com.example.springbootapp.repository.FinitionMotorisationRepository;
import com.example.springbootapp.repository.FinitionRepository;
import com.example.springbootapp.repository.MotorisationRepository;
import com.example.springbootapp.repository.VehicleModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogService {

    private final BrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;
    private final MotorisationRepository motorisationRepository;
    private final FinitionRepository finitionRepository;
    private final FinitionMotorisationRepository variantRepository;

    public CatalogService(
            BrandRepository brandRepository,
            VehicleModelRepository modelRepository,
            MotorisationRepository motorisationRepository,
            FinitionRepository finitionRepository,
            FinitionMotorisationRepository variantRepository
    ) {
        this.brandRepository = brandRepository;
        this.modelRepository = modelRepository;
        this.motorisationRepository = motorisationRepository;
        this.finitionRepository = finitionRepository;
        this.variantRepository = variantRepository;
    }

    // ==========================================
    // 1. BRAND OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<BrandDto> getAllBrands() {
        List<Brand> brands = brandRepository.findAll();
        Map<Long, Long> modelCountByBrand = modelRepository.findAll().stream()
                .collect(Collectors.groupingBy(m -> m.getBrand().getId(), Collectors.counting()));

        return brands.stream()
                .sorted(Comparator.comparing(Brand::getName, String.CASE_INSENSITIVE_ORDER))
                .map(b -> new BrandDto(
                        b.getId(),
                        b.getName(),
                        b.getLogoUrl(),
                        modelCountByBrand.getOrDefault(b.getId(), 0L).intValue()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marque introuvable avec l'id : " + id));
    }

    public Brand createBrand(Brand brand) {
        if (brand.getName() == null || brand.getName().isBlank()) {
            throw new IllegalArgumentException("Le nom de la marque est obligatoire.");
        }
        String cleanName = brand.getName().trim();
        if (brandRepository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("Une marque avec le nom '" + cleanName + "' existe déjà.");
        }
        brand.setName(cleanName);
        return brandRepository.save(brand);
    }

    public Brand updateBrand(Long id, Brand updated) {
        Brand existing = getBrandById(id);
        if (updated.getName() != null && !updated.getName().isBlank()) {
            String cleanName = updated.getName().trim();
            if (!cleanName.equalsIgnoreCase(existing.getName()) && brandRepository.existsByNameIgnoreCase(cleanName)) {
                throw new IllegalArgumentException("Une marque avec le nom '" + cleanName + "' existe déjà.");
            }
            existing.setName(cleanName);
        }
        if (updated.getLogoUrl() != null) {
            existing.setLogoUrl(updated.getLogoUrl());
        }
        return brandRepository.save(existing);
    }

    public void deleteBrand(Long id) {
        Brand existing = getBrandById(id);
        brandRepository.delete(existing);
    }

    // ==========================================
    // 2. MODEL OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<VehicleModelDto> getModels(Long brandId) {
        List<VehicleModel> models = brandId != null
                ? modelRepository.findByBrandIdOrderByNameAsc(brandId)
                : modelRepository.findAll().stream().sorted(Comparator.comparing(VehicleModel::getName, String.CASE_INSENSITIVE_ORDER)).toList();

        Map<Long, Long> motCountByModel = motorisationRepository.findAll().stream()
                .collect(Collectors.groupingBy(m -> m.getModel().getId(), Collectors.counting()));
        Map<Long, Long> finCountByModel = finitionRepository.findAll().stream()
                .collect(Collectors.groupingBy(f -> f.getModel().getId(), Collectors.counting()));

        return models.stream()
                .map(m -> new VehicleModelDto(
                        m.getId(),
                        m.getBrand().getId(),
                        m.getBrand().getName(),
                        m.getBrand().getLogoUrl(),
                        m.getName(),
                        m.getImageUrl(),
                        m.getCategory(),
                        motCountByModel.getOrDefault(m.getId(), 0L).intValue(),
                        finCountByModel.getOrDefault(m.getId(), 0L).intValue()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleModel getModelById(Long id) {
        return modelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modèle introuvable avec l'id : " + id));
    }

    public VehicleModel createModel(Long brandId, VehicleModel model) {
        if (model.getName() == null || model.getName().isBlank()) {
            throw new IllegalArgumentException("Le nom du modèle est obligatoire.");
        }
        Brand brand = getBrandById(brandId);
        String cleanName = model.getName().trim();
        Optional<VehicleModel> duplicate = modelRepository.findByBrandIdAndNameIgnoreCase(brandId, cleanName);
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException("Le modèle '" + cleanName + "' existe déjà pour la marque " + brand.getName());
        }
        model.setBrand(brand);
        model.setName(cleanName);
        return modelRepository.save(model);
    }

    public VehicleModel updateModel(Long id, VehicleModel updated) {
        VehicleModel existing = getModelById(id);
        if (updated.getName() != null && !updated.getName().isBlank()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getImageUrl() != null) {
            existing.setImageUrl(updated.getImageUrl());
        }
        if (updated.getCategory() != null) {
            existing.setCategory(updated.getCategory());
        }
        return modelRepository.save(existing);
    }

    public void deleteModel(Long id) {
        VehicleModel existing = getModelById(id);
        modelRepository.delete(existing);
    }

    // ==========================================
    // 3. MOTORISATION OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<MotorisationDto> getMotorisations(Long modelId) {
        List<Motorisation> list = modelId != null
                ? motorisationRepository.findByModelIdOrderByNameAsc(modelId)
                : motorisationRepository.findAll();

        return list.stream()
                .map(this::toMotorisationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Motorisation getMotorisationById(Long id) {
        return motorisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Motorisation introuvable avec l'id : " + id));
    }

    public Motorisation createMotorisation(Long modelId, Motorisation motorisation) {
        if (motorisation.getName() == null || motorisation.getName().isBlank()) {
            throw new IllegalArgumentException("Le nom de la motorisation est obligatoire.");
        }
        if (motorisation.getFuelType() == null) {
            throw new IllegalArgumentException("Le type de carburant/énergie est obligatoire.");
        }
        if (motorisation.getConsumptionWltp() < 0) {
            throw new IllegalArgumentException("La consommation WLTP ne peut pas être négative.");
        }
        VehicleModel model = getModelById(modelId);
        motorisation.setModel(model);
        motorisation.setName(motorisation.getName().trim());
        return motorisationRepository.save(motorisation);
    }

    public Motorisation updateMotorisation(Long id, Motorisation updated) {
        Motorisation existing = getMotorisationById(id);
        if (updated.getName() != null && !updated.getName().isBlank()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getFuelType() != null) {
            existing.setFuelType(updated.getFuelType());
        }
        if (updated.getConsumptionWltp() >= 0) {
            existing.setConsumptionWltp(updated.getConsumptionWltp());
        }
        if (updated.getPowerHp() != null) {
            existing.setPowerHp(updated.getPowerHp());
        }
        if (updated.getBatteryCapacityKwh() != null) {
            existing.setBatteryCapacityKwh(updated.getBatteryCapacityKwh());
        }
        return motorisationRepository.save(existing);
    }

    public void deleteMotorisation(Long id) {
        Motorisation existing = getMotorisationById(id);
        motorisationRepository.delete(existing);
    }

    // ==========================================
    // 4. FINITION OPERATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<FinitionDto> getFinitions(Long modelId) {
        List<Finition> list = modelId != null
                ? finitionRepository.findByModelIdOrderByNameAsc(modelId)
                : finitionRepository.findAll();

        return list.stream()
                .map(this::toFinitionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Finition getFinitionById(Long id) {
        return finitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Finition introuvable avec l'id : " + id));
    }

    public Finition createFinition(Long modelId, Finition finition) {
        if (finition.getName() == null || finition.getName().isBlank()) {
            throw new IllegalArgumentException("Le nom de la finition est obligatoire.");
        }
        VehicleModel model = getModelById(modelId);
        finition.setModel(model);
        finition.setName(finition.getName().trim());
        return finitionRepository.save(finition);
    }

    public Finition updateFinition(Long id, Finition updated) {
        Finition existing = getFinitionById(id);
        if (updated.getName() != null && !updated.getName().isBlank()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getImageUrl() != null) {
            existing.setImageUrl(updated.getImageUrl());
        }
        return finitionRepository.save(existing);
    }

    public void deleteFinition(Long id) {
        Finition existing = getFinitionById(id);
        finitionRepository.delete(existing);
    }

    // ==========================================
    // 5. FINITION MOTORISATION (VARIANTS / PRICING)
    // ==========================================

    @Transactional(readOnly = true)
    public List<FinitionMotorisationDto> getVariants(Long modelId, Long motorisationId, Long finitionId) {
        List<FinitionMotorisation> list;
        if (motorisationId != null) {
            list = variantRepository.findByMotorisationIdWithDetails(motorisationId);
        } else if (finitionId != null) {
            list = variantRepository.findByFinitionIdWithDetails(finitionId);
        } else if (modelId != null) {
            list = variantRepository.findByModelIdWithDetails(modelId);
        } else {
            list = variantRepository.findAllWithDetails();
        }

        return list.stream()
                .map(this::toFinitionMotorisationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FinitionMotorisation getVariantById(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Variante introuvable avec l'id : " + id));
    }

    public FinitionMotorisation createVariant(Long finitionId, Long motorisationId, FinitionMotorisation variant) {
        Finition finition = getFinitionById(finitionId);
        Motorisation motorisation = getMotorisationById(motorisationId);

        if (!finition.getModel().getId().equals(motorisation.getModel().getId())) {
            throw new IllegalArgumentException("La finition et la motorisation doivent appartenir au même modèle.");
        }

        Optional<FinitionMotorisation> existing = variantRepository.findByFinitionIdAndMotorisationId(finitionId, motorisationId);
        if (existing.isPresent()) {
            FinitionMotorisation current = existing.get();
            current.setPurchasePrice(variant.getPurchasePrice());
            current.setMonthlyLoa(variant.getMonthlyLoa());
            current.setMonthlyLld(variant.getMonthlyLld());
            if (variant.getDefaultInsuranceCost() != null) current.setDefaultInsuranceCost(variant.getDefaultInsuranceCost());
            if (variant.getDefaultMaintenanceCost() != null) current.setDefaultMaintenanceCost(variant.getDefaultMaintenanceCost());
            if (variant.getEstimatedResaleValue() != null) current.setEstimatedResaleValue(variant.getEstimatedResaleValue());
            return variantRepository.save(current);
        }

        variant.setFinition(finition);
        variant.setMotorisation(motorisation);
        return variantRepository.save(variant);
    }

    public FinitionMotorisation updateVariant(Long id, FinitionMotorisation updated) {
        FinitionMotorisation existing = getVariantById(id);
        if (updated.getPurchasePrice() > 0) {
            existing.setPurchasePrice(updated.getPurchasePrice());
        }
        if (updated.getMonthlyLoa() != null) {
            existing.setMonthlyLoa(updated.getMonthlyLoa());
        }
        if (updated.getMonthlyLld() != null) {
            existing.setMonthlyLld(updated.getMonthlyLld());
        }
        if (updated.getDefaultInsuranceCost() != null) {
            existing.setDefaultInsuranceCost(updated.getDefaultInsuranceCost());
        }
        if (updated.getDefaultMaintenanceCost() != null) {
            existing.setDefaultMaintenanceCost(updated.getDefaultMaintenanceCost());
        }
        if (updated.getEstimatedResaleValue() != null) {
            existing.setEstimatedResaleValue(updated.getEstimatedResaleValue());
        }
        return variantRepository.save(existing);
    }

    public void deleteVariant(Long id) {
        FinitionMotorisation existing = getVariantById(id);
        variantRepository.delete(existing);
    }

    // ==========================================
    // 6. FULL HIERARCHY TREE
    // ==========================================

    @Transactional(readOnly = true)
    public List<CatalogHierarchyDto> getFullHierarchy() {
        // 1. Charger toutes les marques (1 seule requête SQL)
        List<Brand> brands = brandRepository.findAll().stream()
                .sorted(Comparator.comparing(Brand::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        // 2. Charger tous les modèles et les grouper par marque (1 requête SQL)
        Map<Long, List<VehicleModel>> modelsByBrand = modelRepository.findAll().stream()
                .sorted(Comparator.comparing(VehicleModel::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.groupingBy(m -> m.getBrand().getId()));

        // 3. Charger toutes les finitions et les grouper par modèle (1 requête SQL)
        Map<Long, List<Finition>> finitionsByModel = finitionRepository.findAll().stream()
                .sorted(Comparator.comparing(Finition::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.groupingBy(f -> f.getModel().getId()));

        // 4. Charger toutes les motorisations et les grouper par modèle (1 requête SQL)
        Map<Long, List<Motorisation>> motorisationsByModel = motorisationRepository.findAll().stream()
                .sorted(Comparator.comparing(Motorisation::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.groupingBy(m -> m.getModel().getId()));

        // 5. Charger toutes les variantes avec JOIN FETCH et les grouper par motorisation (1 requête SQL)
        Map<Long, List<FinitionMotorisation>> variantsByMotorisation = variantRepository.findAllWithDetails().stream()
                .collect(Collectors.groupingBy(v -> v.getMotorisation().getId()));

        List<CatalogHierarchyDto> result = new ArrayList<>();

        for (Brand b : brands) {
            List<VehicleModel> models = modelsByBrand.getOrDefault(b.getId(), List.of());
            List<CatalogHierarchyDto.ModelHierarchyDto> modelDtos = new ArrayList<>();

            for (VehicleModel m : models) {
                List<Finition> finitions = finitionsByModel.getOrDefault(m.getId(), List.of());
                List<CatalogHierarchyDto.FinitionHierarchyDto> finitionDtos = finitions.stream()
                        .map(f -> new CatalogHierarchyDto.FinitionHierarchyDto(f.getId(), f.getName(), f.getImageUrl()))
                        .toList();

                List<Motorisation> motorisations = motorisationsByModel.getOrDefault(m.getId(), List.of());
                List<CatalogHierarchyDto.MotorisationHierarchyDto> motorisationDtos = new ArrayList<>();

                for (Motorisation mot : motorisations) {
                    List<FinitionMotorisation> variants = variantsByMotorisation.getOrDefault(mot.getId(), List.of());
                    List<CatalogHierarchyDto.VariantPriceDto> priceDtos = variants.stream()
                            .map(fm -> new CatalogHierarchyDto.VariantPriceDto(
                                    fm.getId(),
                                    fm.getFinition().getId(),
                                    fm.getFinition().getName(),
                                    fm.getFinition().getImageUrl(),
                                    fm.getPurchasePrice(),
                                    fm.getMonthlyLoa(),
                                    fm.getMonthlyLld(),
                                    fm.getDefaultInsuranceCost(),
                                    fm.getDefaultMaintenanceCost(),
                                    fm.getEstimatedResaleValue()
                            ))
                            .toList();

                    motorisationDtos.add(new CatalogHierarchyDto.MotorisationHierarchyDto(
                            mot.getId(),
                            mot.getName(),
                            mot.getFuelType(),
                            mot.getConsumptionWltp(),
                            mot.getPowerHp(),
                            mot.getBatteryCapacityKwh(),
                            priceDtos
                    ));
                }

                modelDtos.add(new CatalogHierarchyDto.ModelHierarchyDto(
                        m.getId(),
                        m.getName(),
                        m.getImageUrl(),
                        m.getCategory(),
                        motorisationDtos,
                        finitionDtos
                ));
            }

            result.add(new CatalogHierarchyDto(
                    b.getId(),
                    b.getName(),
                    b.getLogoUrl(),
                    modelDtos
            ));
        }

        return result;
    }

    // ==========================================
    // CONVERTER HELPERS
    // ==========================================

    private VehicleModelDto toModelDto(VehicleModel model) {
        return new VehicleModelDto(
                model.getId(),
                model.getBrand().getId(),
                model.getBrand().getName(),
                model.getBrand().getLogoUrl(),
                model.getName(),
                model.getImageUrl(),
                model.getCategory(),
                model.getMotorisations() != null ? model.getMotorisations().size() : 0,
                model.getFinitions() != null ? model.getFinitions().size() : 0
        );
    }

    private MotorisationDto toMotorisationDto(Motorisation m) {
        return new MotorisationDto(
                m.getId(),
                m.getModel().getId(),
                m.getModel().getName(),
                m.getModel().getBrand().getName(),
                m.getName(),
                m.getFuelType(),
                m.getConsumptionWltp(),
                m.getPowerHp(),
                m.getBatteryCapacityKwh()
        );
    }

    private FinitionDto toFinitionDto(Finition f) {
        return new FinitionDto(
                f.getId(),
                f.getModel().getId(),
                f.getModel().getName(),
                f.getModel().getBrand().getName(),
                f.getName(),
                f.getImageUrl()
        );
    }

    public FinitionMotorisationDto toFinitionMotorisationDto(FinitionMotorisation fm) {
        Finition f = fm.getFinition();
        Motorisation m = fm.getMotorisation();
        VehicleModel model = m.getModel();
        Brand brand = model.getBrand();

        return new FinitionMotorisationDto(
                fm.getId(),
                f.getId(),
                f.getName(),
                f.getImageUrl(),
                m.getId(),
                m.getName(),
                m.getFuelType(),
                m.getConsumptionWltp(),
                m.getPowerHp(),
                m.getBatteryCapacityKwh(),
                model.getId(),
                model.getName(),
                model.getImageUrl(),
                model.getCategory(),
                brand.getId(),
                brand.getName(),
                brand.getLogoUrl(),
                fm.getPurchasePrice(),
                fm.getMonthlyLoa(),
                fm.getMonthlyLld(),
                fm.getDefaultInsuranceCost(),
                fm.getDefaultMaintenanceCost(),
                fm.getEstimatedResaleValue()
        );
    }
}
