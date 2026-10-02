ALTER TABLE user_profiles ADD COLUMN interests_onboarding_completed BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE user_profiles ADD COLUMN last_login_at TIMESTAMPTZ;
ALTER TABLE user_profiles ADD COLUMN last_active_at TIMESTAMPTZ;

CREATE TABLE user_interest_categories (
  profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  category_code VARCHAR(100) NOT NULL,
  PRIMARY KEY (profile_id, category_code)
);

CREATE INDEX idx_user_profiles_last_active ON user_profiles(last_active_at) WHERE status = 'ACTIVE';
