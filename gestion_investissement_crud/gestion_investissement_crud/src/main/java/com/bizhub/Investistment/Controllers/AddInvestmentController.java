package com.bizhub.Investistment.Controllers;

import com.bizhub.Investistment.Entitites.Investment;
import com.bizhub.Investistment.Entitites.User;
import com.bizhub.Investistment.Services.InvestmentServiceImpl;
import com.bizhub.Investistment.Services.AI.FraudDetectionSystem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class AddInvestmentController {

    @FXML private ComboBox<ProjectItem> projectComboBox;
    @FXML private ComboBox<InvestorItem> investorComboBox;
    @FXML private TextField amountField;
    @FXML private TextField contractUrlField;
    @FXML private Button submitButton;
    @FXML private Label messageLabel;
    @FXML private TextArea aiAnalysisArea;
    @FXML private Label projectErrorLabel;
    @FXML private Label investorErrorLabel;
    @FXML private Label amountErrorLabel;

    private InvestmentServiceImpl investmentService;
    private final FraudDetectionSystem fraudDetectionSystem = new FraudDetectionSystem();

    @FXML
    public void initialize() {
        investmentService = new InvestmentServiceImpl();

        loadProjects();
        loadInvestors();

        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldVal);
            }
        });
    }

    private void loadProjects() {
        try {
            projectComboBox.getItems().addAll(
                    investmentService.getProjects()
            );
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Erreur lors du chargement des projets");
        }
    }

    // Ajoutez cette méthode à AddInvestmentController.java
    public void setPreSelectedProject(Integer projectId, String projectTitle) {
        try {
            // Charger les projets si ce n'est pas déjà fait
            if (projectComboBox.getItems().isEmpty()) {
                loadProjects();
            }

            // Trouver et sélectionner le projet
            for (AddInvestmentController.ProjectItem item : projectComboBox.getItems()) {
                if (item.getId().equals(projectId)) {
                    projectComboBox.setValue(item);
                    break;
                }
            }

            // Optionnel: afficher un message
            System.out.println("Projet pré-sélectionné: " + projectTitle);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInvestors() {
        try {
            // Récupérer les investisseurs depuis la base
            List<User> investors = investmentService.getInvestors();

            for (User u : investors) {
                investorComboBox.getItems().add(
                        new InvestorItem(u.getUserId(), "Investisseur #" + u.getUserId())
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Erreur lors du chargement des investisseurs");
        }
    }

    @FXML
    private void handleAIAnalyze() {
        resetErrors();

        if (!validateForm()) {
            return;
        }

        try {
            int projectId = projectComboBox.getValue().getId();
            int investorId = investorComboBox.getValue().getId();
            BigDecimal amount = new BigDecimal(amountField.getText());

            FraudDetectionSystem.FraudAnalysisResult fraud =
                    fraudDetectionSystem.analyzeInvestment(0, investorId, amount, projectId);

            StringBuilder sb = new StringBuilder();
            sb.append("ANALYSE IA\n");
            sb.append("Risque: ").append(fraud.getRiskLevel()).append("\n");
            sb.append("Score: ").append(fraud.getRiskScore()).append("\n");

            if (fraud.getRiskFactors() != null && !fraud.getRiskFactors().isEmpty()) {
                sb.append("Facteurs:\n");
                for (FraudDetectionSystem.FraudRiskFactor f : fraud.getRiskFactors()) {
                    sb.append("- ").append(f.getDescription()).append("\n");
                }
            }

            if (fraud.getRiskLevel() == FraudDetectionSystem.FraudRiskLevel.CRITICAL) {
                sb.append("\nCONSEIL: Investissement bloqué (risque critique).\n");
            } else if (fraud.isSuspicious()) {
                sb.append("\nCONSEIL: Investissement à vérifier (risque élevé/moyen).\n");
            } else {
                sb.append("\nCONSEIL: Risque faible.\n");
            }

            if (aiAnalysisArea != null) {
                aiAnalysisArea.setText(sb.toString());
            }

        } catch (Exception e) {
            if (aiAnalysisArea != null) {
                aiAnalysisArea.setText("Erreur analyse IA: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSubmit() {
        resetErrors();

        if (!validateForm()) {
            return;
        }

        try {
            int projectId = projectComboBox.getValue().getId();
            int investorId = investorComboBox.getValue().getId();
            BigDecimal amount = new BigDecimal(amountField.getText());

            // Pré-vérification IA (fraude)
            FraudDetectionSystem.FraudAnalysisResult fraud =
                    fraudDetectionSystem.analyzeInvestment(0, investorId, amount, projectId);

            if (fraud.getRiskLevel() == FraudDetectionSystem.FraudRiskLevel.CRITICAL) {
                if (aiAnalysisArea != null) {
                    aiAnalysisArea.setText("ANALYSE IA\nRisque: " + fraud.getRiskLevel() + "\nScore: " + fraud.getRiskScore() +
                            "\n\nInvestissement bloqué: risque critique.");
                }
                showErrorMessage("❌ Investissement bloqué (risque de fraude critique)");
                return;
            }

            Investment investment = new Investment();
            investment.setProjectId(projectId);
            investment.setInvestorId(investorId);
            investment.setAmount(amount);
            investment.setContractUrl(contractUrlField.getText().trim().isEmpty()
                    ? null : contractUrlField.getText().trim());

            int investmentId = investmentService.add(investment);

            if (investmentId > 0) {
                showSuccessMessage("✅ Investissement créé avec succès!");

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() -> {
                            try {
                                navigateToInvestmentsList();
                            } catch (IOException e) {
                                e.printStackTrace();
                                showErrorMessage("Erreur lors de la navigation");
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showErrorMessage("❌ Erreur lors de la création");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("❌ Erreur base de données: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (projectComboBox.getValue() == null) {
            projectErrorLabel.setText("⚠ Veuillez sélectionner un projet");
            projectErrorLabel.setVisible(true);
            isValid = false;
        }

        if (investorComboBox.getValue() == null) {
            investorErrorLabel.setText("⚠ Veuillez sélectionner un investisseur");
            investorErrorLabel.setVisible(true);
            isValid = false;
        }

        if (amountField.getText().trim().isEmpty()) {
            amountErrorLabel.setText("⚠ Le montant est obligatoire");
            amountErrorLabel.setVisible(true);
            isValid = false;
        } else {
            try {
                BigDecimal amount = new BigDecimal(amountField.getText());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    amountErrorLabel.setText("⚠ Le montant doit être supérieur à 0");
                    amountErrorLabel.setVisible(true);
                    isValid = false;
                }

                // Évite les erreurs MySQL 'Out of range value' (colonne amount trop petite)
                // On accepte au maximum 999,999,999,999.99 (12 chiffres + 2 décimales)
                BigDecimal maxAllowed = new BigDecimal("999999999999.99");
                if (amount.compareTo(maxAllowed) > 0) {
                    amountErrorLabel.setText("⚠ Montant trop grand (max: 999999999999.99)");
                    amountErrorLabel.setVisible(true);
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                amountErrorLabel.setText("⚠ Montant invalide");
                amountErrorLabel.setVisible(true);
                isValid = false;
            }
        }

        return isValid;
    }

    private void resetErrors() {
        projectErrorLabel.setVisible(false);
        investorErrorLabel.setVisible(false);
        amountErrorLabel.setVisible(false);
        messageLabel.setVisible(false);
    }

    private void showSuccessMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; " +
                "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;");
        messageLabel.setVisible(true);
    }

    private void showErrorMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24; " +
                "-fx-font-size: 13px; -fx-padding: 10; -fx-background-radius: 5;");
        messageLabel.setVisible(true);
    }

    private void navigateToInvestmentsList() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/InvestmentsListView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Liste des Investissements");
    }

    @FXML
    private void handleCancel() {
        try {
            navigateToInvestmentsList();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Erreur de navigation: " + e.getMessage());
        }
    }

    public static class ProjectItem {
        private final Integer id;
        private final String title;

        public ProjectItem(Integer id, String title) {
            this.id = id;
            this.title = title;
        }

        public Integer getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static class InvestorItem {
        private final Integer id;
        private final String name;

        public InvestorItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}