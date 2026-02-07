package com.bizhub.services;

import javafx.animation.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class NavigationService {

    private static final Duration TRANSITION_DURATION = Duration.millis(520);

    private final Stage stage;

    public NavigationService(Stage stage) {
        this.stage = stage;
    }

    public void goToLogin() {
        loadIntoStage("/com/bizhub/fxml/login.fxml", 980, 640);
    }

    public void goToAdminDashboard() {
        loadIntoStage("/com/bizhub/fxml/admin-dashboard.fxml", 1200, 760);
    }

    public void goToUserManagement() {
        loadIntoStage("/com/bizhub/fxml/user-management.fxml", 1200, 760);
    }

    public void goToReviews() {
        loadIntoStage("/com/bizhub/fxml/reviews-list.fxml", 1200, 760);
    }

    public void goToProfile() {
        loadIntoStage("/com/bizhub/fxml/user-profile.fxml", 1000, 700);
    }

    public void goToSignup() {
        loadIntoStage("/com/bizhub/fxml/signup.fxml", 980, 700);
    }

    public void goToFormations() {
        loadIntoStage("/com/bizhub/fxml/formations.fxml", 1200, 760);
    }

    public void goToFormationDetails() {
        loadIntoStage("/com/bizhub/fxml/formation-details.fxml", 1200, 760);
    }

    private void loadIntoStage(String fxmlPath, double w, double h) {
        URL res = NavigationService.class.getResource(fxmlPath);
        if (res == null) throw new IllegalStateException("Missing FXML: " + fxmlPath);

        Parent nextRoot;
        try {
            nextRoot = FXMLLoader.load(res);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxmlPath + ": " + e.getMessage(), e);
        }

        // Prepare next root for entry animation
        nextRoot.setOpacity(0.0);
        nextRoot.setTranslateY(16);
        nextRoot.setScaleX(0.985);
        nextRoot.setScaleY(0.985);

        final Scene sceneRef = stage.getScene();
        if (sceneRef == null) {
            Scene newScene = new Scene(nextRoot, w, h);
            stage.setScene(newScene);
            stage.show();
            playEnter(nextRoot);
            return;
        }

        Parent currentRoot = sceneRef.getRoot();
        if (currentRoot == null) {
            sceneRef.setRoot(nextRoot);
            playEnter(nextRoot);
            return;
        }

        // Exit current, then swap root, then enter next
        Animation exit = playExit(currentRoot);
        exit.setOnFinished(ev -> {
            sceneRef.setRoot(nextRoot);
            playEnter(nextRoot);
        });
        exit.play();
    }

    private static Animation playExit(Parent node) {
        FadeTransition ft = new FadeTransition(TRANSITION_DURATION, node);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        TranslateTransition tt = new TranslateTransition(TRANSITION_DURATION, node);
        tt.setFromY(0);
        tt.setToY(-12);

        ScaleTransition st = new ScaleTransition(TRANSITION_DURATION, node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.99);
        st.setToY(0.99);

        ParallelTransition pt = new ParallelTransition(ft, tt, st);
        pt.setInterpolator(Interpolator.EASE_BOTH);
        return pt;
    }

    private static void playEnter(Parent node) {
        FadeTransition ft = new FadeTransition(TRANSITION_DURATION, node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(TRANSITION_DURATION, node);
        tt.setFromY(16);
        tt.setToY(0);

        ScaleTransition st = new ScaleTransition(TRANSITION_DURATION, node);
        st.setFromX(0.985);
        st.setFromY(0.985);
        st.setToX(1.0);
        st.setToY(1.0);

        ParallelTransition pt = new ParallelTransition(ft, tt, st);
        pt.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0));
        pt.play();
    }
}
