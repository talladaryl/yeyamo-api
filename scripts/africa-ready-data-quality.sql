-- Run this PostgreSQL script once per service database before and after an
-- Africa-ready backfill. It is read-only with respect to business tables.
-- It creates a small, append-only measurement table in the target database.

CREATE TABLE IF NOT EXISTS africa_ready_migration_measurements (
    id BIGSERIAL PRIMARY KEY,
    measured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    table_schema TEXT NOT NULL,
    table_name TEXT NOT NULL,
    metric TEXT NOT NULL,
    metric_value BIGINT NOT NULL,
    details TEXT,
    UNIQUE (measured_at, table_schema, table_name, metric)
);

DO $$
DECLARE
    relation RECORD;
    total_rows BIGINT;
    null_countries BIGINT;
    invalid_countries BIGINT;
BEGIN
    FOR relation IN
        SELECT table_schema, table_name
        FROM information_schema.columns
        WHERE column_name = 'country_code'
          AND table_schema NOT IN ('pg_catalog', 'information_schema')
        GROUP BY table_schema, table_name
    LOOP
        EXECUTE format('SELECT count(*) FROM %I.%I', relation.table_schema, relation.table_name)
            INTO total_rows;
        EXECUTE format('SELECT count(*) FROM %I.%I WHERE country_code IS NULL', relation.table_schema, relation.table_name)
            INTO null_countries;
        EXECUTE format($q$SELECT count(*) FROM %I.%I
                        WHERE country_code IS NOT NULL AND country_code !~ '^[A-Z]{2}$'$q$,
                       relation.table_schema, relation.table_name)
            INTO invalid_countries;

        INSERT INTO africa_ready_migration_measurements(table_schema, table_name, metric, metric_value)
        VALUES
            (relation.table_schema, relation.table_name, 'rows_scanned', total_rows),
            (relation.table_schema, relation.table_name, 'country_code_null', null_countries),
            (relation.table_schema, relation.table_name, 'country_code_invalid', invalid_countries);
    END LOOP;
END $$;

-- Review the newest measurements. Add service-specific joins here only when
-- parent country provenance is documented; city/region identifiers are not
-- portable across service databases and must not be guessed.
SELECT table_schema, table_name, metric, metric_value, measured_at
FROM africa_ready_migration_measurements
WHERE measured_at >= CURRENT_TIMESTAMP - INTERVAL '5 minutes'
ORDER BY table_schema, table_name, metric;
