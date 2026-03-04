package com.bizhub.model.services.elearning.payment;

import com.bizhub.model.elearning.formation.Formation;
import com.bizhub.model.elearning.participation.Participation;
import com.bizhub.model.users_avis.user.User;

import java.util.UUID;

/**
 * Fournisseur de paiement simulé.
 * Ne contacte aucun service externe : le paiement est toujours accepté.
 */
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public CheckoutResult createCheckout(Participation participation, Formation formation, User user) {
        String ref = "MOCK-" + UUID.randomUUID();
        return new CheckoutResult(ref, "https://example.com/mock-payment-success");
    }

    @Override
    public boolean verifyPayment(String reference) {
        return true;
    }
}

