package com.bizhub.controller.elearning.formation;

import com.bizhub.model.elearning.formation.Formation;
import com.bizhub.model.services.elearning.formation.FormationContext;
import com.bizhub.controller.elearning.payment.PaymentController;
import com.bizhub.controller.elearning.participation.ParticipationFormController;
import com.bizhub.model.elearning.participation.Participation;
import com.bizhub.model.services.elearning.participation.ParticipationContext;
import com.bizhub.controller.users_avis.review.ReviewFormController;
import com.bizhub.model.users_avis.review.Review;
import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AlertHelper;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.bizhub.controller.users_avis.user.TopbarProfileHelper;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FormationDetailsController {

    @FXML private Text titleText;
    @FXML private Label trainerLabel;
    @FXML private Label startLabel;
    @FXML private Label endLabel;
    @FXML private Label costLabel;
    @FXML private Label lieuLabel;
    @FXML private Label enLigneLabel;
    @FXML private TextArea descArea;

    @FXML private Button leaveReviewButton;
    @FXML private Button addParticipationButton;

    @FXML private TableView<Review> reviewsTable;
    @FXML private TableColumn<Review, String> colReviewer, colRating, colComment, colDate;

    @FXML private TableView<Participation> participationsTable;
    @FXML private TableColumn<Participation, String> colPartUser, colPartDate, colPartRemarques;

    @FXML private HBox topbar;

    private int formationId;

    @FXML
    public void initialize() {
        // Add user profile to topbar
        if (topbar != null) {
            topbar.getChildren().add(TopbarProfileHelper.createProfileBox());
        }

        // Set up review table columns
        if (colReviewer != null) {
            colReviewer.setCellValueFactory(c ->
                    new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getReviewerName())));
        }
        if (colRating != null) {
            colRating.setCellValueFactory(c ->
                    new javafx.beans.property.SimpleStringProperty(
                            c.getValue().getRating() == null ? "" : c.getValue().getRating().toString()));
        }
        if (colComment != null) {
            colComment.setCellValueFactory(c -> {
                String s = nullToEmpty(c.getValue().getComment());
                if (s.length() > 90) s = s.substring(0, 87) + "...";
                return new javafx.beans.property.SimpleStringProperty(s);
            });
        }
        if (colDate != null) {
            colDate.setCellValueFactory(c -> {
                var dt = c.getValue().getCreatedAt();
                return new javafx.beans.property.SimpleStringProperty(
                        dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            });
        }

        // Set up participation table columns
        if (colPartUser != null) {
            colPartUser.setCellValueFactory(c ->
                    new javafx.beans.property.SimpleStringProperty(userNameFor(c.getValue().getUserId())));
        }
        if (colPartDate != null) {
            colPartDate.setCellValueFactory(c -> {
                var dt = c.getValue().getDateAffectation();
                return new javafx.beans.property.SimpleStringProperty(
                        dt == null ? "" : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            });
        }
        if (colPartRemarques != null) {
            colPartRemarques.setCellValueFactory(c ->
                    new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getRemarques())));
        }

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
            if (lieuLabel != null) lieuLabel.setText(nullToEmpty(f.getLieu()));
            if (enLigneLabel != null) enLigneLabel.setText(f.isEnLigne() ? "Oui" : "Non");
            descArea.setText(nullToEmpty(f.getDescription()));

            reviewsTable.setItems(FXCollections.observableArrayList(Services.reviews().findAllByFormationId(formationId)));

            // Load participations table
            if (participationsTable != null) {
                List<Participation> parts = Services.participations().findByFormationId(formationId);
                participationsTable.setItems(FXCollections.observableArrayList(parts));
            }

            var me = AppSession.getCurrentUser();

            // Participation button: visible for everyone
            if (addParticipationButton != null) {
                addParticipationButton.setVisible(true);
                addParticipationButton.setManaged(true);
                addParticipationButton.setText("Participer");

                if (me == null) {
                    addParticipationButton.setDisable(false);
                } else if (!AppSession.isAdmin()) {
                    boolean alreadyParticipates =
                            Services.participations().findByFormationAndUser(formationId, me.getUserId()).isPresent();
                    addParticipationButton.setDisable(alreadyParticipates);
                    if (alreadyParticipates) addParticipationButton.setText("Déjà participé");
                } else {
                    addParticipationButton.setDisable(false);
                }
            }

            // Review button
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

    @FXML
    public void addParticipation() {
        var me = AppSession.getCurrentUser();
        Formation f = null;
        try {
            f = Services.formations().findById(formationId).orElse(null);
        } catch (SQLException e) {
            showError(e.getMessage());
            return;
        }
        if (f == null) {
            showError("Formation introuvable.");
            return;
        }

        if (AppSession.isAdmin()) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/bizhub/fxml/participation-form.fxml"));
                Parent root = loader.load();
                ParticipationFormController ctl = loader.getController();
                Participation p = new Participation();
                p.setFormationId(formationId);
                if (me != null) p.setUserId(me.getUserId());
                ctl.setEditing(p, this::load);
                Stage dialog = new Stage();
                dialog.initOwner(addParticipationButton.getScene().getWindow());
                dialog.initModality(Modality.WINDOW_MODAL);
                dialog.setScene(new Scene(root));
                dialog.showAndWait();
            } catch (Exception e) {
                showError(e.getMessage());
            }
            return;
        }

        if (me == null) {
            openSelectUserThenPayment(f);
            return;
        }

        try {
            if (Services.participations().findByFormationAndUser(formationId, me.getUserId()).isPresent()) {
                info("Vous participez déjà à cette formation.");
                return;
            }
            Participation p = new Participation();
            p.setFormationId(formationId);
            p.setUserId(me.getUserId());
            p.setDateAffectation(LocalDateTime.now());
            p.setRemarques(null);
            p.setPaymentStatus("PENDING");
            p.setAmount(f.getCost() == null ? BigDecimal.ZERO : f.getCost());
            Services.participations().create(p);
            openPaymentDialog(f, p, me, this::load);
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void openSelectUserThenPayment(Formation f) {
        try {
            List<User> users = Services.users().findAll();
            if (users.isEmpty()) {
                info("Aucun utilisateur dans la base.");
                return;
            }
            ComboBox<User> userBox = new ComboBox<>(FXCollections.observableArrayList(users));
            userBox.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    setText(empty || u == null ? null : (u.getFullName() + " (" + u.getEmail() + ")"));
                }
            });
            userBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(User u, boolean empty) {
                    super.updateItem(u, empty);
                    setText(empty || u == null ? "Choisir un participant..." : (u.getFullName() + " (" + u.getEmail() + ")"));
                }
            });
            userBox.setMaxWidth(Double.MAX_VALUE);
            Label label = new Label("Participant (utilisateur en base)");
            Label err = new Label();
            err.getStyleClass().add("error-text");
            err.setWrapText(true);
            Button goBtn = new Button("Continuer vers paiement");
            goBtn.getStyleClass().add("btn-primary");
            VBox box = new VBox(12, label, userBox, err, goBtn);
            box.setPadding(new Insets(16));
            Stage dialog = new Stage();
            dialog.initOwner(addParticipationButton.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Participer à \"" + (f.getTitle() != null ? f.getTitle() : "") + "\"");
            Scene scene = new Scene(box);
            if (getClass().getResource("/com/bizhub/css/theme.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/com/bizhub/css/theme.css").toExternalForm());
            }
            dialog.setScene(scene);
            goBtn.setOnAction(e -> {
                User selected = userBox.getValue();
                if (selected == null) {
                    err.setText("Veuillez choisir un participant.");
                    return;
                }
                try {
                    if (Services.participations().findByFormationAndUser(f.getFormationId(), selected.getUserId()).isPresent()) {
                        err.setText("Ce participant est déjà inscrit à cette formation.");
                        return;
                    }
                    Participation p = new Participation();
                    p.setFormationId(f.getFormationId());
                    p.setUserId(selected.getUserId());
                    p.setDateAffectation(LocalDateTime.now());
                    p.setRemarques(null);
                    p.setPaymentStatus("PENDING");
                    p.setAmount(f.getCost() == null ? BigDecimal.ZERO : f.getCost());
                    Services.participations().create(p);
                    dialog.close();
                    openPaymentDialog(f, p, selected, this::load);
                } catch (SQLException ex) {
                    err.setText(ex.getMessage());
                }
            });
            dialog.showAndWait();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void openPaymentDialog(Formation f, Participation p, User u, Runnable onPaid) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/payment.fxml"));
            Parent root = loader.load();
            PaymentController ctl = loader.getController();
            ctl.setContext(p, f, u, onPaid);
            Stage stage = new Stage();
            stage.initOwner(addParticipationButton.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Paiement - " + (f.getTitle() != null ? f.getTitle() : "Formation"));
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
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
            dialog.initOwner(reviewsTable.getScene().getWindow());
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
        Stage stage = (Stage) reviewsTable.getScene().getWindow();
        new NavigationService(stage).goToFormations();
    }

    private void disable(String msg) {
        titleText.setText(msg);
        leaveReviewButton.setDisable(true);
        if (addParticipationButton != null) {
            var me = AppSession.getCurrentUser();
            if (me == null) {
                addParticipationButton.setVisible(false);
                addParticipationButton.setManaged(false);
            }
        }
    }

    private static String userNameFor(int userId) {
        try {
            return Services.users().findById(userId)
                    .map(com.bizhub.model.users_avis.user.User::getFullName).orElse("#" + userId);
        } catch (SQLException e) {
            return "#" + userId;
        }
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
}
