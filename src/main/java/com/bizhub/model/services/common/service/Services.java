package com.bizhub.model.services.common.service;

import com.bizhub.model.services.common.DB.MyDatabase;
import com.bizhub.model.services.elearning.formation.FormationService;
import com.bizhub.model.services.elearning.participation.ParticipationService;
import com.bizhub.model.services.elearning.payment.PaymentService;
import com.bizhub.model.services.user_avis.review.ReviewService;
import com.bizhub.model.services.user_avis.user.UserService;

import java.sql.Connection;

/**
 * Minimal service locator for this mini-project.
 * Keeps things simple (no DI framework) while allowing controllers to share services.
 */
public final class Services {

    private static Connection cnx;
    private static UserService userService;
    private static ReviewService reviewService;
    private static FormationService formationService;
    private static AuthService authService;
    private static ValidationService validationService;
    private static ReportService reportService;
    private static Auth0Service auth0Service;
    private static InfobipService infobipService;
    private static TotpService totpService;
    private static CloudflareAiService cloudflareAiService;
    private static ParticipationService participationService;
    private static PaymentService paymentService;
    private static FormationEmailService formationEmailService;
    private static GoogleMeetService googleMeetService;

    private Services() {
    }

    public static synchronized Connection cnx() {
        if (cnx == null) {
            cnx = MyDatabase.getInstance().getCnx();
        }
        return cnx;
    }

    public static synchronized UserService users() {
        if (userService == null) {
            userService = new UserService();
        }
        return userService;
    }

    public static synchronized ReviewService reviews() {
        if (reviewService == null) {
            reviewService = new ReviewService();
        }
        return reviewService;
    }

    public static synchronized FormationService formations() {
        if (formationService == null) {
            formationService = new FormationService();
        }
        return formationService;
    }

    public static synchronized AuthService auth() {
        if (authService == null) {
            authService = new AuthService(users(), new AuthService.Session());
        }
        return authService;
    }

    public static synchronized ValidationService validation() {
        if (validationService == null) {
            validationService = new ValidationService();
        }
        return validationService;
    }

    public static synchronized ReportService reports() {
        if (reportService == null) {
            reportService = new ReportService(users(), reviews());
        }
        return reportService;
    }

    public static synchronized Auth0Service auth0() {
        if (auth0Service == null) {
            auth0Service = new Auth0Service();
        }
        return auth0Service;
    }

    public static synchronized InfobipService infobip() {
        if (infobipService == null) {
            infobipService = new InfobipService();
        }
        return infobipService;
    }

    public static synchronized TotpService totp() {
        if (totpService == null) {
            totpService = new TotpService();
        }
        return totpService;
    }

    public static synchronized CloudflareAiService cloudflareAi() {
        if (cloudflareAiService == null) {
            cloudflareAiService = new CloudflareAiService();
        }
        return cloudflareAiService;
    }

    public static synchronized ParticipationService participations() {
        if (participationService == null) {
            participationService = new ParticipationService();
        }
        return participationService;
    }

    public static synchronized PaymentService payments() {
        if (paymentService == null) {
            paymentService = new PaymentService();
        }
        return paymentService;
    }

    public static synchronized FormationEmailService email() {
        if (formationEmailService == null) {
            formationEmailService = new FormationEmailService();
        }
        return formationEmailService;
    }

    public static synchronized GoogleMeetService googleMeet() {
        if (googleMeetService == null) {
            googleMeetService = new GoogleMeetService();
        }
        return googleMeetService;
    }
}
