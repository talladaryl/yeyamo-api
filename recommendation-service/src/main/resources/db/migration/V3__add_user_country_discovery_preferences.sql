ALTER TABLE recommendation_user_preferences ADD COLUMN IF NOT EXISTS country_code VARCHAR(2);
CREATE TABLE IF NOT EXISTS recommendation_preference_countries (
    user_id VARCHAR(120) NOT NULL REFERENCES recommendation_user_preferences(user_id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    PRIMARY KEY (user_id, country_code)
);
CREATE TABLE IF NOT EXISTS recommendation_preference_languages (
    user_id VARCHAR(120) NOT NULL REFERENCES recommendation_user_preferences(user_id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (user_id, language_code)
);
