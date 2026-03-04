-- Migration: ajouter les colonnes de paiement sur la table participation
-- À exécuter une seule fois sur votre base existante.

ALTER TABLE `participation`
    ADD COLUMN IF NOT EXISTS `payment_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER `remarques`,
    ADD COLUMN IF NOT EXISTS `payment_provider` VARCHAR(30) NULL AFTER `payment_status`,
    ADD COLUMN IF NOT EXISTS `payment_ref` VARCHAR(255) NULL AFTER `payment_provider`,
    ADD COLUMN IF NOT EXISTS `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER `payment_ref`,
    ADD COLUMN IF NOT EXISTS `paid_at` DATETIME NULL AFTER `amount`;

