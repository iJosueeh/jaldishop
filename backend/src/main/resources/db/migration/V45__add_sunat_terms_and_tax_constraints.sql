-- ==============================================================================
-- JaldiShop Migration V45: SUNAT Terms Acceptance & Tax Compatibility Constraints
-- Version: V45__add_sunat_terms_and_tax_constraints.sql
-- Description:
--   1. Adds sunat_terms_accepted_at TIMESTAMPTZ column to stores.
--   2. Normalizes tax_rate = 0.00 for existing RUS / NONE stores.
--   3. Enforces CHECK constraints for TaxRegime x TaxIdType x can_issue_invoices x tax_rate.
-- ==============================================================================

ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS sunat_terms_accepted_at TIMESTAMPTZ;

UPDATE stores
SET tax_rate = 0.00
WHERE tax_regime IN ('RUS', 'NONE')
  AND (tax_rate IS NULL OR tax_rate <> 0.00);

UPDATE stores
SET can_issue_invoices = false
WHERE tax_regime IN ('RUS', 'NONE')
  AND can_issue_invoices = true;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_stores_tax_regime_doc_compat'
    ) THEN
        ALTER TABLE stores ADD CONSTRAINT chk_stores_tax_regime_doc_compat CHECK (
            (tax_regime IN ('RUS', 'NONE') AND tax_id_type IN ('DNI', 'RUC_10') AND can_issue_invoices = false) OR
            (tax_regime IN ('RER', 'MYPE', 'GENERAL') AND tax_id_type IN ('RUC_10', 'RUC_20'))
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_stores_rus_zero_igv'
    ) THEN
        ALTER TABLE stores ADD CONSTRAINT chk_stores_rus_zero_igv CHECK (
            (tax_regime IN ('RUS', 'NONE') AND (tax_rate IS NULL OR tax_rate = 0.00)) OR
            (tax_regime IN ('RER', 'MYPE', 'GENERAL'))
        );
    END IF;
END $$;
