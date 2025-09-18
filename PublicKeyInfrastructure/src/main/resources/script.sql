-- pass for everyone is: (CspCsp567)
INSERT INTO users (email, password, role, user_type, name, surname, organization, enabled, created_at, email_verified_at)
VALUES
    -- ADMIN
    ('admin@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'ADMIN',
     'User',
     '-', '-', '-', true, NOW(), NULL),

    -- Certificate Authority 1
    -- password = ca1pass
    ('ca1@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'CERTIFICATE_AUTHORITY',
     'User',
     '-', '-', '-', true, NOW(), NULL),

    -- Certificate Authority 2
    -- password = ca2pass
    ('ca2@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'CERTIFICATE_AUTHORITY',
     'User',
     '-', '-', '-', true, NOW(), NULL),

    -- Regular User 1
    -- password = user1pass
    ('user1@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'REGULAR_USER',
     'RegularUser',
     'John', 'Doe', 'Org1', true, NOW(), NULL),

    -- Regular User 2
    -- password = user2pass
    ('user2@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'REGULAR_USER',
     'RegularUser',
     'Jane', 'Smith', 'Org2', true, NOW(), NULL),

    -- Regular User 3
    -- password = user3pass
    ('user3@example.com',
     '$argon2id$v=19$m=16384,t=2,p=1$flQ3UN7LUmIBSuP1YoZ90Q$Eu/1z+4lLmh4+oVq+VC8MkMLafbuF5Ql/ZGuL3SSGaI',
     'REGULAR_USER',
     'RegularUser',
     'Alice', 'Johnson', 'Org3', true, NOW(), NULL);