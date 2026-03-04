package com.bizhub.controller.elearning.formation;

import com.bizhub.model.elearning.formation.Formation;
import com.bizhub.model.elearning.participation.Participation;
import com.bizhub.model.users_avis.user.User;
import com.bizhub.model.services.common.service.AlertHelper;
import com.bizhub.model.services.common.service.AppSession;
import com.bizhub.model.services.common.service.NavigationService;
import com.bizhub.model.services.common.service.Services;
import com.bizhub.model.services.elearning.formation.FormationContext;
import com.bizhub.model.services.elearning.participation.ParticipationContext;
import com.bizhub.controller.elearning.payment.PaymentController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import com.bizhub.controller.users_avis.user.TopbarProfileHelper;
import com.bizhub.controller.users_avis.user.SidebarController;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public class FormationManagementController {

    @FXML private SidebarController sidebarController;

    @FXML private TextField searchField;
    @FXML private ListView<Formation> formationsList;
    @FXML private Label footerLabel;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private BorderPane root;
    @FXML private HBox topbar;

    private final ObservableList<Formation> backing = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActivePage("formations");
        // Add user profile to topbar
        if (topbar != null) {
            topbar.getChildren().add(TopbarProfileHelper.createProfileBox());
        }

        boolean admin = AppSession.isAdmin();
        if (addButton != null) addButton.setDisable(!admin);
        if (editButton != null) editButton.setDisable(!admin);
        if (deleteButton != null) deleteButton.setDisable(!admin);

        formationsList.setItems(backing);
        formationsList.setCellFactory(lv -> {
            FormationCardCell cell = new FormationCardCell();
            if (AppSession.isAdmin()) {
                cell.setParticiperCallback(this::participerAdmin);
            } else {
                cell.setParticiperCallback(this::participerStartup);
            }
            cell.setQrCallback(this::showQrCode);
            cell.setMeetCallback(this::openMeetDialog);
            return cell;
        });

        formationsList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Formation f = formationsList.getSelectionModel().getSelectedItem();
                if (f != null) {
                    FormationContext.setSelectedFormationId(f.getFormationId());
                    Stage stage = (Stage) formationsList.getScene().getWindow();
                    new NavigationService(stage).goToFormationDetails();
                }
            }
        });

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        refresh();
    }

    @FXML
    public void refresh() {
        try {
            backing.setAll(Services.formations().findAll());
            applyFilter();
            footerLabel.setText("Loaded " + formationsList.getItems().size() + " formations");
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void applyFilter() {
        String q = nullToEmpty(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            formationsList.setItems(backing);
            return;
        }

        List<Formation> filtered = new ArrayList<>();
        for (Formation f : backing) {
            if (f.getTitle() != null && f.getTitle().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(f);
            }
        }
        formationsList.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void editSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        Formation f = formationsList.getSelectionModel().getSelectedItem();
        if (f == null) {
            info("Select a formation first");
            return;
        }
        openForm(f, this::refresh);
    }

    @FXML
    public void deleteSelected() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        Formation f = formationsList.getSelectionModel().getSelectedItem();
        if (f == null) {
            info("Select a formation first");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete formation?");
        confirm.setContentText("This will also delete related reviews (FK cascade).\n" + f.getTitle());
        AlertHelper.styleAlert(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Services.formations().delete(f.getFormationId());
            refresh();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void addFormation() {
        if (!AppSession.isAdmin()) {
            info("Admin only");
            return;
        }
        openForm(new Formation(), this::refresh);
    }

    // ==================== Participer ====================

    /** Clic Participer (non-admin) : invité = choisir un user puis paiement ; connecté = paiement. */
    private void participerStartup(Formation f) {
        User me = AppSession.getCurrentUser();
        if (me == null) {
            openSelectUserThenPayment(f);
            return;
        }
        try {
            if (Services.participations().findByFormationAndUser(f.getFormationId(), me.getUserId()).isPresent()) {
                info("Vous participez déjà à cette formation.");
                return;
            }
            Participation p = new Participation();
            p.setFormationId(f.getFormationId());
            p.setUserId(me.getUserId());
            p.setDateAffectation(LocalDateTime.now());
            p.setPaymentStatus("PENDING");
            p.setAmount(f.getCost() == null ? BigDecimal.ZERO : f.getCost());
            Services.participations().create(p);
            openPaymentDialog(f, p, me, this::refresh);
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    /** Invité : dialogue pour choisir un utilisateur en base, puis création PENDING + écran paiement. */
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
            dialog.initOwner(formationsList.getScene().getWindow());
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
                    p.setPaymentStatus("PENDING");
                    p.setAmount(f.getCost() == null ? BigDecimal.ZERO : f.getCost());
                    Services.participations().create(p);
                    dialog.close();
                    openPaymentDialog(f, p, selected, this::refresh);
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
            stage.initOwner(formationsList.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Paiement - " + (f.getTitle() != null ? f.getTitle() : "Formation"));
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    /** Admin: ouvre la liste des participations filtrée par cette formation. */
    private void participerAdmin(Formation f) {
        ParticipationContext.setFormationIdForParticipation(f.getFormationId());
        Stage stage = (Stage) formationsList.getScene().getWindow();
        new NavigationService(stage).goToParticipations();
    }

    // ==================== QR Code ====================

    /**
     * Affiche un QR code Google Maps pour la localisation de la formation.
     * Utilise ZXing (déjà dans pom.xml).
     */
    private void showQrCode(Formation f) {
        String loc = f.getLieu();
        if (loc == null || loc.isBlank()) {
            info("Aucune localisation définie pour cette formation.");
            return;
        }

        try {
            String mapsUrl = "https://www.google.com/maps/search/?api=1&query="
                    + URLEncoder.encode(loc, "UTF-8");

            int size = 300;
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(mapsUrl, BarcodeFormat.QR_CODE, size, size);

            WritableImage fxImage = new WritableImage(size, size);
            PixelWriter writer = fxImage.getPixelWriter();
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    boolean black = matrix.get(x, y);
                    writer.setColor(x, y, black ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView imageView = new ImageView(fxImage);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(true);

            Label title = new Label("QR code Google Maps");
            title.getStyleClass().add("h2");

            Label hint = new Label("Scannez ce QR code avec votre téléphone pour ouvrir la localisation dans Google Maps.");
            hint.getStyleClass().add("hint");
            hint.setWrapText(true);

            VBox box = new VBox(12, title, imageView, hint);
            box.setPadding(new Insets(16));

            Stage dialog = new Stage();
            dialog.initOwner(formationsList.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("QR code localisation");
            dialog.setScene(new Scene(box));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Erreur lors de la génération du QR code : " + e.getMessage());
        }
    }

    // ==================== Google Meet ====================

    private static HBox linkRowWithButton(TextField linkField, Button createLinkBtn) {
        HBox row = new HBox(8, linkField, createLinkBtn);
        HBox.setHgrow(linkField, Priority.ALWAYS);
        return row;
    }

    /** Ouvre le dialogue "Créer un meet" : date, heure, lien Google Meet, puis envoie l'invitation par email. */
    private void openMeetDialog(Formation f) {
        String formationTitle = f.getTitle() != null ? f.getTitle() : ("Formation #" + f.getFormationId());
        DatePicker datePicker = new DatePicker(LocalDate.now());
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 14);
        hourSpinner.setEditable(true);
        Spinner<Integer> minSpinner = new Spinner<>(0, 59, 0);
        minSpinner.setEditable(true);
        TextField linkField = new TextField();
        linkField.setPromptText("https://meet.google.com/xxx-xxxx-xxx");
        linkField.setPrefColumnCount(35);
        Label errLabel = new Label();
        errLabel.getStyleClass().add("error-text");
        errLabel.setWrapText(true);

        Button createLinkBtn = new Button("Créer le lien automatiquement");
        createLinkBtn.getStyleClass().add("btn");
        createLinkBtn.setOnAction(e -> {
            LocalDate d = datePicker.getValue();
            if (d == null) {
                errLabel.setText("Choisissez d'abord la date du meet.");
                return;
            }
            int h = hourSpinner.getValue();
            int m = minSpinner.getValue();
            LocalTime t = LocalTime.of(h, m);
            errLabel.setText("Création du lien en cours...");
            createLinkBtn.setDisable(true);
            new Thread(() -> {
                java.util.Optional<String> linkOpt = Services.googleMeet().createMeetingWithMeetLink(d, t, 60, formationTitle);
                Platform.runLater(() -> {
                    createLinkBtn.setDisable(false);
                    if (linkOpt.isPresent()) {
                        linkField.setText(linkOpt.get());
                        errLabel.setText("");
                    } else {
                        if (!Services.googleMeet().isConfigured()) {
                            errLabel.setText("Configurez GOOGLE_MEET_CREDENTIALS_PATH dans application.properties.");
                        } else {
                            errLabel.setText("Impossible de créer le lien. Vérifiez le calendrier partagé.");
                        }
                    }
                });
            }).start();
        });

        Stage dialog = new Stage();
        dialog.initOwner(formationsList.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Créer un meet - " + formationTitle);

        Button sendBtn = new Button("Envoyer les invitations");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setOnAction(e -> {
            LocalDate meetDate = datePicker.getValue();
            if (meetDate == null) {
                errLabel.setText("Veuillez choisir une date.");
                return;
            }
            int h = hourSpinner.getValue();
            int m = minSpinner.getValue();
            LocalTime meetTime = LocalTime.of(h, m);
            String link = linkField.getText();
            if (link == null || link.isBlank()) {
                errLabel.setText("Veuillez saisir le lien Google Meet.");
                return;
            }
            try {
                List<Participation> participations = Services.participations().findByFormationId(f.getFormationId());
                int sent = 0;
                for (Participation p : participations) {
                    User user = Services.users().findById(p.getUserId()).orElse(null);
                    if (user == null || user.getEmail() == null || user.getEmail().isBlank()) continue;
                    boolean ok = Services.email().sendMeetInvitation(
                            user.getEmail().trim(),
                            user.getFullName(),
                            formationTitle,
                            meetDate,
                            meetTime,
                            link);
                    if (ok) sent++;
                }
                dialog.close();
                info(sent + " invitation(s) envoyée(s) aux participants."
                        + (participations.isEmpty() ? " Aucun participant inscrit." : ""));
            } catch (SQLException ex) {
                errLabel.setText(ex.getMessage());
            }
        });
        Button cancelBtn = new Button("Annuler");
        cancelBtn.getStyleClass().add("btn");
        cancelBtn.setOnAction(e -> dialog.close());
        HBox buttons = new HBox(10, cancelBtn, sendBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox box = new VBox(12,
                new Label("Date du meet"),
                datePicker,
                new Label("Heure (HH:MM)"),
                new HBox(8, hourSpinner, new Label("h"), minSpinner),
                new Label("Lien Google Meet"),
                linkRowWithButton(linkField, createLinkBtn),
                errLabel,
                buttons);
        box.setPadding(new Insets(20));

        Scene scene = new Scene(box, 550, 450);
        if (getClass().getResource("/com/bizhub/css/theme.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/com/bizhub/css/theme.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.setMinWidth(550);
        dialog.setMinHeight(450);
        dialog.showAndWait();
    }

    // ==================== Navigation ====================

    @FXML
    public void goBack() {
        Stage stage = (Stage) formationsList.getScene().getWindow();
        if (AppSession.isAdmin()) {
            new NavigationService(stage).goToAdminDashboard();
        } else {
            new NavigationService(stage).goToProfile();
        }
    }

    // ==================== Card Cell ====================

    private static final class FormationCardCell extends ListCell<Formation> {
        private final VBox card = new VBox(8);
        private final Label title = new Label();
        private final Label meta = new Label();
        private final Button participerButton = new Button("Participer");
        private final Button qrButton = new Button("QR Map");
        private final Button meetButton = new Button("Créer un meet");
        private java.util.function.Consumer<Formation> participerCallback;
        private java.util.function.Consumer<Formation> qrCallback;
        private java.util.function.Consumer<Formation> meetCallback;

        FormationCardCell() {
            card.getStyleClass().add("formation-card");
            card.setPadding(new Insets(16));

            title.getStyleClass().add("formation-title");
            meta.getStyleClass().add("formation-meta");
            participerButton.getStyleClass().add("btn-primary");
            qrButton.getStyleClass().add("btn");
            meetButton.getStyleClass().add("btn");

            participerButton.setOnAction(e -> {
                Formation f = getItem();
                if (f != null && participerCallback != null) participerCallback.accept(f);
            });
            qrButton.setOnAction(e -> {
                Formation f = getItem();
                if (f != null && qrCallback != null) qrCallback.accept(f);
            });
            meetButton.setOnAction(e -> {
                Formation f = getItem();
                if (f != null && meetCallback != null) meetCallback.accept(f);
            });

            HBox actions = new HBox(8, participerButton, qrButton, meetButton);
            card.getChildren().addAll(title, meta, actions);
        }

        void setParticiperCallback(java.util.function.Consumer<Formation> cb) { this.participerCallback = cb; }
        void setQrCallback(java.util.function.Consumer<Formation> cb) { this.qrCallback = cb; }
        void setMeetCallback(java.util.function.Consumer<Formation> cb) { this.meetCallback = cb; }

        @Override
        protected void updateItem(Formation item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            title.setText(item.getTitle() == null ? "(untitled)" : item.getTitle());
            meta.setText("#" + item.getFormationId() + "   •   trainer_id: " + item.getTrainerId());
            setGraphic(card);
        }
    }

    // ==================== Helpers ====================

    private void openForm(Formation f, Runnable onSaved) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/formation-form.fxml"));
            Parent root = loader.load();
            FormationFormController ctl = loader.getController();
            ctl.setEditing(f, onSaved);

            Stage dialog = new Stage();
            dialog.initOwner(formationsList.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Formation");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            showError(e.getMessage());
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
