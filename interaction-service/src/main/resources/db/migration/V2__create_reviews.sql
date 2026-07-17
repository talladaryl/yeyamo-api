-- Reviews table for user ratings and comments on places
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    place_id UUID NOT NULL,
    rating SMALLINT NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_review_user_place UNIQUE (user_id, place_id),
    CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Index for querying reviews by place (most common query)
CREATE INDEX idx_reviews_place_created ON reviews(place_id, created_at DESC);

-- Index for querying reviews by user (for user profile aggregation)
CREATE INDEX idx_reviews_user ON reviews(user_id, created_at DESC);

COMMENT ON TABLE reviews IS 'User reviews and ratings for places (catalog assets)';
COMMENT ON COLUMN reviews.place_id IS 'References catalog_asset_id from catalog-service';
COMMENT ON COLUMN reviews.rating IS 'Rating from 1 (lowest) to 5 (highest)';
COMMENT ON CONSTRAINT uk_review_user_place ON reviews IS 'One review per user per place';
