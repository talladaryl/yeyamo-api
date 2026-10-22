INSERT INTO notification_templates(id,event_type,channel,locale,subject_template,body_template) VALUES
('80000000-0000-0000-0000-000000000001','social.followed','IN_APP','fr','Nouvel abonné','Une personne vient de s’abonner à votre profil.'),
('80000000-0000-0000-0000-000000000002','social.followed','PUSH','fr','Nouvel abonné YeYamo','Une personne vient de s’abonner à votre profil.')
ON CONFLICT (event_type, channel, locale) DO NOTHING;
