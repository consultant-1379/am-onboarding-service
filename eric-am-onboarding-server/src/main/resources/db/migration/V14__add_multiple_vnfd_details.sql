ALTER TABLE app_packages ADD is_multiple_vnfd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_packages ADD vnfd_zip BYTEA;