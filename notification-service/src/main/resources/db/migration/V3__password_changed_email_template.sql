INSERT INTO notification_templates(id,event_type,channel,locale,subject_template,body_template) VALUES
('10000000-0000-0000-0000-000000000017','user.password_changed','IN_APP','fr','Mot de passe modifié','Votre mot de passe YeYamo a été modifié le {{changedAt}}.'),
('10000000-0000-0000-0000-000000000018','user.password_changed','EMAIL','fr','Votre mot de passe YeYamo a été modifié','Votre mot de passe YeYamo a été modifié le {{changedAt}}. Si vous n''êtes pas à l''origine de cette action, sécurisez immédiatement votre compte.')
ON CONFLICT (event_type,channel,locale) DO NOTHING;
