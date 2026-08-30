package com.example.springbootapp.repository;

import com.example.springbootapp.model.entity.FinitionMotorisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinitionMotorisationRepository extends JpaRepository<FinitionMotorisation, Long> {
    
    @Query("SELECT fm FROM FinitionMotorisation fm " +
           "JOIN FETCH fm.finition f " +
           "JOIN FETCH fm.motorisation m " +
           "JOIN FETCH m.model mo " +
           "JOIN FETCH mo.brand b")
    List<FinitionMotorisation> findAllWithDetails();

    @Query("SELECT fm FROM FinitionMotorisation fm " +
           "JOIN FETCH fm.finition f " +
           "JOIN FETCH fm.motorisation m " +
           "JOIN FETCH m.model mo " +
           "JOIN FETCH mo.brand b " +
           "WHERE fm.motorisation.id = :motorisationId")
    List<FinitionMotorisation> findByMotorisationIdWithDetails(@Param("motorisationId") Long motorisationId);

    @Query("SELECT fm FROM FinitionMotorisation fm " +
           "JOIN FETCH fm.finition f " +
           "JOIN FETCH fm.motorisation m " +
           "JOIN FETCH m.model mo " +
           "JOIN FETCH mo.brand b " +
           "WHERE fm.finition.id = :finitionId")
    List<FinitionMotorisation> findByFinitionIdWithDetails(@Param("finitionId") Long finitionId);

    @Query("SELECT fm FROM FinitionMotorisation fm " +
           "JOIN FETCH fm.finition f " +
           "JOIN FETCH fm.motorisation m " +
           "JOIN FETCH m.model mo " +
           "JOIN FETCH mo.brand b " +
           "WHERE fm.motorisation.model.id = :modelId")
    List<FinitionMotorisation> findByModelIdWithDetails(@Param("modelId") Long modelId);

    List<FinitionMotorisation> findByMotorisationId(Long motorisationId);
    List<FinitionMotorisation> findByFinitionId(Long finitionId);
    Optional<FinitionMotorisation> findByFinitionIdAndMotorisationId(Long finitionId, Long motorisationId);

    @Query("SELECT fm FROM FinitionMotorisation fm WHERE fm.motorisation.model.id = :modelId")
    List<FinitionMotorisation> findByModelId(@Param("modelId") Long modelId);

    @Query("SELECT fm FROM FinitionMotorisation fm WHERE fm.motorisation.model.brand.id = :brandId")
    List<FinitionMotorisation> findByBrandId(@Param("brandId") Long brandId);
}
