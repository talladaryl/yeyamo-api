CREATE TABLE culture_proverb_details (
    content_id UUID PRIMARY KEY REFERENCES culture_contents(id) ON DELETE CASCADE,
    literal_translation TEXT NOT NULL,
    meaning TEXT NOT NULL,
    origin_language_code VARCHAR(35) NOT NULL REFERENCES languages(code),
    audio_url VARCHAR(2048)
);

CREATE TABLE culture_recipe_details (
    content_id UUID PRIMARY KEY REFERENCES culture_contents(id) ON DELETE CASCADE,
    prep_time_minutes INTEGER CHECK(prep_time_minutes > 0),
    servings INTEGER CHECK(servings > 0)
);

CREATE TABLE culture_recipe_ingredients (
    content_id UUID NOT NULL REFERENCES culture_recipe_details(content_id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity VARCHAR(80),
    unit VARCHAR(80),
    PRIMARY KEY(content_id, display_order)
);

CREATE TABLE culture_recipe_steps (
    content_id UUID NOT NULL REFERENCES culture_recipe_details(content_id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    PRIMARY KEY(content_id, display_order)
);
