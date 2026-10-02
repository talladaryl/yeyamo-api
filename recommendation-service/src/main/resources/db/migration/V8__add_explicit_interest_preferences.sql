CREATE TABLE recommendation_preference_interests (
  user_id VARCHAR(120) NOT NULL REFERENCES recommendation_user_preferences(user_id) ON DELETE CASCADE,
  category_code VARCHAR(100) NOT NULL,
  PRIMARY KEY (user_id, category_code)
);
