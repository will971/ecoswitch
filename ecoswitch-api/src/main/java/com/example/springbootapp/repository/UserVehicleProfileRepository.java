package com.example.springbootapp.repository;

import com.example.springbootapp.model.entity.UserVehicleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVehicleProfileRepository extends JpaRepository<UserVehicleProfile, Long> {
    List<UserVehicleProfile> findByUserEmail(String userEmail);
}
