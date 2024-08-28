ALTER TABLE operation_detail
    ADD CONSTRAINT operation_package_unique_key UNIQUE (operation_name, package_id);