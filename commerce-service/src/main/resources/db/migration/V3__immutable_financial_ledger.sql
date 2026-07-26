CREATE OR REPLACE FUNCTION reject_commerce_ledger_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'commerce_ledger is immutable; append a correcting movement';
END;
$$;

CREATE TRIGGER commerce_ledger_reject_update
BEFORE UPDATE ON commerce_ledger
FOR EACH ROW EXECUTE FUNCTION reject_commerce_ledger_mutation();

CREATE TRIGGER commerce_ledger_reject_delete
BEFORE DELETE ON commerce_ledger
FOR EACH ROW EXECUTE FUNCTION reject_commerce_ledger_mutation();
