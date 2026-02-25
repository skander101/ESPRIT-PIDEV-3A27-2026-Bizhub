package com.bizhub.Investistment.Controllers;

import com.bizhub.Investistment.Entitites.Project;
import com.bizhub.Investistment.Services.AI.AIIntegrationService;
import com.bizhub.Investistment.Services.AI.RecommendationEngine;
import com.bizhub.Investistment.Services.ProjectServiceImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProjectsListController {

    @FXML private VBox projectsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label summaryLabel;
    @FXML private Label emptyLabel;
    @FXML private ScrollPane scrollPane; // AJOUTÉ

    private ProjectServiceImpl projectService;
    private AIIntegrationService aiService;
    private List<Project> allProjects;
    private NumberFormat currencyFormat;
    private DateTimeFormatter dateFormatter;

    private static final int AI_DEMO_USER_ID = 6;
    private Map<Integer, Double> aiScoreByProjectId = new HashMap<>();

    @FXML
    public void initialize() {
        projectService = new ProjectServiceImpl();
        aiService = new AIIntegrationService();
        currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Configuration du ScrollPane
        Platform.runLater(() -> {
            if (scrollPane != null) {
                scrollPane.setFitToWidth(true);

                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            }
        });

        // Initialiser les filtres
        statusFilterComboBox.getItems().addAll(
                "Tous les statuts",
                "pending",
                "funded",
                "in_progress",
                "complete"
        );
        statusFilterComboBox.setValue("Tous les statuts");

        // Listeners pour les filtres
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterProjects());
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> filterProjects());

        loadProjects();
    }

    @FXML
    public void handleAIRecommendations() {
        if (allProjects == null || allProjects.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Recommandations IA", "Aucun projet à analyser.");
            return;
        }

        try {
            List<RecommendationEngine.ProjectRecommendation> recs = aiService.getSmartRecommendations(AI_DEMO_USER_ID);
            aiScoreByProjectId.clear();
            for (RecommendationEngine.ProjectRecommendation rec : recs) {
                if (rec.getProject() != null) {
                    aiScoreByProjectId.put(rec.getProject().getProjectId(), rec.getScore());
                }
            }

            List<Project> sorted = allProjects.stream()
                    .sorted((a, b) -> Double.compare(
                            aiScoreByProjectId.getOrDefault(b.getProjectId(), -1.0),
                            aiScoreByProjectId.getOrDefault(a.getProjectId(), -1.0)
                    ))
                    .collect(Collectors.toList());

            displayProjects(sorted);

            if (aiScoreByProjectId.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Recommandations IA",
                        "Aucune recommandation disponible pour l'utilisateur de démo (ID=" + AI_DEMO_USER_ID + ").");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Recommandations IA",
                        "Les projets ont été triés selon les recommandations IA (utilisateur de démo ID=" + AI_DEMO_USER_ID + ").");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur recommandations IA: " + e.getMessage());
        }
    }

    private void loadProjects() {
        try {
            allProjects = projectService.getAllWithStats();
            
            // Note: Sample project creation disabled to avoid foreign key constraints
            // Users can test AI recommendations with the demo fallback system
            
            displayProjects(allProjects);
            updateSummary();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors du chargement des projets: " + e.getMessage());
        }
    }

    private void displayProjects(List<Project> projects) {
        projectsContainer.getChildren().clear();

        if (projects.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        for (Project project : projects) {
            VBox card = createProjectCard(project);
            projectsContainer.getChildren().add(card);
        }

        // Forcer le rafraîchissement du ScrollPane
        Platform.runLater(() -> {
            if (scrollPane != null) {
                scrollPane.requestLayout();
                scrollPane.layout();
            }
        });
    }

    private VBox createProjectCard(Project project) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);");

        Double aiScore = aiScoreByProjectId.get(project.getProjectId());
        if (aiScore != null) {
            card.setStyle(card.getStyle() + "-fx-border-color: #9c27b0; -fx-border-width: 2; -fx-border-radius: 12;");
        }

        // Header
        HBox header = createCardHeader(project);
        Separator separator1 = new Separator();

        // Barre de progression
        VBox progressSection = createProgressSection(project);
        Separator separator2 = new Separator();

        // Statistiques
        HBox statsSection = createStatsSection(project);
        Separator separator3 = new Separator();

        // Actions
        HBox actionsBox = createActionsBox(project);

        card.getChildren().addAll(header, separator1, progressSection, separator2, statsSection, separator3, actionsBox);

        return card;
    }

    private HBox createCardHeader(Project project) {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(60, 60);
        String statusColor = getStatusColor(project.getStatus());
        iconBox.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 10;");

        String icon = getStatusIcon(project.getStatus());
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");
        iconBox.getChildren().add(iconLabel);

        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(project.getTitle());
        titleLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox idBox = new HBox(10);
        idBox.setAlignment(Pos.CENTER_LEFT);
        Label idIcon = new Label("🆔");
        idIcon.setStyle("-fx-font-size: 13px;");
        Label idLabel = new Label("Projet #" + project.getProjectId());
        idLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        idBox.getChildren().addAll(idIcon, idLabel);

        HBox dateBox = new HBox(10);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        dateIcon.setStyle("-fx-font-size: 13px;");
        Label dateLabel = new Label(project.getCreatedAt() != null ?
                project.getCreatedAt().format(dateFormatter) : "Date inconnue");
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        infoBox.getChildren().addAll(titleLabel, idBox, dateBox);

        VBox statusBox = new VBox(3);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        Label statusTitle = new Label("Statut");
        statusTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        Label statusBadge = new Label(project.getStatus());
        statusBadge.setStyle(getStatusBadgeStyle(project.getStatus()) +
                "-fx-background-radius: 12; -fx-padding: 5 15; -fx-font-size: 12px; -fx-font-weight: bold;");

        statusBox.getChildren().addAll(statusTitle, statusBadge);

        Double aiScore = aiScoreByProjectId.get(project.getProjectId());
        if (aiScore != null) {
            Label aiBadge = new Label(String.format("IA: %.0f%%", aiScore * 100));
            aiBadge.setStyle("-fx-background-color: #f3e5f5; -fx-text-fill: #6a1b9a; " +
                    "-fx-background-radius: 12; -fx-padding: 5 12; -fx-font-size: 12px; -fx-font-weight: bold;");
            statusBox.getChildren().add(aiBadge);
        }

        header.getChildren().addAll(iconBox, infoBox, statusBox);

        return header;
    }

    private VBox createProgressSection(Project project) {
        VBox section = new VBox(8);

        BigDecimal totalInvested = project.getTotalInvested() != null ? project.getTotalInvested() : BigDecimal.ZERO;
        BigDecimal requiredBudget = project.getRequiredBudget();

        double progressPercent = requiredBudget.compareTo(BigDecimal.ZERO) > 0 ?
                totalInvested.divide(requiredBudget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue() : 0;

        HBox progressInfo = new HBox(10);
        progressInfo.setAlignment(Pos.CENTER_LEFT);

        Label progressTitle = new Label("Progression:");
        progressTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

        Label progressLabel = new Label(String.format("%.1f%%", progressPercent));
        progressLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label investedLabel = new Label("Investi: " + currencyFormat.format(totalInvested) + " TND");
        investedLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label budgetLabel = new Label(" / " + currencyFormat.format(requiredBudget) + " TND");
        budgetLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

        progressInfo.getChildren().addAll(progressTitle, progressLabel, spacer, investedLabel, budgetLabel);

        ProgressBar progressBar = new ProgressBar(progressPercent / 100);
        progressBar.setPrefHeight(10);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + getProgressColor(progressPercent) + ";");

        section.getChildren().addAll(progressInfo, progressBar);

        return section;
    }

    private HBox createStatsSection(Project project) {
        HBox stats = new HBox(30);
        stats.setAlignment(Pos.CENTER);

        VBox investmentsStat = new VBox(5);
        investmentsStat.setAlignment(Pos.CENTER);
        Label investmentsIcon = new Label("💰");
        investmentsIcon.setStyle("-fx-font-size: 20px;");
        Label investmentsCount = new Label(String.valueOf(project.getInvestmentsCount()));
        investmentsCount.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label investmentsLabel = new Label("Investissements");
        investmentsLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        investmentsStat.getChildren().addAll(investmentsIcon, investmentsCount, investmentsLabel);

        VBox descStat = new VBox(5);
        descStat.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(descStat, Priority.ALWAYS);

        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            Label descIcon = new Label("📝");
            descIcon.setStyle("-fx-font-size: 20px;");
            Label descPreview = new Label(project.getDescription().length() > 100 ?
                    project.getDescription().substring(0, 100) + "..." : project.getDescription());
            descPreview.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            descPreview.setWrapText(true);
            descStat.getChildren().addAll(descIcon, descPreview);
        }

        stats.getChildren().addAll(investmentsStat, descStat);

        return stats;
    }

    private HBox createActionsBox(Project project) {
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button addInvestmentBtn = new Button("➕ Ajouter Investissement");
        addInvestmentBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15; -fx-cursor: hand;");
        addInvestmentBtn.setOnAction(e -> handleAddInvestment(project));

        Button viewInvestmentsBtn = new Button("👁 Voir Investissements");
        viewInvestmentsBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15; -fx-cursor: hand;");
        viewInvestmentsBtn.setOnAction(e -> handleViewInvestments(project));

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEditProject(project));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-font-size: 12px; -fx-padding: 8 15; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteProject(project));

        actionsBox.getChildren().addAll(addInvestmentBtn, viewInvestmentsBtn, editBtn, deleteBtn);
        return actionsBox;
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/admin-dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) projectsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Tableau de Bord");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleNewProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/AddProjectView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) projectsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Nouveau Projet");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRefresh() {
        loadProjects();
    }

    @FXML
    public void handleResetFilters() {
        searchField.clear();
        statusFilterComboBox.setValue("Tous les statuts");
    }

    private void handleAddInvestment(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/AddInvestmentView.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("❌ ERREUR: Fichier AddInvestmentView.fxml introuvable!");
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Fichier AddInvestmentView.fxml introuvable!");
                return;
            }

            Parent root = loader.load();
            AddInvestmentController controller = loader.getController();
            controller.setPreSelectedProject(project.getProjectId(), project.getTitle());

            Stage stage = (Stage) projectsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Nouvel Investissement - " + project.getTitle());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur de navigation: " + e.getMessage());
        }
    }

    private void handleViewInvestments(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bizhub/fxml/InvestmentsListView.fxml"));
            Parent root = loader.load();

            InvestmentsListController controller = loader.getController();
            controller.filterByProject(project.getProjectId(), project.getTitle());

            Stage stage = (Stage) projectsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Investissements - " + project.getTitle());

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de navigation: " + e.getMessage());
        }
    }

    private void handleEditProject(Project project) {
        Dialog<Project> dialog = new Dialog<>();
        dialog.setTitle("Modifier le projet");
        dialog.setHeaderText("Modifier les informations du projet");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(project.getTitle());
        titleField.setPrefWidth(300);

        TextArea descArea = new TextArea(project.getDescription());
        descArea.setPrefRowCount(3);
        descArea.setPrefWidth(300);

        TextField budgetField = new TextField(project.getRequiredBudget().toString());
        budgetField.setPrefWidth(300);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("pending", "funded", "in_progress", "complete");
        statusBox.setValue(project.getStatus());
        statusBox.setPrefWidth(300);

        grid.add(new Label("Titre:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Budget:"), 0, 2);
        grid.add(budgetField, 1, 2);
        grid.add(new Label("Statut:"), 0, 3);
        grid.add(statusBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    project.setTitle(titleField.getText());
                    project.setDescription(descArea.getText());
                    project.setRequiredBudget(new BigDecimal(budgetField.getText()));
                    project.setStatus(statusBox.getValue());
                    return project;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Budget invalide");
                    return null;
                }
            }
            return null;
        });

        Optional<Project> result = dialog.showAndWait();
        result.ifPresent(updatedProject -> {
            try {
                boolean updated = projectService.update(updatedProject);
                if (updated) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Projet modifié!");
                    loadProjects();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de la modification");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        });
    }

    private void handleDeleteProject(Project project) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer le projet ?");
        confirmAlert.setContentText("Projet: " + project.getTitle() +
                "\n\n⚠️ Tous les investissements associés seront également supprimés!");

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = projectService.delete(project.getProjectId());

                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Projet supprimé!");
                    loadProjects();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Échec de la suppression");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "❌ Erreur: " + e.getMessage());
            }
        }
    }

    public void filterByProject(Integer projectId, String projectTitle) {
        loadProjects();

        if (allProjects != null && projectId != null) {
            List<Project> filtered = allProjects.stream()
                    .filter(p -> p.getProjectId().equals(projectId))
                    .collect(Collectors.toList());

            displayProjects(filtered);
            searchField.setText(projectTitle);
        }
    }

    private void filterProjects() {
        if (allProjects == null) return;

        String searchText = searchField.getText().toLowerCase().trim();
        String selectedStatus = statusFilterComboBox.getValue();

        List<Project> filtered = allProjects.stream()
                .filter(project -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            project.getTitle().toLowerCase().contains(searchText) ||
                            (project.getDescription() != null &&
                                    project.getDescription().toLowerCase().contains(searchText));

                    boolean matchesStatus = selectedStatus.equals("Tous les statuts") ||
                            project.getStatus().equals(selectedStatus);

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());

        displayProjects(filtered);
    }

    private void updateSummary() {
        if (allProjects == null || allProjects.isEmpty()) {
            summaryLabel.setText("Aucun projet");
            return;
        }

        int totalProjects = allProjects.size();
        long fundedProjects = allProjects.stream()
                .filter(p -> "funded".equals(p.getStatus()) || "complete".equals(p.getStatus()))
                .count();
        BigDecimal totalBudget = allProjects.stream()
                .map(Project::getRequiredBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInvested = allProjects.stream()
                .map(p -> p.getTotalInvested() != null ? p.getTotalInvested() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summaryLabel.setText(totalProjects + " projet(s) • " + fundedProjects + " financé(s) • Budget total: " +
                currencyFormat.format(totalBudget) + " TND • Investi: " + currencyFormat.format(totalInvested) + " TND");
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "pending": return "#f39c12";
            case "funded": return "#27ae60";
            case "in_progress": return "#3498db";
            case "complete": return "#2ecc71";
            default: return "#95a5a6";
        }
    }

    private String getStatusIcon(String status) {
        switch (status) {
            case "pending": return "⏳";
            case "funded": return "✅";
            case "in_progress": return "🚧";
            case "complete": return "🎉";
            default: return "📋";
        }
    }

    private String getStatusBadgeStyle(String status) {
        switch (status) {
            case "pending":
                return "-fx-background-color: #fff3cd; -fx-text-fill: #856404; ";
            case "funded":
                return "-fx-background-color: #d4edda; -fx-text-fill: #155724; ";
            case "in_progress":
                return "-fx-background-color: #cce5ff; -fx-text-fill: #004085; ";
            case "complete":
                return "-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460; ";
            default:
                return "-fx-background-color: #e2e3e5; -fx-text-fill: #383d41; ";
        }
    }

    private String getProgressColor(double percent) {
        if (percent >= 100) return "#27ae60";
        if (percent >= 70) return "#3498db";
        if (percent >= 30) return "#f39c12";
        return "#e74c3c";
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}