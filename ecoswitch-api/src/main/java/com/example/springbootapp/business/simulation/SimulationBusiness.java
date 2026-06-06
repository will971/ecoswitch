package com.example.springbootapp.business.simulation;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.springbootapp.model.entity.Simulation;
import com.example.springbootapp.service.SimulationService;

@Component
public class SimulationBusiness {

    private final SimulationService simulationService;

    public SimulationBusiness(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public List<Simulation> getUserSimulations(String email) {
        return simulationService.findByUserEmailOrderBySavedAtDesc(email);
    }

    public Simulation saveSimulation(String email, String name, String simulationData) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de la simulation est obligatoire.");
        }
        if (simulationData == null || simulationData.isBlank()) {
            throw new IllegalArgumentException("Les données de simulation sont obligatoires.");
        }

        Simulation sim = new Simulation(
                email,
                name.trim(),
                LocalDateTime.now(),
                simulationData
        );
        return simulationService.save(sim);
    }

    public int deleteSimulation(Long id, String email) {
        return simulationService.deleteByIdAndUserEmail(id, email);
    }
}
