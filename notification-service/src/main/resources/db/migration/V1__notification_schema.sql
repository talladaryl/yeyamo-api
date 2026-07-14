CREATE TABLE notification_preferences (
  user_id VARCHAR(120) PRIMARY KEY,
  in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  email_address VARCHAR(320),
  push_token VARCHAR(500),
  locale VARCHAR(10) NOT NULL DEFAULT 'fr',
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE notification_templates (
  id UUID PRIMARY KEY,
  event_type VARCHAR(120) NOT NULL,
  channel VARCHAR(20) NOT NULL,
  locale VARCHAR(10) NOT NULL,
  subject_template VARCHAR(300),
  body_template TEXT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_notification_template UNIQUE(event_type, channel, locale)
);

CREATE TABLE notifications (
  id UUID PRIMARY KEY,
  source_event_id UUID NOT NULL,
  event_type VARCHAR(120) NOT NULL,
  recipient_id VARCHAR(120) NOT NULL,
  title VARCHAR(300) NOT NULL,
  body TEXT NOT NULL,
  data_json TEXT NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL,
  read_at TIMESTAMPTZ,
  CONSTRAINT uk_notification_source_recipient UNIQUE(source_event_id, recipient_id)
);
CREATE INDEX idx_notification_recipient_created ON notifications(recipient_id, created_at DESC);

CREATE TABLE notification_deliveries (
  id UUID PRIMARY KEY,
  notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
  channel VARCHAR(20) NOT NULL,
  destination VARCHAR(500),
  status VARCHAR(30) NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ,
  delivered_at TIMESTAMPTZ,
  last_error VARCHAR(1000),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_notification_delivery UNIQUE(notification_id, channel)
);
CREATE INDEX idx_delivery_due ON notification_deliveries(status, next_attempt_at);

CREATE TABLE notification_processed_events (
  event_id UUID PRIMARY KEY,
  event_type VARCHAR(120) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL
);

INSERT INTO notification_templates(id,event_type,channel,locale,subject_template,body_template) VALUES
('10000000-0000-0000-0000-000000000001','user.created','IN_APP','fr','Bienvenue sur YeYamo','Bienvenue {{userId}}, votre compte YeYamo est prêt.'),
('10000000-0000-0000-0000-000000000002','user.created','EMAIL','fr','Bienvenue sur YeYamo','Bienvenue sur YeYamo. Votre compte est maintenant actif.'),
('10000000-0000-0000-0000-000000000003','user.role_added','IN_APP','fr','Nouveau rôle','Le rôle {{role}} a été ajouté à votre compte.'),
('10000000-0000-0000-0000-000000000004','partner.submitted','IN_APP','fr','Dossier envoyé','Votre dossier partenaire a été envoyé pour vérification.'),
('10000000-0000-0000-0000-000000000005','partner.approved','IN_APP','fr','Partenaire approuvé','Votre compte partenaire {{legalName}} a été approuvé.'),
('10000000-0000-0000-0000-000000000006','partner.rejected','IN_APP','fr','Dossier partenaire refusé','Votre dossier partenaire nécessite votre attention.'),
('10000000-0000-0000-0000-000000000007','partner.needs_info','IN_APP','fr','Informations demandées','Des informations supplémentaires sont nécessaires pour votre dossier.'),
('10000000-0000-0000-0000-000000000008','partner.requires_changes','IN_APP','fr','Modifications demandées','Votre dossier partenaire doit être modifié.'),
('10000000-0000-0000-0000-000000000009','moderation.report.approved','IN_APP','fr','Signalement traité','Le signalement {{reportId}} a été approuvé.'),
('10000000-0000-0000-0000-000000000010','moderation.report.rejected','IN_APP','fr','Signalement traité','Le signalement {{reportId}} a été rejeté.'),
('10000000-0000-0000-0000-000000000011','default','EMAIL','fr','Notification YeYamo','{{message}}'),
('10000000-0000-0000-0000-000000000012','default','PUSH','fr','YeYamo','{{message}}'),
('10000000-0000-0000-0000-000000000013','default','IN_APP','fr','YeYamo','{{message}}');
