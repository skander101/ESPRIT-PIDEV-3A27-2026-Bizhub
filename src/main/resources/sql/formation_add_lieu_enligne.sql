-- Add lieu and en_ligne columns to the formation table
ALTER TABLE formation ADD COLUMN IF NOT EXISTS lieu VARCHAR(255) DEFAULT NULL;
ALTER TABLE formation ADD COLUMN IF NOT EXISTS en_ligne TINYINT(1) DEFAULT 0;

