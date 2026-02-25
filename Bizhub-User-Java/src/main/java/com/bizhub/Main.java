package com.bizhub;

import com.bizhub.controller.marketplace.StripeWebhookServer;
import com.bizhub.model.services.marketplace.payment.StripeGatewayClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // Garder en champ pour log/debug si besoin
    private String webhookSecret = "";

    @Override
    public void init() throws Exception {
        super.init();

        // 1) Vérifier Stripe config (clé API etc.) -> warning seulement
        initStripe();

        // 2) Lire stripe.webhook.secret depuis stripe.properties
        webhookSecret = readWebhookSecretFromProperties();

        // 3) Démarrer le serveur webhook Stripe (port 8081, thread daemon)
        //    Si secret vide, ton serveur peut quand même démarrer (selon ton impl),
        //    mais il rejettera les events signés -> log utile
        try {
            StripeWebhookServer.start(webhookSecret);
            LOGGER.info("✅ StripeWebhookServer démarré sur : http://localhost:8081/webhook/stripe");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠ Impossible de démarrer StripeWebhookServer : " + e.getMessage(), e);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {

        // ── Code UI existant (inchangé) ────────────────────────────────
        URL fxml = Main.class.getResource("/com/bizhub/fxml/login.fxml");
        if (fxml == null)
            throw new IllegalStateException("Cannot find /com/bizhub/fxml/login.fxml on classpath");

        Parent initial = FXMLLoader.load(fxml);

        StackPane appShell = new StackPane();
        appShell.setId("appShell");
        appShell.getChildren().add(initial);

        URL overlayFxml = Main.class.getResource("/com/bizhub/fxml/loading-overlay.fxml");
        if (overlayFxml == null)
            throw new IllegalStateException("Cannot find /com/bizhub/fxml/loading-overlay.fxml on classpath");

        Parent overlay = FXMLLoader.load(overlayFxml);
        overlay.setId("navOverlay");
        appShell.getChildren().add(overlay);

        var bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(appShell,
                Math.max(980, bounds.getWidth()),
                Math.max(640, bounds.getHeight()));

        URL themeCss = Main.class.getResource("/com/bizhub/css/theme.css");
        if (themeCss != null)
            scene.getStylesheets().add(themeCss.toExternalForm());

        stage.setTitle("BizHub - Users & Reviews Administration");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        try {
            StripeWebhookServer.stop();
            LOGGER.info("✅ StripeWebhookServer stoppé.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "⚠ Erreur stop StripeWebhookServer : " + e.getMessage(), e);
        }
        super.stop();
    }

    /**
     * Vérifie que stripe.properties est présent et que la clé est configurée.
     * Ne fait PAS d'appel réseau — juste une lecture/validation de config.
     * En cas d'erreur : warning seulement (l'app démarre quand même).
     */
    private void initStripe() {
        try {
            new StripeGatewayClient(); // valide stripe.properties / stripe.secret.key etc.
            LOGGER.info("✅ Stripe configuré — paiement disponible.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "⚠ Stripe non configuré : " + e.getMessage()
                            + " — Créez src/main/resources/stripe.properties", e);
        }
    }

    /**
     * Lit stripe.webhook.secret depuis stripe.properties dans resources.
     */
    private String readWebhookSecretFromProperties() {
        String secret = "";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("stripe.properties")) {
            if (in == null) {
                LOGGER.warning("⚠ stripe.properties introuvable (resources). Webhook secret = vide.");
                return "";
            }
            Properties props = new Properties();
            props.load(in);
            secret = props.getProperty("stripe.webhook.secret", "").trim();

            if (secret.isEmpty()) {
                LOGGER.warning("⚠ stripe.webhook.secret est vide dans stripe.properties (whsec_...).");
            } else {
                LOGGER.info("✅ stripe.webhook.secret chargé (whsec_...).");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "⚠ Erreur lecture stripe.properties : " + e.getMessage(), e);
        }
        return secret;
    }

    public static void main(String[] args) {
        launch(args);
    }
}