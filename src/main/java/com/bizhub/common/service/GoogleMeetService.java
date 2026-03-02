package com.bizhub.common.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

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
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Crée un événement Google Calendar avec un lien Google Meet via l'API Calendar.
 * Configuration : GOOGLE_APPLICATION_CREDENTIALS (chemin vers le JSON du compte de service)
 * ou GOOGLE_MEET_CREDENTIALS_PATH dans application.properties.
 * GOOGLE_CALENDAR_ID : ID du calendrier (partagé avec le compte de service), ex. email@domain.com.
 */
public class GoogleMeetService {

    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";
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

    private static Optional<GoogleCredentials> loadCredentials() {
        String path = getConfig("GOOGLE_APPLICATION_CREDENTIALS", null);
        if (path == null) path = getConfig("GOOGLE_MEET_CREDENTIALS_PATH", null);
        if (path == null || path.isBlank()) return Optional.empty();
        Path file = Paths.get(path);
        if (!Files.isRegularFile(file)) return Optional.empty();
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            GoogleCredentials cred = GoogleCredentials.fromStream(fis)
                    .createScoped(Collections.singletonList(CALENDAR_SCOPE));
            return Optional.of(cred);
        } catch (Exception e) {
            return Optional.empty();
        }
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
        Optional<GoogleCredentials> credOpt = loadCredentials();
        if (credOpt.isEmpty()) return Optional.empty();

        String calendarId = getConfig("GOOGLE_CALENDAR_ID", "primary");
        if (calendarId == null || calendarId.isBlank()) calendarId = "primary";

        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            Calendar calendar = new Calendar.Builder(transport, jsonFactory, new HttpCredentialsAdapter(credOpt.get()))
                    .setApplicationName("BizHub")
                    .build();

            ZoneId zone = ZoneId.of(DEFAULT_TIMEZONE);
            LocalDateTime startLdt = LocalDateTime.of(date, time);
            LocalDateTime endLdt = startLdt.plusMinutes(durationMinutes);
            String startRfc = startLdt.atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String endRfc = endLdt.atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            com.google.api.client.util.DateTime startDt = com.google.api.client.util.DateTime.parseRfc3339(startRfc);
            com.google.api.client.util.DateTime endDt = com.google.api.client.util.DateTime.parseRfc3339(endRfc);

            Event event = new Event();
            event.setSummary(title != null && !title.isBlank() ? title : "Meet BizHub");
            event.setStart(new EventDateTime().setDateTime(startDt).setTimeZone(DEFAULT_TIMEZONE));
            event.setEnd(new EventDateTime().setDateTime(endDt).setTimeZone(DEFAULT_TIMEZONE));

            ConferenceSolutionKey key = new ConferenceSolutionKey();
            key.setType("hangoutsMeet");
            CreateConferenceRequest createReq = new CreateConferenceRequest();
            createReq.setRequestId(UUID.randomUUID().toString());
            createReq.setConferenceSolutionKey(key);
            ConferenceData confData = new ConferenceData();
            confData.setCreateRequest(createReq);
            event.setConferenceData(confData);

            Event created = calendar.events().insert(calendarId, event).setConferenceDataVersion(1).execute();
            String link = created.getHangoutLink();
            return link != null && !link.isBlank() ? Optional.of(link) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Retourne true si les identifiants Google sont configurés. */
    public boolean isConfigured() {
        return loadCredentials().isPresent();
    }
}
