package com.example.springbootapp.controller.upload;

import com.example.springbootapp.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class FileUploadControllerTest {

    private FileUploadService fileUploadService;
    private FileUploadController fileUploadController;

    @BeforeEach
    void setUp() {
        fileUploadService = Mockito.mock(FileUploadService.class);
        fileUploadController = new FileUploadController(fileUploadService);
    }

    @Test
    void shouldUploadImageSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-logo.png",
                "image/png",
                "fake image content".getBytes()
        );

        when(fileUploadService.saveImage(any(), eq("brands"))).thenReturn("/uploads/brands/abc12345.png");

        ResponseEntity<Map<String, String>> response = fileUploadController.uploadImage(file, "brands");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("/uploads/brands/abc12345.png", response.getBody().get("url"));
    }
}
