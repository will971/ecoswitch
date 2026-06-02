package com.example.springbootapp.config;

import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

	@Bean
	public LoggingSystem loggingSystem() {
		return LoggingSystem.get(getClass().getClassLoader());
	}
}
