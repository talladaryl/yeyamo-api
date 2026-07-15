INSERT INTO notification_templates(id,event_type,channel,locale,subject_template,body_template) VALUES
('10000000-0000-0000-0000-000000000014','messaging.message.sent','IN_APP','fr','Nouveau message','{{senderId}} : {{bodyPreview}}'),
('10000000-0000-0000-0000-000000000015','messaging.message.sent','PUSH','fr','Nouveau message YeYamo','{{senderId}} : {{bodyPreview}}'),
('10000000-0000-0000-0000-000000000016','messaging.message.sent','EMAIL','fr','Nouveau message YeYamo','Vous avez reçu un nouveau message de {{senderId}}.')
ON CONFLICT (event_type,channel,locale) DO NOTHING;
