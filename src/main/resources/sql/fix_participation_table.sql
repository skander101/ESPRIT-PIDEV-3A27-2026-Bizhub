-- ============================================================
-- Corriger la table participation pour le CRUD BizHub
-- Erreur: "Unknown column 'formation_id' in 'field list'"
-- ============================================================
-- À exécuter dans phpMyAdmin ou MySQL (une commande à la fois).
-- Si une commande échoue (ex: "Duplicate column"), passer à la suivante.
-- ============================================================

-- 1) Si votre colonne s'appelle id_formation au lieu de formation_id, la renommer :
-- ALTER TABLE `participation` CHANGE `id_formation` `formation_id` INT(11) NOT NULL;

-- 2) Si la colonne formation_id n'existe pas du tout, l'ajouter :
ALTER TABLE `participation` ADD COLUMN `formation_id` INT(11) NOT NULL DEFAULT 1;

-- 3) Optionnel : index et clé étrangère (ignorer si déjà présents)
-- ALTER TABLE `participation` ADD KEY `idx_participation_formation` (`formation_id`);
-- ALTER TABLE `participation` ADD CONSTRAINT `participation_ibfk_1` FOREIGN KEY (`formation_id`) REFERENCES `formation` (`formation_id`) ON DELETE CASCADE;

-- 4) S'assurer que id_candidature est la clé primaire et AUTO_INCREMENT (obligatoire pour l'ajout)
-- Si actuellement la PK est user_id, exécuter (après sauvegarde des données si besoin) :
-- ALTER TABLE `participation` DROP PRIMARY KEY;
-- ALTER TABLE `participation` MODIFY `id_candidature` INT(11) NOT NULL AUTO_INCREMENT;
-- ALTER TABLE `participation` ADD PRIMARY KEY (`id_candidature`);
