-- File Upload Project - Development Seed Data
-- Local/demo only.


-- Plain demo credentials known by project requirement:
-- admin1 / 1234
-- admin2 / 4321
--
-- pgcrypto crypt(..., gen_salt('bf', 12)) generates BCrypt hashes.
-- Only the resulting hash is stored in users.password_hash.
INSERT INTO users (username, password_hash, role, enabled)
VALUES
    ('admin1', crypt('1234', gen_salt('bf', 12)), 'ADMIN', TRUE),
    ('admin2', crypt('4321', gen_salt('bf', 12)), 'ADMIN', TRUE)
ON CONFLICT (username) DO NOTHING;

-- Fixed extensions start unchecked => blocked=false.
-- DO NOTHING prevents seed re-runs from resetting administrator changes.
INSERT INTO file_extension_policy (
    extension,
    policy_kind,
    blocked,
    created_by,
    updated_by
)
VALUES
    ('bat', 'FIXED', FALSE, NULL, NULL),
    ('cmd', 'FIXED', FALSE, NULL, NULL),
    ('com', 'FIXED', FALSE, NULL, NULL),
    ('cpl', 'FIXED', FALSE, NULL, NULL),
    ('exe', 'FIXED', FALSE, NULL, NULL),
    ('scr', 'FIXED', FALSE, NULL, NULL),
    ('js',  'FIXED', FALSE, NULL, NULL)
ON CONFLICT (extension) DO NOTHING;

