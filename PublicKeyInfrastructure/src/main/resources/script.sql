-- pass for everyone is: (CspCsp567)
-- First insert organizations to get their IDs
INSERT INTO organisations (name, unit, country)
VALUES
    ('TechCorp Solutions', 'IT Department', 'Serbia'),
    ('Global Finance Ltd', 'Security Division', 'Germany'),
    ('Healthcare Systems Inc', 'Digital Infrastructure', 'United States'),
    ('Educational Network', 'Technology Services', 'France'),
    ('Government Services', 'Digital Security', 'United Kingdom');

-- Insert users with proper column names and organisation_id references
INSERT INTO users (email, password, role, user_type, name, surname, organisation_id, enabled, created_at)
VALUES
    -- ADMIN (no organisation required)
    ('admin@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'ADMIN',
     'RegularUser',
     'Admin', 'Admin', NULL, true, NOW()),

    -- Certificate Authority 1
    -- password = ca1pass
    ('ca1@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'CERTIFICATE_AUTHORITY',
     'RegularUser',
     'John', 'Doe', 1, true, NOW()),

    -- Certificate Authority 2
    -- password = ca2pass
    ('ca2@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'CERTIFICATE_AUTHORITY',
     'RegularUser',
     'Jane', 'Smith', 2, true, NOW()),

    -- Regular User 1
    -- password = user1pass
    ('user1@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'REGULAR_USER',
     'RegularUser',
     'John', 'Doe', 1, true, NOW()),

    -- Regular User 2
    -- password = user2pass
    ('user2@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'REGULAR_USER',
     'RegularUser',
     'Jane', 'Smith', 2, true, NOW()),

    -- Regular User 3
    -- password = user3pass
    ('joksovic.veljko@gmail.com',
     '$argon2id$v=19$m=16384,t=2,p=1$2YRB0ZtQRuiusvSnS45sIQ$9lu4DMWa6uq4DJQUooEjp+vTm/fUmRG2qnvjVPjW4Z8',
     'REGULAR_USER',
     'RegularUser',
     'Veljko', 'Joksovic', 3, true, NOW());
