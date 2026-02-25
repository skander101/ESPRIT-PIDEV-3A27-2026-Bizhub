package com.bizhub.Investistment.Controllers;

import com.bizhub.Investistment.Entitites.Investment;
import com.bizhub.Investistment.Services.InvestmentServiceImpl;
import com.bizhub.Investistment.Services.ExchangeRateService;
import com.bizhub.Investistment.Services.PortfolioAnalyticsService;
import com.bizhub.Investistment.Services.GNewsService;
import com.bizhub.Investistment.Services.AI.FraudDetectionSystem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class InvestmentsListController {

    @FXML private VBox investmentsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Label summaryLabel;
    @FXML private Label emptyLabel;
    @FXML private Label fxRatesLabel;
    @FXML private Label newsSummaryLabel;
    @FXML private Label aiSummaryLabel;
    @FXML private Label portfolioHealthLabel;

    private InvestmentServiceImpl investmentService;
    private List<Investment> allInvestments;
    private NumberFormat currencyFormat;
    private DateTimeFormatter dateFormatter;

    private ExchangeRateService exchangeRateService;
    private PortfolioAnalyticsService portfolioAnalyticsService;
    private GNewsService gNewsService;
    private FraudDetectionSystem fraudDetectionSystem;

    private ExchangeRateService.FxRates fxRates;
    private GNewsService.NewsSnapshot newsSnapshot;

    @FXML
    public void initialize() {
        investmentService = new InvestmentServiceImpl();
        exchangeRateService = new ExchangeRateService();
        portfolioAnalyticsService = new PortfolioAnalyticsService();
        gNewsService = new GNewsService();
        fraudDetectionSystem = new FraudDetectionSystem();
        currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        filterComboBox.getItems().addAll(
                "Tous les statuts",
                "Paiement complété",
                "Paiement en attente",
                "Paiement échoué"
        );
        filterComboBox.setValue("Tous les statuts");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterInvestments());
        filterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> filterInvestments());

        loadInvestments();
    }

    private void loadInvestments() {
        try {
            allInvestments = investmentService.getAll();

            // FX rates (best effort)
            loadFxRates();

            // News & sentiment (best effort)
            loadNewsSnapshot();

            displayInvestments(allInvestments);
            updateSummary();

            updatePortfolioHealth();
            updateAiSummary();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors du chargement des investissements: " + e.getMessage());
        }
    }

    private void loadNewsSnapshot() {
        if (newsSummaryLabel == null) return;
        try {
            String query = null;
            if (allInvestments != null) {
                for (Investment inv : allInvestments) {
                    if (inv != null && inv.getProjectTitle() != null && !inv.getProjectTitle().isBlank()) {
                        query = inv.getProjectTitle().trim();
                        break;
                    }
                }
            }

            if (query == null) {
                newsSummaryLabel.setText("Aucune actu");
                newsSnapshot = null;
                return;
            }

            newsSnapshot = gNewsService.searchProjectNews(query, "fr", 3);
            String sentiment = newsSnapshot.getSentiment() == null ? "UNKNOWN" : newsSnapshot.getSentiment().name();
            int n = newsSnapshot.getArticles() == null ? 0 : newsSnapshot.getArticles().size();
            newsSummaryLabel.setText("Sentiment: " + sentiment + " | Top actus: " + n);
        } catch (Exception e) {
            if (newsSummaryLabel != null) {
                String msg = e.getMessage();
                if (msg == null || msg.isBlank()) {
                    newsSummaryLabel.setText("Actus indisponibles");
                } else {
                    newsSummaryLabel.setText(msg.length() > 80 ? msg.substring(0, 80) + "..." : msg);
                }
            }
            newsSnapshot = null;
        }
    }

    private void loadFxRates() {
        try {
            fxRates = exchangeRateService.getTndRates();
            if (fxRatesLabel != null && fxRates != null) {
                fxRatesLabel.setText("1 TND = " + fxRates.getEur().setScale(4, java.math.RoundingMode.HALF_UP) + " EUR | "
                        + fxRates.getUsd().setScale(4, java.math.RoundingMode.HALF_UP) + " USD");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (fxRatesLabel != null) {
                fxRatesLabel.setText("Taux indisponibles");
            }
            fxRates = null;
        }
    }

    private void updatePortfolioHealth() {
        if (portfolioHealthLabel == null) return;
        try {
            PortfolioAnalyticsService.PortfolioHealth health = portfolioAnalyticsService.compute(allInvestments);
            String diversification;
            if (health.getTopProjectSharePct().compareTo(new java.math.BigDecimal("60")) >= 0) {
                diversification = "Faible";
            } else if (health.getTopProjectSharePct().compareTo(new java.math.BigDecimal("35")) >= 0) {
                diversification = "Moyenne";
            } else {
                diversification = "Bonne";
            }

            portfolioHealthLabel.setText("Top projet: " + health.getTopProjectSharePct() + "% | Top investisseur: "
                    + health.getTopInvestorSharePct() + "% | Diversification: " + diversification);
        } catch (Exception e) {
            e.printStackTrace();
            portfolioHealthLabel.setText("Analyse indisponible");
        }
    }

    private void updateAiSummary() {
        if (aiSummaryLabel == null) return;
        if (allInvestments == null || allInvestments.isEmpty()) {
            aiSummaryLabel.setText("Aucune donnée à analyser");
            return;
        }

        int high = 0;
        int medium = 0;
        int low = 0;

        int sampleLimit = Math.min(allInvestments.size(), 30);
        for (int i = 0; i < sampleLimit; i++) {
            Investment inv = allInvestments.get(i);
            try {
                java.math.BigDecimal amount = inv != null ? inv.getAmount() : null;
                Integer investorId = inv != null ? inv.getInvestorId() : null;
                Integer projectId = inv != null ? inv.getProjectId() : null;
                Integer investmentId = inv != null ? inv.getInvestmentId() : null;
                if (amount == null || investorId == null || projectId == null) continue;

                FraudDetectionSystem.FraudAnalysisResult res = fraudDetectionSystem.analyzeInvestment(
                        investmentId != null ? investmentId : 0,
                        investorId,
                        amount,
                        projectId
                );

                switch (res.getRiskLevel()) {
                    case HIGH:
                    case CRITICAL:
                        high++;
                        break;
                    case MEDIUM:
                        medium++;
                        break;
                    default:
                        low++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        aiSummaryLabel.setText("Échantillon " + sampleLimit + ": Faible " + low + " | Moyen " + medium + " | Élevé " + high);
    }

    public void filterByProject(Integer projectId, String projectTitle) {
        loadInvestments();

        if (allInvestments != null && projectId != null) {
            List<Investment> filtered = allInvestments.stream()
                    .filter(inv -> inv.getProjectId().equals(projectId))
                    .collect(Collectors.toList());

            displayInvestments(filtered);

            if (investmentsContainer.getScene() != null && investmentsContainer.getScene().getWindow() != null) {
                Stage stage = (Stage) investmentsContainer.getScene().getWindow();
                stage.setTitle("Investissements - " + projectTitle);
            }

            searchField.setText(projectTitle);
        }
    }

    private void displayInvestments(List<Investment> investments) {
        if (investmentsContainer == null) {
            System.out.println("[InvestmentsListController] investmentsContainer is NULL (fx:id mismatch or controller not used)");
            return;
        }

        investmentsContainer.getChildren().clear();

        if (investments == null || investments.isEmpty()) {
            if (emptyLabel != null) {
                emptyLabel.setText("Aucun investissement trouvé");
                emptyLabel.setVisible(true);
            }
            return;
        }

        if (emptyLabel != null) {
            emptyLabel.setVisible(false);
        }

        for (Investment investment : investments) {
            try {
                VBox card = createInvestmentCard(investment);
                investmentsContainer.getChildren().add(card);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (investmentsContainer.getChildren().isEmpty() && emptyLabel != null) {
            emptyLabel.setText("Erreur affichage: aucune carte n'a pu être générée (voir console)");
            emptyLabel.setVisible(true);
        }
    }

    private VBox createInvestmentCard(Investment investment) throws SQLException {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);");

        HBox header = createCardHeader(investment);
        Separator separator = new Separator();
        HBox actionsBox = createActionsBox(investment);

        VBox extraBox = createAdvancedInfoBox(investment);

        if (extraBox != null) {
            card.getChildren().addAll(header, extraBox, separator, actionsBox);
        } else {
            card.getChildren().addAll(header, separator, actionsBox);
        }

        return card;
    }

    private VBox createAdvancedInfoBox(Investment investment) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: transparent;");

        // FX conversions
        if (fxRates != null && exchangeRateService != null && investment != null && investment.getAmount() != null) {
            try {
                java.math.BigDecimal eur = exchangeRateService.convert(investment.getAmount(), fxRates.getEur());
                java.math.BigDecimal usd = exchangeRateService.convert(investment.getAmount(), fxRates.getUsd());
                Label fx = new Label("≈ " + (eur != null ? eur : "-") + " EUR | ≈ " + (usd != null ? usd : "-") + " USD");
                fx.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
                box.getChildren().add(fx);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Risk badge
        try {
            if (fraudDetectionSystem != null && investment != null && investment.getAmount() != null
                    && investment.getInvestorId() != null && investment.getProjectId() != null) {
                FraudDetectionSystem.FraudAnalysisResult res = fraudDetectionSystem.analyzeInvestment(
                        investment.getInvestmentId() != null ? investment.getInvestmentId() : 0,
                        investment.getInvestorId(),
                        investment.getAmount(),
                        investment.getProjectId()
                );

                String label = "Risque: " + res.getRiskLevel().name();
                Label risk = new Label(label);

                String color;
                switch (res.getRiskLevel()) {
                    case HIGH:
                    case CRITICAL:
                        color = "#e74c3c";
                        break;
                    case MEDIUM:
                        color = "#f39c12";
                        break;
                    default:
                        color = "#27ae60";
                }
                risk.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
                box.getChildren().add(risk);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // News headline (best effort)
        try {
            if (newsSnapshot != null && newsSnapshot.getArticles() != null && !newsSnapshot.getArticles().isEmpty()) {
                GNewsService.Article a = newsSnapshot.getArticles().get(0);
                String title = a.getTitle() == null ? "" : a.getTitle();
                String source = a.getSourceName() == null ? "" : a.getSourceName();
                String s = "Actu: " + title + (source.isBlank() ? "" : (" (" + source + ")"));
                Label news = new Label(s);
                news.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 12px;");
                box.getChildren().add(news);
            }
        } catch (Exception ignored) {
        }

        return box.getChildren().isEmpty() ? null : box;
    }

    private HBox createCardHeader(Investment investment) {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(60, 60);
        iconBox.setStyle("-fx-background-color: #3498db; -fx-background-radius: 10;");
        Label iconLabel = new Label("💼");
        iconLabel.setStyle("-fx-font-size: 28px;");
        iconBox.getChildren().add(iconLabel);

        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String safeProjectTitle = (investment != null && investment.getProjectTitle() != null && !investment.getProjectTitle().trim().isEmpty())
                ? investment.getProjectTitle()
                : "(Projet inconnu)";
        Label projectLabel = new Label(safeProjectTitle);
        projectLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox investorBox = new HBox(10);
        investorBox.setAlignment(Pos.CENTER_LEFT);
        Label investorIcon = new Label("👤");
        String safeInvestorName = (investment != null && investment.getInvestorName() != null && !investment.getInvestorName().trim().isEmpty())
                ? investment.getInvestorName()
                : "(Investisseur inconnu)";
        Label investorLabel = new Label(safeInvestorName);
        investorLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        investorBox.getChildren().addAll(investorIcon, investorLabel);

        HBox dateBox = new HBox(10);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        String safeDate = (investment != null && investment.getInvestmentDate() != null)
                ? investment.getInvestmentDate().format(dateFormatter)
                : "(Date inconnue)";
        Label dateLabel = new Label(safeDate);
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        infoBox.getChildren().addAll(projectLabel, investorBox, dateBox);

        VBox amountBox = new VBox(3);
        amountBox.setAlignment(Pos.CENTER_RIGHT);
        Label amountTitle = new Label("Montant Total");
        amountTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        BigDecimal safeAmount = (investment != null) ? investment.getAmount() : null;
        String safeAmountText = (safeAmount != null) ? (currencyFormat.format(safeAmount) + " TND") : "0.00 TND";
        Label amountValue = new Label(safeAmountText);
        amountValue.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 22px; -fx-font-weight: bold;");
        amountBox.getChildren().addAll(amountTitle, amountValue);

        header.getChildren().addAll(iconBox, infoBox, amountBox);

        return header;
    }

    private HBox createActionsBox(Investment investment) {
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button addPaymentBtn = new Button("➕ Ajouter Paiement");
        addPaymentBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15;");

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15;");
        editBtn.setOnAction(e -> handleEditInvestment(investment));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15;");
        deleteBtn.setOnAction(e -> handleDeleteInvestment(investment));

        actionsBox.getChildren().addAll(addPaymentBtn, editBtn, deleteBtn);

        return actionsBox;
    }

    private void handleEditInvestment(Investment investment) {
        TextInputDialog dialog = new TextInputDialog(investment.getAmount().toString());
        dialog.setTitle("Modifier l'Investissement");
        dialog.setHeaderText("Modifier le montant de l'investissement");
        dialog.setContentText("Nouveau montant (TND):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            try {
                investment.setAmount(new BigDecimal(amount));
                boolean updated = investmentService.update(investment);

                if (updated) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Investissement modifié!");
                    loadInvestments();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de la modification");
                }
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        });
    }

    private void handleDeleteInvestment(Investment investment) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer l'investissement ?");
        confirmAlert.setContentText("Projet: " + investment.getProjectTitle() + "\n\n⚠️ Tous les paiements seront supprimés!");

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = investmentService.delete(investment.getInvestmentId());

                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Investissement supprimé!");
                    loadInvestments();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleNewInvestment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/AddInvestmentView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) investmentsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Nouvel Investissement");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadInvestments();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        filterComboBox.setValue("Tous les statuts");
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/admin-dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) investmentsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Tableau de Bord Administrateur");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner au tableau de bord: " + e.getMessage());
        }
    }

    private void filterInvestments() {
        if (allInvestments == null) return;

        String searchText = searchField.getText().toLowerCase();

        List<Investment> filtered = allInvestments.stream()
                .filter(inv -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            inv.getProjectTitle().toLowerCase().contains(searchText) ||
                            inv.getInvestorName().toLowerCase().contains(searchText);

                    return matchesSearch ;
                })
                .collect(Collectors.toList());

        displayInvestments(filtered);
    }

    private void updateSummary() {
        if (allInvestments == null || allInvestments.isEmpty()) {
            summaryLabel.setText("Aucun investissement");
            return;
        }

        int totalInvestments = allInvestments.size();
        BigDecimal totalAmount = allInvestments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summaryLabel.setText(totalInvestments + " investissement(s) • Total: " +
                currencyFormat.format(totalAmount) + " TND");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
