package com.example.springbootapp.controller.vehicule;

import java.security.Principal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.business.vehicule.VehiculeBusiness;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/vehicules")
@Tag(name = "Vehicules", description = "Operations CRUD sur les vehicules")
public class VehiculeController {

	private final VehiculeBusiness vehiculeBusiness;

	public VehiculeController(VehiculeBusiness vehiculeBusiness) {
		this.vehiculeBusiness = vehiculeBusiness;
	}

	@PostMapping
	@Operation(summary = "Creer un vehicule")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Vehicule cree"),
			@ApiResponse(responseCode = "400", description = "Donnees invalides"),
			@ApiResponse(responseCode = "401", description = "Non authentifie")
	})
	public ResponseEntity<Vehicule> create(@RequestBody Vehicule vehicule, Principal principal) {
		String email = principal != null ? principal.getName() : null;
		return ResponseEntity.status(HttpStatus.CREATED).body(vehiculeBusiness.create(vehicule, email));
	}

	@GetMapping
	@Operation(summary = "Lister tous les vehicules avec pagination et filtres")
	@ApiResponse(responseCode = "200", description = "Liste des vehicules")
	public ResponseEntity<List<Vehicule>> findAll(
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String fuelType,
			@RequestParam(required = false) String brand,
			@RequestParam(required = false) String model,
			@RequestParam(required = false) String version,
			Principal principal) {
		
		String principalName = principal != null ? principal.getName() : null;
		List<Vehicule> filtered = vehiculeBusiness.findAll(name, fuelType, brand, model, version, principalName);

		int totalElements = filtered.size();
		int totalPages = 1;
		List<Vehicule> content = filtered;

		if (page != null && size != null && size > 0) {
			totalPages = (int) Math.ceil((double) totalElements / size);
			if (totalPages == 0) {
				totalPages = 1;
			}
			int fromIndex = Math.min(page * size, totalElements);
			int toIndex = Math.min(fromIndex + size, totalElements);
			content = filtered.subList(fromIndex, toIndex);
		}

		return ResponseEntity.ok()
				.header("Access-Control-Expose-Headers", "X-Total-Count, X-Total-Pages")
				.header("X-Total-Count", String.valueOf(totalElements))
				.header("X-Total-Pages", String.valueOf(totalPages))
				.body(content);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Recuperer un vehicule par id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicule trouve"),
			@ApiResponse(responseCode = "400", description = "Vehicule introuvable"),
			@ApiResponse(responseCode = "403", description = "Acces refuse")
	})
	public Vehicule findById(@PathVariable Long id, Principal principal) {
		String principalName = principal != null ? principal.getName() : null;
		return vehiculeBusiness.findById(id, principalName);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Mettre a jour un vehicule")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicule mis a jour"),
			@ApiResponse(responseCode = "400", description = "Donnees invalides ou id introuvable"),
			@ApiResponse(responseCode = "401", description = "Non authentifie"),
			@ApiResponse(responseCode = "403", description = "Permissions insuffisantes")
	})
	public Vehicule update(@PathVariable Long id, @RequestBody Vehicule vehicule, Principal principal) {
		String principalName = principal != null ? principal.getName() : null;
		return vehiculeBusiness.update(id, vehicule, principalName);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Supprimer un vehicule")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Vehicule supprime"),
			@ApiResponse(responseCode = "400", description = "Id introuvable"),
			@ApiResponse(responseCode = "401", description = "Non authentifie"),
			@ApiResponse(responseCode = "403", description = "Permissions insuffisantes")
	})
	public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
		String principalName = principal != null ? principal.getName() : null;
		vehiculeBusiness.delete(id, principalName);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/brands")
	@Operation(summary = "Lister toutes les marques uniques présentes dans le catalogue")
	public ResponseEntity<List<String>> getCatalogBrands() {
		return ResponseEntity.ok(vehiculeBusiness.getCatalogBrands());
	}

	@GetMapping("/models")
	@Operation(summary = "Lister tous les modèles pour une marque présente dans le catalogue")
	public ResponseEntity<List<String>> getCatalogModels(@RequestParam String brand) {
		return ResponseEntity.ok(vehiculeBusiness.getCatalogModels(brand));
	}

	@GetMapping("/versions")
	@Operation(summary = "Lister toutes les versions pour un couple marque et modèle dans le catalogue")
	public ResponseEntity<List<Map<String, String>>> getCatalogVersions(@RequestParam String brand, @RequestParam String model) {
		return ResponseEntity.ok(vehiculeBusiness.getCatalogVersions(brand, model));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ApiResponse(responseCode = "400", description = "Erreur metier", content = @Content(schema = @Schema(implementation = Map.class)))
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
	}
}
