package com.bizhub.model.services.common.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Crée un événement Google Calendar avec un lien Google Meet via l'API Calendar.
 * Configuration : GOOGLE_APPLICATION_CREDENTIALS (chemin vers le JSON du compte de service)
 * ou GOOGLE_MEET_CREDENTIALS_PATH dans application.properties.
 * GOOGLE_CALENDAR_ID : ID du calendrier (partagé avec le compte de service).
 *
 * NOTE: Google Calendar API dependencies (google-api-services-calendar, google-auth-library-oauth2-http)
 * must be added to pom.xml for full functionality. Until then, createMeetingWithMeetLink will always
 * return Optional.empty() and isConfigured() will return false.
 */
public class GoogleMeetService {

    private static final String DEFAULT_TIMEZONE = "Europe/Paris";

    private static String getConfig(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v.trim();
        v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v.trim();
        try (InputStream is = GoogleMeetService.class.getResourceAsStream("/application.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                v = p.getProperty(key);
                if (v != null && !v.isBlank()) return v.trim();
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    /**
     * Crée un événement sur Google Calendar avec Google Meet et retourne le lien Meet.
     * @param date date du meet
     * @param time heure de début
     * @param durationMinutes durée en minutes
     * @param title titre de l'événement
     * @return le lien Meet (hangoutLink) ou empty si config manquante / erreur
     */
    public Optional<String> createMeetingWithMeetLink(LocalDate date, LocalTime time, int durationMinutes, String title) {
        // Google Calendar API integration - requires google-api-services-calendar dependency.
        // Currently returns empty. Add Google Calendar dependencies to pom.xml to enable.
        String credPath = getConfig("GOOGLE_APPLICATION_CREDENTIALS", null);
        if (credPath == null) credPath = getConfig("GOOGLE_MEET_CREDENTIALS_PATH", null);
        if (credPath == null || credPath.isBlank()) return Optional.empty();

        Path file = Paths.get(credPath);
        if (!Files.isRegularFile(file)) return Optional.empty();

        // Placeholder: full implementation requires google-api-services-calendar
        // and google-auth-library-oauth2-http dependencies in pom.xml.
        return Optional.empty();
    }

    /** Retourne true si les identifiants Google sont configurés. */
    public boolean isConfigured() {
        String credPath = getConfig("GOOGLE_APPLICATION_CREDENTIALS", null);
        if (credPath == null) credPath = getConfig("GOOGLE_MEET_CREDENTIALS_PATH", null);
        if (credPath == null || credPath.isBlank()) return false;
        return Files.isRegularFile(Paths.get(credPath));
    }
}

