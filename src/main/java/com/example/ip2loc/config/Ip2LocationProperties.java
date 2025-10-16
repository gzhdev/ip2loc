package com.example.ip2loc.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for the IP2Location integration.
 */
@Configuration
@ConfigurationProperties(prefix = "ip2location")
public class Ip2LocationProperties {

    /**
     * Authentication token issued by IP2Location. When provided, the application automatically
     * downloads the database file on startup and at the configured update interval.
     */
    private String token;

    /**
     * IP2Location edition to download. Defaults to the free DB11 Lite edition in binary format.
     */
    private String edition = "DB11LITEBIN";

    /**
     * Directory that will contain the decompressed BIN file.
     */
    private Path databasePath = Path.of("data", "IP2LOCATION-LITE-DB11.BIN");

    /**
     * Update interval for refreshing the database. Defaults to one day.
     */
    private Duration updateInterval = Duration.ofDays(1);

    public Optional<String> getToken() {
        return Optional.ofNullable(token).filter(s -> !s.isBlank());
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public void setDatabasePath(Path databasePath) {
        this.databasePath = databasePath;
    }

    public Duration getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(Duration updateInterval) {
        this.updateInterval = updateInterval;
    }
}
