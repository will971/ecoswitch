package com.example.springbootapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.springbootapp.model.entity.AppUser;
import com.example.springbootapp.repository.AppUserRepository;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail("admin")) {
            logger.info("Seeding default admin user (admin/admin)");
            AppUser admin = new AppUser(
                "admin",
                "Admin",
                passwordEncoder.encode("admin"),
                "email",
                "Pro",
                "ADMIN"
            );
            userRepository.save(admin);
            logger.info("Admin user seeded successfully.");
        } else {
            logger.info("Admin user already exists in database.");
        }
    }
}
