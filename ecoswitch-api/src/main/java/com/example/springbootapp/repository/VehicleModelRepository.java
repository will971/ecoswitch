package com.example.springbootapp.repository;

import com.example.springbootapp.model.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    List<VehicleModel> findByBrandIdOrderByNameAsc(Long brandId);
    List<VehicleModel> findByBrandNameIgnoreCaseOrderByNameAsc(String brandName);
    Optional<VehicleModel> findByBrandIdAndNameIgnoreCase(Long brandId, String name);
}
