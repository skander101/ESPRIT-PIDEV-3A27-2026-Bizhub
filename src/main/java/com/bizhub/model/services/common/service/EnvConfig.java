package com.bizhub.model.services.common.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvConfig: Loads environment variables from a .env file in the project root.
 *
 * Format: simple KEY=VALUE pairs. Lines starting with '#' are comments.
 * Values may be optionally quoted with single or double quotes.
 *
 * No external library is used.
 */
public final class EnvConfig {

    private static volatile Map<String, String> env;

    private EnvConfig() {
    }

    private static Map<String, String> loadEnvFile() {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            System.err.println("[EnvConfig] .env file not found at project root (" + envPath.toAbsolutePath() + "). " +
                    "Falling back to system environment variables.");
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(envPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int idx = trimmed.indexOf('=');
                if (idx <= 0) continue;

                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();

                // Support optional export keyword: export KEY=VALUE
                if (key.startsWith("export ")) {
                    key = key.substring("export ".length()).trim();
                }

                // Strip optional quotes
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) value = value.substring(1, value.length() - 1);
                }

                map.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("[EnvConfig] Failed to read .env file: " + e.getMessage());
            return Collections.emptyMap();
        }

        return map;
    }

    private static Map<String, String> getEnv() {
        if (env == null) {
            synchronized (EnvConfig.class) {
                if (env == null) {
                    env = loadEnvFile();
                }
            }
        }
        return env;
    }

    /**
     * Get an environment variable value.
     * First checks .env file, then falls back to system environment variables.
     */
    public static String get(String key) {
        String value = getEnv().get(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        if (value == null || value.isBlank()) {
            System.err.println("[EnvConfig] Missing key: " + key + " (not found in .env nor system env)");
        }
        return value;
    }

    /**
     * Get an environment variable value with a default fallback.
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    // Auth0 Configuration
    public static String getAuth0Domain() {
        return get("AUTH0_DOMAIN");
    }

    public static String getAuth0ClientId() {
        return get("AUTH0_CLIENT_ID");
    }

    public static String getAuth0ClientSecret() {
        return get("AUTH0_CLIENT_SECRET");
    }

    // Infobip Configuration
    public static String getInfobipApiKey() {
        return get("INFOBIP_API_KEY");
    }

    public static String getInfobipBaseUrl() {
        return get("INFOBIP_BASE_URL");
    }

    // Cloudflare Workers AI
    public static String getCloudflareApiToken() {
        return get("CLOUDFLARE_API_TOKEN");
    }

    public static String getCloudflareAccountId() {
        return get("CLOUDFLARE_ACC_ID");
    }
}
