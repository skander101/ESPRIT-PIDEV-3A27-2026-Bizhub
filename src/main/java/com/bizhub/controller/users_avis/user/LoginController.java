package com.bizhub.controller.users_avis.user;

import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.AuthService;
import com.bizhub.model.services.common.service.FacePlusPlusService;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import com.bizhub.service.WebcamService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Login controller with Face Recognition verification.
 */
public class LoginController {

    // Login section
    @FXML private VBox loginSection;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;

    // Face verification section
    @FXML private VBox faceVerificationSection;
    @FXML private ImageView webcamPreview;
    @FXML private Button captureFaceButton;
    @FXML private Button backToLoginButton;
    @FXML private ProgressIndicator faceProgress;
    @FXML private Label faceStatusLabel;
    @FXML private Rectangle faceBoundingBox;

    @FXML private Label errorLabel;

    private AuthService authService;
    private User pendingUser; // User awaiting face verification
    private WebcamService webcamService;
    private FacePlusPlusService faceService;

    @FXML
    public void initialize() {
        errorLabel.setText("");

        // Basic UX: press Enter in password field triggers login
        passwordField.setOnAction(e -> {
            try {
                onLogin(new ActionEvent());
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        try {
            // Ensure DB is initialized
            if (Services.cnx() == null) {
                showError("DB connection failed: connection is null");
                loginButton.setDisable(true);
                return;
            }
            authService = Services.auth();
            webcamService = new WebcamService();
            faceService = new FacePlusPlusService();
        } catch (Exception e) {
            showError("DB init failed: " + e.getMessage());
            loginButton.setDisable(true);
        }
    }

    @FXML
    public void onLogin(ActionEvent event) {
        clearError();

        try {
            String email = emailField.getText();
            String password = passwordField.getText();

            Optional<User> loggedIn = authService.login(email, password);
            if (loggedIn.isEmpty()) {
                showError("Invalid credentials or inactive user.");
                return;
            }

            User u = loggedIn.get();

            // Check if user has face token for verification
            if (u.getFaceToken() != null && !u.getFaceToken().isBlank()) {
                // User has face token - show face verification
                pendingUser = u;
                showFaceVerificationSection();
            } else {
                // No face token - complete login directly (fallback)
                completeLogin(u);
            }

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void showFaceVerificationSection() {
        loginSection.setVisible(false);
        loginSection.setManaged(false);
        faceVerificationSection.setVisible(true);
        faceVerificationSection.setManaged(true);
        faceBoundingBox.setVisible(false);
        clearError();
        faceStatusLabel.setText("Position your face in the camera and click Verify");
        faceStatusLabel.setStyle("-fx-text-fill: #4CAF50;");

        // Start webcam preview
        webcamService.start(webcamPreview);
    }

    @FXML
    public void onVerifyFace() {
        if (pendingUser == null) {
            showError("Session expired. Please login again.");
            onBackToLogin();
            return;
        }

        String faceToken = pendingUser.getFaceToken();
        if (faceToken == null || faceToken.isBlank()) {
            showError("No face reference found. Please contact support.");
            return;
        }

        setFaceLoading(true);
        faceStatusLabel.setText("Capturing and comparing face...");
        faceStatusLabel.setStyle("-fx-text-fill: #2196F3;");

        // Capture current frame from webcam
        Image currentFrame = webcamService.captureSnapshot();
        if (currentFrame == null) {
            setFaceLoading(false);
            showError("Failed to capture image from camera.");
            faceStatusLabel.setText("Camera capture failed. Please try again.");
            faceStatusLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }

        // Compare captured face with stored token
        Task<Double> compareTask = faceService.compareWithFaceTokenTask(faceToken, currentFrame);

        compareTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                setFaceLoading(false);
                double confidence = compareTask.getValue();

                // Threshold for face match (Face++ recommends 70-80 for high confidence)
                double threshold = 70.0;

                if (confidence >= threshold) {
                    faceStatusLabel.setText("Face verified! Confidence: " + String.format("%.1f", confidence) + "%");
                    faceStatusLabel.setStyle("-fx-text-fill: #4CAF50;");

                    // Stop webcam
                    webcamService.stop();

                    // Complete login after brief delay
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException ignored) {}
                        completeLogin(pendingUser);
                    });
                } else {
                    faceStatusLabel.setText("Face mismatch. Confidence: " + String.format("%.1f", confidence) + "%");
                    faceStatusLabel.setStyle("-fx-text-fill: #f44336;");
                    showError("Face verification failed. Please try again.");
                }
            });
        });

        compareTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setFaceLoading(false);
                String errorMsg = compareTask.getException() != null 
                    ? compareTask.getException().getMessage() 
                    : "Unknown error";
                showError("Face verification error: " + errorMsg);
                faceStatusLabel.setText("Verification failed. Please try again.");
                faceStatusLabel.setStyle("-fx-text-fill: #f44336;");
            });
        });

        new Thread(compareTask).start();
    }

    @FXML
    public void onBackToLogin() {
        pendingUser = null;
        webcamService.stop();
        faceVerificationSection.setVisible(false);
        faceVerificationSection.setManaged(false);
        loginSection.setVisible(true);
        loginSection.setManaged(true);
        clearError();
        passwordField.clear();
    }

    private void setFaceLoading(boolean loading) {
        faceProgress.setVisible(loading);
        captureFaceButton.setDisable(loading);
    }

    private void completeLogin(User u) {
        AppSession.setCurrentUser(u);

        Stage stage = (Stage) loginButton.getScene().getWindow();
        NavigationService nav = new NavigationService(stage);

        if ("admin".equalsIgnoreCase(u.getUserType())) {
            nav.goToAdminDashboard();
        } else {
            nav.goToProfile();
        }
    }

    @FXML
    public void goToSignup() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        new NavigationService(stage).goToSignup();
    }

    @FXML
    public void goToForgotPassword() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        new NavigationService(stage).goToForgotPassword();
    }

    private void showError(String msg) {
        errorLabel.setText(msg == null ? "" : msg);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
