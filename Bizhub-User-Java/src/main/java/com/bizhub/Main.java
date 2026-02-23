package com.bizhub;

import com.bizhub.model.services.marketplace.payment.StripeGatewayClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage stage) throws Exception {

        // ── NOUVEAU : vérifier la config Stripe au démarrage ──
        // Lance une exception claire si stripe.properties est mal configuré
        // AVANT que l'utilisateur essaie de payer.
        initStripe();

        // ── Votre code existant (inchangé) ────────────────
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

    /**
     * Vérifie que stripe.properties est présent et que la clé est configurée.
     * Ne fait PAS d'appel réseau — juste une lecture du fichier.
     * En cas d'erreur : log un warning mais ne bloque pas le démarrage de l'app.
     */
    private void initStripe() {
        try {
            // Instancier StripeGatewayClient pour valider stripe.properties
            new StripeGatewayClient();
            LOGGER.info("✅ Stripe configuré — paiement disponible.");
        } catch (Exception e) {
            // Warning seulement : l'app démarre quand même,
            // l'erreur sera visible à l'utilisateur quand il cliquera "Payer"
            LOGGER.log(Level.WARNING,
                    "⚠ Stripe non configuré : " + e.getMessage()
                            + " — Créez src/main/resources/stripe.properties", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
