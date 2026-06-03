-- V1.1.1__BackfillTransactionCategoryFk.sql
--
-- Link existing transactions to the new `category` taxonomy (see 0003).
-- The legacy `bank_transaction.category` free-text column is kept for now;
-- the domain still reads/writes it. This migration only ADDS a FK and
-- backfills it, so nothing breaks. Switching the domain to use category_id
-- is a separate slice (see the `// TODO: convert to category` in FinancialRecord).

-- 1. Add the FK column (nullable: blanks / unmatched stay uncategorized).
ALTER TABLE bank_transaction
    ADD COLUMN category_id INTEGER REFERENCES category(id);

CREATE INDEX idx_transaction_category_id ON bank_transaction(category_id);

-- 2. Seed PET as a new top-level System Category. The legacy data used the
--    custom label 'NICO' (a pet's name); PET is the general, shippable category.
INSERT INTO category (name, type, user_id, parent_id) VALUES
    ('PET', 'EXPENSE', NULL, NULL);

-- 3. Rewrite the legacy free-text label 'NICO' -> 'PET' so it lines up with the
--    new taxonomy and the classifier/search (which still read the text column).
UPDATE bank_transaction
SET category = 'PET'
WHERE upper(trim(category)) = 'NICO';

-- 4. Backfill category_id by matching the legacy text against System Category
--    names. Names are unique in `category`, so the match is deterministic and
--    may resolve to either a top-level Category or a Subcategory.
UPDATE bank_transaction bt
SET category_id = c.id
FROM category c
WHERE c.user_id IS NULL
  AND bt.category IS NOT NULL
  AND upper(trim(bt.category)) = c.name;

-- Remaining unmatched rows keep category_id = NULL (uncategorized): only the
-- blank / empty-string transactions, which are re-categorized from the UI.