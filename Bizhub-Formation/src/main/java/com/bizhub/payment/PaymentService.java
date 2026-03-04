package com.bizhub.payment;

public class PaymentService {

    private final PaymentProvider provider;

    public PaymentService() {
        // Pour l'instant, toujours le fournisseur simulé.
        this.provider = new MockPaymentProvider();
    }

    public PaymentProvider getProvider() {
        return provider;
    }
}

