package com.bizhub.controller;

import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.FaceDetectionResult;
import com.bizhub.model.services.user_avis.user.UserService;
import com.bizhub.model.users_avis.user.User;
import com.bizhub.service.WebcamService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Example controller demonstrating Face++ facial recognition integration.
 * Shows how to capture webcam images and detect faces using FacePlusPlusService.
 */
public class FaceRecognitionController {

    @FXML private ImageView webcamView;
    @FXML private Button captureButton;
    @FXML private Label statusLabel;
    @FXML private Label faceTokenLabel;
    @FXML private Pane overlayPane;
    @FXML private ProgressIndicator progressIndicator;

    private WebcamService webcamService;
    private Image currentSnapshot;
    private Rectangle boundingBoxRect;

    /**
     * Initialize controller. Called automatically after FXML load.
     */
    @FXML
    public void initialize() {
        webcamService = new WebcamService();
        webcamService.start(webcamView);
        statusLabel.setText("Ready. Click 'Capture & Detect' to scan face.");
        faceTokenLabel.setText("");
        progressIndicator.setVisible(false);
    }

    /**
     * Captures a webcam snapshot and performs face detection.
     * This method demonstrates the FacePlusPlusService integration.
     */
    @FXML
    public void onCaptureAndDetect() {
        // Clear previous results
        clearOverlay();
        statusLabel.setText("Capturing...");
        faceTokenLabel.setText("");

        // Capture snapshot from webcam (placeholder - integrate with your webcam capture)
        currentSnapshot = captureWebcamSnapshot();

        if (currentSnapshot == null) {
            statusLabel.setText("Failed to capture image from webcam.");
            return;
        }

        // Display captured image
        webcamView.setImage(currentSnapshot);

        // Start face detection
        performFaceDetection(currentSnapshot);
    }

    /**
     * Performs face detection using FacePlusPlusService.
     * Runs asynchronously to avoid blocking the UI thread.
     *
     * @param image the image to analyze
     */
    private void performFaceDetection(Image image) {
        statusLabel.setText("Detecting face...");
        progressIndicator.setVisible(true);
        captureButton.setDisable(true);

        Task<FaceDetectionResult> detectTask = webcamService.detectFaceInCurrentFrame();

        detectTask.setOnSucceeded(event -> {
            FaceDetectionResult result = detectTask.getValue();
            handleDetectionResult(result);
        });

        detectTask.setOnFailed(event -> {
            Throwable error = detectTask.getException();
            statusLabel.setText("Detection failed: " + error.getMessage());
            progressIndicator.setVisible(false);
            captureButton.setDisable(false);
        });

        // Start the task on a background thread
        Thread taskThread = new Thread(detectTask);
        taskThread.setDaemon(true);
        taskThread.start();
    }

    /**
     * Handles the face detection result and updates the UI.
     *
     * @param result the detection result
     */
    private void handleDetectionResult(FaceDetectionResult result) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            captureButton.setDisable(false);

            if (result.faceDetected()) {
                statusLabel.setText("Face detected successfully!");
                faceTokenLabel.setText("Face Token: " + result.faceToken());

                // Draw bounding box
                if (result.boundingBox() != null) {
                    drawBoundingBox(result.boundingBox());
                }

                // Save token to database here
                saveFaceTokenToDatabase(result.faceToken());

            } else {
                statusLabel.setText("No face detected. Please try again.");
                faceTokenLabel.setText("");
            }
        });
    }

    /**
     * Draws a rectangle overlay showing the detected face bounding box.
     *
     * @param boundingBox the rectangle defining face location
     */
    private void drawBoundingBox(Rectangle2D boundingBox) {
        clearOverlay();

        boundingBoxRect = new Rectangle(
                boundingBox.getMinX(),
                boundingBox.getMinY(),
                boundingBox.getWidth(),
                boundingBox.getHeight()
        );
        boundingBoxRect.setFill(Color.TRANSPARENT);
        boundingBoxRect.setStroke(Color.LIME);
        boundingBoxRect.setStrokeWidth(3);

        overlayPane.getChildren().add(boundingBoxRect);
    }

    /**
     * Clears the bounding box overlay.
     */
    private void clearOverlay() {
        overlayPane.getChildren().clear();
        boundingBoxRect = null;
    }

    /**
     * Captures a snapshot from the webcam.
     * Integrate this with your existing webcam capture mechanism.
     *
     * @return the captured image, or null if capture failed
     */
    private Image captureWebcamSnapshot() {
        // TODO: Integrate with your existing webcam capture
        // Example implementation:
        // return webcamService.captureSnapshot();

        // Placeholder: return the current image from webcam view
        return webcamView.getImage();
    }

    /**
     * Example method for comparing two faces.
     * Demonstrates the face comparison feature.
     */
    @FXML
    public void onCompareFaces() {
        if (currentSnapshot == null) {
            statusLabel.setText("Please capture a face first.");
            return;
        }

        // Load reference image (e.g., from database or file)
        Image referenceImage = loadReferenceImage();

        if (referenceImage == null) {
            statusLabel.setText("No reference image available.");
            return;
        }

        statusLabel.setText("Comparing faces...");
        progressIndicator.setVisible(true);
        captureButton.setDisable(true);

        Task<Double> compareTask = webcamService.compareWithReference(referenceImage);

        compareTask.setOnSucceeded(event -> {
            double confidence = compareTask.getValue();
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                captureButton.setDisable(false);
                statusLabel.setText(String.format("Match confidence: %.2f%%", confidence));
            });
        });

        compareTask.setOnFailed(event -> {
            Throwable error = compareTask.getException();
            Platform.runLater(() -> {
                statusLabel.setText("Comparison failed: " + error.getMessage());
                progressIndicator.setVisible(false);
                captureButton.setDisable(false);
            });
        });

        Thread taskThread = new Thread(compareTask);
        taskThread.setDaemon(true);
        taskThread.start();
    }

    /**
     * Loads a reference image for face comparison.
     * Integrate this with your database or file storage.
     *
     * @return the reference image, or null if not available
     */
    private Image loadReferenceImage() {
        // TODO: Load reference image from your storage
        // Example:
        // - Load from database blob
        // - Load from file system
        // - Load from cached user profile

        return null;
    }

    /**
     * Saves the detected face token to the database.
     * Call this method after successful face detection.
     *
     * @param faceToken the Face++ face token to save
     */
    private void saveFaceTokenToDatabase(String faceToken) {
        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            statusLabel.setText("Error: No user logged in");
            return;
        }

        try {
            UserService userService = new UserService();
            userService.updateFaceToken(currentUser.getUserId(), faceToken);
            statusLabel.setText("Face token saved successfully!");
        } catch (Exception e) {
            statusLabel.setText("Failed to save face token: " + e.getMessage());
        }
    }
}
