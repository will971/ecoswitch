package com.example.springbootapp.repository;

import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class VehiculeRepository {

	private final JdbcTemplate jdbcTemplate;

	public VehiculeRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Vehicule save(Vehicule vehicule) {
		final String sql = """
			INSERT INTO vehicule
			(name, brand, model, generation, version, purchase_price, fuel_type, consumption, annual_mileage, insurance_cost, maintenance_cost, resale_value, url, visibility, created_by)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, vehicule.getName());
			statement.setString(2, vehicule.getBrand());
			statement.setString(3, vehicule.getModel());
			statement.setString(4, vehicule.getGeneration());
			statement.setString(5, vehicule.getVersion());
			statement.setDouble(6, vehicule.getPurchasePrice());
			statement.setString(7, vehicule.getFuelType().name());
			statement.setDouble(8, vehicule.getConsumption());
			statement.setInt(9, vehicule.getAnnualMileage());
			statement.setDouble(10, vehicule.getInsuranceCost());
			statement.setDouble(11, vehicule.getMaintenanceCost());
			statement.setDouble(12, vehicule.getResaleValue());
			statement.setString(13, vehicule.getUrl());
			statement.setString(14, vehicule.getVisibility());
			statement.setString(15, vehicule.getCreatedBy());
			return statement;
		}, keyHolder);

		Number key = keyHolder.getKey();
		if (key != null) {
			vehicule.setId(key.longValue());
		}
		return vehicule;
	}

	public List<Vehicule> findAll() {
		final String sql = """
			SELECT id, name, brand, model, generation, version, purchase_price, fuel_type, consumption, annual_mileage, insurance_cost, maintenance_cost, resale_value, url, visibility, created_by
			FROM vehicule
			ORDER BY id DESC
			""";
		return jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet));
	}

	public List<Vehicule> findAll(Integer page, Integer size, String name, String fuelType) {
		return findAll(page, size, name, fuelType, null, null, null);
	}

	public List<Vehicule> findAll(Integer page, Integer size, String name, String fuelType, String brand, String model, String version) {
		StringBuilder sql = new StringBuilder("""
			SELECT id, name, brand, model, generation, version, purchase_price, fuel_type, consumption, annual_mileage, insurance_cost, maintenance_cost, resale_value, url, visibility, created_by
			FROM vehicule
			WHERE 1=1
			""");
		List<Object> params = new java.util.ArrayList<>();
		if (name != null && !name.isBlank()) {
			String[] words = name.trim().split("\\s+");
			for (String word : words) {
				if (!word.isBlank()) {
					sql.append(" AND LOWER(name) LIKE ?");
					params.add("%" + word.toLowerCase() + "%");
				}
			}
		}
		if (fuelType != null && !fuelType.isBlank()) {
			sql.append(" AND fuel_type = ?");
			params.add(fuelType.trim().toUpperCase());
		}
		if (brand != null && !brand.isBlank()) {
			sql.append(" AND LOWER(brand) = ?");
			params.add(brand.trim().toLowerCase());
		}
		if (model != null && !model.isBlank()) {
			// Extract generation inside model name if it contains bracketed info e.g. "Série 1 (F40)" or "Série 1"
			String cleanModel = model.split("\\(")[0].trim().toLowerCase();
			sql.append(" AND LOWER(model) = ?");
			params.add(cleanModel);
			if (model.contains("(")) {
				String gen = model.substring(model.indexOf("(") + 1, model.indexOf(")")).trim().toLowerCase();
				sql.append(" AND LOWER(generation) = ?");
				params.add(gen);
			}
		}
		if (version != null && !version.isBlank()) {
			String cleanVersion = version.split("\\(")[0].trim().toLowerCase();
			sql.append(" AND LOWER(version) LIKE ?");
			params.add("%" + cleanVersion + "%");
		}
		sql.append(" ORDER BY id DESC");
		if (page != null && size != null) {
			sql.append(" LIMIT ? OFFSET ?");
			params.add(size);
			params.add(page * size);
		}
		return jdbcTemplate.query(sql.toString(), (resultSet, rowNum) -> mapRow(resultSet), params.toArray());
	}

	public Optional<Vehicule> findById(Long id) {
		final String sql = """
			SELECT id, name, brand, model, generation, version, purchase_price, fuel_type, consumption, annual_mileage, insurance_cost, maintenance_cost, resale_value, url, visibility, created_by
			FROM vehicule
			WHERE id = ?
			""";
		List<Vehicule> results = jdbcTemplate.query(sql, (resultSet, rowNum) -> mapRow(resultSet), id);
		return results.stream().findFirst();
	}

	public int update(Vehicule vehicule) {
		final String sql = """
			UPDATE vehicule
			SET name = ?, brand = ?, model = ?, generation = ?, version = ?, purchase_price = ?, fuel_type = ?, consumption = ?, annual_mileage = ?, insurance_cost = ?,
			    maintenance_cost = ?, resale_value = ?, url = ?, visibility = ?, created_by = ?
			WHERE id = ?
			""";
		return jdbcTemplate.update(
			sql,
			vehicule.getName(),
			vehicule.getBrand(),
			vehicule.getModel(),
			vehicule.getGeneration(),
			vehicule.getVersion(),
			vehicule.getPurchasePrice(),
			vehicule.getFuelType().name(),
			vehicule.getConsumption(),
			vehicule.getAnnualMileage(),
			vehicule.getInsuranceCost(),
			vehicule.getMaintenanceCost(),
			vehicule.getResaleValue(),
			vehicule.getUrl(),
			vehicule.getVisibility(),
			vehicule.getCreatedBy(),
			vehicule.getId()
		);
	}

	public int deleteById(Long id) {
		final String sql = "DELETE FROM vehicule WHERE id = ?";
		return jdbcTemplate.update(sql, id);
	}

	private Vehicule mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
		Vehicule vehicule = new Vehicule();
		vehicule.setId(resultSet.getLong("id"));
		vehicule.setName(resultSet.getString("name"));
		vehicule.setBrand(resultSet.getString("brand"));
		vehicule.setModel(resultSet.getString("model"));
		vehicule.setGeneration(resultSet.getString("generation"));
		vehicule.setVersion(resultSet.getString("version"));
		vehicule.setPurchasePrice(resultSet.getDouble("purchase_price"));
		vehicule.setFuelType(FuelType.valueOf(resultSet.getString("fuel_type")));
		vehicule.setConsumption(resultSet.getDouble("consumption"));
		vehicule.setAnnualMileage(resultSet.getInt("annual_mileage"));
		vehicule.setInsuranceCost(resultSet.getDouble("insurance_cost"));
		vehicule.setMaintenanceCost(resultSet.getDouble("maintenance_cost"));
		vehicule.setResaleValue(resultSet.getDouble("resale_value"));
		vehicule.setUrl(resultSet.getString("url"));
		vehicule.setVisibility(resultSet.getString("visibility"));
		vehicule.setCreatedBy(resultSet.getString("created_by"));
		return vehicule;
	}
}
