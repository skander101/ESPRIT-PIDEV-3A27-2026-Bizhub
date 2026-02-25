package com.bizhub.Investistment.Utils;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Utilitaire pour créer les tables IA dans la base de données
 */
public class AIDatabaseSetup {
    
    private static final Logger logger = Logger.getLogger(AIDatabaseSetup.class.getName());
    
    public static void main(String[] args) {
        System.out.println("🔧 CRÉATION DES TABLES IA - BIZHUB");
        System.out.println("===================================");
        
        try {
            setupAITables();
            System.out.println("Tables IA créées avec succès!");
        } catch (Exception e) {
            System.err.println("Erreur creation tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void setupAITables() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("📊 Création des tables pour les systèmes IA...");
            
            // Table chatbot_conversation
            String createChatbotTable = """
                CREATE TABLE IF NOT EXISTS `chatbot_conversation` (
                  `conversation_id` int(11) NOT NULL AUTO_INCREMENT,
                  `user_id` int(11) NOT NULL,
                  `message` text NOT NULL,
                  `message_type` enum('user','bot') NOT NULL,
                  `intent` varchar(50) DEFAULT NULL,
                  `created_at` datetime DEFAULT current_timestamp(),
                  PRIMARY KEY (`conversation_id`),
                  KEY `idx_conversation_user` (`user_id`),
                  KEY `idx_conversation_date` (`created_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """;
            
            stmt.execute(createChatbotTable);
            System.out.println("✅ Table chatbot_conversation créée");
            
            // Table fraud_analysis
            String createFraudAnalysisTable = """
                CREATE TABLE IF NOT EXISTS `fraud_analysis` (
                  `analysis_id` int(11) NOT NULL AUTO_INCREMENT,
                  `investment_id` int(11) NOT NULL,
                  `investor_id` int(11) NOT NULL,
                  `risk_score` decimal(5,2) NOT NULL,
                  `risk_level` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
                  `analysis_date` datetime DEFAULT current_timestamp(),
                  `risk_factors` text DEFAULT NULL,
                  PRIMARY KEY (`analysis_id`),
                  KEY `idx_fraud_analysis_investment` (`investment_id`),
                  KEY `idx_fraud_analysis_investor` (`investor_id`),
                  KEY `idx_fraud_analysis_date` (`analysis_date`),
                  KEY `idx_fraud_analysis_risk_level` (`risk_level`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """;
            
            stmt.execute(createFraudAnalysisTable);
            System.out.println("✅ Table fraud_analysis créée");
            
            // Table fraud_alert
            String createFraudAlertTable = """
                CREATE TABLE IF NOT EXISTS `fraud_alert` (
                  `alert_id` int(11) NOT NULL AUTO_INCREMENT,
                  `investment_id` int(11) NOT NULL,
                  `investor_id` int(11) NOT NULL,
                  `alert_level` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
                  `alert_message` text NOT NULL,
                  `created_at` datetime DEFAULT current_timestamp(),
                  `status` enum('PENDING','REVIEWED','RESOLVED','FALSE_POSITIVE') DEFAULT 'PENDING',
                  PRIMARY KEY (`alert_id`),
                  KEY `idx_fraud_alert_investment` (`investment_id`),
                  KEY `idx_fraud_alert_investor` (`investor_id`),
                  KEY `idx_fraud_alert_status` (`status`),
                  KEY `idx_fraud_alert_date` (`created_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """;
            
            stmt.execute(createFraudAlertTable);
            System.out.println("✅ Table fraud_alert créée");
            
            // Table recommendation_analytics
            String createRecommendationTable = """
                CREATE TABLE IF NOT EXISTS `recommendation_analytics` (
                  `analytics_id` int(11) NOT NULL AUTO_INCREMENT,
                  `user_id` int(11) NOT NULL,
                  `project_id` int(11) NOT NULL,
                  `recommendation_score` decimal(5,4) NOT NULL,
                  `recommended_at` datetime DEFAULT current_timestamp(),
                  `clicked` tinyint(1) DEFAULT 0,
                  `invested` tinyint(1) DEFAULT 0,
                  `investment_amount` decimal(15,2) DEFAULT NULL,
                  `clicked_at` datetime DEFAULT NULL,
                  `invested_at` datetime DEFAULT NULL,
                  PRIMARY KEY (`analytics_id`),
                  KEY `idx_recommendation_user` (`user_id`),
                  KEY `idx_recommendation_project` (`project_id`),
                  KEY `idx_recommendation_date` (`recommended_at`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """;
            
            stmt.execute(createRecommendationTable);
            System.out.println("✅ Table recommendation_analytics créée");
            
            // Ajouter la colonne manquante created_at à la table investment si elle n'existe pas
            try {
                stmt.execute("ALTER TABLE investment ADD COLUMN IF NOT EXISTS created_at datetime DEFAULT current_timestamp()");
                System.out.println(" Colonne created_at ajoutée à la table investment");
            } catch (Exception e) {
                System.out.println("Colonne created_at probablement deja existante");
            }
            
            System.out.println("\n Toutes les tables IA sont prêtes!");
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la création des tables: " + e.getMessage());
            throw e;
        }
    }
}
