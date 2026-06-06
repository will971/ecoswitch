package com.example.springbootapp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.springbootapp.model.entity.Simulation;
import com.example.springbootapp.repository.SimulationRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationService {

    private final SimulationRepository simulationRepository;

    public SimulationService(SimulationRepository simulationRepository) {
        this.simulationRepository = simulationRepository;
    }

    public List<Simulation> findByUserEmailOrderBySavedAtDesc(String email) {
        return simulationRepository.findByUserEmailOrderBySavedAtDesc(email);
    }

    public Simulation save(Simulation simulation) {
        return simulationRepository.save(simulation);
    }

    @Transactional
    public int deleteByIdAndUserEmail(Long id, String email) {
        return simulationRepository.deleteByIdAndUserEmail(id, email);
    }
}
