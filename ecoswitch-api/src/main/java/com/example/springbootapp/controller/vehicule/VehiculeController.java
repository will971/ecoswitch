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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.repository.AppUserRepository;
import com.example.springbootapp.service.VehiculeService;

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

	private final VehiculeService vehiculeService;
	private final AppUserRepository userRepository;

	public VehiculeController(VehiculeService vehiculeService, AppUserRepository userRepository) {
		this.vehiculeService = vehiculeService;
		this.userRepository = userRepository;
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
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vous devez être connecté pour ajouter un véhicule.");
		}
		AppUser user = userRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
		
		vehicule.setCreatedBy(user.getEmail());
		if (vehicule.getVisibility() == null || vehicule.getVisibility().isBlank()) {
			vehicule.setVisibility("PUBLIC");
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(vehiculeService.create(vehicule));
	}

	@GetMapping
	@Operation(summary = "Lister tous les vehicules")
	@ApiResponse(responseCode = "200", description = "Liste des vehicules")
	public List<Vehicule> findAll(Principal principal) {
		List<Vehicule> all = vehiculeService.findAll();
		if (principal != null) {
			AppUser user = userRepository.findByEmail(principal.getName()).orElse(null);
			if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
				return all;
			}
			final String userEmail = (user != null) ? user.getEmail() : "";
			return all.stream()
					.filter(v -> "PUBLIC".equalsIgnoreCase(v.getVisibility()) || userEmail.equalsIgnoreCase(v.getCreatedBy()))
					.toList();
		} else {
			return all.stream()
					.filter(v -> "PUBLIC".equalsIgnoreCase(v.getVisibility()))
					.toList();
		}
	}

	@GetMapping("/{id}")
	@Operation(summary = "Recuperer un vehicule par id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Vehicule trouve"),
			@ApiResponse(responseCode = "400", description = "Vehicule introuvable"),
			@ApiResponse(responseCode = "403", description = "Acces refuse")
	})
	public Vehicule findById(@PathVariable Long id, Principal principal) {
		Vehicule v = vehiculeService.findById(id);
		if ("PRIVATE".equalsIgnoreCase(v.getVisibility())) {
			if (principal == null) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
			}
			AppUser user = userRepository.findByEmail(principal.getName())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé."));
			if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !user.getEmail().equalsIgnoreCase(v.getCreatedBy())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé.");
			}
		}
		return v;
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
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
		}
		AppUser user = userRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
		Vehicule existing = vehiculeService.findById(id);
		if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !user.getEmail().equalsIgnoreCase(existing.getCreatedBy())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le créateur ou un administrateur peut modifier ce véhicule.");
		}
		vehicule.setCreatedBy(existing.getCreatedBy());
		if (vehicule.getVisibility() == null || vehicule.getVisibility().isBlank()) {
			vehicule.setVisibility(existing.getVisibility());
		}
		return vehiculeService.update(id, vehicule);
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
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
		}
		AppUser user = userRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
		Vehicule existing = vehiculeService.findById(id);
		if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !user.getEmail().equalsIgnoreCase(existing.getCreatedBy())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le créateur ou un administrateur peut supprimer ce véhicule.");
		}
		vehiculeService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ApiResponse(responseCode = "400", description = "Erreur metier", content = @Content(schema = @Schema(implementation = Map.class)))
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
	}
}
