package com.example.springbootapp.business.admin;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.springbootapp.monitoring.UsageMonitor;
import com.example.springbootapp.service.JvmUsageService;
import com.example.springbootapp.service.LogFileService;

@Component
public class AdminApiBusiness {

    private final LogFileService logFileService;
    private final UsageMonitor usageMonitor;
    private final JvmUsageService jvmUsageService;
    private final LoggingSystem loggingSystem;

    public AdminApiBusiness(
            LogFileService logFileService,
            UsageMonitor usageMonitor,
            JvmUsageService jvmUsageService,
            LoggingSystem loggingSystem) {
        this.logFileService = logFileService;
        this.usageMonitor = usageMonitor;
        this.jvmUsageService = jvmUsageService;
        this.loggingSystem = loggingSystem;
    }

    public List<LogFileService.LogFileInfo> listLogFiles() throws IOException {
        return logFileService.listLogFiles();
    }

    public String readLogFileTail(String fileName, int lines) throws IOException {
        int safeLines = Math.min(Math.max(1, lines), 2_000);
        return logFileService.readTail(fileName, safeLines);
    }

    public List<UsageMonitor.UsageSnapshot> getUsageSnapshots() {
        return usageMonitor.getSnapshots();
    }

    public JvmUsageService.JvmUsageSnapshot getJvmUsageSnapshot() {
        return jvmUsageService.snapshot();
    }

    public Resource getLogFileAsResource(String fileName) {
        Resource resource = logFileService.asResource(fileName);
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("Log file not found.");
        }
        return resource;
    }

    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
        return loggingSystem.getLoggerConfiguration(loggerName);
    }

    public void setLogLevel(String loggerName, LogLevel level) {
        loggingSystem.setLogLevel(loggerName, level);
    }
}
