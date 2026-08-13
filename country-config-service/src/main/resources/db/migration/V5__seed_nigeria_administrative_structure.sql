-- Seed Nigeria Administrative Structure using Generic Model
-- Level 1: States (36 states + FCT)
-- Level 2: Local Government Areas (LGAs)

-- Configure administrative level labels for Nigeria
INSERT INTO administrative_level_labels (country_code, level, label, label_plural, localized_labels, display_order) VALUES
    ('NG', 1, 'State', 'States', '{"en": "State"}', 1),
    ('NG', 2, 'Local Government Area', 'Local Government Areas', '{"en": "Local Government Area", "abbr": "LGA"}', 2);

-- Level 1: Nigeria States (sample of major states)
INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, official_code, latitude, longitude, active) VALUES
    ('NG', NULL, 1, 'Lagos', 'lagos', 'LA', 6.5244, 3.3792, true),
    ('NG', NULL, 1, 'Abuja Federal Capital Territory', 'abuja-fct', 'FC', 9.0765, 7.3986, true),
    ('NG', NULL, 1, 'Kano', 'kano', 'KN', 12.0, 8.5, true),
    ('NG', NULL, 1, 'Rivers', 'rivers', 'RI', 4.8396, 6.9115, true),
    ('NG', NULL, 1, 'Oyo', 'oyo', 'OY', 8.0, 4.0, true),
    ('NG', NULL, 1, 'Kaduna', 'kaduna', 'KD', 10.5, 7.5, true),
    ('NG', NULL, 1, 'Enugu', 'enugu', 'EN', 6.5, 7.5, true);

-- Get state IDs
DO $$
DECLARE
    state_lagos_id UUID;
    state_abuja_id UUID;
    state_rivers_id UUID;
    state_kano_id UUID;
BEGIN
    -- Lagos State
    SELECT id INTO state_lagos_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'lagos';
    
    -- Level 2: LGAs in Lagos State
    INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, latitude, longitude, active) VALUES
        ('NG', state_lagos_id, 2, 'Lagos Island', 'lagos-island', 6.4541, 3.3947, true),
        ('NG', state_lagos_id, 2, 'Lagos Mainland', 'lagos-mainland', 6.5027, 3.3778, true),
        ('NG', state_lagos_id, 2, 'Ikeja', 'ikeja', 6.5964, 3.3425, true),
        ('NG', state_lagos_id, 2, 'Eti-Osa', 'eti-osa', 6.4531, 3.5467, true),
        ('NG', state_lagos_id, 2, 'Alimosho', 'alimosho', 6.6112, 3.2644, true),
        ('NG', state_lagos_id, 2, 'Surulere', 'surulere', 6.4981, 3.3606, true);
    
    -- Major cities in Lagos
    DECLARE
        lga_lagos_island_id UUID;
        lga_ikeja_id UUID;
    BEGIN
        SELECT id INTO lga_lagos_island_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'lagos-island';
        SELECT id INTO lga_ikeja_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'ikeja';
        
        INSERT INTO cities (country_code, administrative_area_id, name, slug, latitude, longitude, population, active) VALUES
            ('NG', lga_lagos_island_id, 'Lagos', 'lagos-city', 6.4541, 3.3947, 15000000, true),
            ('NG', lga_ikeja_id, 'Ikeja', 'ikeja-city', 6.5964, 3.3425, 500000, true);
        
        -- Localities (areas/wards) in Lagos city
        DECLARE
            city_lagos_id UUID;
        BEGIN
            SELECT id INTO city_lagos_id FROM cities WHERE country_code = 'NG' AND slug = 'lagos-city';
            
            INSERT INTO localities (country_code, city_id, locality_type, name, slug, active) VALUES
                ('NG', city_lagos_id, 'area', 'Victoria Island', 'victoria-island', true),
                ('NG', city_lagos_id, 'area', 'Ikoyi', 'ikoyi', true),
                ('NG', city_lagos_id, 'area', 'Lekki', 'lekki', true),
                ('NG', city_lagos_id, 'area', 'Ajah', 'ajah', true),
                ('NG', city_lagos_id, 'area', 'Yaba', 'yaba', true);
        END;
    END;
    
    -- Abuja FCT
    SELECT id INTO state_abuja_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'abuja-fct';
    
    -- Level 2: Area Councils in Abuja FCT
    INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, latitude, longitude, active) VALUES
        ('NG', state_abuja_id, 2, 'Abuja Municipal Area Council', 'abuja-municipal', 9.0579, 7.4951, true),
        ('NG', state_abuja_id, 2, 'Gwagwalada', 'gwagwalada', 8.9420, 7.0836, true),
        ('NG', state_abuja_id, 2, 'Kuje', 'kuje', 8.8792, 7.2247, true),
        ('NG', state_abuja_id, 2, 'Bwari', 'bwari', 9.2865, 7.3825, true),
        ('NG', state_abuja_id, 2, 'Abaji', 'abaji', 8.7333, 6.9167, true),
        ('NG', state_abuja_id, 2, 'Kwali', 'kwali', 8.8833, 7.0167, true);
    
    -- Major city: Abuja
    DECLARE
        council_abuja_municipal_id UUID;
    BEGIN
        SELECT id INTO council_abuja_municipal_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'abuja-municipal';
        
        INSERT INTO cities (country_code, administrative_area_id, name, slug, latitude, longitude, population, active) VALUES
            ('NG', council_abuja_municipal_id, 'Abuja', 'abuja-city', 9.0579, 7.4951, 3000000, true);
        
        -- Localities (districts) in Abuja
        DECLARE
            city_abuja_id UUID;
        BEGIN
            SELECT id INTO city_abuja_id FROM cities WHERE country_code = 'NG' AND slug = 'abuja-city';
            
            INSERT INTO localities (country_code, city_id, locality_type, name, slug, active) VALUES
                ('NG', city_abuja_id, 'district', 'Maitama', 'maitama', true),
                ('NG', city_abuja_id, 'district', 'Asokoro', 'asokoro', true),
                ('NG', city_abuja_id, 'district', 'Wuse', 'wuse', true),
                ('NG', city_abuja_id, 'district', 'Garki', 'garki', true),
                ('NG', city_abuja_id, 'district', 'Gwarinpa', 'gwarinpa', true);
        END;
    END;
    
    -- Rivers State
    SELECT id INTO state_rivers_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'rivers';
    
    -- Level 2: LGAs in Rivers State
    INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, latitude, longitude, active) VALUES
        ('NG', state_rivers_id, 2, 'Port Harcourt', 'port-harcourt-lga', 4.8156, 7.0498, true),
        ('NG', state_rivers_id, 2, 'Obio-Akpor', 'obio-akpor', 4.8854, 7.0086, true),
        ('NG', state_rivers_id, 2, 'Eleme', 'eleme', 4.7833, 7.1167, true);
    
    -- Major city: Port Harcourt
    DECLARE
        lga_port_harcourt_id UUID;
    BEGIN
        SELECT id INTO lga_port_harcourt_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'port-harcourt-lga';
        
        INSERT INTO cities (country_code, administrative_area_id, name, slug, latitude, longitude, population, active) VALUES
            ('NG', lga_port_harcourt_id, 'Port Harcourt', 'port-harcourt-city', 4.8156, 7.0498, 2000000, true);
    END;
    
    -- Kano State
    SELECT id INTO state_kano_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'kano';
    
    -- Level 2: LGAs in Kano State
    INSERT INTO administrative_areas (country_code, parent_id, level, name, slug, latitude, longitude, active) VALUES
        ('NG', state_kano_id, 2, 'Kano Municipal', 'kano-municipal', 12.0, 8.5167, true),
        ('NG', state_kano_id, 2, 'Nassarawa', 'nassarawa', 11.9833, 8.5333, true),
        ('NG', state_kano_id, 2, 'Fagge', 'fagge', 12.0167, 8.5167, true);
    
    -- Major city: Kano
    DECLARE
        lga_kano_municipal_id UUID;
    BEGIN
        SELECT id INTO lga_kano_municipal_id FROM administrative_areas WHERE country_code = 'NG' AND slug = 'kano-municipal';
        
        INSERT INTO cities (country_code, administrative_area_id, name, slug, latitude, longitude, population, active) VALUES
            ('NG', lga_kano_municipal_id, 'Kano', 'kano-city', 12.0, 8.5167, 4000000, true);
    END;
END $$;

COMMENT ON TABLE administrative_areas IS 'Nigeria structure: 36 states + FCT, 774 LGAs (sample provided)';
COMMENT ON TABLE administrative_level_labels IS 'Nigeria uses State/LGA terminology, not Region/Department';
