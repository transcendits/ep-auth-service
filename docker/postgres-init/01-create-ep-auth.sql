SELECT 'CREATE ROLE ep_auth LOGIN PASSWORD ''ep_auth'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ep_auth')\gexec

SELECT 'CREATE DATABASE ep_auth OWNER ep_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ep_auth')\gexec

GRANT ALL PRIVILEGES ON DATABASE ep_auth TO ep_auth;
