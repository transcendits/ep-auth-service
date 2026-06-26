INSERT INTO permissions (id, code, description) VALUES
('00000000-0000-0000-0000-000000000008', 'member:admin', 'Administer employee management service'),
('00000000-0000-0000-0000-000000000009', 'member:write', 'Create and update employee management service data'),
('00000000-0000-0000-0000-000000000010', 'member:read', 'Read employee management service data')
ON CONFLICT (code) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO profile_permissions (profile_id, permission_id)
SELECT p.id, perm.id
FROM profiles p
JOIN permissions perm ON perm.code IN ('member:admin', 'member:write', 'member:read')
WHERE p.name IN ('SUPER_ADMIN', 'HR_ADMIN')
ON CONFLICT DO NOTHING;

INSERT INTO profile_permissions (profile_id, permission_id)
SELECT p.id, perm.id
FROM profiles p
JOIN permissions perm ON perm.code = 'member:read'
WHERE p.name IN ('ACCOUNTS_ADMIN', 'EMPLOYEE')
ON CONFLICT DO NOTHING;
