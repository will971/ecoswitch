package com.example.springbootapp.controller.vehicule;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootapp.business.vehicule.ImmatriculationBusiness;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/immatriculation")
@Tag(name = "Immatriculation", description = "Recherche de vehicule par plaque d'immatriculation via Oscaro + Base locale de secours")
public class ImmatriculationController {

	private final ImmatriculationBusiness immatriculationBusiness;

	public ImmatriculationController(ImmatriculationBusiness immatriculationBusiness) {
		this.immatriculationBusiness = immatriculationBusiness;
	}

	@GetMapping("/{plaque}")
	@Operation(summary = "Rechercher un vehicule par plaque d'immatriculation")
	@ApiResponse(responseCode = "200", description = "Vehicule trouve")
	@ApiResponse(responseCode = "404", description = "Vehicule introuvable")
	public ResponseEntity<?> rechercherPlaque(@PathVariable String plaque) {
		try {
			return immatriculationBusiness.rechercherPlaque(plaque)
					.map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.status(404).body(Map.of(
							"error", "Plaque d'immatriculation introuvable. Veuillez saisir manuellement les informations."
					)));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}
