package com.example.ip2loc.service;

import com.example.ip2loc.config.Ip2LocationProperties;
import com.example.ip2loc.model.LocationResponse;
import com.ip2location.IP2Location;
import com.ip2location.IPResult;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service that encapsulates the lifecycle of the IP2Location database and exposes lookup
 * functionality.
 */
@Service
public class Ip2LocationService {

    private static final Logger log = LoggerFactory.getLogger(Ip2LocationService.class);
    private static final String DOWNLOAD_TEMPLATE = "https://www.ip2location.com/download?token=%s&file=%s";

    private final Ip2LocationProperties properties;
    private final ReentrantReadWriteLock dbLock = new ReentrantReadWriteLock();
    private IP2Location database;

    public Ip2LocationService(Ip2LocationProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        ensureDatabase();
    }

    /**
     * Periodically refresh the database file using the configured token.
     */
    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "#{@ip2LocationProperties.updateInterval.toMillis()}")
    public void refreshDatabase() {
        if (properties.getToken().isEmpty()) {
            log.debug("Skipping database refresh because no IP2Location token is configured.");
            return;
        }
        log.info("Refreshing IP2Location database");
        downloadDatabaseIfNecessary(true);
    }

    /**
     * Look up the location data for the provided IP address.
     *
     * @param ip dotted IPv4 or IPv6 address
     * @return optional response when the lookup succeeded
     */
    public Optional<LocationResponse> lookup(String ip) {
        Objects.requireNonNull(ip, "ip must not be null");
        dbLock.readLock().lock();
        try {
            IP2Location db = database;
            if (db == null) {
                return Optional.empty();
            }
            IPResult result = db.IPQuery(ip);
            if (!"OK".equalsIgnoreCase(result.getStatus())) {
                log.debug("IP2Location lookup returned status {} for ip {}", result.getStatus(), ip);
                return Optional.empty();
            }
            return Optional.of(mapResult(result, ip));
        } catch (Exception e) {
            log.warn("Failed to resolve IP {}: {}", ip, e.getMessage());
            return Optional.empty();
        } finally {
            dbLock.readLock().unlock();
        }
    }

    private LocationResponse mapResult(IPResult result, String ip) {
        return new LocationResponse(
                ip,
                emptyToNull(result.getCountryShort()),
                emptyToNull(result.getCountryLong()),
                emptyToNull(result.getRegion()),
                emptyToNull(result.getCity()),
                emptyToNull(result.getZipCode()),
                parseFloat(result.getLatitude()),
                parseFloat(result.getLongitude()),
                emptyToNull(result.getTimeZone()),
                emptyToNull(result.getISP()));
    }

    private Double parseFloat(float value) {
        if (Float.isNaN(value)) {
            return null;
        }
        return (double) value;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private void ensureDatabase() {
        if (Files.exists(properties.getDatabasePath())) {
            loadDatabase(properties.getDatabasePath());
            return;
        }
        downloadDatabaseIfNecessary(false);
    }

    private void downloadDatabaseIfNecessary(boolean force) {
        Optional<String> token = properties.getToken();
        if (token.isEmpty()) {
            if (force) {
                log.warn("Cannot refresh database without IP2Location token. Set ip2location.token or "
                        + "IP2LOCATION_TOKEN environment variable.");
            }
            return;
        }
        try {
            Path target = properties.getDatabasePath();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path downloaded = downloadDatabase(token.get(), properties.getEdition());
            if (downloaded != null) {
                Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING);
                loadDatabase(target);
            }
        } catch (IOException e) {
            log.error("Failed to download IP2Location database: {}", e.getMessage());
        }
    }

    private void loadDatabase(Path databasePath) {
        dbLock.writeLock().lock();
        try {
            IP2Location db = new IP2Location();
            db.Open(databasePath.toString(), true); // use memory mapped mode for faster lookups
            IP2Location old = this.database;
            this.database = db;
            if (old != null) {
                old.Close();
            }
            log.info("Loaded IP2Location database from {} at {}", databasePath, Instant.now());
        } catch (IOException e) {
            log.error("Unable to load IP2Location database: {}", e.getMessage());
        } finally {
            dbLock.writeLock().unlock();
        }
    }

    private Path downloadDatabase(String token, String edition) throws IOException {
        String url = DOWNLOAD_TEMPLATE.formatted(token, edition);
        log.info("Downloading IP2Location edition {}", edition);
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);
        int status = connection.getResponseCode();
        if (status >= 400) {
            try (InputStream errorStream = connection.getErrorStream()) {
                String errorMessage = errorStream != null
                        ? new String(errorStream.readAllBytes())
                        : "HTTP status " + status;
                log.error("Failed to download IP2Location database: {}", errorMessage);
            }
            return null;
        }
        Path tempZip = Files.createTempFile("ip2location-", ".zip");
        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, tempZip, StandardCopyOption.REPLACE_EXISTING);
        }
        Path tempBin = Files.createTempFile("ip2location-", ".bin");
        boolean extracted = false;
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(tempZip))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".bin")) {
                    Files.copy(zipInputStream, tempBin, StandardCopyOption.REPLACE_EXISTING);
                    extracted = true;
                    break;
                }
            }
        }
        Files.deleteIfExists(tempZip);
        if (!extracted) {
            log.error("Downloaded archive did not contain a BIN file");
            Files.deleteIfExists(tempBin);
            return null;
        }
        return tempBin;
    }
}
