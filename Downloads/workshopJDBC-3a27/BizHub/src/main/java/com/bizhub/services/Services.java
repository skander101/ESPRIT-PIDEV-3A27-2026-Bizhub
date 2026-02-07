package com.bizhub.services;

import com.bizhub.dao.FormationDAO;
import com.bizhub.dao.ReviewDAO;
import com.bizhub.dao.UserDAO;
import utils.MyDatabase;

import java.sql.Connection;

/**
 * Minimal service locator for this mini-project.
 * Keeps things simple (no DI framework) while allowing controllers to share DAOs/services.
 */
public final class Services {

    private static Connection cnx;
    private static UserService userService;
    private static ReviewDAO reviewDAO;
    private static FormationDAO formationDAO;
    private static AuthService authService;
    private static ValidationService validationService;
    private static ReportService reportService;

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

    public static synchronized ReviewDAO reviews() {
        if (reviewDAO == null) {
            reviewDAO = new ReviewDAO(cnx());
        }
        return reviewDAO;
    }

    public static synchronized FormationDAO formations() {
        if (formationDAO == null) {
            formationDAO = new FormationDAO(cnx());
        }
        return formationDAO;
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
}
