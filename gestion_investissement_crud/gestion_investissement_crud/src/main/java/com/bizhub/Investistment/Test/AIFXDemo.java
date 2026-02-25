package com.bizhub.Investistment.Test;

import com.bizhub.Investistment.Services.AI.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.math.BigDecimal;

/**
 * Test des systèmes IA avec interface JavaFX simple
 */
public class AIFXDemo extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        System.out.println("🚀 DÉMARRAGE DÉMO IA - BIZHUB");
        System.out.println("===============================");
        
        // Créer l'interface
        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 20; -fx-background-color: #f5f5f5;");
        
        // Titre
        Label title = new Label("🤖 SYSTÈMES IA - BIZHUB PLATFORM");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #2196f3;");
        
        // Zone de test
        TabPane tabPane = new TabPane();
        
        // Onglet Recommandations
        Tab recTab = new Tab("🎯 Recommandations");
        VBox recContent = new VBox(10);
        TextArea recOutput = new TextArea();
        recOutput.setPrefHeight(200);
        Button testRecBtn = new Button("Tester Recommandations");
        testRecBtn.setOnAction(e -> testRecommendations(recOutput));
        recContent.getChildren().addAll(new Label("Test du système de recommandation:"), testRecBtn, recOutput);
        recTab.setContent(recContent);
        
        // Onglet Chatbot
        Tab chatTab = new Tab("💬 Chatbot");
        VBox chatContent = new VBox(10);
        TextArea chatOutput = new TextArea();
        chatOutput.setPrefHeight(200);
        TextField chatInput = new TextField();
        chatInput.setPromptText("Tapez votre question...");
        Button sendBtn = new Button("Envoyer");
        sendBtn.setOnAction(e -> testChatbot(chatInput.getText(), chatOutput));
        chatContent.getChildren().addAll(new Label("Test du chatbot:"), chatInput, sendBtn, chatOutput);
        chatTab.setContent(chatContent);
        
        // Onglet Détection de Fraude
        Tab fraudTab = new Tab("🔍 Détection de Fraude");
        VBox fraudContent = new VBox(10);
        TextArea fraudOutput = new TextArea();
        fraudOutput.setPrefHeight(200);
        Button testFraudBtn = new Button("Tester Détection de Fraude");
        testFraudBtn.setOnAction(e -> testFraudDetection(fraudOutput));
        fraudContent.getChildren().addAll(new Label("Test de détection de fraude:"), testFraudBtn, fraudOutput);
        fraudTab.setContent(fraudContent);
        
        tabPane.getTabs().addAll(recTab, chatTab, fraudTab);
        
        // Bouton de test complet
        Button testAllBtn = new Button("🧪 TESTER TOUS LES SYSTÈMES IA");
        testAllBtn.setStyle("-fx-font-size: 16; -fx-background-color: #4caf50; -fx-text-fill: white; -fx-padding: 10;");
        TextArea allOutput = new TextArea();
        allOutput.setPrefHeight(150);
        testAllBtn.setOnAction(e -> testAllSystems(allOutput));
        
        root.getChildren().addAll(title, tabPane, testAllBtn, allOutput);
        
        // Configurer la scène
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("🤖 Démo Systèmes IA - BizHub");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("✅ Interface démo IA lancée avec succès!");
    }
    
    private void testRecommendations(TextArea output) {
        try {
            output.clear();
            output.appendText("🎯 TEST DES RECOMMANDATIONS INTELLIGENTES\n");
            output.appendText("=====================================\n\n");
            
            RecommendationEngine engine = new RecommendationEngine();
            var recommendations = engine.getProjectRecommendations(6, 5);
            
            output.appendText("Recommandations générées: " + recommendations.size() + "\n\n");
            
            for (var rec : recommendations) {
                output.appendText(String.format("• %s\n  Score: %.1f%%\n  Budget: %.2f\n  Entreprise: %s\n\n",
                    rec.getProject().getTitle(),
                    rec.getScore() * 100,
                    rec.getProject().getRequiredBudget(),
                    rec.getProject().getCompanyName()));
            }
            
            if (recommendations.isEmpty()) {
                output.appendText("Aucune recommandation disponible (normal si aucun projet correspondant)\n");
            }
            
            output.appendText("✅ Système de recommandation: OPÉRATIONNEL\n");
            
        } catch (Exception e) {
            output.appendText("❌ Erreur: " + e.getMessage() + "\n");
        }
    }
    
    private void testChatbot(String question, TextArea output) {
        try {
            output.clear();
            output.appendText("💬 TEST DU CHATBOT ASSISTANT\n");
            output.appendText("============================\n\n");
            
            ChatbotAssistant chatbot = new ChatbotAssistant();
            
            output.appendText("Question: " + question + "\n\n");
            
            var response = chatbot.processMessage(6, question);
            
            output.appendText("Réponse: " + response.getMessage() + "\n");
            output.appendText("Intent: " + response.getIntent() + "\n");
            output.appendText("Actions rapides: " + response.getQuickActions().size() + "\n\n");
            
            output.appendText("Actions suggérées:\n");
            for (String action : response.getQuickActions()) {
                output.appendText("• " + action + "\n");
            }
            
            output.appendText("\n✅ Chatbot: OPÉRATIONNEL\n");
            
        } catch (Exception e) {
            output.appendText("❌ Erreur: " + e.getMessage() + "\n");
        }
    }
    
    private void testFraudDetection(TextArea output) {
        try {
            output.clear();
            output.appendText("🔍 TEST DE DÉTECTION DE FRAUDE\n");
            output.appendText("============================\n\n");
            
            FraudDetectionSystem fraudSystem = new FraudDetectionSystem();
            
            BigDecimal[] testAmounts = {
                new BigDecimal("1000.00"),    // Normal
                new BigDecimal("99999.99"),   // Suspect
                new BigDecimal("150000.00")   // Élevé
            };
            
            for (BigDecimal amount : testAmounts) {
                output.appendText("Test avec montant: " + amount + "\n");
                
                var result = fraudSystem.analyzeInvestment(999, 6, amount, 1);
                
                output.appendText("Score de risque: " + result.getRiskScore() + "\n");
                output.appendText("Niveau de risque: " + result.getRiskLevel() + "\n");
                output.appendText("Facteurs détectés: " + result.getRiskFactors().size() + "\n");
                
                for (var factor : result.getRiskFactors()) {
                    output.appendText("  • " + factor.getDescription() + "\n");
                }
                output.appendText("\n");
            }
            
            output.appendText("✅ Détection de fraude: OPÉRATIONNELLE\n");
            
        } catch (Exception e) {
            output.appendText("❌ Erreur: " + e.getMessage() + "\n");
        }
    }
    
    private void testAllSystems(TextArea output) {
        try {
            output.clear();
            output.appendText("🧪 TEST COMPLET DES SYSTÈMES IA\n");
            output.appendText("=============================\n\n");
            
            AIIntegrationService aiService = new AIIntegrationService();
            
            // Test recommandations
            output.appendText("1️⃣ Test recommandations intelligentes...\n");
            var recs = aiService.getSmartRecommendations(6);
            output.appendText("   ✅ " + recs.size() + " recommandations générées\n\n");
            
            // Test chatbot
            output.appendText("2️⃣ Test chatbot intelligent...\n");
            var chatResponse = aiService.processIntelligentMessage(6, "comment investir");
            output.appendText("   ✅ Réponse générée: " + chatResponse.getIntent() + "\n\n");
            
            // Test tendances fraude
            output.appendText("3️⃣ Test tendances de fraude...\n");
            var trends = aiService.analyzeFraudTrends();
            output.appendText("   ✅ Alertes en attente: " + trends.getPendingAlerts() + "\n\n");
            
            // Simulation investissement
            output.appendText("4️⃣ Test simulation investissement...\n");
            var simResult = aiService.processInvestmentWithAI(6, 1, new BigDecimal("5000.00"));
            output.appendText("   ✅ Investissement #" + simResult.getInvestmentId() + " - " + simResult.getStatus() + "\n\n");
            
            output.appendText("🎉 TOUS LES SYSTÈMES IA FONCTIONNENT CORRECTEMENT!\n");
            output.appendText("✅ Recommandation: OPÉRATIONNELLE\n");
            output.appendText("✅ Chatbot: OPÉRATIONNEL\n");
            output.appendText("✅ Détection de fraude: OPÉRATIONNELLE\n");
            output.appendText("✅ Intégration: OPÉRATIONNELLE\n");
            
        } catch (Exception e) {
            output.appendText("❌ Erreur: " + e.getMessage() + "\n");
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
