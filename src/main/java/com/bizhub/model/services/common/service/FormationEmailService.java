package com.bizhub.model.services.common.service;

import com.bizhub.model.elearning.formation.Formation;
import com.bizhub.model.elearning.participation.Participation;
import com.bizhub.model.users_avis.user.User;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Envoi d'emails (validation participation, invitation meet).
 * Configuration SMTP (ordre de priorité) :
 * 1. Variables d'environnement (SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM)
 * 2. Propriétés système (-DSMTP_HOST=... au lancement)
 * 3. Fichier application.properties dans les ressources (SMTP_*)
 * Si non configuré, sendParticipationConfirmation ne fait rien et retourne false.
 *
 * Named FormationEmailService to avoid clash with the investissement EmailService.
 */
public class FormationEmailService {

    private static Properties fileProps;

    static {
        fileProps = new Properties();
        try (InputStream is = FormationEmailService.class.getResourceAsStream("/application.properties")) {
            if (is != null) {
                fileProps.load(is);
            }
        } catch (Exception ignored) {
        }
        if (fileProps.getProperty("SMTP_HOST") == null || fileProps.getProperty("SMTP_HOST").isBlank()) {
            try {
                Path external = Paths.get(System.getProperty("user.dir", ".")).resolve("application.properties");
                if (Files.isRegularFile(external)) {
                    try (InputStream is = Files.newInputStream(external)) {
                        fileProps.load(is);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static String getConfig(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v.trim();
        v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v.trim();
        if (fileProps != null) {
            v = fileProps.getProperty(key);
            if (v != null && !v.isBlank()) return v.trim();
        }
        return defaultValue;
    }

    /**
     * Envoie un email de validation de participation à l'utilisateur.
     * @return true si l'email a été envoyé, false si SMTP non configuré ou erreur
     */
    public boolean sendParticipationConfirmation(User user, Formation formation, Participation participation) {
        String host = getConfig("SMTP_HOST", null);
        String port = getConfig("SMTP_PORT", "587");
        String userMail = getConfig("SMTP_USER", null);
        String pass = getConfig("SMTP_PASS", null);
        String from = getConfig("SMTP_FROM", userMail);

        if (host == null || userMail == null || pass == null || user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            javax.mail.Session session = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(userMail, pass);
                }
            });

            javax.mail.internet.MimeMessage msg = new javax.mail.internet.MimeMessage(session);
            msg.setFrom(new javax.mail.internet.InternetAddress(from));
            msg.setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(user.getEmail().trim()));
            msg.setSubject("BizHub - Participation confirmée - Attestation en pièce jointe");

            String formationTitle = formation != null && formation.getTitle() != null ? formation.getTitle() : ("Formation #" + (participation != null ? participation.getFormationId() : ""));
            String dateStr = participation != null && participation.getPaidAt() != null
                    ? participation.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
                    : java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
            String prenom = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName().trim() : "Participant";

            String body = "Bonjour " + prenom + ",\n\n"
                    + "Votre participation à la formation \"" + formationTitle + "\" a été confirmée.\n\n"
                    + "Vous trouverez en pièce jointe votre attestation de participation (PDF) avec l'ensemble des informations d'inscription.\n\n"
                    + "Date de validation : " + dateStr + "\n\n"
                    + "Cordialement,\nL'équipe BizHub";

            javax.mail.internet.MimeMultipart multipart = new javax.mail.internet.MimeMultipart();

            javax.mail.internet.MimeBodyPart textPart = new javax.mail.internet.MimeBodyPart();
            textPart.setText(body, "UTF-8");
            multipart.addBodyPart(textPart);

            byte[] pdfBytes = new ParticipationPdfGenerator().generate(user, formation, participation);
            if (pdfBytes != null && pdfBytes.length > 0) {
                javax.mail.internet.MimeBodyPart pdfPart = new javax.mail.internet.MimeBodyPart();
                pdfPart.setDataHandler(new javax.activation.DataHandler(new javax.mail.util.ByteArrayDataSource(pdfBytes, "application/pdf")));
                String safeBase = formationTitle.replaceAll("[^a-zA-Z0-9\\s-]", "").replaceAll("\\s+", "-");
                if (safeBase.length() > 40) safeBase = safeBase.substring(0, 40);
                String safeName = "Attestation-participation-" + (safeBase.isEmpty() ? "BizHub" : safeBase) + ".pdf";
                pdfPart.setFileName(safeName);
                multipart.addBodyPart(pdfPart);
            }

            msg.setContent(multipart);
            javax.mail.Transport.send(msg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Envoie une invitation meet (lien Google Meet) à un participant.
     * @return true si l'email a été envoyé, false si SMTP non configuré ou erreur
     */
    public boolean sendMeetInvitation(String toEmail, String participantName, String formationTitle,
                                      LocalDate meetDate, LocalTime meetTime, String meetLink) {
        String host = getConfig("SMTP_HOST", null);
        String port = getConfig("SMTP_PORT", "587");
        String userMail = getConfig("SMTP_USER", null);
        String pass = getConfig("SMTP_PASS", null);
        String from = getConfig("SMTP_FROM", userMail);

        if (host == null || userMail == null || pass == null || toEmail == null || toEmail.isBlank()) {
            return false;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            javax.mail.Session session = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(userMail, pass);
                }
            });

            javax.mail.internet.MimeMessage msg = new javax.mail.internet.MimeMessage(session);
            msg.setFrom(new javax.mail.internet.InternetAddress(from));
            msg.setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(toEmail.trim()));
            msg.setSubject("BizHub - Invitation Meet : " + (formationTitle != null ? formationTitle : "Formation"));

            String prenom = (participantName != null && !participantName.isBlank()) ? participantName.trim() : "Participant";
            String dateStr = meetDate != null ? meetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            String timeStr = meetTime != null ? meetTime.format(DateTimeFormatter.ofPattern("HH:mm")) : "";
            String link = (meetLink != null && !meetLink.isBlank()) ? meetLink.trim() : "(lien non fourni)";

            String body = "Bonjour " + prenom + ",\n\n"
                    + "Vous êtes inscrit(e) à la formation \"" + (formationTitle != null ? formationTitle : "") + "\".\n\n"
                    + "Invitation à la session en ligne :\n"
                    + "  • Date : " + dateStr + "\n"
                    + "  • Heure : " + timeStr + "\n"
                    + "  • Lien Google Meet : " + link + "\n\n"
                    + "Cordialement,\nL'équipe BizHub";

            msg.setText(body, "UTF-8");
            javax.mail.Transport.send(msg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

