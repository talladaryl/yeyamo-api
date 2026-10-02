CREATE TABLE reengagement_digest_state (
  user_id VARCHAR(120) PRIMARY KEY,
  last_sent_at TIMESTAMPTZ NOT NULL
);
INSERT INTO notification_templates(id,event_type,channel,locale,subject_template,body_template) VALUES
('10000000-0000-0000-0000-000000000026','REENGAGEMENT_DIGEST','IN_APP','fr','Pendant votre absence','{{summary}} vous attendent sur YeYamo.'),
('10000000-0000-0000-0000-000000000027','REENGAGEMENT_DIGEST','EMAIL','fr','Pendant votre absence sur YeYamo','Pendant votre absence : {{summary}}. Revenez découvrir ce qui vous attend.');
