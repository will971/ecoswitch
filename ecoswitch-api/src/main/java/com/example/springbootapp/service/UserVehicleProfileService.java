package com.example.springbootapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.springbootapp.model.entity.UserVehicleProfile;
import com.example.springbootapp.repository.UserVehicleProfileRepository;

@Service
public class UserVehicleProfileService {

    private final UserVehicleProfileRepository repository;

    public UserVehicleProfileService(UserVehicleProfileRepository repository) {
        this.repository = repository;
    }

    public List<UserVehicleProfile> findByUserEmail(String email) {
        return repository.findByUserEmail(email);
    }

    public Optional<UserVehicleProfile> findById(Long id) {
        return repository.findById(id);
    }

    public UserVehicleProfile save(UserVehicleProfile profile) {
        return repository.save(profile);
    }

    public void delete(UserVehicleProfile profile) {
        repository.delete(profile);
    }
}
