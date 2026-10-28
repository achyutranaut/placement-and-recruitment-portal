package com.placement.portal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class PlacementPortalApplication {

    private static final Logger log = LoggerFactory.getLogger(PlacementPortalApplication.class);

    public static void main(String[] args) {
        loadDotEnvIfExists();
        SpringApplication.run(PlacementPortalApplication.class, args);
    }

    /**
     * Inspects candidate directories for local .env files and safely populates
     * missing environment configuration into JVM System Properties before Spring Boot initializes.
     * Never overwrites explicitly provided OS environment variables or command-line flags.
     */
    static void loadDotEnvIfExists() {
        Path[] candidatePaths = {
                Path.of(".env"),
                Path.of("../.env"),
                Path.of("../../.env"),
                Path.of("backend/.env"),
                Path.of("Project-2-DA3/.env")
        };

        for (Path path : candidatePaths) {
            if (Files.isRegularFile(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    int count = 0;
                    String rawLine;
                    while ((rawLine = reader.readLine()) != null) {
                        String line = rawLine.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String value = line.substring(eqIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                    (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            // Do not override pre-existing OS env vars or system properties
                            if (System.getenv(key) == null && System.getProperty(key) == null) {
                                System.setProperty(key, value);
                                count++;
                            }
                        }
                    }
                    if (count > 0) {
                        log.info("Loaded {} configuration properties from {}", count, path.toAbsolutePath().normalize());
                    }
                    break;
                } catch (Exception e) {
                    log.debug("Unable to read .env from {}: {}", path, e.getMessage());
                }
            }
        }
    }
}
