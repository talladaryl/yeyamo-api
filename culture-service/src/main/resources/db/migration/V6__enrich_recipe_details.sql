ALTER TABLE culture_recipe_details
 ADD COLUMN cook_time_minutes INTEGER CHECK (cook_time_minutes > 0),
 ADD COLUMN rest_time_minutes INTEGER CHECK (rest_time_minutes > 0),
 ADD COLUMN difficulty VARCHAR(32) CHECK (difficulty IN ('SIMPLE','INTERMEDIAIRE','AVANCE')),
 ADD COLUMN yield_text VARCHAR(255), ADD COLUMN hero_media_id UUID, ADD COLUMN video_media_id UUID,
 ADD COLUMN youtube_video_url VARCHAR(512), ADD COLUMN preparation_ahead TEXT, ADD COLUMN plating TEXT,
 ADD COLUMN storage TEXT, ADD COLUMN reheating TEXT, ADD COLUMN tips TEXT, ADD COLUMN variants TEXT,
 ADD COLUMN accompaniments TEXT, ADD COLUMN safety_notes TEXT;

ALTER TABLE culture_recipe_ingredients ADD COLUMN id UUID;
UPDATE culture_recipe_ingredients SET id = (substr(md5(content_id::text || ':ingredient:' || display_order),1,8)||'-'||substr(md5(content_id::text || ':ingredient:' || display_order),9,4)||'-'||substr(md5(content_id::text || ':ingredient:' || display_order),13,4)||'-'||substr(md5(content_id::text || ':ingredient:' || display_order),17,4)||'-'||substr(md5(content_id::text || ':ingredient:' || display_order),21,12))::uuid WHERE id IS NULL;
ALTER TABLE culture_recipe_ingredients ALTER COLUMN id SET NOT NULL, ADD CONSTRAINT uq_culture_recipe_ingredient_id UNIQUE(id),
 ADD COLUMN group_name VARCHAR(120), ADD COLUMN optional BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN substitution_notes TEXT, ADD COLUMN quantity_value NUMERIC(19,6), ADD COLUMN quantity_max_value NUMERIC(19,6),
 ADD COLUMN quantity_unit_code VARCHAR(16), ADD CONSTRAINT ck_recipe_ingredient_quantity_range CHECK(quantity_max_value IS NULL OR quantity_value IS NULL OR quantity_max_value >= quantity_value),
 ADD CONSTRAINT ck_recipe_ingredient_unit CHECK(quantity_unit_code IS NULL OR quantity_unit_code IN ('G','KG','ML','L','UNIT','TSP','TBSP'));

ALTER TABLE culture_recipe_steps ADD COLUMN id UUID;
UPDATE culture_recipe_steps SET id = (substr(md5(content_id::text || ':step:' || display_order),1,8)||'-'||substr(md5(content_id::text || ':step:' || display_order),9,4)||'-'||substr(md5(content_id::text || ':step:' || display_order),13,4)||'-'||substr(md5(content_id::text || ':step:' || display_order),17,4)||'-'||substr(md5(content_id::text || ':step:' || display_order),21,12))::uuid WHERE id IS NULL;
ALTER TABLE culture_recipe_steps ALTER COLUMN id SET NOT NULL, ADD CONSTRAINT uq_culture_recipe_step_id UNIQUE(id),
 ADD COLUMN title VARCHAR(255), ADD COLUMN duration_minutes INTEGER CHECK(duration_minutes > 0),
 ADD COLUMN temperature_value NUMERIC(8,2), ADD COLUMN temperature_unit VARCHAR(4) CHECK(temperature_unit IN ('C','F')),
 ADD COLUMN expected_result TEXT, ADD COLUMN tip TEXT, ADD COLUMN safety_note TEXT, ADD COLUMN media_id UUID;

CREATE TABLE culture_recipe_step_ingredients(step_id UUID NOT NULL REFERENCES culture_recipe_steps(id) ON DELETE CASCADE,ingredient_id UUID NOT NULL REFERENCES culture_recipe_ingredients(id) ON DELETE CASCADE,PRIMARY KEY(step_id,ingredient_id));
CREATE FUNCTION enforce_recipe_step_ingredient_same_content() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT content_id FROM culture_recipe_steps WHERE id=NEW.step_id) IS DISTINCT FROM
    (SELECT content_id FROM culture_recipe_ingredients WHERE id=NEW.ingredient_id) THEN
  RAISE EXCEPTION 'Step and ingredient must belong to the same recipe';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER trg_recipe_step_ingredient_same_content BEFORE INSERT OR UPDATE ON culture_recipe_step_ingredients FOR EACH ROW EXECUTE FUNCTION enforce_recipe_step_ingredient_same_content();
CREATE TABLE culture_recipe_equipment(id UUID PRIMARY KEY,content_id UUID NOT NULL REFERENCES culture_recipe_details(content_id) ON DELETE CASCADE,name VARCHAR(255) NOT NULL,optional BOOLEAN NOT NULL DEFAULT FALSE,display_order INTEGER NOT NULL,UNIQUE(content_id,display_order));
CREATE TABLE culture_recipe_media(id UUID PRIMARY KEY,content_id UUID NOT NULL REFERENCES culture_recipe_details(content_id) ON DELETE CASCADE,media_id UUID NOT NULL,role VARCHAR(32) NOT NULL CHECK(role IN ('GALLERY','RESULT','PROCESS')),caption VARCHAR(500),alt_text VARCHAR(500),display_order INTEGER NOT NULL,UNIQUE(content_id,display_order));

CREATE TABLE culture_recipe_revisions(
 id UUID PRIMARY KEY, content_id UUID NOT NULL REFERENCES culture_contents(id) ON DELETE CASCADE,
 author_id VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL,
 base_version BIGINT NOT NULL, candidate_json TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, submitted_at TIMESTAMPTZ, decided_at TIMESTAMPTZ,
 reviewed_by VARCHAR(255), review_reason TEXT,
 CONSTRAINT ck_recipe_revision_status CHECK(status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED'))
);
CREATE INDEX idx_recipe_revision_content_status ON culture_recipe_revisions(content_id,status,created_at DESC);
CREATE UNIQUE INDEX uq_recipe_active_revision ON culture_recipe_revisions(content_id) WHERE status IN ('DRAFT','SUBMITTED','UNDER_REVIEW');
