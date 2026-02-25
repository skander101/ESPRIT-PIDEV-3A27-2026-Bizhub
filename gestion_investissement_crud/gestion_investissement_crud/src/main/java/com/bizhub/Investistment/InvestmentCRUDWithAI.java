package com.bizhub.Investistment;

import com.bizhub.Investistment.Services.AI.*;
import com.bizhub.Investistment.Utils.DatabaseConnection;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Application principale - CRUD Gestion Investissement avec IA
 * Version optimisée avec tous les systèmes d'intelligence artificielle intégrés
 */
public class InvestmentCRUDWithAI extends Application {
    
    private AIIntegrationService aiService;
    private ListView<String> investmentsListView;
    private TextArea aiInsightsArea;
    private int currentUserId = 6; // Utilisateur de démo
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🚀 DÉMARRAGE CRUD INVESTISSEMENT AVEC IA");
        System.out.println("=======================================");
        
        // Initialiser les services IA
        aiService = new AIIntegrationService();
        
        // Créer l'interface principale
        VBox root = createMainInterface();
        
        // Configurer la scène
        Scene scene = new Scene(root, 1200, 800);
        
        primaryStage.setTitle("💰 CRUD Gestion Investissement - Powered by AI");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Charger les données initiales
        loadInvestments();
        updateAIInsights();
        
        System.out.println("✅ Application CRUD Investissement avec IA lancée!");
    }
    
    private VBox createMainInterface() {
        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 20; -fx-background-color: #f8f9fa;");
        
        // Header avec titre et stats IA
        HBox header = createHeader();
        
        // Corps avec tabs
        TabPane tabPane = new TabPane();
        
        // Tab Investissements
        Tab investmentsTab = new Tab("💰 Investissements");
        investmentsTab.setContent(createInvestmentsTab());
        investmentsTab.setClosable(false);
        
        // Tab IA Insights
        Tab aiTab = new Tab("🤖 IA Insights");
        aiTab.setContent(createAITab());
        aiTab.setClosable(false);
        
        // Tab Nouvel Investissement avec IA
        Tab newInvestmentTab = new Tab("➕ Nouvel Investissement IA");
        newInvestmentTab.setContent(createNewInvestmentTab());
        newInvestmentTab.setClosable(false);
        
        tabPane.getTabs().addAll(investmentsTab, aiTab, newInvestmentTab);
        
        root.getChildren().addAll(header, tabPane);
        
        return root;
    }
    
    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10;");
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("💰 CRUD Gestion Investissement");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2196f3;");
        
        Label aiStatus = new Label("🤖 IA: ACTIF");
        aiStatus.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20;");
        
        Label dbStatus = new Label("🗄️ DB: CONNECTÉ");
        dbStatus.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 20;");
        
        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-background-radius: 20;");
        refreshBtn.setOnAction(e -> {
            loadInvestments();
            updateAIInsights();
        });
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(title, spacer, aiStatus, dbStatus, refreshBtn);
        
        return header;
    }
    
    private VBox createInvestmentsTab() {
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");
        
        // Header de la tab
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("📊 Mes Investissements");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        
        Button addBtn = new Button("➕ Ajouter");
        addBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
        
        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        
        header.getChildren().addAll(title, addBtn, deleteBtn);
        
        // Liste des investissements
        investmentsListView = new ListView<>();
        investmentsListView.setPrefHeight(400);
        investmentsListView.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
        
        content.getChildren().addAll(header, investmentsListView);
        
        return content;
    }
    
    private VBox createAITab() {
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");
        
        // Header IA
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("🤖 Insights & Analyse IA");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        
        Button analyzeBtn = new Button("🔍 Analyser");
        analyzeBtn.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white;");
        analyzeBtn.setOnAction(e -> updateAIInsights());
        
        header.getChildren().addAll(title, analyzeBtn);
        
        // Zone d'insights IA
        aiInsightsArea = new TextArea();
        aiInsightsArea.setPrefHeight(400);
        aiInsightsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12; -fx-background-color: #f5f5f5;");
        aiInsightsArea.setEditable(false);
        
        content.getChildren().addAll(header, aiInsightsArea);
        
        return content;
    }
    
    private VBox createNewInvestmentTab() {
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");
        
        // Header
        Label title = new Label("➕ Nouvel Investissement avec Analyse IA");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        
        // Formulaire
        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(10);
        
        // Champ Projet
        form.add(new Label("📁 Projet:"), 0, 0);
        ComboBox<String> projectCombo = new ComboBox<>();
        projectCombo.setPromptText("Sélectionner un projet");
        loadProjects(projectCombo);
        form.add(projectCombo, 1, 0);
        
        // Champ Montant
        form.add(new Label("💰 Montant (€):"), 0, 1);
        TextField amountField = new TextField();
        amountField.setPromptText("1000.00");
        form.add(amountField, 1, 1);
        
        // Zone d'analyse IA
        TextArea aiAnalysis = new TextArea();
        aiAnalysis.setPrefHeight(150);
        aiAnalysis.setPromptText("L'analyse IA apparaîtra ici...");
        aiAnalysis.setEditable(false);
        aiAnalysis.setStyle("-fx-background-color: #e8f5e8;");
        
        // Boutons
        HBox buttons = new HBox(10);
        
        Button analyzeBtn = new Button("🤖 Analyser avec IA");
        analyzeBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white;");
        analyzeBtn.setOnAction(e -> analyzeInvestment(projectCombo, amountField, aiAnalysis));
        
        Button investBtn = new Button("💰 Investir");
        investBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-size: 16; -fx-padding: 10 20;");
        investBtn.setOnAction(e -> createInvestment(projectCombo, amountField, aiAnalysis));
        
        buttons.getChildren().addAll(analyzeBtn, investBtn);
        
        content.getChildren().addAll(title, form, new Label("🤍 Analyse IA:"), aiAnalysis, buttons);
        
        return content;
    }
    
    private void loadInvestments() {
        try {
            investmentsListView.getItems().clear();
            
            String sql = "SELECT i.investment_id, p.title, i.amount, i.investment_date, " +
                        "u.full_name as investor_name " +
                        "FROM investment i " +
                        "JOIN project p ON i.project_id = p.project_id " +
                        "JOIN user u ON i.investor_id = u.user_id " +
                        "WHERE i.investor_id = ? " +
                        "ORDER BY i.investment_date DESC";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, currentUserId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String item = String.format("ID: %d | %s | %.2f€ | %s | %s",
                        rs.getInt("investment_id"),
                        rs.getString("title"),
                        rs.getDouble("amount"),
                        rs.getDate("investment_date"),
                        rs.getString("investor_name"));
                    
                    investmentsListView.getItems().add(item);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur chargement investissements: " + e.getMessage());
        }
    }
    
    private void loadProjects(ComboBox<String> combo) {
        try {
            String sql = "SELECT project_id, title, required_budget FROM project " +
                        "WHERE status IN ('pending', 'in_progress') " +
                        "ORDER BY title";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String item = String.format("%d - %s (Budget: %.2f€)",
                        rs.getInt("project_id"),
                        rs.getString("title"),
                        rs.getDouble("required_budget"));
                    
                    combo.getItems().add(item);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur chargement projets: " + e.getMessage());
        }
    }
    
    private void analyzeInvestment(ComboBox<String> projectCombo, TextField amountField, TextArea analysisArea) {
        try {
            String projectSelection = projectCombo.getValue();
            String amountText = amountField.getText();
            
            if (projectSelection == null || amountText.isEmpty()) {
                analysisArea.setText("❌ Veuillez sélectionner un projet et entrer un montant.");
                return;
            }
            
            // Extraire l'ID du projet
            int projectId = Integer.parseInt(projectSelection.split(" - ")[0]);
            BigDecimal amount = new BigDecimal(amountText);
            
            // Analyse avec l'IA
            AIIntegrationService.InvestmentProcessingResult result = 
                aiService.processInvestmentWithAI(currentUserId, projectId, amount);
            
            // Afficher l'analyse
            StringBuilder analysis = new StringBuilder();
            analysis.append("🤍 ANALYSE IA DE L'INVESTISSEMENT\n");
            analysis.append("=====================================\n\n");
            
            analysis.append("📊 Statut: ").append(result.getStatus()).append("\n");
            analysis.append("💰 Montant: ").append(amount).append("€\n");
            analysis.append("📁 Projet ID: ").append(projectId).append("\n\n");
            
            if (result.getFraudAnalysis() != null) {
                analysis.append("🔍 ANALYSE DE RISQUE:\n");
                analysis.append("Score: ").append(result.getFraudAnalysis().getRiskScore()).append("\n");
                analysis.append("Niveau: ").append(result.getFraudAnalysis().getRiskLevel()).append("\n");
                
                if (!result.getFraudAnalysis().getRiskFactors().isEmpty()) {
                    analysis.append("Facteurs:\n");
                    for (var factor : result.getFraudAnalysis().getRiskFactors()) {
                        analysis.append("• ").append(factor.getDescription()).append("\n");
                    }
                }
                analysis.append("\n");
            }
            
            if (result.getChatbotMessage() != null) {
                analysis.append("💬 CONSEIL IA:\n");
                analysis.append(result.getChatbotMessage()).append("\n\n");
            }
            
            analysis.append("✅ Analyse terminée avec succès!");
            
            analysisArea.setText(analysis.toString());
            
        } catch (Exception e) {
            analysisArea.setText("❌ Erreur lors de l'analyse: " + e.getMessage());
        }
    }
    
    private void createInvestment(ComboBox<String> projectCombo, TextField amountField, TextArea analysisArea) {
        try {
            String projectSelection = projectCombo.getValue();
            String amountText = amountField.getText();
            
            if (projectSelection == null || amountText.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs.");
                return;
            }
            
            int projectId = Integer.parseInt(projectSelection.split(" - ")[0]);
            BigDecimal amount = new BigDecimal(amountText);
            
            // Créer l'investissement avec analyse IA
            AIIntegrationService.InvestmentProcessingResult result = 
                aiService.processInvestmentWithAI(currentUserId, projectId, amount);
            
            if (result.getStatus() == AIIntegrationService.InvestmentStatus.BLOCKED) {
                showAlert(Alert.AlertType.WARNING, "Investissement Bloqué", 
                    "Cet investissement a été bloqué par le système de sécurité pour des raisons de fraude potentielle.");
                return;
            }
            
            // Rafraîchir la liste
            loadInvestments();
            updateAIInsights();
            
            // Vider le formulaire
            projectCombo.setValue(null);
            amountField.clear();
            analysisArea.clear();
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", 
                "Investissement #" + result.getInvestmentId() + " créé avec succès!\n" +
                "Statut: " + result.getStatus());
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la création: " + e.getMessage());
        }
    }
    
    private void updateAIInsights() {
        try {
            StringBuilder insights = new StringBuilder();
            insights.append("🤖 INSIGHTS IA - GESTION INVESTISSEMENT\n");
            insights.append("==========================================\n\n");
            
            // 1. Recommandations intelligentes
            insights.append("🎯 RECOMMANDATIONS INTELLIGENTES:\n");
            List<RecommendationEngine.ProjectRecommendation> recommendations = 
                aiService.getSmartRecommendations(currentUserId);
            
            if (recommendations.isEmpty()) {
                insights.append("   Aucune recommandation disponible actuellement\n");
            } else {
                for (var rec : recommendations) {
                    insights.append(String.format("   • %s (Score: %.1f%%)\n", 
                        rec.getProject().getTitle(), rec.getScore() * 100));
                }
            }
            insights.append("\n");
            
            // 2. Statistiques de fraude
            insights.append("🔍 SÉCURITÉ - DÉTECTION DE FRAUDE:\n");
            AIIntegrationService.FraudTrendReport fraudReport = aiService.analyzeFraudTrends();
            insights.append(String.format("   • Analyses 24h: %d\n", fraudReport.getLast24hTotal()));
            insights.append(String.format("   • À risque élevé: %d\n", fraudReport.getLast24hHighRisk()));
            insights.append(String.format("   • Score moyen: %.2f\n", fraudReport.getLast24hAvgScore()));
            insights.append(String.format("   • Alertes en attente: %d\n", fraudReport.getPendingAlerts()));
            insights.append("\n");
            
            // 3. Mes investissements
            insights.append("💰 MES INVESTISSEMENTS:\n");
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) as count, COALESCE(SUM(amount), 0) as total " +
                     "FROM investment WHERE investor_id = ?")) {
                
                stmt.setInt(1, currentUserId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    insights.append(String.format("   • Nombre d'investissements: %d\n", rs.getInt("count")));
                    insights.append(String.format("   • Montant total investi: %.2f€\n", rs.getDouble("total")));
                }
            }
            insights.append("\n");
            
            // 4. Performance système
            insights.append("⚡ PERFORMANCE SYSTÈME IA:\n");
            insights.append("   • Recommandation: ✅ OPÉRATIONNEL\n");
            insights.append("   • Chatbot: ✅ OPÉRATIONNEL\n");
            insights.append("   • Détection fraude: ✅ OPÉRATIONNEL\n");
            insights.append("   • Base de données: ✅ CONNECTÉE\n");
            insights.append("\n");
            
            insights.append("🔄 Dernière mise à jour: ").append(java.time.LocalDateTime.now().toString());
            
            aiInsightsArea.setText(insights.toString());
            
        } catch (Exception e) {
            aiInsightsArea.setText("❌ Erreur lors de la mise à jour des insights: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
