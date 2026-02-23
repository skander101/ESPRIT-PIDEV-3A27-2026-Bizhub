package com.bizhub.model.services.marketplace.payment;

import com.bizhub.model.marketplace.CommandeJoinProduit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class StripeGatewayClient implements PaymentProvider {

    // URL de TON backend (local au début)
    private final String gatewayBaseUrl;
    private final HttpClient http = HttpClient.newHttpClient();

    public StripeGatewayClient(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    @Override
    public PaymentResult createCheckout(CommandeJoinProduit c) throws Exception {
        // JSON simple (sans libs)
        String body = "{"
                + "\"orderId\":" + c.getIdCommande() + ","
                + "\"product\":\"" + safe(c.getProduitNom()) + "\","
                + "\"quantity\":" + c.getQuantiteCommande()
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(gatewayBaseUrl + "/api/payments/stripe/checkout"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("Gateway error: " + resp.statusCode() + " | " + resp.body());
        }

        // Réponse attendue: {"ref":"cs_test_...","url":"https://checkout.stripe.com/...","status":"created"}
        String json = resp.body();
        String ref = pick(json, "ref");
        String url = pick(json, "url");
        String status = pick(json, "status");

        return new PaymentResult(ref, url, status);
    }

    private static String safe(String s) { return s == null ? "" : s.replace("\"", "'"); }

    // petit parseur JSON naïf (ok pour demo)
    private static String pick(String json, String key) {
        String k = "\"" + key + "\":";
        int i = json.indexOf(k);
        if (i < 0) return "";
        int start = json.indexOf('"', i + k.length()) + 1;
        int end = json.indexOf('"', start);
        return (start > 0 && end > start) ? json.substring(start, end) : "";
    }
}