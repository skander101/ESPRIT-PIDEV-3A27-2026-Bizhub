package com.bizhub.payment;

import com.bizhub.formation.model.Formation;
import com.bizhub.participation.model.Participation;
import com.bizhub.user.model.User;

import java.util.UUID;

/**
 * Fournisseur de paiement simulé.
 * Ne contacte aucun service externe : le paiement est toujours accepté.
 */
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public CheckoutResult createCheckout(Participation participation, Formation formation, User user) {
        String ref = "MOCK-" + UUID.randomUUID();
        // Pas de vraie URL de redirection; on simule uniquement.
        return new CheckoutResult(ref, "https://example.com/mock-payment-success");
    }

    @Override
    public boolean verifyPayment(String reference) {
        // Dans ce mode, tout paiement est considéré comme réussi.
        return true;
    }
}

