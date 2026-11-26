package com.example.expensetracker.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * App settings. Looked up in this order (first one wins):
 *   1. environment variable   SERVER_PORT
 *   2. .env file              SERVER_PORT=9090
 *   3. application.properties server.port=8080   (inside the jar)
 */
public class AppConfig {

    private static final Logger log = Logger.getLogger(AppConfig.class.getName());

    private final Properties defaults = new Properties();
    private final Properties dotEnv = new Properties();

    public AppConfig() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties is missing from the classpath");
            }
            defaults.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read application.properties", e);
        }

        // .env uses KEY=value lines, the same format a properties file reads
        Path envFile = Path.of(".env");
        if (Files.exists(envFile)) {
            try (Reader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
                dotEnv.load(reader);
                log.info("Loaded settings from .env");
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't read .env", e);
            }
        }
    }

    public String get(String key) {
        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            value = dotEnv.getProperty(envKey);
        }
        if (value == null || value.isBlank()) {
            value = defaults.getProperty(key);
        }
        return value == null ? null : value.trim();
    }

    public String require(String key) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            String envKey = key.toUpperCase().replace('.', '_');
            throw new IllegalStateException(envKey + " must be set (in .env or as an environment variable)");
        }
        return value;
    }

    public int getInt(String key) {
        return Integer.parseInt(require(key));
    }

    public List<String> getList(String key) {
        return Arrays.stream(require(key).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
