-- FeedPostEntity reads these optional projection fields for every feed query.
-- They must exist before Hibernate selects feed_posts in deployed databases.
ALTER TABLE feed_posts ADD COLUMN IF NOT EXISTS city_id VARCHAR(100);
ALTER TABLE feed_posts ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE feed_posts ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

ALTER TABLE feed_posts
    DROP CONSTRAINT IF EXISTS ck_feed_posts_coordinates;

ALTER TABLE feed_posts
    ADD CONSTRAINT ck_feed_posts_coordinates
    CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL));
