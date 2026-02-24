package com.bizhub.service;

import com.bizhub.model.services.common.service.FaceDetectionResult;
import com.bizhub.model.services.common.service.FacePlusPlusService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

/**
 * Webcam service for capturing images from the default webcam.
 * Integrates with Face++ for real-time face detection.
 */
public class WebcamService {

    private final FacePlusPlusService faceService;
    private Webcam webcam;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ImageView targetView;
    private Thread captureThread;

    /**
     * Creates a new WebcamService with Face++ integration.
     */
    public WebcamService() {
        this.faceService = new FacePlusPlusService();
    }

    /**
     * Initializes and starts the webcam feed.
     *
     * @param imageView the ImageView to display the webcam feed
     * @return true if started successfully, false otherwise
     */
    public boolean start(ImageView imageView) {
        if (isRunning.get()) {
            return true;
        }

        this.targetView = imageView;
        this.webcam = Webcam.getDefault();

        if (webcam == null) {
            return false;
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());

        if (!webcam.open()) {
            return false;
        }

        isRunning.set(true);

        captureThread = new Thread(() -> {
            while (isRunning.get()) {
                try {
                    BufferedImage bufferedImage = webcam.getImage();
                    if (bufferedImage != null) {
                        Image fxImage = convertToFxImage(bufferedImage);
                        Platform.runLater(() -> {
                            if (targetView != null && isRunning.get()) {
                                targetView.setImage(fxImage);
                            }
                        });
                    }
                    Thread.sleep(33); // ~30 FPS
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Log error but keep running
                }
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();

        return true;
    }

    /**
     * Stops the webcam feed and releases resources.
     */
    public void stop() {
        isRunning.set(false);

        if (captureThread != null) {
            captureThread.interrupt();
        }

        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }

        if (targetView != null) {
            Platform.runLater(() -> targetView.setImage(null));
        }
    }

    /**
     * Captures the current frame from the webcam.
     *
     * @return the captured image, or null if capture failed
     */
    public Image captureSnapshot() {
        if (webcam == null || !webcam.isOpen()) {
            return null;
        }

        BufferedImage bufferedImage = webcam.getImage();
        if (bufferedImage == null) {
            return null;
        }

        return convertToFxImage(bufferedImage);
    }

    /**
     * Performs face detection on the current webcam frame.
     *
     * @return a Task that completes with the detection result
     */
    public Task<FaceDetectionResult> detectFaceInCurrentFrame() {
        Image snapshot = captureSnapshot();
        if (snapshot == null) {
            return new Task<>() {
                @Override
                protected FaceDetectionResult call() {
                    return new FaceDetectionResult(false, null, null);
                }
            };
        }
        return faceService.detectFaceTask(snapshot);
    }

    /**
     * Compares the current frame with a reference image.
     *
     * @param referenceImage the reference face image
     * @return a Task that completes with the confidence score
     */
    public Task<Double> compareWithReference(Image referenceImage) {
        Image current = captureSnapshot();
        if (current == null || referenceImage == null) {
            return new Task<>() {
                @Override
                protected Double call() {
                    return 0.0;
                }
            };
        }
        return faceService.compareFacesTask(current, referenceImage);
    }

    /**
     * Converts a BufferedImage to JavaFX Image.
     *
     * @param bufferedImage the AWT image
     * @return the JavaFX image
     */
    private Image convertToFxImage(BufferedImage bufferedImage) {
        WritableImage writableImage = new WritableImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight()
        );
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                int argb = bufferedImage.getRGB(x, y);
                pixelWriter.setArgb(x, y, argb);
            }
        }

        return writableImage;
    }

    /**
     * Checks if the webcam is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Gets the FacePlusPlusService for direct API access.
     *
     * @return the face service instance
     */
    public FacePlusPlusService getFaceService() {
        return faceService;
    }
}
