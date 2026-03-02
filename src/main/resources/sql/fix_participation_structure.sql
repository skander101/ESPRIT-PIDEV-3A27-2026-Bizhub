-- Correction de la structure de la table participation pour le flux paiement
-- À exécuter dans phpMyAdmin (ou en ligne de commande MySQL/MariaDB).
-- Adapté à une table existante avec PK sur user_id et colonnes payment déjà présentes.

-- 1) Supprimer la contrainte de clé primaire actuelle (PK sur user_id)
ALTER TABLE `participation` DROP PRIMARY KEY;

-- 2) Remplir les id_candidature NULL par des valeurs uniques (1, 2, 3...)
-- Si erreur (version SQL trop ancienne), exécuter à la main pour chaque ligne, ex. :
--   UPDATE participation SET id_candidature = 1 WHERE user_id = 6 AND formation_id = 6;
--   UPDATE participation SET id_candidature = 2 WHERE user_id = 7 AND formation_id = 5;
--   UPDATE participation SET id_candidature = 3 WHERE user_id = 8 AND formation_id = 3;
SET @r = 0;
UPDATE `participation` SET `id_candidature` = (@r := @r + 1) WHERE `id_candidature` IS NULL ORDER BY `user_id`, `date_affectation`;

-- 3) Remplir les formation_id NULL si besoin (sinon la ligne suivante échouera)
-- UPDATE `participation` SET `formation_id` = 1 WHERE `formation_id` IS NULL;

-- 4) Déclarer id_candidature comme clé primaire auto-incrémentée
ALTER TABLE `participation`
  MODIFY `id_candidature` INT(11) NOT NULL AUTO_INCREMENT,
  ADD PRIMARY KEY (`id_candidature`);

-- 5) Rendre formation_id obligatoire et ajouter la clé étrangère vers formation
ALTER TABLE `participation`
  MODIFY `formation_id` INT(11) NOT NULL;

ALTER TABLE `participation`
  ADD CONSTRAINT `participation_ibfk_1` FOREIGN KEY (`formation_id`) REFERENCES `formation` (`formation_id`) ON DELETE CASCADE;

-- 6) (Optionnel) Empêcher deux inscriptions identiques (même user, même formation)
-- ALTER TABLE `participation` ADD UNIQUE KEY `uq_participation_formation_user` (`formation_id`, `user_id`);
