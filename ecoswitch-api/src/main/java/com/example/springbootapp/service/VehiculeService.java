package com.example.springbootapp.service;

import com.example.springbootapp.model.entity.Vehicule;
import com.example.springbootapp.repository.VehiculeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehiculeService {

	private final VehiculeRepository vehiculeRepository;

	public VehiculeService(VehiculeRepository vehiculeRepository) {
		this.vehiculeRepository = vehiculeRepository;
	}

	@Transactional
	public Vehicule create(Vehicule vehicule) {
		validateVehicule(vehicule);
		return vehiculeRepository.save(vehicule);
	}

	@Transactional(readOnly = true)
	public List<Vehicule> findAll() {
		return vehiculeRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Vehicule findById(Long id) {
		return vehiculeRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Vehicule introuvable avec l'id: " + id));
	}

	@Transactional
	public Vehicule update(Long id, Vehicule vehicule) {
		validateVehicule(vehicule);
		findById(id);
		vehicule.setId(id);
		int updated = vehiculeRepository.update(vehicule);
		if (updated == 0) {
			throw new IllegalArgumentException("Vehicule introuvable avec l'id: " + id);
		}
		return vehicule;
	}

	@Transactional
	public void delete(Long id) {
		int deleted = vehiculeRepository.deleteById(id);
		if (deleted == 0) {
			throw new IllegalArgumentException("Vehicule introuvable avec l'id: " + id);
		}
	}

	private void validateVehicule(Vehicule vehicule) {
		if (vehicule.getName() == null || vehicule.getName().isBlank()) {
			throw new IllegalArgumentException("Le nom du vehicule est obligatoire.");
		}
		if (vehicule.getFuelType() == null) {
			throw new IllegalArgumentException("Le type de carburant est obligatoire.");
		}
	}
}
