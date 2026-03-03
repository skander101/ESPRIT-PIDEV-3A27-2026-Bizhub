package com.bizhub.model.services.marketplace;

import com.bizhub.model.services.common.config.EnvLoader;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TwilioSmsService {

    private static final Logger LOGGER = Logger.getLogger(TwilioSmsService.class.getName());

    // Indicatif par défaut si le numéro est local (sans préfixe pays)
    // ✅ Changer ici si vos clients ne sont pas tous tunisiens
    private static final String DEFAULT_COUNTRY_CODE = "+216";

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsService() {
        this.accountSid = EnvLoader.getRequired("TWILIO_ACCOUNT_SID");
        this.authToken  = EnvLoader.getRequired("TWILIO_AUTH_TOKEN");
        this.fromNumber = EnvLoader.getRequired("TWILIO_FROM_NUMBER");

        Twilio.init(accountSid, authToken);
        LOGGER.info("✅ TwilioSmsService initialisé (from=" + fromNumber + ")");
    }

    // =========================================================================
    // SEND SMS — point d'entrée principal
    // =========================================================================

    public boolean sendSms(String toNumber, String body) {
        // ── Validation body ───────────────────────────────────────────────────
        if (body == null || body.isBlank()) {
            LOGGER.warning("❌ SMS annulé : body vide.");
            return false;
        }

        // ── Normalisation numéro ──────────────────────────────────────────────
        String normalized = normalizePhone(toNumber);
        if (normalized == null) {
            LOGGER.warning("❌ SMS annulé : numéro invalide ou vide → \"" + toNumber + "\"");
            return false;
        }

        LOGGER.info("⏳ Envoi SMS → " + masked(normalized));

        // ── Envoi Twilio ──────────────────────────────────────────────────────
        try {
            Message msg = Message.creator(
                    new com.twilio.type.PhoneNumber(normalized),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    body
            ).create();

            String status = msg.getStatus() != null ? msg.getStatus().toString() : "unknown";
            LOGGER.info("✅ SMS envoyé (sid=" + msg.getSid() + ", status=" + status + ") → " + masked(normalized));
            return true;

        } catch (ApiException e) {
            // ── Erreurs Twilio connues — explication claire dans les logs ─────
            String hint = getTwilioErrorHint(e.getCode());
            LOGGER.log(Level.WARNING,
                    "❌ SMS Twilio échoué (code=" + e.getCode() + ") → " + masked(normalized)
                            + "\n   Raison : " + e.getMessage()
                            + "\n   💡 " + hint, e);
            return false;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "❌ SMS échec inattendu → " + masked(normalized) + " : " + e.getMessage(), e);
            return false;
        }
    }

    // =========================================================================
    // NORMALISATION E.164
    // Twilio exige le format international strict : +<indicatif><numéro>
    // Exemples valides   : +21612345678, +33612345678, +12025551234
    // Exemples invalides : 12345678, 0012345678, 06 12 34 56 78
    // =========================================================================

    /**
     * Normalise un numéro de téléphone vers le format E.164.
     * @return numéro normalisé (ex: "+21612345678") ou null si invalide
     */
    public static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;

        // Supprimer espaces, tirets, points, parenthèses
        String clean = raw.trim().replaceAll("[\\s\\-\\.\\(\\)/]", "");

        if (clean.isEmpty()) return null;

        // ── Déjà en E.164 : commence par + ───────────────────────────────────
        if (clean.startsWith("+")) {
            // Minimum : +XX + 7 chiffres = 10 chars
            return clean.length() >= 10 ? clean : null;
        }

        // ── Préfixe international 00 → + ─────────────────────────────────────
        // ex: 0021612345678 → +21612345678
        if (clean.startsWith("00")) {
            String withPlus = "+" + clean.substring(2);
            return withPlus.length() >= 10 ? withPlus : null;
        }

        // ── Numéro local tunisien : 8 chiffres → +216XXXXXXXX ────────────────
        // ex: 12345678 → +21612345678
        if (clean.matches("\\d{8}")) {
            return DEFAULT_COUNTRY_CODE + clean;
        }

        // ── Numéro local français : 0X XX XX XX XX → +33XXXXXXXXX ────────────
        // ex: 0612345678 → +33612345678
        if (clean.matches("0\\d{9}")) {
            return "+33" + clean.substring(1);
        }

        // ── Autres cas : numéro tout chiffres de longueur raisonnable ─────────
        if (clean.matches("\\d{7,15}")) {
            LOGGER.warning("⚠ Numéro sans indicatif détecté : \"" + clean
                    + "\" → ajout de " + DEFAULT_COUNTRY_CODE + " par défaut.");
            return DEFAULT_COUNTRY_CODE + clean;
        }

        // Format non reconnu
        LOGGER.warning("⚠ Format numéro non reconnu : \"" + raw + "\"");
        return null;
    }

    // =========================================================================
    // HINTS pour les codes d'erreur Twilio courants
    // =========================================================================

    private static String getTwilioErrorHint(int code) {
        return switch (code) {
            case 21608 ->
                    "Compte Trial : le numéro destinataire n'est pas vérifié. " +
                            "Allez sur console.twilio.com → Verified Caller IDs et ajoutez ce numéro.";
            case 21211 ->
                    "Numéro destinataire invalide. Vérifiez le format E.164 (+indicatif+numéro).";
            case 21614 ->
                    "Numéro non compatible SMS (ex: numéro fixe). Impossible d'envoyer.";
            case 20003 ->
                    "Authentification échouée. Vérifiez TWILIO_ACCOUNT_SID et TWILIO_AUTH_TOKEN dans .env.";
            case 21610 ->
                    "Ce numéro a bloqué vos messages (opt-out). Impossible d'envoyer.";
            case 30008 ->
                    "SMS non délivré (opérateur inconnu). Vérifiez le réseau du destinataire.";
            case 21212 ->
                    "Numéro expéditeur invalide. Vérifiez TWILIO_FROM_NUMBER dans .env.";
            default ->
                    "Consultez https://www.twilio.com/docs/api/errors/" + code + " pour les détails.";
        };
    }

    // =========================================================================
    // HELPER — masquer le numéro dans les logs (RGPD)
    // =========================================================================

    private static String masked(String phone) {
        if (phone == null || phone.length() < 5) return "***";
        return phone.substring(0, Math.min(5, phone.length())) + "****";
    }
}