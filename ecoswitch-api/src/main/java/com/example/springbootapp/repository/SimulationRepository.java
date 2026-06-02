package com.example.springbootapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springbootapp.model.entity.Simulation;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    /** Retourne les simulations d'un utilisateur, triées du plus récent au plus ancien */
    List<Simulation> findByUserEmailOrderBySavedAtDesc(String userEmail);

    /**
     * Supprime une simulation uniquement si elle appartient à l'utilisateur spécifié.
     * Évite qu'un utilisateur puisse supprimer les simulations d'un autre.
     */
    @Modifying
    @Query("DELETE FROM Simulation s WHERE s.id = :id AND s.userEmail = :email")
    int deleteByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);
}
