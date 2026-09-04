package com.example.springbootapp.business.comparison;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.springbootapp.controller.comparison.ComparisonController.CustomProfitabilityComparisonRequest;
import com.example.springbootapp.controller.comparison.ComparisonController.DirectProfitabilityRequest;
import com.example.springbootapp.controller.comparison.ComparisonController.DirectProfitabilityResponse;
import com.example.springbootapp.controller.comparison.ComparisonController.ProfitabilityComparisonRequest;
import com.example.springbootapp.controller.comparison.ComparisonController.ProfitabilityComparisonResponse;
import com.example.springbootapp.controller.comparison.ComparisonController.VehicleProfitability;
import com.example.springbootapp.model.dto.FinitionMotorisationDto;
import com.example.springbootapp.model.entity.Brand;
import com.example.springbootapp.model.entity.Finition;
import com.example.springbootapp.model.entity.FinitionMotorisation;
import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Motorisation;
import com.example.springbootapp.model.entity.VehicleModel;
import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.repository.FinitionMotorisationRepository;
import com.example.springbootapp.service.CatalogService;
import com.example.springbootapp.service.CostCalculationService;
import com.example.springbootapp.service.VehiculeService;
import java.util.Optional;

@Component
public class ComparisonBusiness {

    private static final int DEFAULT_MAX_YEARS = 15;

    private final VehiculeService vehiculeService;
    private final FinitionMotorisationRepository finitionMotorisationRepository;
    private final CostCalculationService costCalculationService;
    private final CatalogService catalogService;

    public ComparisonBusiness(VehiculeService vehiculeService,
                              FinitionMotorisationRepository finitionMotorisationRepository,
                              CostCalculationService costCalculationService) {
        this(vehiculeService, finitionMotorisationRepository, costCalculationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ComparisonBusiness(VehiculeService vehiculeService,
                              FinitionMotorisationRepository finitionMotorisationRepository,
                              CostCalculationService costCalculationService,
                              CatalogService catalogService) {
        this.vehiculeService = vehiculeService;
        this.finitionMotorisationRepository = finitionMotorisationRepository;
        this.costCalculationService = costCalculationService;
        this.catalogService = catalogService;
    }

    public ProfitabilityComparisonResponse compareProfitability(ProfitabilityComparisonRequest request) {
        validateRequest(request);

        Vehicule currentVehicle = resolveVehicle(request.currentVehicleId());
        Integer requestedMaxYears = request.maxYears();
        int maxYears = requestedMaxYears == null ? DEFAULT_MAX_YEARS : requestedMaxYears;
        List<VehicleProfitability> alternatives = new ArrayList<>();

        for (Long targetVehicleId : request.targetVehicleIds()) {
            if (targetVehicleId.equals(request.currentVehicleId())) {
                continue;
            }
            Vehicule targetVehicle = resolveVehicle(targetVehicleId);
            double currentAnnualCost = costCalculationService.calculateAnnualCost(
                    currentVehicle,
                    costCalculationService.resolveFuelPrice(currentVehicle, request.fuelPricesByType()));
            double targetAnnualCost = costCalculationService.calculateAnnualCost(
                    targetVehicle,
                    costCalculationService.resolveFuelPrice(targetVehicle, request.fuelPricesByType()));
            double annualSavings = currentAnnualCost - targetAnnualCost;
            double switchInvestment = Math.max(0.0, targetVehicle.getPurchasePrice() - currentVehicle.getResaleValue());
            double immediateRepairCost = request.immediateRepairCost() == null ? 0.0 : request.immediateRepairCost();
            double totalCostDeltaAtHorizon = costCalculationService.calculateSwitchCostAtYear(
                    currentVehicle,
                    targetVehicle,
                    maxYears,
                    request.fuelPricesByType(),
                    immediateRepairCost);
            Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(
                    currentVehicle,
                    targetVehicle,
                    maxYears,
                    request.fuelPricesByType(),
                    immediateRepairCost);

            alternatives.add(
                    new VehicleProfitability(
                            targetVehicle.getId(),
                            targetVehicle.getName(),
                            switchInvestment,
                            currentAnnualCost,
                            targetAnnualCost,
                            annualSavings,
                            breakEvenYear,
                            totalCostDeltaAtHorizon));
        }

        alternatives.sort(
                Comparator
                        .comparing((VehicleProfitability item) -> item.breakEvenYear() == null)
                        .thenComparing(this::breakEvenSortingValue)
                        .thenComparing(VehicleProfitability::totalCostDeltaAtHorizon));

        return new ProfitabilityComparisonResponse(
                currentVehicle.getId(),
                currentVehicle.getName(),
                maxYears,
                alternatives);
    }

    public ProfitabilityComparisonResponse compareCustomProfitability(CustomProfitabilityComparisonRequest request) {
        if (request == null || request.currentVehicle() == null) {
            throw new IllegalArgumentException("Le véhicule actuel est obligatoire.");
        }
        if (request.targetVehicleIds() == null || request.targetVehicleIds().isEmpty()) {
            throw new IllegalArgumentException("Au moins un véhicule cible est obligatoire.");
        }

        Vehicule currentVehicle = request.currentVehicle();
        Integer requestedMaxYears = request.maxYears();
        int maxYears = requestedMaxYears == null ? DEFAULT_MAX_YEARS : requestedMaxYears;
        double immediateRepairCost = request.immediateRepairCost() == null ? 0.0 : request.immediateRepairCost();

        double currentFuelPrice = costCalculationService.resolveFuelPrice(currentVehicle, request.fuelPricesByType());
        double currentAnnualCost = costCalculationService.calculateAnnualCost(currentVehicle, currentFuelPrice);

        List<VehicleProfitability> alternatives = new ArrayList<>();

        for (Long targetVehicleId : request.targetVehicleIds()) {
            Vehicule targetVehicle = resolveVehicle(targetVehicleId);
            if (targetVehicle == null) continue;

            double targetFuelPrice = costCalculationService.resolveFuelPrice(targetVehicle, request.fuelPricesByType());
            double targetAnnualCost = costCalculationService.calculateAnnualCost(targetVehicle, targetFuelPrice);
            double annualSavings = currentAnnualCost - targetAnnualCost;

            double switchInvestment = Math.max(0.0, targetVehicle.getPurchasePrice() - currentVehicle.getResaleValue());
            double totalCostDeltaAtHorizon = costCalculationService.calculateSwitchCostAtYear(
                    currentVehicle,
                    targetVehicle,
                    maxYears,
                    currentFuelPrice,
                    targetFuelPrice,
                    switchInvestment,
                    immediateRepairCost);
            Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(
                    currentVehicle,
                    targetVehicle,
                    maxYears,
                    currentFuelPrice,
                    targetFuelPrice,
                    switchInvestment,
                    immediateRepairCost);

            alternatives.add(
                    new VehicleProfitability(
                            targetVehicle.getId(),
                            targetVehicle.getName(),
                            switchInvestment,
                            currentAnnualCost,
                            targetAnnualCost,
                            annualSavings,
                            breakEvenYear,
                            totalCostDeltaAtHorizon));
        }

        alternatives.sort(
                Comparator
                        .comparing((VehicleProfitability item) -> item.breakEvenYear() == null)
                        .thenComparing(this::breakEvenSortingValue)
                        .thenComparing(VehicleProfitability::totalCostDeltaAtHorizon));

        return new ProfitabilityComparisonResponse(
                currentVehicle.getId() != null ? currentVehicle.getId() : -1L,
                currentVehicle.getName(),
                maxYears,
                alternatives);
    }

    public DirectProfitabilityResponse compareDirect(DirectProfitabilityRequest request) {
        validateDirectRequest(request);

        Vehicule current = request.currentVehicle();
        Vehicule target = request.targetVehicle();
        Integer requestedMaxYears = request.maxYears();
        int maxYears = requestedMaxYears == null ? DEFAULT_MAX_YEARS : requestedMaxYears;
        double immediateRepairCost = request.immediateRepairCost() == null ? 0.0 : request.immediateRepairCost();

        double rawCurrentPrice = costCalculationService.resolveFuelPrice(current, request.fuelPricesByType());
        double currentPrice = costCalculationService.resolveWeightedFuelPrice(current, rawCurrentPrice, request.homeChargingRatio());

        double rawTargetPrice = costCalculationService.resolveFuelPrice(target, request.fuelPricesByType());
        double targetPrice = costCalculationService.resolveWeightedFuelPrice(target, rawTargetPrice, request.homeChargingRatio());

        // Prise en compte PHEV pour target et current
        double currentAnnualCost = costCalculationService.calculateAnnualCost(current, currentPrice, request.fuelPricesByType(), request.homeChargingRatio(), null);
        double targetAnnualCost = costCalculationService.calculateAnnualCost(target, targetPrice, request.fuelPricesByType(), request.homeChargingRatio(), null);
        double annualSavings = currentAnnualCost - targetAnnualCost;

        double bonusEcologique = costCalculationService.calculateBonusEcologique(target, request.taxIncome());
        double primeConversion = costCalculationService.calculatePrimeConversion(current, target, request.scrapVehicle(), request.taxIncome());
        double totalSubsidies = bonusEcologique + primeConversion;

        double rawSwitchInvestment = Math.max(0.0, target.getPurchasePrice() - current.getResaleValue());
        double switchInvestment = Math.max(0.0, rawSwitchInvestment - totalSubsidies);

        double totalCostDeltaAtHorizon = costCalculationService.calculateSwitchCostAtYear(
                current,
                target,
                maxYears,
                currentPrice,
                targetPrice,
                switchInvestment,
                immediateRepairCost);

        Integer breakEvenYear = costCalculationService.calculateBreakEvenYear(
                current,
                target,
                maxYears,
                currentPrice,
                targetPrice,
                switchInvestment,
                immediateRepairCost);

        // CO2 calculations
        double currentAnnualCO2 = costCalculationService.calculateAnnualCO2Kg(current);
        double targetAnnualCO2 = costCalculationService.calculateAnnualCO2Kg(target);
        double annualCO2Savings = currentAnnualCO2 - targetAnnualCO2;

        // Monthly / Leasing calculations
        double leasingMonthlyPrice = 0.0;
        if (request.isLeasing() != null && request.isLeasing()) {
            if (request.customLeasingMonthlyPrice() != null && request.customLeasingMonthlyPrice() > 0) {
                leasingMonthlyPrice = request.customLeasingMonthlyPrice();
            } else {
                double prix = target.getPurchasePrice();
                double apport = prix * 0.10;
                double residuel = prix * 0.45;
                double capitalFinance = prix - apport - residuel;
                double dureeMois = 48.0;
                double tauxAnnuel = 0.039;
                double tauxMensuel = tauxAnnuel / 12.0;

                double loyerAmortissement = (capitalFinance * tauxMensuel) / (1.0 - Math.pow(1.0 + tauxMensuel, -dureeMois));
                double loyerResiduel = residuel * tauxMensuel;
                leasingMonthlyPrice = loyerAmortissement + loyerResiduel;
            }
        }
        double currentMonthlyTotalCost = currentAnnualCost / 12.0;
        double targetMonthlyTotalCost = leasingMonthlyPrice + (targetAnnualCost / 12.0);
        double monthlySavings = currentMonthlyTotalCost - targetMonthlyTotalCost;

        // Recommendations intelligentes filtrées par adéquation d'usage (Kilométrage vs Autonomie)
        List<CatalogVariantItem> catalog = getCatalogVariantsWithDetails();
        List<VehicleProfitability> recommendations = new ArrayList<>();
        int userAnnualMileage = current.getAnnualMileage() > 0 ? current.getAnnualMileage() : 15000;

        for (CatalogVariantItem item : catalog) {
            Vehicule catalogVehicle = item.vehicule;
            if (catalogVehicle.getName().equalsIgnoreCase(current.getName()) || 
                catalogVehicle.getName().equalsIgnoreCase(target.getName())) {
                continue;
            }

            // Filtre d'adéquation d'autonomie :
            // Si l'utilisateur roule beaucoup (> 22 000 km/an), on élimine les véhicules électriques urbains à faible autonomie (< 320 km)
            if (catalogVehicle.getFuelType() == FuelType.ELECTRIC) {
                int wltp = item.autonomieWltp != null ? item.autonomieWltp : 350;
                if (userAnnualMileage >= 25000 && wltp < 400) {
                    continue; // Inadapté aux gros rouleurs (ex: Dacia Spring / petite citadine)
                }
                if (userAnnualMileage >= 18000 && wltp < 300) {
                    continue;
                }
            }

            try {
                double catFuelPrice = costCalculationService.resolveFuelPrice(catalogVehicle, request.fuelPricesByType());
                double weightedCatPrice = costCalculationService.resolveWeightedFuelPrice(catalogVehicle, catFuelPrice, request.homeChargingRatio());

                double catAnnualCost = costCalculationService.calculateAnnualCost(catalogVehicle, weightedCatPrice, request.fuelPricesByType(), request.homeChargingRatio(), item.autonomieWltp);
                double catSavings = currentAnnualCost - catAnnualCost;

                double catBonus = costCalculationService.calculateBonusEcologique(catalogVehicle, request.taxIncome());
                double catPrime = costCalculationService.calculatePrimeConversion(current, catalogVehicle, request.scrapVehicle(), request.taxIncome());
                double catTotalSubsidies = catBonus + catPrime;

                double catRawSwitch = Math.max(0.0, catalogVehicle.getPurchasePrice() - current.getResaleValue());
                double catSwitchInvestment = Math.max(0.0, catRawSwitch - catTotalSubsidies);

                double catTotalCostDelta = costCalculationService.calculateSwitchCostAtYear(
                        current,
                        catalogVehicle,
                        maxYears,
                        currentPrice,
                        weightedCatPrice,
                        catSwitchInvestment,
                        immediateRepairCost);

                Integer catBreakEvenYear = costCalculationService.calculateBreakEvenYear(
                        current,
                        catalogVehicle,
                        maxYears,
                        currentPrice,
                        weightedCatPrice,
                        catSwitchInvestment,
                        immediateRepairCost);

                if (catBreakEvenYear != null || catSavings > 0) {
                    recommendations.add(new VehicleProfitability(
                            catalogVehicle.getId(),
                            catalogVehicle.getName(),
                            catSwitchInvestment,
                            currentAnnualCost,
                            catAnnualCost,
                            catSavings,
                            catBreakEvenYear,
                            catTotalCostDelta));
                }
            } catch (Exception ignored) {
            }
        }

        recommendations.sort(
                Comparator
                        .comparing((VehicleProfitability item) -> item.breakEvenYear() == null)
                        .thenComparing(this::breakEvenSortingValue)
                        .thenComparing(VehicleProfitability::totalCostDeltaAtHorizon));

        List<VehicleProfitability> topRecommendations = recommendations.stream()
                .limit(3)
                .collect(Collectors.toList());

        return new DirectProfitabilityResponse(
                currentAnnualCost,
                targetAnnualCost,
                annualSavings,
                switchInvestment,
                breakEvenYear,
                totalCostDeltaAtHorizon,
                topRecommendations,
                bonusEcologique,
                primeConversion,
                totalSubsidies,
                currentAnnualCO2,
                targetAnnualCO2,
                annualCO2Savings,
                currentMonthlyTotalCost,
                targetMonthlyTotalCost,
                monthlySavings);
    }

    private void validateRequest(ProfitabilityComparisonRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requete est obligatoire.");
        }
        if (request.currentVehicleId() == null) {
            throw new IllegalArgumentException("L'id du vehicule actuel est obligatoire.");
        }
        if (request.targetVehicleIds() == null || request.targetVehicleIds().isEmpty()) {
            throw new IllegalArgumentException("Au moins un vehicule cible est obligatoire.");
        }
        if (request.maxYears() != null && request.maxYears() <= 0) {
            throw new IllegalArgumentException("maxYears doit etre superieur a 0.");
        }
    }

    private void validateDirectRequest(DirectProfitabilityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requete est obligatoire.");
        }
        if (request.currentVehicle() == null) {
            throw new IllegalArgumentException("Le vehicule actuel est obligatoire.");
        }
        if (request.targetVehicle() == null) {
            throw new IllegalArgumentException("Le vehicule cible est obligatoire.");
        }
        if (request.currentVehicle().getFuelType() == null) {
            throw new IllegalArgumentException("Le type de carburant du vehicule actuel est obligatoire.");
        }
        if (request.targetVehicle().getFuelType() == null) {
            throw new IllegalArgumentException("Le type de carburant du vehicule cible est obligatoire.");
        }
        if (request.maxYears() != null && request.maxYears() <= 0) {
            throw new IllegalArgumentException("maxYears doit etre superieur a 0.");
        }
    }

    private Integer breakEvenSortingValue(VehicleProfitability item) {
        Integer breakEvenYear = item.breakEvenYear();
        return breakEvenYear == null ? Integer.MAX_VALUE : breakEvenYear;
    }

    private Vehicule resolveVehicle(Long id) {
        if (id == null) return null;
        if (catalogService != null) {
            try {
                List<FinitionMotorisationDto> all = catalogService.getVariants(null, null, null);
                for (FinitionMotorisationDto dto : all) {
                    if (dto.id().equals(id)) {
                        Vehicule v = new Vehicule();
                        v.setId(dto.id());
                        v.setName(dto.brandName() + " " + dto.modelName() + " " + dto.motorisationName() + " " + dto.finitionName());
                        v.setBrand(dto.brandName());
                        v.setModel(dto.modelName());
                        v.setVersion(dto.motorisationName() + " - " + dto.finitionName());
                        v.setFuelType(dto.fuelType());
                        v.setConsumption(dto.consumptionWltp());
                        v.setPurchasePrice(dto.purchasePrice());
                        v.setMaintenanceCost(dto.defaultMaintenanceCost() != null ? dto.defaultMaintenanceCost() : 250.0);
                        v.setResaleValue(dto.estimatedResaleValue() != null ? dto.estimatedResaleValue() : 0.0);
                        v.setUrl(dto.finitionImageUrl() != null ? dto.finitionImageUrl() : dto.modelImageUrl());
                        return v;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        Optional<FinitionMotorisation> opt = finitionMotorisationRepository.findById(id);
        if (opt.isPresent()) {
            FinitionMotorisation fm = opt.get();
            Motorisation m = fm.getMotorisation();
            Finition f = fm.getFinition();
            VehicleModel model = m.getModel();
            Brand brand = model.getBrand();

            Vehicule v = new Vehicule();
            v.setId(fm.getId());
            v.setName(brand.getName() + " " + model.getName() + " " + m.getName() + " " + f.getName());
            v.setBrand(brand.getName());
            v.setModel(model.getName());
            v.setVersion(m.getName() + " - " + f.getName());
            v.setFuelType(m.getFuelType());
            v.setConsumption(m.getConsumptionWltp());
            v.setPurchasePrice(fm.getPurchasePrice());
            v.setMaintenanceCost(fm.getDefaultMaintenanceCost() != null ? fm.getDefaultMaintenanceCost() : 250.0);
            v.setResaleValue(fm.getEstimatedResaleValue() != null ? fm.getEstimatedResaleValue() : 0.0);
            v.setUrl(f.getImageUrl() != null ? f.getImageUrl() : model.getImageUrl());
            return v;
        }
        return vehiculeService.findById(id);
    }

    private record CatalogVariantItem(Vehicule vehicule, Integer autonomieWltp, Double batteryCapacity) {}

    private List<CatalogVariantItem> getCatalogVariantsWithDetails() {
        if (catalogService != null) {
            try {
                List<FinitionMotorisationDto> dtoList = catalogService.getVariants(null, null, null);
                List<CatalogVariantItem> list = new ArrayList<>(dtoList.size());
                for (FinitionMotorisationDto dto : dtoList) {
                    Vehicule v = new Vehicule();
                    v.setId(dto.id());
                    v.setName(dto.brandName() + " " + dto.modelName() + " " + dto.motorisationName() + " " + dto.finitionName());
                    v.setBrand(dto.brandName());
                    v.setModel(dto.modelName());
                    v.setVersion(dto.motorisationName() + " - " + dto.finitionName());
                    v.setFuelType(dto.fuelType());
                    v.setConsumption(dto.consumptionWltp());
                    v.setPurchasePrice(dto.purchasePrice());
                    v.setMaintenanceCost(dto.defaultMaintenanceCost() != null ? dto.defaultMaintenanceCost() : 250.0);
                    v.setResaleValue(dto.estimatedResaleValue() != null ? dto.estimatedResaleValue() : 0.0);
                    v.setUrl(dto.finitionImageUrl() != null ? dto.finitionImageUrl() : dto.modelImageUrl());

                    list.add(new CatalogVariantItem(v, dto.autonomieWltpKm(), dto.batteryCapacityKwh()));
                }
                return list;
            } catch (Exception ignored) {
            }
        }

        List<CatalogVariantItem> list = new ArrayList<>();
        List<FinitionMotorisation> all = finitionMotorisationRepository.findAllWithDetails();
        for (FinitionMotorisation fm : all) {
            Motorisation m = fm.getMotorisation();
            Finition f = fm.getFinition();
            VehicleModel model = m.getModel();
            Brand brand = model.getBrand();

            Vehicule v = new Vehicule();
            v.setId(fm.getId());
            v.setName(brand.getName() + " " + model.getName() + " " + m.getName() + " " + f.getName());
            v.setBrand(brand.getName());
            v.setModel(model.getName());
            v.setVersion(m.getName() + " - " + f.getName());
            v.setFuelType(m.getFuelType());
            v.setConsumption(m.getConsumptionWltp());
            v.setPurchasePrice(fm.getPurchasePrice());
            v.setMaintenanceCost(fm.getDefaultMaintenanceCost() != null ? fm.getDefaultMaintenanceCost() : 250.0);
            v.setResaleValue(fm.getEstimatedResaleValue() != null ? fm.getEstimatedResaleValue() : 0.0);
            v.setUrl(f.getImageUrl() != null ? f.getImageUrl() : model.getImageUrl());

            list.add(new CatalogVariantItem(v, m.getAutonomieWltpKm(), m.getBatteryCapacityKwh()));
        }
        return list;
    }
}
