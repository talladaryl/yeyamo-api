ALTER TABLE content_posts
    ALTER COLUMN latitude TYPE DOUBLE PRECISION USING latitude::double precision,
    ALTER COLUMN longitude TYPE DOUBLE PRECISION USING longitude::double precision;

ALTER TABLE stories
    ALTER COLUMN latitude TYPE DOUBLE PRECISION USING latitude::double precision,
    ALTER COLUMN longitude TYPE DOUBLE PRECISION USING longitude::double precision;
