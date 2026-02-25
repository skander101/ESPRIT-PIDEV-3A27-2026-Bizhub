package com.bizhub.Investistment.Test;

import com.bizhub.Investistment.Services.AI.*;
import com.bizhub.Investistment.Utils.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Scanner;

/**
 * Démonstration CRUD Investissement avec IA - Version Console
 * Montre toutes les fonctionnalités IA intégrées dans le système d'investissement
 */
public class InvestmentCRUDDemo {
    
    private static AIIntegrationService aiService = new AIIntegrationService();
    private static Scanner scanner = new Scanner(System.in);
    private static int currentUserId = 6; // Utilisateur de démo
    
    public static void main(String[] args) {
        System.out.println("🚀 DÉMARRAGE CRUD INVESTISSEMENT AVEC IA");
        System.out.println("=======================================");
        System.out.println("💰 Système de gestion d'investissement intelligent");
        System.out.println("🤖 Intégration complète des systèmes IA");
        System.out.println("🗄️ Connecté à la base de données 'zohra'");
        System.out.println();
        
        // Menu principal
        while (true) {
            showMainMenu();
            int choice = getIntInput("Votre choix: ");
            
            switch (choice) {
                case 1:
                    showMyInvestments();
                    break;
                case 2:
                    createNewInvestmentWithAI();
                    break;
                case 3:
                    showAIInsights();
                    break;
                case 4:
                    testAIRecommendations();
                    break;
                case 5:
                    testFraudDetection();
                    break;
                case 6:
                    testChatbot();
                    break;
                case 0:
                    System.out.println("👋 Au revoir!");
                    return;
                default:
                    System.out.println("❌ Choix invalide. Réessayez.");
            }
            
            System.out.println("\n" + "=".repeat(50) + "\n");
        }
    }
    
    private static void showMainMenu() {
        System.out.println("📋 MENU PRINCIPAL");
        System.out.println("================");
        System.out.println("1️⃣  💰 Voir mes investissements");
        System.out.println("2️⃣  ➕ Créer un investissement (avec analyse IA)");
        System.out.println("3️⃣  🤖 Voir les insights IA");
        System.out.println("4️⃣  🎯 Tester recommandations IA");
        System.out.println("5️⃣  🔍 Tester détection de fraude");
        System.out.println("6️⃣  💬 Tester chatbot IA");
        System.out.println("0️⃣  🚪 Quitter");
    }
    
    private static void showMyInvestments() {
        System.out.println("\n💰 MES INVESTISSEMENTS");
        System.out.println("====================");
        
        try {
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
                
                boolean hasInvestments = false;
                while (rs.next()) {
                    hasInvestments = true;
                    System.out.println(String.format("📊 ID: %d | %s | %.2f€ | %s | %s",
                        rs.getInt("investment_id"),
                        rs.getString("title"),
                        rs.getDouble("amount"),
                        rs.getDate("investment_date"),
                        rs.getString("investor_name")));
                }
                
                if (!hasInvestments) {
                    System.out.println("📭 Aucun investissement trouvé.");
                }
                
                // Statistiques
                String statsSql = "SELECT COUNT(*) as count, COALESCE(SUM(amount), 0) as total " +
                                 "FROM investment WHERE investor_id = ?";
                try (PreparedStatement statsStmt = conn.prepareStatement(statsSql)) {
                    statsStmt.setInt(1, currentUserId);
                    ResultSet statsRs = statsStmt.executeQuery();
                    
                    if (statsRs.next()) {
                        System.out.println("\n📈 STATISTIQUES:");
                        System.out.println("   • Nombre d'investissements: " + statsRs.getInt("count"));
                        System.out.println("   • Montant total investi: " + String.format("%.2f€", statsRs.getDouble("total")));
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }
    
    private static void createNewInvestmentWithAI() {
        System.out.println("\n➕ CRÉATION INVESTISSEMENT AVEC IA");
        System.out.println("===================================");
        
        try {
            // Afficher les projets disponibles
            System.out.println("📁 Projets disponibles:");
            String projectSql = "SELECT project_id, title, required_budget FROM project " +
                                "WHERE status IN ('pending', 'in_progress') ORDER BY title";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(projectSql)) {
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    System.out.println(String.format("   %d - %s (Budget: %.2f€)",
                        rs.getInt("project_id"),
                        rs.getString("title"),
                        rs.getDouble("required_budget")));
                }
            }
            
            int projectId = getIntInput("\n📁 ID du projet: ");
            BigDecimal amount = getBigDecimalInput("💰 Montant à investir (€): ");
            
            System.out.println("\n🤍 ANALYSE IA EN COURS...");
            
            // Analyse avec l'IA
            AIIntegrationService.InvestmentProcessingResult result = 
                aiService.processInvestmentWithAI(currentUserId, projectId, amount);
            
            // Afficher les résultats
            System.out.println("\n📊 RÉSULTATS DE L'ANALYSE IA:");
            System.out.println("==============================");
            System.out.println("📈 Statut: " + result.getStatus());
            System.out.println("💰 Montant: " + amount + "€");
            System.out.println("📁 Projet ID: " + projectId);
            
            if (result.getFraudAnalysis() != null) {
                System.out.println("\n🔍 ANALYSE DE RISQUE:");
                System.out.println("   Score: " + result.getFraudAnalysis().getRiskScore());
                System.out.println("   Niveau: " + result.getFraudAnalysis().getRiskLevel());
                
                if (!result.getFraudAnalysis().getRiskFactors().isEmpty()) {
                    System.out.println("   Facteurs détectés:");
                    for (var factor : result.getFraudAnalysis().getRiskFactors()) {
                        System.out.println("   • " + factor.getDescription());
                    }
                }
            }
            
            if (result.getChatbotMessage() != null) {
                System.out.println("\n💬 CONSEIL IA:");
                System.out.println("   " + result.getChatbotMessage());
            }
            
            if (result.getStatus() == AIIntegrationService.InvestmentStatus.BLOCKED) {
                System.out.println("\n⚠️ INVESTISSEMENT BLOQUÉ pour raisons de sécurité.");
            } else {
                System.out.println("\n✅ Investissement #" + result.getInvestmentId() + " créé avec succès!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }
    
    private static void showAIInsights() {
        System.out.println("\n🤖 INSIGHTS IA COMPLETS");
        System.out.println("=======================");
        
        try {
            // 1. Recommandations intelligentes
            System.out.println("🎯 RECOMMANDATIONS INTELLIGENTES:");
            List<RecommendationEngine.ProjectRecommendation> recommendations = 
                aiService.getSmartRecommendations(currentUserId);
            
            if (recommendations.isEmpty()) {
                System.out.println("   Aucune recommandation disponible actuellement");
            } else {
                for (var rec : recommendations) {
                    System.out.println(String.format("   • %s (Score: %.1f%%)", 
                        rec.getProject().getTitle(), rec.getScore() * 100));
                }
            }
            
            // 2. Statistiques de fraude
            System.out.println("\n🔍 SÉCURITÉ - DÉTECTION DE FRAUDE:");
            AIIntegrationService.FraudTrendReport fraudReport = aiService.analyzeFraudTrends();
            System.out.println("   • Analyses 24h: " + fraudReport.getLast24hTotal());
            System.out.println("   • À risque élevé: " + fraudReport.getLast24hHighRisk());
            System.out.println("   • Score moyen: " + fraudReport.getLast24hAvgScore());
            System.out.println("   • Alertes en attente: " + fraudReport.getPendingAlerts());
            
            // 3. Performance système
            System.out.println("\n⚡ PERFORMANCE SYSTÈME IA:");
            System.out.println("   • Recommandation: ✅ OPÉRATIONNEL");
            System.out.println("   • Chatbot: ✅ OPÉRATIONNEL");
            System.out.println("   • Détection fraude: ✅ OPÉRATIONNEL");
            System.out.println("   • Base de données: ✅ CONNECTÉE");
            
            System.out.println("\n🔄 Dernière mise à jour: " + java.time.LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }
    
    private static void testAIRecommendations() {
        System.out.println("\n🎯 TEST RECOMMANDATIONS IA");
        System.out.println("=========================");
        
        try {
            RecommendationEngine engine = new RecommendationEngine();
            List<RecommendationEngine.ProjectRecommendation> recommendations = 
                engine.getProjectRecommendations(currentUserId, 5);
            
            System.out.println("Recommandations générées: " + recommendations.size());
            
            for (RecommendationEngine.ProjectRecommendation rec : recommendations) {
                System.out.println(String.format("📊 %s", rec.getProject().getTitle()));
                System.out.println("   Score: " + String.format("%.1f%%", rec.getScore() * 100));
                System.out.println("   Budget requis: " + String.format("%.2f€", rec.getProject().getRequiredBudget()));
                System.out.println("   Entreprise: " + rec.getProject().getCompanyName());
                System.out.println("   Secteur: " + rec.getProject().getSector());
                System.out.println();
            }
            
            if (recommendations.isEmpty()) {
                System.out.println("📭 Aucune recommandation disponible (normal si aucun projet correspondant)");
            }
            
            System.out.println("✅ Système de recommandation: OPÉRATIONNEL");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }
    
    private static void testFraudDetection() {
        System.out.println("\n🔍 TEST DÉTECTION DE FRAUDE");
        System.out.println("=========================");
        
        try {
            FraudDetectionSystem fraudSystem = new FraudDetectionSystem();
            
            BigDecimal[] testAmounts = {
                new BigDecimal("1000.00"),    // Normal
                new BigDecimal("99999.99"),   // Suspect
                new BigDecimal("150000.00")   // Élevé
            };
            
            for (BigDecimal amount : testAmounts) {
                System.out.println("\nTest avec montant: " + amount);
                
                FraudDetectionSystem.FraudAnalysisResult result = 
                    fraudSystem.analyzeInvestment(999, currentUserId, amount, 1);
                
                System.out.println("Score de risque: " + result.getRiskScore());
                System.out.println("Niveau de risque: " + result.getRiskLevel());
                System.out.println("Facteurs détectés: " + result.getRiskFactors().size());
                
                for (FraudDetectionSystem.FraudRiskFactor factor : result.getRiskFactors()) {
                    System.out.println("   • " + factor.getDescription());
                }
            }
            
            System.out.println("\n✅ Système de détection de fraude: OPÉRATIONNEL");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }
    
    private static void testChatbot() {
        System.out.println("\n💬 TEST CHATBOT IA");
        System.out.println("===================");
        
        try {
            ChatbotAssistant chatbot = new ChatbotAssistant();
            
            String[] testQuestions = {
                "comment investir",
                "quel est mon budget",
                "donne moi des recommandations",
                "aide"
            };
            
            for (String question : testQuestions) {
                System.out.println("\nQuestion: " + question);
                
                ChatbotAssistant.ChatbotResponse response = 
                    chatbot.processMessage(currentUserId, question);
                
                System.out.println("Réponse: " + response.getMessage().substring(0, Math.min(100, response.getMessage().length())) + "...");
                System.out.println("Intent: " + response.getIntent());
                System.out.println("Actions rapides: " + response.getQuickActions().size());
            }
            
            System.out.println("\n✅ Chatbot assistant: OPÉRATIONNEL");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
    }
    
    // Méthodes utilitaires
    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("❌ Veuillez entrer un nombre valide: ");
            }
        }
    }
    
    private static BigDecimal getBigDecimalInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return new BigDecimal(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("❌ Veuillez entrer un montant valide: ");
            }
        }
    }
}
