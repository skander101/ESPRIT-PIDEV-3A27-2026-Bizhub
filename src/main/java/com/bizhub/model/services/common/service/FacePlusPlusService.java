package com.bizhub.model.services.common.service;

import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FacePlusPlusService: minimal Face++ API wrapper.
 *
 * - Loads FACEPP_API_KEY and FACEPP_API_SECRET from .env via EnvConfig (no hardcoded secrets)
 * - Uses Java 21 HttpClient
 * - Parses JSON via org.json
 * - Provides Task-based APIs to avoid blocking JavaFX UI thread
 */
public class FacePlusPlusService {

    private static final URI DETECT_URI = URI.create("https://api-us.faceplusplus.com/facepp/v3/detect");
    private static final URI COMPARE_URI = URI.create("https://api-us.faceplusplus.com/facepp/v3/compare");

    private final String apiKey;
    private final String apiSecret;
    private final HttpClient http;

    public FacePlusPlusService() {
        this.apiKey = EnvConfig.get("FACEPP_API_KEY");
        this.apiSecret = EnvConfig.get("FACEPP_API_SECRET");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank();
    }

    /**
     * Detect single face, returning token and bounding box when available.
     *
     * NOTE: Network call; wrap in Task via detectFaceTask(...).
     */
    public FaceDetectionResult detectFace(Image fxImage) throws IOException {
        ensureConfigured();
        if (fxImage == null) {
            return new FaceDetectionResult(false, null, null);
        }

        String imageBase64 = imageToBase64(fxImage);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("api_key", apiKey);
        form.put("api_secret", apiSecret);
        form.put("image_base64", imageBase64);
        form.put("return_attributes", "none");

        String body = formUrlEncode(form);

        HttpRequest request = HttpRequest.newBuilder(DETECT_URI)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Face++ detect interrupted", e);
        } catch (Exception e) {
            throw new IOException("Face++ detect network error: " + e.getMessage(), e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new IOException("Face++ detect failed: HTTP " + response.statusCode() + " - " + safeBody(response.body()));
        }

        try {
            JSONObject json = new JSONObject(response.body());
            JSONArray faces = json.optJSONArray("faces");
            if (faces == null || faces.isEmpty()) {
                return new FaceDetectionResult(false, null, null);
            }

            JSONObject face0 = faces.getJSONObject(0);
            String token = face0.optString("face_token", null);

            Rectangle2D box = null;
            JSONObject rect = face0.optJSONObject("face_rectangle");
            if (rect != null) {
                double top = rect.optDouble("top", Double.NaN);
                double left = rect.optDouble("left", Double.NaN);
                double width = rect.optDouble("width", Double.NaN);
                double height = rect.optDouble("height", Double.NaN);
                if (Double.isFinite(top) && Double.isFinite(left) && Double.isFinite(width) && Double.isFinite(height)) {
                    box = new Rectangle2D(left, top, width, height);
                }
            }

            return new FaceDetectionResult(true, token, box);
        } catch (JSONException e) {
            throw new IOException("Invalid JSON from Face++ detect", e);
        }
    }

    /**
     * Compare two images and return confidence score.
     *
     * NOTE: Network call; wrap in Task via compareFacesTask(...).
     */
    public double compareFaces(Image img1, Image img2) throws IOException {
        ensureConfigured();
        if (img1 == null || img2 == null) {
            throw new IOException("compareFaces requires two images");
        }

        String base641 = imageToBase64(img1);
        String base642 = imageToBase64(img2);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("api_key", apiKey);
        form.put("api_secret", apiSecret);
        form.put("image_base64_1", base641);
        form.put("image_base64_2", base642);

        String body = formUrlEncode(form);

        HttpRequest request = HttpRequest.newBuilder(COMPARE_URI)
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Face++ compare interrupted", e);
        } catch (Exception e) {
            throw new IOException("Face++ compare network error: " + e.getMessage(), e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new IOException("Face++ compare failed: HTTP " + response.statusCode() + " - " + safeBody(response.body()));
        }

        try {
            JSONObject json = new JSONObject(response.body());
            if (json.has("confidence")) {
                return json.getDouble("confidence");
            }
            throw new IOException("Face++ compare response missing 'confidence': " + safeBody(response.body()));
        } catch (JSONException e) {
            throw new IOException("Invalid JSON from Face++ compare", e);
        }
    }

    /**
     * JavaFX friendly API: returns a Task that runs detectFace off the UI thread.
     */
    public Task<FaceDetectionResult> detectFaceTask(Image fxImage) {
        return new Task<>() {
            @Override
            protected FaceDetectionResult call() throws Exception {
                return detectFace(fxImage);
            }
        };
    }

    /**
     * JavaFX friendly API: returns a Task that runs compareFaces off the UI thread.
     */
    public Task<Double> compareFacesTask(Image img1, Image img2) {
        return new Task<>() {
            @Override
            protected Double call() throws Exception {
                return compareFaces(img1, img2);
            }
        };
    }

    /**
     * Compare a stored face token with a captured image for login verification.
     *
     * @param faceToken the stored face token from user profile
     * @param capturedImage the captured image from webcam
     * @return confidence score (0-100), higher means more similar
     * @throws IOException if comparison fails
     */
    public double compareWithFaceToken(String faceToken, Image capturedImage) throws IOException {
        ensureConfigured();
        if (faceToken == null || faceToken.isBlank() || capturedImage == null) {
            throw new IOException("compareWithFaceToken requires face token and image");
        }

        String base64 = imageToBase64(capturedImage);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("api_key", apiKey);
        form.put("api_secret", apiSecret);
        form.put("face_token1", faceToken);
        form.put("image_base64_2", base64);

        String body = formUrlEncode(form);

        HttpRequest request = HttpRequest.newBuilder(COMPARE_URI)
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Face++ compare interrupted", e);
        } catch (Exception e) {
            throw new IOException("Face++ compare network error: " + e.getMessage(), e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new IOException("Face++ compare failed: HTTP " + response.statusCode() + " - " + safeBody(response.body()));
        }

        try {
            JSONObject json = new JSONObject(response.body());
            if (json.has("confidence")) {
                return json.getDouble("confidence");
            }
            throw new IOException("Face++ compare response missing 'confidence': " + safeBody(response.body()));
        } catch (JSONException e) {
            throw new IOException("Invalid JSON from Face++ compare", e);
        }
    }

    /**
     * JavaFX friendly API: returns a Task that runs compareWithFaceToken off the UI thread.
     */
    public Task<Double> compareWithFaceTokenTask(String faceToken, Image capturedImage) {
        return new Task<>() {
            @Override
            protected Double call() throws Exception {
                return compareWithFaceToken(faceToken, capturedImage);
            }
        };
    }

    /**
     * Convert JavaFX Image to Base64 (JPEG).
     */
    private String imageToBase64(Image fxImage) throws IOException {
        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();

        // Read pixels from JavaFX Image
        PixelReader pixelReader = fxImage.getPixelReader();
        if (pixelReader == null) {
            throw new IOException("Cannot read pixels from image");
        }

        WritableImage writableImage = new WritableImage(pixelReader, width, height);

        // Convert to BufferedImage
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = writableImage.getPixelReader().getArgb(x, y);
                // Convert ARGB to RGB
                int rgb = ((argb >> 16) & 0xFF) << 16
                        | ((argb >> 8) & 0xFF) << 8
                        | (argb & 0xFF);
                bufferedImage.setRGB(x, y, rgb);
            }
        }

        // Encode to JPEG Base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean ok = ImageIO.write(bufferedImage, "jpg", baos);
            if (!ok) {
                // Fallback to PNG if JPEG writer isn't available
                baos.reset();
                if (!ImageIO.write(bufferedImage, "png", baos)) {
                    throw new IOException("No ImageIO writer for jpg/png");
                }
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    private static String formUrlEncode(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (e.getValue() == null) continue;
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private void ensureConfigured() throws IOException {
        if (!isConfigured()) {
            throw new IOException("Face++ not configured. Add FACEPP_API_KEY and FACEPP_API_SECRET to .env");
        }
    }

    private static String safeBody(String body) {
        if (body == null) return "";
        String trimmed = body.trim();
        if (trimmed.length() > 600) return trimmed.substring(0, 600) + "...";
        return trimmed;
    }
}

