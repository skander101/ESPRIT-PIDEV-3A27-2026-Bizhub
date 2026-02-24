package com.bizhub.model.services.common.service;

import javafx.geometry.Rectangle2D;

/**
 * Small immutable DTO for Face++ detect results.
 */
public record FaceDetectionResult(
        boolean faceDetected,
        String faceToken,
        Rectangle2D boundingBox
) {
}

