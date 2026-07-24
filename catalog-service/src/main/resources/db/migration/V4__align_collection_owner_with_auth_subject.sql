ALTER TABLE collections
    ALTER COLUMN user_id TYPE VARCHAR(120) USING user_id::text;
