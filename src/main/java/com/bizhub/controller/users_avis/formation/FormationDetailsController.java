package com.bizhub.controller.users_avis.formation;

import com.bizhub.model.users_avis.formation.Formation;
import com.bizhub.model.services.user_avis.formation.FormationContext;
import com.bizhub.controller.users_avis.review.ReviewFormController;
import com.bizhub.model.users_avis.review.Review;
import com.bizhub.model.services.common.service.AlertHelper;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.bizhub.controller.users_avis.user.TopbarProfileHelper;

import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormationDetailsController {

    @FXML private Text titleText;
    @FXML private Label trainerLabel;
    @FXML private Label startLabel;
    @FXML private Label endLabel;
    @FXML private Label costLabel;
    @FXML private TextArea descArea;

    @FXML private Button leaveReviewButton;

    @FXML private ListView<Review> reviewsList;

    @FXML private HBox topbar;

    private int formationId;

    @FXML
    public void initialize() {
        // Add user profile to topbar
        if (topbar != null) {
            topbar.getChildren().add(TopbarProfileHelper.createProfileBox());
        }

        // Enable multiple selection for reviews list
        reviewsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        reviewsList.setCellFactory(lv -> new ReviewCardCell());

        Integer id = FormationContext.getSelectedFormationId();
        if (id == null) {
            disable("No formation selected");
            return;
        }
        formationId = id;


        load();
    }

    private void load() {
        try {
            Formation f = Services.formations().findById(formationId).orElse(null);
            if (f == null) {
                disable("Formation not found");
                return;
            }

            titleText.setText(f.getTitle());
            trainerLabel.setText(String.valueOf(f.getTrainerId()));
            startLabel.setText(f.getStartDate() == null ? "" : f.getStartDate().toString());
            endLabel.setText(f.getEndDate() == null ? "" : f.getEndDate().toString());
            costLabel.setText(f.getCost() == null ? "0.00" : f.getCost().toPlainString());
            descArea.setText(nullToEmpty(f.getDescription()));

            reviewsList.setItems(FXCollections.observableArrayList(Services.reviews().findAllByFormationId(formationId)));

            // If user already reviewed, change button behavior to edit
            var me = AppSession.getCurrentUser();
            if (me == null) {
                leaveReviewButton.setDisable(true);
            } else {
                boolean already = Services.reviews().findByReviewerAndFormation(me.getUserId(), formationId).isPresent();
                leaveReviewButton.setText(already ? "Edit my review" : "Leave a review");
            }

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void leaveReview() {
        var me = AppSession.getCurrentUser();
        if (me == null) {
            info("Please login first");
            return;
        }

        try {
            Review existing = Services.reviews().findByReviewerAndFormation(me.getUserId(), formationId).orElse(null);
            openReviewForm(existing);
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void openReviewForm(Review existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/review-form.fxml"));
            Parent root = loader.load();
            ReviewFormController ctl = loader.getController();
            ctl.setContext(formationId, existing, this::load);

            Stage dialog = new Stage();
            dialog.initOwner(reviewsList.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Review");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        Stage stage = (Stage) reviewsList.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    private void disable(String msg) {
        titleText.setText(msg);
        leaveReviewButton.setDisable(true);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void showError(String msg) {
        AlertHelper.showError(msg);
    }

    private void info(String msg) {
        AlertHelper.showInfo(msg);
    }

    // --- Card cell (same style as reviews section) ---

    private static final class ReviewCardCell extends ListCell<Review> {
        private final VBox card = new VBox(10);

        private final HBox topRow = new HBox(12);
        private final StackPane avatar = new StackPane();
        private final ImageView avatarImage = new ImageView();
        private final Label avatarLabel = new Label();

        private final VBox headText = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label metaLabel = new Label();

        private final Region spacer = new Region();

        private final VBox body = new VBox(8);
        private final HBox starsRow = new HBox(2);
        private final Label[] stars = new Label[5];
        private final Label reviewText = new Label();

        ReviewCardCell() {
            getStyleClass().add("review-cell");

            card.getStyleClass().addAll("review-card");
            card.setPadding(new Insets(16));

            // Toggle selected style on the card when cell is selected
            selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    card.getStyleClass().add("review-card-selected");
                } else {
                    card.getStyleClass().remove("review-card-selected");
                }
            });

            avatar.getStyleClass().add("review-avatar");
            avatar.setMinSize(44, 44);
            avatar.setPrefSize(44, 44);
            avatar.setMaxSize(44, 44);

            avatarImage.setFitWidth(44);
            avatarImage.setFitHeight(44);
            avatarImage.setPreserveRatio(true);
            avatarImage.setSmooth(true);
            avatarImage.getStyleClass().add("review-avatar-img");

            avatarLabel.getStyleClass().add("review-avatar-text");
            avatar.getChildren().addAll(avatarImage, avatarLabel);

            nameLabel.getStyleClass().add("review-name");
            metaLabel.getStyleClass().add("review-meta");
            headText.getChildren().addAll(nameLabel, metaLabel);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            topRow.setAlignment(Pos.CENTER_LEFT);
            topRow.getChildren().addAll(avatar, headText, spacer);

            starsRow.getStyleClass().add("review-stars");
            starsRow.setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < 5; i++) {
                Label s = new Label("★");
                s.getStyleClass().add("star");
                stars[i] = s;
                starsRow.getChildren().add(s);
            }

            reviewText.getStyleClass().add("review-text");
            reviewText.setWrapText(true);
            reviewText.setMaxWidth(Double.MAX_VALUE);

            body.getChildren().addAll(starsRow, reviewText);

            card.getChildren().addAll(topRow, body);
        }

        @Override
        protected void updateItem(Review item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            String name = pickDisplayName(item);
            nameLabel.setText(name);

            String dt = item.getCreatedAt() == null
                    ? ""
                    : item.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm", Locale.ENGLISH));
            metaLabel.setText(dt.isBlank() ? ("Review #" + item.getAvisId()) : (dt + "   •   #" + item.getAvisId()));

            setAvatar(item.getReviewerAvatarUrl(), name);

            int rating = item.getRating() == null ? 0 : Math.max(0, Math.min(5, item.getRating()));
            for (int i = 0; i < 5; i++) {
                stars[i].getStyleClass().removeAll("filled", "empty");
                stars[i].getStyleClass().add(i < rating ? "filled" : "empty");
            }

            String txt = nullToEmpty(item.getComment()).trim();
            reviewText.setText(txt.isBlank() ? "No comment provided." : txt);

            setGraphic(card);
        }

        private void setAvatar(String avatarUrl, String displayName) {
            Image img = loadAvatarImage(avatarUrl);
            if (img != null) {
                avatarImage.setImage(img);
                avatarImage.setVisible(true);
                avatarLabel.setVisible(false);
            } else {
                avatarImage.setImage(null);
                avatarImage.setVisible(false);
                avatarLabel.setText(initials(displayName));
                avatarLabel.setVisible(true);
            }
        }

        private Image loadAvatarImage(String avatarUrl) {
            if (avatarUrl == null || avatarUrl.isBlank()) return null;

            try {
                var res = FormationDetailsController.class.getClassLoader().getResource(avatarUrl);
                if (res != null) {
                    return new Image(res.toExternalForm(), true);
                }
            } catch (Exception ignored) {
            }

            try {
                File f = new File("src/main/resources/" + avatarUrl);
                if (f.exists()) {
                    return new Image(f.toURI().toString(), true);
                }
            } catch (Exception ignored) {
            }

            return null;
        }

        private static String pickDisplayName(Review r) {
            String n = nullToEmpty(r.getReviewerName()).trim();
            if (!n.isBlank()) return n;
            String e = nullToEmpty(r.getReviewerEmail()).trim();
            return e.isBlank() ? "Anonymous" : e;
        }

        private static String initials(String s) {
            if (s == null) return "?";
            String t = s.trim();
            if (t.isEmpty()) return "?";
            String[] parts = t.split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
            }
            String a = parts[0].isEmpty() ? "" : parts[0].substring(0, 1);
            String b = parts[1].isEmpty() ? "" : parts[1].substring(0, 1);
            String res = (a + b).trim();
            return res.isEmpty() ? "?" : res.toUpperCase(Locale.ROOT);
        }

        private static String nullToEmpty(String s) {
            return s == null ? "" : s;
        }
    }
}
