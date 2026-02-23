package com.bizhub.model.services.marketplace.payment;
import java.awt.Desktop;
import java.net.URI;
import com.bizhub.model.marketplace.CommandeJoinProduit;

public class PaymentService {

    private final StripeGatewayClient stripe = new StripeGatewayClient();

    // ✅ utilisé dans AUTO-CONFIRM (ton controller investisseur)
    public PaymentResult initiateStripeCheckout(CommandeJoinProduit c) {
        if (c == null) return PaymentResult.fail("Commande null");
        return stripe.createStripeCheckout(
                c.getIdCommande(),
                c.getProduitNom(),
                c.getQuantiteCommande()
        );
    }

    // ✅ utilisé dans onConfirmerCommande() (ouvre navigateur)
    public PaymentResult creerEtOuvrirLienPaiement(CommandeJoinProduit c) {
        PaymentResult res = initiateStripeCheckout(c);
        if (!res.isSuccess()) return res;

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(res.getPaymentUrl()));
            }
            return res;
        } catch (Exception e) {
            return PaymentResult.fail("Lien Stripe créé mais impossible d'ouvrir le navigateur : " + e.getMessage());
        }
    }

    public void openInBrowser(String url) {
        try {
            if (url == null || url.isBlank()) return;

            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("Desktop non supporté sur cette machine.");
            }
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'ouvrir le navigateur : " + e.getMessage(), e);
        }
    }
}
