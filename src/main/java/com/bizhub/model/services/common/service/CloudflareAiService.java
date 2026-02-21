package com.bizhub.model.services.common.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cloudflare Workers AI client.
 *
 * Uses the SDXL Base 1.0 model endpoint and returns raw image bytes.
 */
public class CloudflareAiService {

    private static final String MODEL = "@cf/stabilityai/stable-diffusion-xl-base-1.0";

    private final HttpClient http;

    public CloudflareAiService() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    /**
     * Generate an image using Cloudflare Workers AI.
     *
     * @param prompt generation prompt
     * @return raw bytes of the returned image (png/jpeg)
     */
    public byte[] generateFormationImage(String prompt) throws IOException, InterruptedException {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be empty");
        }

        String token = EnvConfig.getCloudflareApiToken();
        String accountId = EnvConfig.getCloudflareAccountId();

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("CLOUDFLARE_API_TOKEN is missing (check .env)");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException("CLOUDFLARE_ACC_ID is missing (check .env)");
        }

        String endpoint = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/" + MODEL;

        String jsonBody = "{\"prompt\":" + toJsonString(prompt) + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());

        int status = res.statusCode();
        if (status < 200 || status >= 300) {
            String bodyPreview;
            try {
                bodyPreview = new String(res.body(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                bodyPreview = "<non-text response>";
            }
            throw new IOException("Cloudflare AI request failed (HTTP " + status + "): " + bodyPreview);
        }

        byte[] bytes = res.body();
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Cloudflare AI returned an empty response");
        }

        return bytes;
    }

    private static String toJsonString(String s) {
        // Minimal JSON string escaping (enough for prompt text)
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}

