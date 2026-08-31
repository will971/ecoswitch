package com.example.springbootapp.controller.upload;

import com.example.springbootapp.model.entity.MediaFile;
import com.example.springbootapp.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@Tag(name = "Uploads & Media", description = "Gestion et service des images stockées en Base de Données")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(value = "/api/v1/uploads/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader une image et la stocker en base de données de manière persistante")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "") String folder
    ) {
        String url = fileUploadService.saveImage(file, folder);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping(value = {
            "/uploads/{folder}/{fileName:.+}",
            "/uploads/{fileName:.+}"
    })
    @Operation(summary = "Servir une image depuis la base de données avec mise en cache HTTP")
    public ResponseEntity<byte[]> serveMediaImage(
            @PathVariable(value = "folder", required = false) String folder,
            @PathVariable(value = "fileName") String fileName
    ) {
        String effectiveFolder = (folder == null || folder.isBlank()) ? "general" : folder;
        Optional<MediaFile> mediaOpt = fileUploadService.getMediaFile(effectiveFolder, fileName);

        if (mediaOpt.isEmpty() && folder != null && !folder.equals("general")) {
            // Fallback si le dossier racine est utilisé
            mediaOpt = fileUploadService.getMediaFile("general", fileName);
        }

        if (mediaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        MediaFile media = mediaOpt.get();
        String contentType = media.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(media.getData());
    }
}
