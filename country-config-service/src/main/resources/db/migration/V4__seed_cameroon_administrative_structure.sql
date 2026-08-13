-- Seed Cameroon Administrative Structure using Generic Model
-- Level 1: Régions
-- Level 2: Départements  
-- Level 3: Arrondissements

-- Configure administrative level labels for Cameroon
INSERT INTO administrative_level_labels (country_code, level, label, label_plural, localized_labels, display_order) VALUES
    ('CM', 1, 'Région', 'Régions', '{"fr": "Région", "en": "Region"}', 1),
    ('CM', 2, 'Département', 'Départements', '{"fr": "Département", "en": "Department"}', 2),
    ('CM', 3, 'Arrondissement', 'Arrondissements', '{"fr": "Arrondissement", "en": "District"}', 3);

-- Level 1: Cameroon Regions
INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, official_code, localized_names, latitude, longitude, active) VALUES
    ('CM', NULL, 1, 'Adamaoua', 'adamaoua', '01', '{"fr": "Adamaoua", "en": "Adamawa"}', 7.0, 13.0, true),
    ('CM', NULL, 1, 'Centre', 'centre', '02', '{"fr": "Centre", "en": "Centre"}', 4.0, 11.5, true),
    ('CM', NULL, 1, 'Est', 'est', '03', '{"fr": "Est", "en": "East"}', 4.5, 14.5, true),
    ('CM', NULL, 1, 'Extrême-Nord', 'extreme-nord', '04', '{"fr": "Extrême-Nord", "en": "Far North"}', 10.5, 14.5, true),
    ('CM', NULL, 1, 'Littoral', 'littoral', '05', '{"fr": "Littoral", "en": "Littoral"}', 4.05, 9.7, true),
    ('CM', NULL, 1, 'Nord', 'nord', '06', '{"fr": "Nord", "en": "North"}', 8.5, 13.5, true),
    ('CM', NULL, 1, 'Nord-Ouest', 'nord-ouest', '07', '{"fr": "Nord-Ouest", "en": "Northwest"}', 6.0, 10.0, true),
    ('CM', NULL, 1, 'Ouest', 'ouest', '08', '{"fr": "Ouest", "en": "West"}', 5.5, 10.5, true),
    ('CM', NULL, 1, 'Sud', 'sud', '09', '{"fr": "Sud", "en": "South"}', 2.5, 10.5, true),
    ('CM', NULL, 1, 'Sud-Ouest', 'sud-ouest', '10', '{"fr": "Sud-Ouest", "en": "Southwest"}', 4.5, 9.0, true);

-- Get region IDs for foreign keys
DO $$
DECLARE
    region_littoral_id UUID;
    region_centre_id UUID;
    region_ouest_id UUID;
    region_adamaoua_id UUID;
BEGIN
    -- Littoral Region
    SELECT id INTO region_littoral_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'littoral';
    
    -- Level 2: Departments in Littoral
    INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, latitude, longitude, active) VALUES
        ('CM', region_littoral_id, 2, 'Wouri', 'wouri', 4.05, 9.7, true),
        ('CM', region_littoral_id, 2, 'Moungo', 'moungo', 4.6, 9.8, true),
        ('CM', region_littoral_id, 2, 'Nkam', 'nkam', 4.7, 10.4, true),
        ('CM', region_littoral_id, 2, 'Sanaga-Maritime', 'sanaga-maritime', 3.8, 10.0, true);
    
    -- Major cities in Littoral
    DECLARE
        dept_wouri_id UUID;
    BEGIN
        SELECT id INTO dept_wouri_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'wouri';
        
        INSERT INTO cities (country_code, administrative_area_id, name, slug, latitude, longitude, population, active) VALUES
            ('CM', dept_wouri_id, 'Douala', 'douala', 4.0511, 9.7679, 3000000, true);
        
        -- Localities (Quartiers) in Douala
        DECLARE
            city_douala_id UUID;
        BEGIN
            SELECT id INTO city_douala_id FROM cities WHERE country_code = 'CM' AND slug = 'douala';
            
            INSERT INTO localities (country_code, city_id, locality_type, name, slug, active) VALUES
                ('CM', city_douala_id, 'quartier', 'Akwa', 'akwa', true),
                ('CM', city_douala_id, 'quartier', 'Bonanjo', 'bonanjo', true),
                ('CM', city_douala_id, 'quartier', 'Bonabéri', 'bonaberi', true),
                ('CM', city_douala_id, 'quartier', 'Bali', 'bali', true),
                ('CM', city_douala_id, 'quartier', 'New Bell', 'new-bell', true),
                ('CM', city_douala_id, 'quartier', 'Makepe', 'makepe', true),
                ('CM', city_douala_id, 'quartier', 'Logpom', 'logpom', true);
        END;
    END;
    
    -- Centre Region
    SELECT id INTO region_centre_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'centre';
    
    -- Level 2: Departments in Centre
    INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, latitude, longitude, active) VALUES
        ('CM', region_centre_id, 2, 'Mfoundi', 'mfoundi', 3.8667, 11.5167, true),
        ('CM', region_centre_id, 2, 'Mefou-et-Afamba', 'mefou-et-afamba', 3.7, 11.7, true),
        ('CM', region_centre_id, 2, 'Lékié', 'lekie', 4.2, 11.0, true);
    
    -- Major city: Yaoundé
    DECLARE
        dept_mfoundi_id UUID;
    BEGIN
        SELECT id INTO dept_mfoundi_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'mfoundi';
        
        INSERT INTO cities (country_code, administrative_area_id, name, slug, latitude, longitude, population, active) VALUES
            ('CM', dept_mfoundi_id, 'Yaoundé', 'yaounde', 3.8667, 11.5167, 2800000, true);
        
        -- Localities (Quartiers) in Yaoundé
        DECLARE
            city_yaounde_id UUID;
        BEGIN
            SELECT id INTO city_yaounde_id FROM cities WHERE country_code = 'CM' AND slug = 'yaounde';
            
            INSERT INTO localities (country_code, city_id, locality_type, name, slug, active) VALUES
                ('CM', city_yaounde_id, 'quartier', 'Bastos', 'bastos', true),
                ('CM', city_yaounde_id, 'quartier', 'Nlongkak', 'nlongkak', true),
                ('CM', city_yaounde_id, 'quartier', 'Mvan', 'mvan', true),
                ('CM', city_yaounde_id, 'quartier', 'Essos', 'essos', true),
                ('CM', city_yaounde_id, 'quartier', 'Mokolo', 'mokolo', true),
                ('CM', city_yaounde_id, 'quartier', 'Emana', 'emana', true);
        END;
    END;
    
    -- Ouest Region
    SELECT id INTO region_ouest_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'ouest';
    
    -- Level 2: Departments in Ouest
    INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, latitude, longitude, active) VALUES
        ('CM', region_ouest_id, 2, 'Bamboutos', 'bamboutos', 5.5, 10.0, true),
        ('CM', region_ouest_id, 2, 'Hauts-Plateaux', 'hauts-plateaux', 5.5, 10.5, true),
        ('CM', region_ouest_id, 2, 'Koung-Khi', 'koung-khi', 5.3, 10.3, true),
        ('CM', region_ouest_id, 2, 'Menoua', 'menoua', 5.4, 10.1, true),
        ('CM', region_ouest_id, 2, 'Mifi', 'mifi', 5.4, 10.4, true),
        ('CM', region_ouest_id, 2, 'Ndé', 'nde', 5.5, 10.7, true),
        ('CM', region_ouest_id, 2, 'Noun', 'noun', 5.6, 10.8, true);
    
    -- Major cities in Ouest
    DECLARE
        dept_mifi_id UUID;
        dept_noun_id UUID;
    BEGIN
        SELECT id INTO dept_mifi_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'mifi';
        SELECT id INTO dept_noun_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'noun';
        
        INSERT INTO cities (country_code, administrative_area_id, name, slug, latitude, longitude, active) VALUES
            ('CM', dept_mifi_id, 'Bafoussam', 'bafoussam', 5.4781, 10.4178, 300000, true),
            ('CM', dept_noun_id, 'Foumban', 'foumban', 5.7265, 10.8997, 120000, true);
    END;
    
    -- Adamaoua Region
    SELECT id INTO region_adamaoua_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'adamaoua';
    
    -- Level 2: Departments in Adamaoua
    INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, latitude, longitude, active) VALUES
        ('CM', region_adamaoua_id, 2, 'Djerem', 'djerem', 6.5, 13.5, true),
        ('CM', region_adamaoua_id, 2, 'Faro-et-Déo', 'faro-et-deo', 7.5, 12.5, true),
        ('CM', region_adamaoua_id, 2, 'Mayo-Banyo', 'mayo-banyo', 6.7, 11.8, true),
        ('CM', region_adamaoua_id, 2, 'Mbéré', 'mbere', 6.5, 14.0, true),
        ('CM', region_adamaoua_id, 2, 'Vina', 'vina', 7.3, 13.5, true);
    
    -- Major city: Ngaoundéré
    DECLARE
        dept_vina_id UUID;
    BEGIN
        SELECT id INTO dept_vina_id FROM administrative_areas WHERE country_code = 'CM' AND slug = 'vina';
        
        INSERT INTO cities (country_code, administrative_area_id, name, slug, latitude, longitude, active) VALUES
            ('CM', dept_vina_id, 'Ngaoundéré', 'ngaoundere', 7.3167, 13.5833, 200000, true);
    END;
END $$;

COMMENT ON TABLE administrative_areas IS 'Cameroon structure: 10 regions, 58 departments (sample), arrondissements (to be added)';
COMMENT ON TABLE cities IS 'Major Cameroonian cities: Douala, Yaoundé, Bafoussam, etc.';
COMMENT ON TABLE localities IS 'Quartiers in major cities and villages in rural areas';
