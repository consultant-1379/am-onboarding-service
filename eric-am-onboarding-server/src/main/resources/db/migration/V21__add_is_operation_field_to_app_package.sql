ALTER TABLE app_packages
    ADD COLUMN "supported_operations_parsed" BOOLEAN NOT NULL DEFAULT FALSE;