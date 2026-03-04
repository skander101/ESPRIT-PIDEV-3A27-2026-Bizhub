package com.bizhub.common.service;

import com.bizhub.user.model.User;

/** Simple global session holder for this mini-project. */
public final class AppSession {

    private static User currentUser;

    private AppSession() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User currentUser) {
        AppSession.currentUser = currentUser;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return isAuthenticated() && "admin".equalsIgnoreCase(currentUser.getUserType());
    }

    /** Utilisateur formateur (peut créer/éditer des formations). */
    public static boolean isFormateur() {
        return isAuthenticated() && "formateur".equalsIgnoreCase(currentUser.getUserType());
    }

    /** Admin ou formateur : peut gérer les formations (ajout, édition). */
    public static boolean canManageFormations() {
        return isAdmin() || isFormateur();
    }
}

