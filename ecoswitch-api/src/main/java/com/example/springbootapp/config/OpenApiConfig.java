package com.example.springbootapp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
	info = @Info(
		title = "Springboot App API",
		version = "v1",
		description = "API publique de gestion des vehicules.",
		contact = @Contact(name = "API Support")
	)
)
@SecurityScheme(
	name = "basicAuth",
	type = SecuritySchemeType.HTTP,
	scheme = "basic"
)
public class OpenApiConfig {
}
