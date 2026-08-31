package com.example.springbootapp.service;

import com.example.springbootapp.model.entity.MediaFile;
import com.example.springbootapp.repository.MediaFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    private final Path uploadRoot;
    private final MediaFileRepository mediaFileRepository;

    public FileUploadService(@Value("${app.upload.dir:uploads}") String uploadDir,
                             MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
            Files.createDirectories(this.uploadRoot.resolve("brands"));
            Files.createDirectories(this.uploadRoot.resolve("models"));
            Files.createDirectories(this.uploadRoot.resolve("finitions"));
        } catch (IOException e) {
            logger.error("Impossible de créer le répertoire local d'upload : {}", this.uploadRoot, e);
        }
    }

    @Transactional
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

        String cleanSubFolder = (subFolder != null && !subFolder.isBlank()) ? subFolder.split(",")[0].trim() : "general";
        String safeSubFolder = !cleanSubFolder.isBlank() ? cleanSubFolder.replaceAll("[^a-zA-Z0-9_-]", "") : "general";
        String uniqueName = UUID.randomUUID().toString().replace("-", "").substring(0, 16) + extension;

        try {
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = extension.equals(".svg") ? "image/svg+xml" :
                              extension.equals(".png") ? "image/png" :
                              extension.equals(".webp") ? "image/webp" : "image/jpeg";
            }

            // 1. Sauvegarde en Base de Données (Persistance pérenne)
            MediaFile media = new MediaFile(safeSubFolder, uniqueName, contentType, fileBytes);
            mediaFileRepository.save(media);

            // 2. Sauvegarde miroir sur disque local (Cache rapide si possible)
            try {
                Path targetDir = safeSubFolder.isEmpty() ? this.uploadRoot : this.uploadRoot.resolve(safeSubFolder);
                Files.createDirectories(targetDir);
                Path targetPath = targetDir.resolve(uniqueName);
                Files.write(targetPath, fileBytes);
            } catch (Exception ex) {
                logger.debug("Écriture cache disque locale ignorée : {}", ex.getMessage());
            }

            String publicUrl = "/uploads/" + (safeSubFolder.isEmpty() ? "" : safeSubFolder + "/") + uniqueName;
            logger.info("Image persistée en Base de Données : {}/{} -> {}", safeSubFolder, uniqueName, publicUrl);
            return publicUrl;
        } catch (IOException e) {
            logger.error("Erreur lors de la lecture du fichier envoyé", e);
            throw new RuntimeException("Échec du traitement du fichier.", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<MediaFile> getMediaFile(String folder, String fileName) {
        return mediaFileRepository.findByFolderAndFileName(folder, fileName);
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }
}
