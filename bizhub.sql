-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : sam. 07 fév. 2026 à 23:42
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `bizhub`
--

-- --------------------------------------------------------

--
-- Structure de la table `application`
--

CREATE TABLE `application` (
  `application_id` int(11) NOT NULL,
  `request_id` int(11) NOT NULL,
  `trainer_id` int(11) NOT NULL,
  `assigned_at` datetime DEFAULT current_timestamp(),
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `avis`
--

CREATE TABLE `avis` (
  `avis_id` int(11) NOT NULL,
  `reviewer_id` int(11) NOT NULL,
  `formation_id` int(11) NOT NULL,
  `rating` int(11) DEFAULT NULL CHECK (`rating` >= 1 and `rating` <= 5),
  `comment` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `is_verified` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `avis`
--

INSERT INTO `avis` (`avis_id`, `reviewer_id`, `formation_id`, `rating`, `comment`, `created_at`, `is_verified`) VALUES
(2, 8, 2, 4, 'better but still too serious', '2026-02-06 05:52:06', 0),
(3, 8, 3, 3, 'lol', '2026-02-07 18:43:14', 0);

-- --------------------------------------------------------

--
-- Structure de la table `avis_produit`
--

CREATE TABLE `avis_produit` (
  `avis_produit_id` int(11) NOT NULL,
  `buyer_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `rating` int(11) DEFAULT NULL CHECK (`rating` >= 1 and `rating` <= 5),
  `comment` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `is_verified` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `commentaire`
--

CREATE TABLE `commentaire` (
  `comment_id` int(11) NOT NULL,
  `post_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `formation`
--

CREATE TABLE `formation` (
  `formation_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `trainer_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `cost` decimal(10,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `formation`
--

INSERT INTO `formation` (`formation_id`, `title`, `description`, `trainer_id`, `start_date`, `end_date`, `cost`) VALUES
(2, 'TestFormation', 'Plz work i\'m tired of you', 7, '2026-02-06', '2026-02-07', 50.00),
(3, 'f2', 'lol', 7, '2026-02-09', '2026-02-12', 758.00);

-- --------------------------------------------------------

--
-- Structure de la table `investment`
--

CREATE TABLE `investment` (
  `investment_id` int(11) NOT NULL,
  `project_id` int(11) NOT NULL,
  `investor_id` int(11) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `investment_date` datetime DEFAULT current_timestamp(),
  `contract_url` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `investment`
--

INSERT INTO `investment` (`investment_id`, `project_id`, `investor_id`, `amount`, `investment_date`, `contract_url`) VALUES
(8, 1, 8, 5000.00, '2026-02-07 22:27:17', 'dazd'),
(9, 1, 7, 5000.00, '2026-02-07 22:31:19', '4kj'),
(10, 1, 6, 4000.00, '2026-02-07 22:35:34', 'dzaeza'),
(11, 1, 7, 4100.00, '2026-02-07 22:40:19', 'ddsfsd'),
(12, 1, 6, 5222.00, '2026-02-07 22:41:32', ';nb,b'),
(13, 1, 6, 4111.00, '2026-02-07 22:48:40', 'n'),
(14, 1, 7, 744.00, '2026-02-07 22:49:43', 'dez'),
(15, 1, 6, 522.00, '2026-02-07 22:53:48', 'ddz'),
(16, 1, 6, 50000.00, '2026-02-07 23:22:25', 'dzadaz'),
(17, 1, 6, 52525.00, '2026-02-07 23:25:12', 'dzadaz'),
(18, 1, 7, 8888.00, '2026-02-07 23:26:58', NULL),
(19, 1, 7, 6545.00, '2026-02-07 23:30:38', 'dzadazd'),
(20, 1, 6, 5000.00, '2026-02-07 23:41:23', 'cqscsq');

-- --------------------------------------------------------

--
-- Structure de la table `order`
--

CREATE TABLE `order` (
  `order_id` int(11) NOT NULL,
  `buyer_id` int(11) NOT NULL,
  `product_id` int(11) NOT NULL,
  `quantity` int(11) DEFAULT 1,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `total_price` decimal(10,2) GENERATED ALWAYS AS (`quantity` * `unit_price`) STORED,
  `order_date` datetime DEFAULT current_timestamp(),
  `delivery_address` varchar(255) DEFAULT NULL,
  `status` enum('pending','confirmed','shipped','delivered','cancelled') DEFAULT 'pending'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `payment`
--

CREATE TABLE `payment` (
  `payment_id` int(11) NOT NULL,
  `investment_id` int(11) NOT NULL,
  `amount` decimal(15,0) NOT NULL,
  `payment_date` datetime NOT NULL DEFAULT current_timestamp(),
  `payment_method` varchar(100) NOT NULL,
  `payment_status` varchar(50) NOT NULL,
  `transaction_reference` varchar(255) NOT NULL,
  `notes` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `payment`
--

INSERT INTO `payment` (`payment_id`, `investment_id`, `amount`, `payment_date`, `payment_method`, `payment_status`, `transaction_reference`, `notes`) VALUES
(1, 12, 241, '2026-02-07 22:41:46', 'Chèque', 'pending', '6546465', 'nb;'),
(2, 15, 32, '2026-02-07 22:53:57', 'Virement bancaire', 'completed', 'dze', 'dze'),
(3, 18, 58, '2026-02-07 23:27:10', 'Virement bancaire', 'completed', 'jhkgjh', 'bhvg');

-- --------------------------------------------------------

--
-- Structure de la table `post`
--

CREATE TABLE `post` (
  `post_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `category` varchar(100) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `product_service`
--

CREATE TABLE `product_service` (
  `product_id` int(11) NOT NULL,
  `fournisseur_id` int(11) NOT NULL,
  `name` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `category` varchar(100) DEFAULT NULL,
  `stock_quantity` int(11) DEFAULT 0,
  `is_available` tinyint(1) DEFAULT 1,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `project`
--

CREATE TABLE `project` (
  `project_id` int(11) NOT NULL,
  `startup_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `required_budget` decimal(15,2) NOT NULL,
  `status` enum('pending','funded','in_progress','completed') DEFAULT 'pending',
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `project`
--

INSERT INTO `project` (`project_id`, `startup_id`, `title`, `description`, `required_budget`, `status`, `created_at`) VALUES
(1, 6, 'hekzrje', 'dezdez', 15.20, 'funded', '2026-02-11 21:04:33');

-- --------------------------------------------------------

--
-- Structure de la table `training_request`
--

CREATE TABLE `training_request` (
  `request_id` int(11) NOT NULL,
  `startup_id` int(11) NOT NULL,
  `formation_id` int(11) NOT NULL,
  `request_date` datetime DEFAULT current_timestamp(),
  `status` enum('pending','accepted','rejected','completed') DEFAULT 'pending'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `user`
--

CREATE TABLE `user` (
  `user_id` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `user_type` enum('startup','fournisseur','formateur','investisseur','admin') NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `is_active` tinyint(1) DEFAULT 1,
  `full_name` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `bio` text DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `company_name` varchar(100) DEFAULT NULL,
  `sector` varchar(100) DEFAULT NULL,
  `company_description` text DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `founding_date` date DEFAULT NULL,
  `business_type` varchar(100) DEFAULT NULL,
  `delivery_zones` text DEFAULT NULL,
  `payment_methods` varchar(255) DEFAULT NULL,
  `return_policy` text DEFAULT NULL,
  `investment_sector` varchar(100) DEFAULT NULL,
  `max_budget` decimal(15,2) DEFAULT NULL,
  `years_experience` int(11) DEFAULT NULL,
  `represented_company` varchar(100) DEFAULT NULL,
  `specialty` varchar(100) DEFAULT NULL,
  `hourly_rate` decimal(10,2) DEFAULT NULL,
  `availability` text DEFAULT NULL,
  `cv_url` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user`
--

INSERT INTO `user` (`user_id`, `email`, `password_hash`, `user_type`, `created_at`, `is_active`, `full_name`, `phone`, `address`, `bio`, `avatar_url`, `company_name`, `sector`, `company_description`, `website`, `founding_date`, `business_type`, `delivery_zones`, `payment_methods`, `return_policy`, `investment_sector`, `max_budget`, `years_experience`, `represented_company`, `specialty`, `hourly_rate`, `availability`, `cv_url`) VALUES
(6, 'TestAdmin@gmail.com', '$2a$12$V9CASbvSMt.auCGAmcEz/uVfWV4jN4NFqZM99n8dZy9x6ix9Aus02', 'investisseur', '2026-02-06 05:19:11', 1, 'jeremy', '23456789', NULL, NULL, 'com/bizhub/images/avatars/8ef86e74-d02d-4081-87dc-25d2040b8bf6.png', 'AstroWorld', 'Informatics', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(7, 'TestFormateur@gmail.com', '$2a$12$Vvk/.eHr9tPmH6AtYG10neYwxbh9MmyvN8/0f7QTvZ5.BQar3LX0q', 'investisseur', '2026-02-06 05:26:11', 1, 'BlueWall', '12345678', NULL, NULL, 'com/bizhub/images/avatars/ca10dcf0-e18e-439a-8b6f-a813b23f65cf.jpeg', 'My dd', 'Baby Oil', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(8, 'TestStartup@gmail.com', '$2a$12$UKZh0JBfN.1RQ/g7RZbAVOKU9gBy.FwiRCFlzQJ.6BW2uF58gnB.a', 'investisseur', '2026-02-06 05:30:20', 1, 'skandrr', '12345678', NULL, NULL, 'com/bizhub/images/avatars/80d7668a-949e-4966-af14-756dd9374e73.png', 'Dar Baba', 'Shanghai', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(9, 'Test1@gmail.com', '$2a$12$bvvRVl71HXL5T5y7J6/52.OUGvpT07JDV5U4Y7QPjSGS9awWBXHua', 'investisseur', '2026-02-07 19:21:13', 1, 'skandoura', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Java', 20.00, NULL, NULL),
(10, 'test3@gmail.com', '$2a$12$1S7CUW.6wRB6rKRS53zsNO0s/Xdy7Qu0SZGCnJdfvqJs4208cMIqq', 'investisseur', '2026-02-07 19:22:33', 1, 'skannn', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'FinTech', 50000.00, NULL, NULL, NULL, NULL, NULL, NULL);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `application`
--
ALTER TABLE `application`
  ADD PRIMARY KEY (`application_id`),
  ADD KEY `idx_application_request` (`request_id`),
  ADD KEY `idx_application_trainer` (`trainer_id`);

--
-- Index pour la table `avis`
--
ALTER TABLE `avis`
  ADD PRIMARY KEY (`avis_id`),
  ADD UNIQUE KEY `unique_user_formation` (`reviewer_id`,`formation_id`),
  ADD KEY `idx_avis_reviewer` (`reviewer_id`),
  ADD KEY `idx_avis_formation` (`formation_id`),
  ADD KEY `idx_avis_rating` (`rating`);

--
-- Index pour la table `avis_produit`
--
ALTER TABLE `avis_produit`
  ADD PRIMARY KEY (`avis_produit_id`),
  ADD UNIQUE KEY `unique_buyer_product` (`buyer_id`,`product_id`),
  ADD KEY `idx_avis_produit_buyer` (`buyer_id`),
  ADD KEY `idx_avis_produit_product` (`product_id`),
  ADD KEY `idx_avis_produit_rating` (`rating`);

--
-- Index pour la table `commentaire`
--
ALTER TABLE `commentaire`
  ADD PRIMARY KEY (`comment_id`),
  ADD KEY `idx_comment_post` (`post_id`),
  ADD KEY `idx_comment_user` (`user_id`),
  ADD KEY `idx_comment_created` (`created_at`);

--
-- Index pour la table `formation`
--
ALTER TABLE `formation`
  ADD PRIMARY KEY (`formation_id`),
  ADD KEY `idx_formation_trainer` (`trainer_id`),
  ADD KEY `idx_formation_dates` (`start_date`,`end_date`);

--
-- Index pour la table `investment`
--
ALTER TABLE `investment`
  ADD PRIMARY KEY (`investment_id`),
  ADD KEY `idx_investment_project` (`project_id`),
  ADD KEY `idx_investment_investor` (`investor_id`);

--
-- Index pour la table `order`
--
ALTER TABLE `order`
  ADD PRIMARY KEY (`order_id`),
  ADD KEY `buyer_id` (`buyer_id`),
  ADD KEY `product_id` (`product_id`);

--
-- Index pour la table `payment`
--
ALTER TABLE `payment`
  ADD PRIMARY KEY (`payment_id`),
  ADD KEY `investment_id` (`investment_id`);

--
-- Index pour la table `post`
--
ALTER TABLE `post`
  ADD PRIMARY KEY (`post_id`),
  ADD KEY `idx_post_user` (`user_id`),
  ADD KEY `idx_post_category` (`category`),
  ADD KEY `idx_post_created` (`created_at`);

--
-- Index pour la table `product_service`
--
ALTER TABLE `product_service`
  ADD PRIMARY KEY (`product_id`),
  ADD KEY `idx_product_fournisseur` (`fournisseur_id`),
  ADD KEY `idx_product_category` (`category`),
  ADD KEY `idx_product_price` (`price`),
  ADD KEY `idx_product_available` (`is_available`),
  ADD KEY `idx_product_stock` (`stock_quantity`);

--
-- Index pour la table `project`
--
ALTER TABLE `project`
  ADD PRIMARY KEY (`project_id`),
  ADD KEY `idx_project_startup` (`startup_id`),
  ADD KEY `idx_project_status` (`status`),
  ADD KEY `idx_project_budget` (`required_budget`);

--
-- Index pour la table `training_request`
--
ALTER TABLE `training_request`
  ADD PRIMARY KEY (`request_id`),
  ADD KEY `idx_request_startup` (`startup_id`),
  ADD KEY `idx_request_formation` (`formation_id`),
  ADD KEY `idx_request_status` (`status`);

--
-- Index pour la table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_user_email` (`email`),
  ADD KEY `idx_user_type` (`user_type`),
  ADD KEY `idx_user_company` (`company_name`),
  ADD KEY `idx_user_sector` (`sector`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `application`
--
ALTER TABLE `application`
  MODIFY `application_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `avis`
--
ALTER TABLE `avis`
  MODIFY `avis_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `avis_produit`
--
ALTER TABLE `avis_produit`
  MODIFY `avis_produit_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `commentaire`
--
ALTER TABLE `commentaire`
  MODIFY `comment_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `formation`
--
ALTER TABLE `formation`
  MODIFY `formation_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `investment`
--
ALTER TABLE `investment`
  MODIFY `investment_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT pour la table `order`
--
ALTER TABLE `order`
  MODIFY `order_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `payment`
--
ALTER TABLE `payment`
  MODIFY `payment_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `post`
--
ALTER TABLE `post`
  MODIFY `post_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `product_service`
--
ALTER TABLE `product_service`
  MODIFY `product_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `project`
--
ALTER TABLE `project`
  MODIFY `project_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `training_request`
--
ALTER TABLE `training_request`
  MODIFY `request_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `user`
--
ALTER TABLE `user`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `application`
--
ALTER TABLE `application`
  ADD CONSTRAINT `application_ibfk_1` FOREIGN KEY (`request_id`) REFERENCES `training_request` (`request_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `application_ibfk_2` FOREIGN KEY (`trainer_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `avis`
--
ALTER TABLE `avis`
  ADD CONSTRAINT `avis_ibfk_1` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `avis_ibfk_2` FOREIGN KEY (`formation_id`) REFERENCES `formation` (`formation_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `avis_produit`
--
ALTER TABLE `avis_produit`
  ADD CONSTRAINT `avis_produit_ibfk_1` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `avis_produit_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `product_service` (`product_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `commentaire`
--
ALTER TABLE `commentaire`
  ADD CONSTRAINT `commentaire_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `commentaire_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `formation`
--
ALTER TABLE `formation`
  ADD CONSTRAINT `formation_ibfk_1` FOREIGN KEY (`trainer_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `investment`
--
ALTER TABLE `investment`
  ADD CONSTRAINT `investment_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `investment_ibfk_2` FOREIGN KEY (`investor_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `order`
--
ALTER TABLE `order`
  ADD CONSTRAINT `order_ibfk_1` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `order_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `product_service` (`product_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `post`
--
ALTER TABLE `post`
  ADD CONSTRAINT `post_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `product_service`
--
ALTER TABLE `product_service`
  ADD CONSTRAINT `product_service_ibfk_1` FOREIGN KEY (`fournisseur_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `project`
--
ALTER TABLE `project`
  ADD CONSTRAINT `project_ibfk_1` FOREIGN KEY (`startup_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `training_request`
--
ALTER TABLE `training_request`
  ADD CONSTRAINT `training_request_ibfk_1` FOREIGN KEY (`startup_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `training_request_ibfk_2` FOREIGN KEY (`formation_id`) REFERENCES `formation` (`formation_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
