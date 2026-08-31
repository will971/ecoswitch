package com.example.springbootapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // Le streaming et le cache des images sont gérés de manière réactive et persistante par FileUploadController
}
