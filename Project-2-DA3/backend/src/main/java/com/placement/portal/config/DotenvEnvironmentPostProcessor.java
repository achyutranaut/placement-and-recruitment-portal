package com.placement.portal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads .env configuration into the Spring Environment before beans are instantiated.
 * <p>
 * Configuration Precedence (Highest to Lowest):
 * 1. Process environment variables (System.getenv() / Docker / Shell export) & System properties
 * 2. Project local .env (Project-2-DA3/.env)
 * 3. Repository root .env (student-placement-recruitment-portal/.env)
 * 4. application.yml defaults
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);
    public static final String PROPERTY_SOURCE_NAME = "dotenvProperties";
    public static final String NEO4J_CREDENTIAL_SOURCE_KEY = "portal.neo4j.credential-source";

    @Override
    public int getOrder() {
        // Run after standard config data post-processors so standard environment sources exist
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> dotenvMap = new LinkedHashMap<>();
        Map<String, String> sourceMap = new HashMap<>();

        // Locate candidate .env files in ascending precedence order:
        // 1. Root .env (lower precedence)
        // 2. Project-2-DA3/.env (higher precedence)
        File rootEnv = findRootEnv();
        File projectEnv = findProjectEnv();

        if (rootEnv != null && rootEnv.exists() && rootEnv.isFile()) {
            loadEnvFile(rootEnv, "Root .env (" + rootEnv.getAbsolutePath() + ")", dotenvMap, sourceMap);
        }

        if (projectEnv != null && projectEnv.exists() && projectEnv.isFile()) {
            loadEnvFile(projectEnv, "Project-2-DA3/.env (" + projectEnv.getAbsolutePath() + ")", dotenvMap, sourceMap);
        }

        // Determine effective credential source for Neo4j
        String credentialSource;
        if (System.getenv("NEO4J_PASSWORD") != null || System.getenv("NEO4J_URI") != null) {
            credentialSource = "System Environment Variable (shell/Docker/IDE)";
        } else if (sourceMap.containsKey("NEO4J_PASSWORD") || sourceMap.containsKey("NEO4J_URI")) {
            credentialSource = sourceMap.getOrDefault("NEO4J_PASSWORD", sourceMap.get("NEO4J_URI"));
        } else {
            credentialSource = "application.yml (defaults)";
        }
        dotenvMap.put(NEO4J_CREDENTIAL_SOURCE_KEY, credentialSource);

        if (!dotenvMap.isEmpty()) {
            MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvMap);
            if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
                // Placed right after systemEnvironment so OS env vars override .env, but .env overrides application.yml
                environment.getPropertySources().addAfter(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        propertySource
                );
            } else {
                environment.getPropertySources().addLast(propertySource);
            }
        }
    }

    private File findProjectEnv() {
        String explicit = System.getProperty("dotenv.project.file", System.getenv("PROJECT_DOTENV_FILE"));
        if (explicit != null && !explicit.isBlank()) {
            File f = new File(explicit);
            if (f.exists()) return f;
        }

        // Try standard locations
        File[] candidates = new File[]{
                new File("Project-2-DA3/.env"),
                new File(".env"),
                new File("../.env")
        };

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                // Verify whether this is Project-2-DA3/.env
                String abs = candidate.getAbsolutePath();
                if (abs.contains("Project-2-DA3") && !abs.contains("Project-1")) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private File findRootEnv() {
        String explicit = System.getProperty("dotenv.root.file", System.getenv("ROOT_DOTENV_FILE"));
        if (explicit != null && !explicit.isBlank()) {
            File f = new File(explicit);
            if (f.exists()) return f;
        }

        File[] candidates = new File[]{
                new File(".env"),
                new File("../.env"),
                new File("../../.env")
        };

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                String abs = candidate.getAbsolutePath();
                // If it is directly under repository root and not under Project-2-DA3
                if (!abs.contains("Project-2-DA3") && !abs.contains("Project-1")) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void loadEnvFile(File envFile, String label, Map<String, Object> targetMap, Map<String, String> sourceMap) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();

                // Strip surrounding single or double quotes
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                targetMap.put(key, value);
                sourceMap.put(key, label);

                // Add standard dotted Spring Boot property mappings for key environment variables
                mapSpringDottedProperty(key, value, targetMap);
            }
        } catch (Exception e) {
            log.warn("Could not read .env file at {}: {}", envFile.getAbsolutePath(), e.getMessage());
        }
    }

    private void mapSpringDottedProperty(String key, String value, Map<String, Object> targetMap) {
        switch (key) {
            case "PORTAL_NEO4J_ENABLED" -> targetMap.put("portal.neo4j.enabled", value);
            case "NEO4J_URI" -> targetMap.put("portal.neo4j.uri", value);
            case "NEO4J_USERNAME" -> targetMap.put("portal.neo4j.username", value);
            case "NEO4J_PASSWORD" -> targetMap.put("portal.neo4j.password", value);
            case "JWT_SECRET" -> targetMap.put("jwt.secret", value);
            case "SERVER_PORT" -> targetMap.put("server.port", value);
            default -> {}
        }
    }
}
