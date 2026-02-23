package com.bizhub.model.services.marketplace.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StripeGatewayClient — appelle l'API Stripe directement (pas de serveur local).
 *
 * Lit la config depuis src/main/resources/stripe.properties
 * Instancié par PaymentService.
 */
public class StripeGatewayClient {

    private static final Logger LOGGER = Logger.getLogger(StripeGatewayClient.class.getName());

    private final String secretKey;
    private final String currency;
    private final String successUrl;
    private final String cancelUrl;

    public StripeGatewayClient() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("stripe.properties")) {
            if (in == null)
                throw new IllegalStateException(
                        "stripe.properties introuvable dans src/main/resources/");
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de lire stripe.properties : " + e.getMessage(), e);
        }

        this.secretKey  = props.getProperty("stripe.secret.key", "").trim();
        this.currency   = props.getProperty("stripe.currency", "eur").trim();
        this.successUrl = props.getProperty("stripe.success.url",
                "https://bizhub.app/succes").trim();
        this.cancelUrl  = props.getProperty("stripe.cancel.url",
                "https://bizhub.app/annule").trim();

        if (secretKey.isEmpty())
            throw new IllegalStateException(
                    "stripe.secret.key vide dans stripe.properties");
    }

    /**
     * Crée une Stripe Checkout Session.
     *
     * @param orderId     ID de la commande
     * @param productName Nom du produit (affiché sur la page Stripe)
     * @param quantity    Quantité commandée
     * @return PaymentResult avec l'URL Checkout ou le message d'erreur
     */
    public PaymentResult createStripeCheckout(int orderId,
                                              String productName,
                                              int quantity) {
        try {
            Stripe.apiKey = secretKey;

            // 10.00 EUR par unité en test
            // TODO : passer le vrai prix depuis CommandeJoinProduit.getPrix()
            long unitCentimes = 1000L;

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity((long) quantity)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency)
                                                    .setUnitAmount(unitCentimes)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData
                                                                    .ProductData.builder()
                                                                    .setName(safe(productName).isEmpty()
                                                                            ? "Commande #" + orderId
                                                                            : safe(productName))
                                                                    .setDescription(
                                                                            "BizHub — Commande #" + orderId)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            LOGGER.info("Stripe session créée : " + session.getId());
            return PaymentResult.ok(session.getId(), session.getUrl());

        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Erreur Stripe API", e);
            return PaymentResult.fail("Stripe : " + e.getUserMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur StripeGatewayClient", e);
            return PaymentResult.fail("Erreur : " + e.getMessage());
        }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
