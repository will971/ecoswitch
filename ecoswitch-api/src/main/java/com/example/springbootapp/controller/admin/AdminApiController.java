package com.example.springbootapp.controller.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springbootapp.monitoring.UsageMonitor;
import com.example.springbootapp.service.JvmUsageService;
import com.example.springbootapp.service.LogFileService;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin")
@Hidden
@Tag(name = "Administration", description = "Monitoring logs, usage applicatif et metriques JVM")
@SecurityRequirement(name = "basicAuth")
public class AdminApiController {

	private static final int DEFAULT_LINES = 300;
	private static final int MAX_LINES = 2_000;

	private final LogFileService logFileService;
	private final UsageMonitor usageMonitor;
	private final JvmUsageService jvmUsageService;
	private final LoggingSystem loggingSystem;

	public AdminApiController(
			LogFileService logFileService,
			UsageMonitor usageMonitor,
			JvmUsageService jvmUsageService,
			LoggingSystem loggingSystem) {
		this.logFileService = logFileService;
		this.usageMonitor = usageMonitor;
		this.jvmUsageService = jvmUsageService;
		this.loggingSystem = loggingSystem;
	}

	@GetMapping("/log-files")
	@Operation(summary = "Lister les fichiers de log")
	@ApiResponse(responseCode = "200", description = "Liste des fichiers de log")
	public List<LogFileService.LogFileInfo> logFiles() throws IOException {
		return logFileService.listLogFiles();
	}

	@GetMapping("/log-files/{fileName:.+}")
	@Operation(summary = "Lire les dernieres lignes d'un fichier log")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Contenu du fichier"),
			@ApiResponse(responseCode = "400", description = "Fichier invalide")
	})
	public Map<String, String> readLogFile(
			@Parameter(description = "Nom du fichier .log") @PathVariable String fileName,
			@Parameter(description = "Nombre max de lignes retournees") @RequestParam(defaultValue = ""
					+ DEFAULT_LINES) int lines)
			throws IOException {
		int safeLines = Math.min(Math.max(1, lines), MAX_LINES);
		return Map.of("fileName", fileName, "content", logFileService.readTail(fileName, safeLines));
	}

	@GetMapping("/usage")
	@Operation(summary = "Afficher l'usage des services et DAO instrumentes")
	@ApiResponse(responseCode = "200", description = "Statistiques d'utilisation")
	public List<UsageMonitor.UsageSnapshot> usage() {
		return usageMonitor.getSnapshots();
	}

	@GetMapping("/jvm-usage")
	@Operation(summary = "Afficher l'etat JVM (memoire, threads, CPU, uptime)")
	@ApiResponse(responseCode = "200", description = "Metriques JVM")
	public JvmUsageService.JvmUsageSnapshot jvmUsage() {
		return jvmUsageService.snapshot();
	}

	@GetMapping("/log-files/{fileName:.+}/download")
	@Operation(summary = "Telecharger un fichier log")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Fichier telecharge"),
			@ApiResponse(responseCode = "400", description = "Fichier introuvable")
	})
	public ResponseEntity<Resource> downloadLogFile(@PathVariable String fileName) {
		Resource resource = logFileService.asResource(fileName);
		if (!resource.exists() || !resource.isReadable()) {
			throw new IllegalArgumentException("Log file not found.");
		}
		String safeFileName = fileName.replace("\"", "");
		return ResponseEntity.ok()
				.contentType(MediaType.TEXT_PLAIN)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName + "\"")
				.body(resource);
	}

	@GetMapping("/log-level")
	@Operation(summary = "Lire le niveau de log d'un logger")
	@ApiResponse(responseCode = "200", description = "Niveau de log courant")
	public Map<String, String> logLevel(@RequestParam(defaultValue = "ROOT") String logger) {
		LoggerConfiguration loggerConfig = loggingSystem.getLoggerConfiguration(logger);
		if (loggerConfig == null) {
			return Map.of("logger", logger, "configuredLevel", "N/A", "effectiveLevel", "UNKNOWN");
		}
		String configuredLevel = loggerConfig.getConfiguredLevel() == null
				? "INHERITED"
				: loggerConfig.getConfiguredLevel().name();
		return Map.of(
				"logger",
				loggerConfig.getName(),
				"configuredLevel",
				configuredLevel,
				"effectiveLevel",
				loggerConfig.getEffectiveLevel().name());
	}

	@PutMapping("/log-level")
	@Operation(summary = "Mettre a jour le niveau de log sans redemarrage")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Niveau de log mis a jour"),
			@ApiResponse(responseCode = "400", description = "Requete invalide")
	})
	public ResponseEntity<Map<String, String>> updateLogLevel(@RequestBody UpdateLogLevelRequest request) {
		String logger = (request.logger() == null || request.logger().isBlank()) ? "ROOT" : request.logger();
		if (request.level() == null || request.level().isBlank()) {
			throw new IllegalArgumentException("Log level is required.");
		}
		LogLevel level = LogLevel.valueOf(request.level().toUpperCase());
		loggingSystem.setLogLevel(logger, level);
		return ResponseEntity.ok(logLevel(logger));
	}

	@ExceptionHandler({ IllegalArgumentException.class, IOException.class })
	@ApiResponse(responseCode = "400", description = "Erreur de validation / lecture", content = @Content(schema = @Schema(implementation = Map.class)))
	public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", exception.getMessage()));
	}

	public record UpdateLogLevelRequest(String logger, String level) {
	}
}
