CREATE TABLE culture_content_read_model (
    content_id UUID PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK(type IN ('PROVERB', 'RECIPE')),
    title VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT FALSE
);
