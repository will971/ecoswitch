package com.example.springbootapp.controller.simulation;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootapp.business.simulation.SimulationBusiness;
import com.example.springbootapp.model.entity.Simulation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints de gestion des simulations sauvegardées.
 * Toutes les routes sont protégées par JWT (voir AdminSecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulations", description = "Sauvegarder et consulter les simulations de rentabilité")
@SecurityRequirement(name = "bearerAuth")
public class SimulationController {

    private final SimulationBusiness simulationBusiness;

    public SimulationController(SimulationBusiness simulationBusiness) {
        this.simulationBusiness = simulationBusiness;
    }

    // ── GET — Liste des simulations ───────────────────────────────────────

    @GetMapping
    @Operation(summary = "Récupérer toutes les simulations de l'utilisateur connecté")
    @ApiResponse(responseCode = "200", description = "Liste des simulations (peut être vide)")
    public ResponseEntity<List<SimulationResponse>> getUserSimulations(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        List<Simulation> simulations = simulationBusiness.getUserSimulations(email);
        List<SimulationResponse> response = simulations.stream()
                .map(SimulationResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    // ── POST — Sauvegarder une simulation ─────────────────────────────────

    @PostMapping
    @Operation(summary = "Sauvegarder une nouvelle simulation")
    @ApiResponse(responseCode = "201", description = "Simulation créée")
    @ApiResponse(responseCode = "400", description = "Données manquantes")
    public ResponseEntity<?> saveSimulation(@RequestBody SaveSimulationRequest request,
                                            Authentication authentication) {
        String email = (String) authentication.getPrincipal();

        try {
            Simulation saved = simulationBusiness.saveSimulation(email, request.name(), request.simulationData());
            return ResponseEntity.status(HttpStatus.CREATED).body(SimulationResponse.from(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE — Supprimer une simulation ────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une simulation (uniquement la sienne)")
    @ApiResponse(responseCode = "204", description = "Simulation supprimée")
    @ApiResponse(responseCode = "404", description = "Simulation introuvable ou n'appartient pas à l'utilisateur")
    public ResponseEntity<Void> deleteSimulation(@PathVariable Long id, Authentication authentication) {
        String email  = (String) authentication.getPrincipal();
        int deleted   = simulationBusiness.deleteSimulation(id, email);
        if (deleted == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ── Records internes ─────────────────────────────────────────────────

    /**
     * DTO de réponse : expose les champs utiles sans révéler l'entité JPA.
     */
    public record SimulationResponse(
            Long   id,
            String name,
            String savedAt,
            String simulationData
    ) {
        static SimulationResponse from(Simulation s) {
            return new SimulationResponse(
                    s.getId(),
                    s.getName(),
                    s.getSavedAt().toString(),
                    s.getSimulationData()
            );
        }
    }

    /**
     * DTO de requête pour la sauvegarde d'une simulation.
     */
    public record SaveSimulationRequest(String name, String simulationData) {}
}
