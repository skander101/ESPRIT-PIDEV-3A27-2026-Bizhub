-- Tables pour le système d'IA et détection de fraude
-- Exécuter ces requêtes dans la base de données 'zohra'

-- Table pour enregistrer les conversations du chatbot
CREATE TABLE IF NOT EXISTS `chatbot_conversation` (
  `conversation_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `message_type` enum('user','bot') NOT NULL,
  `intent` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`conversation_id`),
  KEY `idx_conversation_user` (`user_id`),
  KEY `idx_conversation_date` (`created_at`),
  CONSTRAINT `chatbot_conversation_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table pour analyser les recommandations et leur performance
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
  KEY `idx_recommendation_date` (`recommended_at`),
  CONSTRAINT `recommendation_analytics_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `recommendation_analytics_ibfk_2` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table pour enregistrer les analyses de fraude
CREATE TABLE IF NOT EXISTS `fraud_analysis` (
  `analysis_id` int(11) NOT NULL AUTO_INCREMENT,
  `investment_id` int(11) NOT NULL,
  `investor_id` int(11) NOT NULL,
  `risk_score` decimal(5,2) NOT NULL,
  `risk_level` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  `analysis_date` datetime DEFAULT current_timestamp(),
  `risk_factors` text DEFAULT NULL,
  `reviewed_by` int(11) DEFAULT NULL,
  `review_date` datetime DEFAULT NULL,
  `review_notes` text DEFAULT NULL,
  PRIMARY KEY (`analysis_id`),
  KEY `idx_fraud_analysis_investment` (`investment_id`),
  KEY `idx_fraud_analysis_investor` (`investor_id`),
  KEY `idx_fraud_analysis_date` (`analysis_date`),
  KEY `idx_fraud_analysis_risk_level` (`risk_level`),
  CONSTRAINT `fraud_analysis_ibfk_1` FOREIGN KEY (`investment_id`) REFERENCES `investment` (`investment_id`) ON DELETE CASCADE,
  CONSTRAINT `fraud_analysis_ibfk_2` FOREIGN KEY (`investor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fraud_analysis_ibfk_3` FOREIGN KEY (`reviewed_by`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table pour les alertes de fraude
CREATE TABLE IF NOT EXISTS `fraud_alert` (
  `alert_id` int(11) NOT NULL AUTO_INCREMENT,
  `investment_id` int(11) NOT NULL,
  `investor_id` int(11) NOT NULL,
  `alert_level` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  `alert_message` text NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `status` enum('PENDING','REVIEWED','RESOLVED','FALSE_POSITIVE') DEFAULT 'PENDING',
  `reviewed_by` int(11) DEFAULT NULL,
  `review_date` datetime DEFAULT NULL,
  `resolution_notes` text DEFAULT NULL,
  PRIMARY KEY (`alert_id`),
  KEY `idx_fraud_alert_investment` (`investment_id`),
  KEY `idx_fraud_alert_investor` (`investor_id`),
  KEY `idx_fraud_alert_status` (`status`),
  KEY `idx_fraud_alert_date` (`created_at`),
  CONSTRAINT `fraud_alert_ibfk_1` FOREIGN KEY (`investment_id`) REFERENCES `investment` (`investment_id`) ON DELETE CASCADE,
  CONSTRAINT `fraud_alert_ibfk_2` FOREIGN KEY (`investor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fraud_alert_ibfk_3` FOREIGN KEY (`reviewed_by`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table pour les patterns d'apprentissage du système de recommandation
CREATE TABLE IF NOT EXISTS `user_behavior_patterns` (
  `pattern_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `pattern_type` varchar(50) NOT NULL,
  `pattern_data` json DEFAULT NULL,
  `confidence_score` decimal(5,4) DEFAULT 0.0000,
  `created_at` datetime DEFAULT current_timestamp(),
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`pattern_id`),
  KEY `idx_behavior_user` (`user_id`),
  KEY `idx_behavior_type` (`pattern_type`),
  KEY `idx_behavior_confidence` (`confidence_score`),
  CONSTRAINT `user_behavior_patterns_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Vue pour les statistiques de détection de fraude
CREATE OR REPLACE VIEW `fraud_statistics` AS
SELECT 
    DATE(created_at) as analysis_date,
    risk_level,
    COUNT(*) as total_analyses,
    AVG(risk_score) as avg_risk_score,
    SUM(CASE WHEN risk_level IN ('HIGH', 'CRITICAL') THEN 1 ELSE 0 END) as high_risk_count
FROM fraud_analysis 
GROUP BY DATE(created_at), risk_level
ORDER BY analysis_date DESC;

-- Vue pour les performances du chatbot
CREATE OR REPLACE VIEW `chatbot_performance` AS
SELECT 
    DATE(created_at) as conversation_date,
    message_type,
    COUNT(*) as message_count,
    COUNT(DISTINCT user_id) as unique_users
FROM chatbot_conversation 
GROUP BY DATE(created_at), message_type
ORDER BY conversation_date DESC;

-- Vue pour l'efficacité des recommandations
CREATE OR REPLACE VIEW `recommendation_effectiveness` AS
SELECT 
    DATE(recommended_at) as recommendation_date,
    COUNT(*) as total_recommendations,
    SUM(clicked) as total_clicks,
    SUM(invested) as total_investments,
    AVG(investment_amount) as avg_investment_amount,
    (SUM(invested) / COUNT(*)) * 100 as conversion_rate
FROM recommendation_analytics 
GROUP BY DATE(recommended_at)
ORDER BY recommendation_date DESC;
