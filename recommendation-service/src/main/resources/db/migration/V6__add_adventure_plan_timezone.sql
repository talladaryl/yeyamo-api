ALTER TABLE adventure_plans
    ADD COLUMN country_timezone VARCHAR(80) NOT NULL DEFAULT 'UTC';
