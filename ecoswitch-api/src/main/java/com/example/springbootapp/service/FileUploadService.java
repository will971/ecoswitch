package com.example.springbootapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    private final Path uploadRoot;

    public FileUploadService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
            Files.createDirectories(this.uploadRoot.resolve("brands"));
            Files.createDirectories(this.uploadRoot.resolve("models"));
            Files.createDirectories(this.uploadRoot.resolve("finitions"));
        } catch (IOException e) {
            logger.error("Impossible de créer le répertoire d'upload : {}", this.uploadRoot, e);
        }
    }

    public String saveImage(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier envoyé est vide.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        } else {
            extension = ".png";
        }

        if (!extension.matches("^\\.(png|jpe?g|svg|webp|gif)$")) {
            throw new IllegalArgumentException("Format de fichier non supporté. Formats autorisés : png, jpg, jpeg, svg, webp, gif.");
        }

        String cleanSubFolder = (subFolder != null && !subFolder.isBlank()) ? subFolder.split(",")[0].trim() : "";
        String safeSubFolder = !cleanSubFolder.isBlank() ? cleanSubFolder.replaceAll("[^a-zA-Z0-9_-]", "") : "";
        Path targetDir = safeSubFolder.isEmpty() ? this.uploadRoot : this.uploadRoot.resolve(safeSubFolder);

        try {
            Files.createDirectories(targetDir);
            String uniqueName = UUID.randomUUID().toString().replace("-", "").substring(0, 16) + extension;
            Path targetPath = targetDir.resolve(uniqueName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String publicUrl = "/uploads/" + (safeSubFolder.isEmpty() ? "" : safeSubFolder + "/") + uniqueName;
            logger.info("Image enregistrée avec succès : {} -> {}", targetPath, publicUrl);
            return publicUrl;
        } catch (IOException e) {
            logger.error("Erreur lors de l'enregistrement de l'image", e);
            throw new RuntimeException("Échec de l'enregistrement du fichier sur le serveur.", e);
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }
}
