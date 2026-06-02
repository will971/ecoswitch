package com.example.springbootapp.controller.vehicule;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.service.VehiculeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/vehicules")
@Tag(name = "Vehicules", description = "Operations CRUD sur les vehicules")
public class VehiculeController {

	private final VehiculeService vehiculeService;

	public VehiculeController(VehiculeService vehiculeService) {
		this.vehiculeService = vehiculeService;
	}

	@PostMapping
	@Operation(summary = "Creer un vehicule")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Vehicule cree"),
			@ApiResponse(responseCode = "400", description = "Donnees invalides")
	})
	public ResponseEntity<Vehicule> create(@RequestBody Vehicule vehicule) {
		return ResponseEntity.status(HttpStatus.CREATED).body(vehiculeService.create(vehicule));
	}

	@GetMapping
	@Operation(summary = "Lister tous les vehicules")
	@ApiResponse(responseCode = "200", description = "Liste des vehicules")
	public List<Vehicule> findAll() {
		return vehiculeService.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Recuperer un vehicule par id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicule trouve"),
			@ApiResponse(responseCode = "400", description = "Vehicule introuvable")
	})
	public Vehicule findById(@PathVariable Long id) {
		return vehiculeService.findById(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Mettre a jour un vehicule")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicule mis a jour"),
			@ApiResponse(responseCode = "400", description = "Donnees invalides ou id introuvable")
	})
	public Vehicule update(@PathVariable Long id, @RequestBody Vehicule vehicule) {
		return vehiculeService.update(id, vehicule);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Supprimer un vehicule")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Vehicule supprime"),
			@ApiResponse(responseCode = "400", description = "Id introuvable")
	})
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		vehiculeService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ApiResponse(responseCode = "400", description = "Erreur metier", content = @Content(schema = @Schema(implementation = Map.class)))
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
	}
}
