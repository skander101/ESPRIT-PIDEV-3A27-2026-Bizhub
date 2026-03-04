package com.bizhub.model.services.elearning.payment;

import com.bizhub.model.elearning.formation.Formation;
import com.bizhub.model.elearning.participation.Participation;
import com.bizhub.model.users_avis.user.User;

public interface PaymentProvider {

    record CheckoutResult(String reference, String redirectUrl) {
    }

    CheckoutResult createCheckout(Participation participation, Formation formation, User user) throws Exception;

    boolean verifyPayment(String reference) throws Exception;
}

