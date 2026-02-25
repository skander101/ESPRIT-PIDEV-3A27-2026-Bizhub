package com.bizhub.controller.marketplace;

import com.bizhub.model.services.marketplace.CommandeService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StripeWebhookServer
 *
 * Remplace StripeWebhookController (qui nécessitait Spring Boot absent du pom.xml).
 * Utilise com.sun.net.httpserver intégré au JDK — aucune dépendance supplémentaire.
 *
 * ── Démarrage ───────────────────────────────────────────────────────────────
 * Dans Main.java (Application JavaFX), méthode init() :
 *
 *   Properties props = new Properties();
 *   props.load(getClass().getClassLoader().getResourceAsStream("stripe.properties"));
 *   String secret = props.getProperty("stripe.webhook.secret", "").trim();
 *   StripeWebhookServer.start(secret);
 *
 * Dans Main.java, méthode stop() :
 *   StripeWebhookServer.stop();
 *
 * ── Test en local ────────────────────────────────────────────────────────────
 *   stripe listen --forward-to localhost:8081/webhook/stripe
 *   (la commande affiche le whsec_ à mettre dans stripe.properties)
 *
 * ── Configuration Dashboard Stripe (prod) ───────────────────────────────────
 *   Developers > Webhooks > Add endpoint
 *   URL : https://TON_DOMAINE/webhook/stripe
 *   Événement : checkout.session.completed
 */
public class StripeWebhookServer {

    private static final Logger LOGGER = Logger.getLogger(StripeWebhookServer.class.getName());
    private static final int    PORT   = 8081;
    private static final String PATH   = "/webhook/stripe";

    private static HttpServer server;

    // ─────────────────────────────────────────────────────────────────────────
    // CYCLE DE VIE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lance le serveur webhook dans un thread daemon.
     * @param webhookSecret  Secret Stripe (whsec_xxx)
     */
    public static void start(String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            LOGGER.warning("⚠ StripeWebhookServer : webhookSecret vide — serveur non démarré.");
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.createContext(PATH, exchange -> handleRequest(exchange, webhookSecret));

            Thread t = new Thread(server::start, "stripe-webhook-server");
            t.setDaemon(true);
            t.start();

            LOGGER.info("✅ StripeWebhookServer démarré : http://localhost:" + PORT + PATH);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossible de démarrer StripeWebhookServer", e);
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("StripeWebhookServer arrêté.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLER HTTP
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleRequest(HttpExchange exchange, String webhookSecret) throws IOException {

        // Stripe envoie uniquement des POST
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed");
            return;
        }

        // Lire le body brut (Stripe vérifie la signature sur le body exact)
        String payload;
        try (InputStream is = exchange.getRequestBody()) {
            payload = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Header Stripe-Signature obligatoire
        String sigHeader = exchange.getRequestHeaders().getFirst("Stripe-Signature");
        if (sigHeader == null || sigHeader.isBlank()) {
            LOGGER.warning("Webhook reçu sans Stripe-Signature header");
            send(exchange, 400, "Missing signature");
            return;
        }

        // Vérification signature Stripe (HMAC SHA-256)
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Signature webhook invalide : " + e.getMessage());
            send(exchange, 400, "Invalid signature");
            return;
        }

        // Traitement de l'événement — toujours répondre 200 rapidement à Stripe
        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "checkout.session.expired":
                    LOGGER.info("Session Stripe expirée : " + event.getId());
                    break;
                default:
                    LOGGER.fine("Événement Stripe non géré : " + event.getType());
            }
        } catch (Exception e) {
            // On logue mais on répond 200 pour éviter que Stripe ne réessaie indéfiniment
            LOGGER.log(Level.SEVERE, "Erreur traitement événement webhook", e);
        }

        send(exchange, 200, "OK");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRAITEMENT checkout.session.completed
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleCheckoutSessionCompleted(Event event) {

        // Désérialisation de l'objet Session
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;

        if (deserializer.getObject().isPresent()) {
            stripeObject = deserializer.getObject().get();
        } else {
            stripeObject = event.getData().getObject();
        }

        if (!(stripeObject instanceof Session)) {
            LOGGER.warning("Objet inattendu dans checkout.session.completed");
            return;
        }

        Session session = (Session) stripeObject;
        String sessionId     = session.getId();
        String paymentStatus = session.getPaymentStatus(); // "paid", "unpaid", "no_payment_required"

        if (!"paid".equals(paymentStatus)) {
            LOGGER.warning("Paiement non réussi pour session " + sessionId + " : " + paymentStatus);
            return;
        }

        // ✅ Récupérer orderId depuis metadata
        //    Mis dans metadata par StripeGatewayClient.createStripeCheckout() via .putMetadata("orderId", ...)
        var metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("orderId")) {
            LOGGER.severe("❌ orderId absent de la metadata de la session " + sessionId
                    + " — vérifier StripeGatewayClient.createStripeCheckout() → .putMetadata(\"orderId\", ...)");
            return;
        }

        String orderIdStr = metadata.get("orderId");

        try {
            int orderId = Integer.parseInt(orderIdStr);

            // ✅ Marquer commande comme payée en DB
            //    → est_payee = 1, paid_at = NOW(), payment_status = 'paid'
            CommandeService commandeService = new CommandeService();
            int rows = commandeService.markAsPaid(orderId, sessionId);

            if (rows > 0) {
                LOGGER.info("✅ Commande #" + orderId + " marquée payée (session: " + sessionId + ")");
            } else {
                LOGGER.warning("⚠ Aucune ligne mise à jour pour commande #" + orderId
                        + " (déjà payée ou ID introuvable)");
            }

        } catch (NumberFormatException e) {
            LOGGER.severe("orderId invalide dans metadata : '" + orderIdStr + "'");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur SQL lors du markAsPaid", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private static void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
