package com.example.springbootapp.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.example.springbootapp.config.AppAdminProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class LogFileService {

	private final Path logsDir;

	public LogFileService(AppAdminProperties adminProperties) {
		this.logsDir = Paths.get(adminProperties.getLogDirectory()).toAbsolutePath().normalize();
	}

	public List<LogFileInfo> listLogFiles() throws IOException {
		if (!Files.exists(logsDir)) {
			return List.of();
		}
		try (Stream<Path> files = Files.list(logsDir)) {
			return files
				.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".log"))
				.map(this::toInfo)
				.sorted(Comparator.comparing(LogFileInfo::lastModifiedEpochMs).reversed())
				.toList();
		}
	}

	public String readTail(String fileName, int maxLines) throws IOException {
		Path target = resolveSafe(fileName);
		List<String> lines = Files.readAllLines(target, StandardCharsets.UTF_8);
		int fromIndex = Math.max(0, lines.size() - maxLines);
		return String.join("\n", lines.subList(fromIndex, lines.size()));
	}

	public Resource asResource(String fileName) {
		Path target = resolveSafe(fileName);
		return new FileSystemResource(target);
	}

	private Path resolveSafe(String fileName) {
		Path candidate = logsDir.resolve(fileName).normalize();
		if (!candidate.startsWith(logsDir)) {
			throw new IllegalArgumentException("Invalid log file path.");
		}
		return candidate;
	}

	private LogFileInfo toInfo(Path path) {
		try {
			long sizeBytes = Files.size(path);
			long lastModified = Files.getLastModifiedTime(path).toMillis();
			return new LogFileInfo(path.getFileName().toString(), sizeBytes, lastModified);
		} catch (IOException exception) {
			return new LogFileInfo(path.getFileName().toString(), -1L, -1L);
		}
	}

	public record LogFileInfo(String name, long sizeBytes, long lastModifiedEpochMs) {}
}
