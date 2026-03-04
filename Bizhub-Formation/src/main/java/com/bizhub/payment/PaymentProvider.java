package com.bizhub.payment;

import com.bizhub.formation.model.Formation;
import com.bizhub.participation.model.Participation;
import com.bizhub.user.model.User;

public interface PaymentProvider {

    record CheckoutResult(String reference, String redirectUrl) {
    }

    CheckoutResult createCheckout(Participation participation, Formation formation, User user) throws Exception;

    boolean verifyPayment(String reference) throws Exception;
}

