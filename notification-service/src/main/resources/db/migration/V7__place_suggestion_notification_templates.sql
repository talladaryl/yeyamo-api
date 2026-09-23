INSERT INTO notification_templates(id,event_type,channel,locale,subject_template,body_template) VALUES
('70000000-0000-0000-0000-000000000001','PLACE_SUGGESTION_APPROVED','IN_APP','fr',
 'Suggestion de lieu approuvée','Votre suggestion « {{name}} » a été approuvée.'),
('70000000-0000-0000-0000-000000000002','PLACE_SUGGESTION_REJECTED','IN_APP','fr',
 'Suggestion de lieu traitée','Votre suggestion « {{name}} » n''a pas été retenue.')
ON CONFLICT (event_type, channel, locale) DO NOTHING;
