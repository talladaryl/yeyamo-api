ALTER TABLE content_posts ADD COLUMN reference_type VARCHAR(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE content_posts ADD COLUMN reference_id VARCHAR(100);
UPDATE content_posts SET reference_type='PLACE', reference_id=catalog_asset_id::text WHERE catalog_asset_id IS NOT NULL;
CREATE INDEX idx_content_posts_reference ON content_posts(reference_type, reference_id) WHERE reference_type <> 'NONE';
