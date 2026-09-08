ALTER TABLE authenticity_claims
    ALTER COLUMN id TYPE VARCHAR(36) USING id::text;

ALTER TABLE authenticity_evidence
    ALTER COLUMN id TYPE VARCHAR(36) USING id::text;

ALTER TABLE copyright_claims
    ALTER COLUMN id TYPE VARCHAR(36) USING id::text;

ALTER TABLE reviewer_country_scope DROP CONSTRAINT IF EXISTS reviewer_country_scope_reviewer_id_fkey;
ALTER TABLE reviewer_culture_scope DROP CONSTRAINT IF EXISTS reviewer_culture_scope_reviewer_id_fkey;
ALTER TABLE reviewer_language_scope DROP CONSTRAINT IF EXISTS reviewer_language_scope_reviewer_id_fkey;
ALTER TABLE reviewer_content_scope DROP CONSTRAINT IF EXISTS reviewer_content_scope_reviewer_id_fkey;

ALTER TABLE reviewer_country_scope ALTER COLUMN reviewer_id TYPE VARCHAR(36) USING reviewer_id::text;
ALTER TABLE reviewer_culture_scope ALTER COLUMN reviewer_id TYPE VARCHAR(36) USING reviewer_id::text;
ALTER TABLE reviewer_language_scope ALTER COLUMN reviewer_id TYPE VARCHAR(36) USING reviewer_id::text;
ALTER TABLE reviewer_content_scope ALTER COLUMN reviewer_id TYPE VARCHAR(36) USING reviewer_id::text;
ALTER TABLE cultural_reviewers ALTER COLUMN id TYPE VARCHAR(36) USING id::text;

ALTER TABLE reviewer_country_scope ADD CONSTRAINT reviewer_country_scope_reviewer_id_fkey FOREIGN KEY (reviewer_id) REFERENCES cultural_reviewers(id) ON DELETE CASCADE;
ALTER TABLE reviewer_culture_scope ADD CONSTRAINT reviewer_culture_scope_reviewer_id_fkey FOREIGN KEY (reviewer_id) REFERENCES cultural_reviewers(id) ON DELETE CASCADE;
ALTER TABLE reviewer_language_scope ADD CONSTRAINT reviewer_language_scope_reviewer_id_fkey FOREIGN KEY (reviewer_id) REFERENCES cultural_reviewers(id) ON DELETE CASCADE;
ALTER TABLE reviewer_content_scope ADD CONSTRAINT reviewer_content_scope_reviewer_id_fkey FOREIGN KEY (reviewer_id) REFERENCES cultural_reviewers(id) ON DELETE CASCADE;

ALTER TABLE sensitive_content_flags
    ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
