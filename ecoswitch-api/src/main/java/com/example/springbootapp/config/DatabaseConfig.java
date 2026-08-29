package com.example.springbootapp.config;

import java.net.URI;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String rawUrl = System.getenv("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = properties.getUrl();
        }

        String username = properties.getUsername();
        String password = properties.getPassword();
        String jdbcUrl = rawUrl;

        // Auto-adapt Cloud PaaS URL (e.g. postgresql://user:pass@host:port/db or postgres://...)
        if (rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            try {
                URI uri = new URI(rawUrl.replace("jdbc:", ""));
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "ecoswitch";

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    username = userInfo[0];
                    if (userInfo.length > 1) {
                        password = userInfo[1];
                    }
                }

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                logger.info("Auto-configuration JDBC Cloud pour PostgreSQL : host={}, port={}, db={}", host, port, dbName);
            } catch (Exception e) {
                logger.warn("Impossible de parser l'URI de base de données ({}): {}", rawUrl, e.getMessage());
            }
        }

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(jdbcUrl);
        if (username != null && !username.isBlank()) {
            dataSource.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            dataSource.setPassword(password);
        }
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(2);
        dataSource.setIdleTimeout(30000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setConnectionTimeout(20000);
        return dataSource;
    }
}
