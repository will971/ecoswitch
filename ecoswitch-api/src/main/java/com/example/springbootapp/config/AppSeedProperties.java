package com.example.springbootapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.seed")
public class AppSeedProperties {

	private String vehiclesFile = "";
	private String vehiclesUrl = "";

	public String getVehiclesFile() {
		return vehiclesFile;
	}

	public void setVehiclesFile(String vehiclesFile) {
		this.vehiclesFile = vehiclesFile;
	}

	public String getVehiclesUrl() {
		return vehiclesUrl;
	}

	public void setVehiclesUrl(String vehiclesUrl) {
		this.vehiclesUrl = vehiclesUrl;
	}
}
