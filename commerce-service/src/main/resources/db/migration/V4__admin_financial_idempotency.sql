ALTER TABLE commerce_promotions ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
ALTER TABLE commerce_promotions ADD COLUMN IF NOT EXISTS currency VARCHAR(3);
CREATE UNIQUE INDEX IF NOT EXISTS uk_commerce_promotion_idempotency ON commerce_promotions(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_commerce_promotion_admin ON commerce_promotions(status,partner_id,starts_at,ends_at);
ALTER TABLE commerce_commission_rules ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS uk_commerce_commission_idempotency ON commerce_commission_rules(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_commerce_commission_admin ON commerce_commission_rules(partner_id,product_type,effective_from);
ALTER TABLE commerce_ledger ADD COLUMN IF NOT EXISTS balance_after NUMERIC(19,4);
ALTER TABLE commerce_ledger DISABLE TRIGGER commerce_ledger_reject_update;
WITH balances AS (
  SELECT id,SUM(amount) OVER(PARTITION BY partner_id,currency ORDER BY occurred_at,id) AS running_balance
  FROM commerce_ledger
)
UPDATE commerce_ledger ledger SET balance_after=balances.running_balance
FROM balances WHERE ledger.id=balances.id AND ledger.balance_after IS NULL;
ALTER TABLE commerce_ledger ENABLE TRIGGER commerce_ledger_reject_update;
CREATE OR REPLACE FUNCTION set_commerce_ledger_balance_after()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE current_balance NUMERIC(19,4);
BEGIN
  PERFORM pg_advisory_xact_lock(hashtext(NEW.partner_id || ':' || NEW.currency));
  SELECT balance_after INTO current_balance FROM commerce_ledger
    WHERE partner_id=NEW.partner_id AND currency=NEW.currency
    ORDER BY occurred_at DESC,id DESC LIMIT 1;
  NEW.balance_after=COALESCE(current_balance,0)+NEW.amount;
  RETURN NEW;
END $$;
CREATE TRIGGER commerce_ledger_set_balance_before_insert
BEFORE INSERT ON commerce_ledger FOR EACH ROW EXECUTE FUNCTION set_commerce_ledger_balance_after();
