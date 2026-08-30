package com.example.springbootapp.repository;

import com.example.springbootapp.model.entity.Finition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinitionRepository extends JpaRepository<Finition, Long> {
    List<Finition> findByModelIdOrderByNameAsc(Long modelId);
    Optional<Finition> findByModelIdAndNameIgnoreCase(Long modelId, String name);
}
