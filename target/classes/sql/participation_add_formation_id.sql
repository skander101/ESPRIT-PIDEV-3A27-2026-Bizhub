-- Migration: ajouter la colonne formation_id à participation si elle n'existe pas
-- Erreur possible: "Unknown column 'formation_id' in 'field list'"
-- Vérifier d'abord: DESCRIBE participation;
-- Si formation_id n'apparaît pas, exécuter les commandes ci-dessous (une par une si besoin).

ALTER TABLE participation ADD COLUMN formation_id INT NOT NULL DEFAULT 1 AFTER id_candidature;
ALTER TABLE participation ADD KEY idx_participation_formation (formation_id);
ALTER TABLE participation ADD CONSTRAINT participation_ibfk_1 FOREIGN KEY (formation_id) REFERENCES formation(formation_id) ON DELETE CASCADE;
