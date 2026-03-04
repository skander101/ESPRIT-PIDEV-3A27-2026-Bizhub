package com.bizhub.payment.controller;

import com.bizhub.common.service.Services;
import com.bizhub.formation.model.Formation;
import com.bizhub.participation.model.Participation;
import com.bizhub.payment.PaymentProvider;
import com.bizhub.user.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class PaymentController {

    private static final Pattern CVV_PATTERN = Pattern.compile("\\d{3,4}");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\d{12,19}");

    @FXML private Text titleText;
    @FXML private Label formationLabel;
    @FXML private Label amountLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> methodBox;
    @FXML private VBox cardFormBox;
    @FXML private TextField cardHolderNameField;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardMonthField;
    @FXML private TextField cardYearField;
    @FXML private TextField cardCvvField;
    @FXML private Button payButton;

    private Participation participation;
    private String amountStr = "0 TND";
    private Formation formation;
    private User user;
    private Runnable onPaid;

    @FXML
    public void initialize() {
        errorLabel.setText("");
        statusLabel.setText("");
        methodBox.setItems(FXCollections.observableArrayList("Paiement simulé", "Carte bancaire"));
        methodBox.getSelectionModel().selectFirst();
        methodBox.valueProperty().addListener((obs, oldV, newV) -> {
            boolean card = "Carte bancaire".equals(newV);
            if (cardFormBox != null) {
                cardFormBox.setVisible(card);
                cardFormBox.setManaged(card);
            }
            updatePayButtonLook(card);
        });
        updatePayButtonLook(false);
    }

    private void updatePayButtonLook(boolean cardMode) {
        if (payButton == null) return;
        payButton.getStyleClass().removeAll("btn-pay-primary", "btn-primary");
        if (cardMode) {
            payButton.getStyleClass().add("btn-pay-primary");
            payButton.setText("🔒 Payer " + amountStr);
        } else {
            payButton.getStyleClass().add("btn-primary");
            payButton.setText("Payer (simulé)");
        }
    }

    @FXML
    public void showCvvHelp() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Cryptogramme visuel");
        a.setHeaderText("De quoi s'agit-il ?");
        a.setContentText("Le cryptogramme visuel (CVV ou CVC) est le code à 3 ou 4 chiffres inscrit au dos de votre carte, généralement à côté de la bande de signature. Il permet de sécuriser les paiements sans présentation physique de la carte.");
        a.showAndWait();
    }

    public void setContext(Participation participation, Formation formation, User user, Runnable onPaid) {
        this.participation = participation;
        this.formation = formation;
        this.user = user;
        this.onPaid = onPaid;

        String title = formation != null && formation.getTitle() != null
                ? formation.getTitle()
                : ("Formation #" + (formation != null ? formation.getFormationId() : participation.getFormationId()));
        formationLabel.setText(title);

        BigDecimal amount = participation.getAmount();
        if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) {
            if (formation != null && formation.getCost() != null) {
                amount = formation.getCost();
                participation.setAmount(amount);
            } else {
                amount = new BigDecimal("0.00");
                participation.setAmount(amount);
            }
        }
        amountStr = amount.toPlainString() + " TND";
        amountLabel.setText(amountStr);

        if (participation.getPaymentStatus() == null) {
            participation.setPaymentStatus("PENDING");
        }
        statusLabel.setText(participation.getPaymentStatus());
        updatePayButtonLook("Carte bancaire".equals(methodBox.getValue()));
    }

    @FXML
    public void pay() {
        errorLabel.setText("");

        if (participation == null || formation == null || user == null) {
            errorLabel.setText("Contexte de paiement incomplet.");
            return;
        }

        boolean isCard = "Carte bancaire".equals(methodBox.getValue());
        if (isCard && !validateCardForm()) {
            return;
        }

        try {
            String providerName = "MOCK";
            String ref;

            if (isCard) {
                ref = "CARD-" + System.currentTimeMillis();
            } else {
                PaymentProvider provider = Services.payments().getProvider();
                PaymentProvider.CheckoutResult checkout = provider.createCheckout(participation, formation, user);
                ref = checkout.reference();
                if (!provider.verifyPayment(ref)) {
                    statusLabel.setText("ÉCHEC");
                    errorLabel.setText("Le paiement n'a pas été confirmé.");
                    return;
                }
            }

            participation.setPaymentStatus("PAID");
            participation.setPaymentProvider(isCard ? "CARD" : "MOCK");
            participation.setPaymentRef(ref);
            if (participation.getAmount() == null) {
                participation.setAmount(formation.getCost() == null ? new BigDecimal("0.00") : formation.getCost());
            }
            participation.setPaidAt(LocalDateTime.now());

            Services.participations().update(participation);
            statusLabel.setText("PAID");

            boolean emailSent = Services.email().sendParticipationConfirmation(user, formation, participation);

            if (onPaid != null) {
                onPaid.run();
            }

            close();

            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                a.setTitle("Paiement réussi");
                a.setHeaderText(null);
                a.setContentText(emailSent
                        ? "Un email de validation a été envoyé à " + (user.getEmail() != null ? user.getEmail() : "votre adresse") + "."
                        : "Paiement enregistré. Pour recevoir l'email de validation, configurez SMTP (SMTP_HOST, SMTP_USER, SMTP_PASS, SMTP_FROM).");
                a.showAndWait();
            });
        } catch (Exception e) {
            errorLabel.setText(e.getMessage() == null ? "Erreur de paiement." : e.getMessage());
        }
    }

    private boolean validateCardForm() {
        String name = cardHolderNameField == null ? "" : (cardHolderNameField.getText() != null ? cardHolderNameField.getText().trim() : "");
        String num = cardNumberField == null ? "" : (cardNumberField.getText() != null ? cardNumberField.getText().replaceAll("\\s", "") : "");
        String mm = cardMonthField == null ? "" : (cardMonthField.getText() != null ? cardMonthField.getText().trim() : "");
        String aa = cardYearField == null ? "" : (cardYearField.getText() != null ? cardYearField.getText().trim() : "");
        String cvv = cardCvvField == null ? "" : (cardCvvField.getText() != null ? cardCvvField.getText().trim() : "");
        if (name.isEmpty()) {
            errorLabel.setText("Veuillez saisir le nom figurant sur la carte.");
            return false;
        }
        if (!CARD_NUMBER_PATTERN.matcher(num).matches()) {
            errorLabel.setText("Numéro de carte invalide (12 à 19 chiffres).");
            return false;
        }
        if (!mm.matches("0[1-9]|1[0-2]")) {
            errorLabel.setText("Mois d'expiration invalide (01 à 12).");
            return false;
        }
        if (!aa.matches("\\d{2}")) {
            errorLabel.setText("Année d'expiration invalide (format AA, ex. 28).");
            return false;
        }
        if (!CVV_PATTERN.matcher(cvv).matches()) {
            errorLabel.setText("Cryptogramme visuel invalide (3 ou 4 chiffres).");
            return false;
        }
        return true;
    }

    @FXML
    public void cancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) payButton.getScene().getWindow();
        stage.close();
    }
}

