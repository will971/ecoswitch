package com.example.springbootapp.repository;

import com.example.springbootapp.model.entity.Motorisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MotorisationRepository extends JpaRepository<Motorisation, Long> {
    List<Motorisation> findByModelIdOrderByNameAsc(Long modelId);
    Optional<Motorisation> findByModelIdAndNameIgnoreCase(Long modelId, String name);
}
