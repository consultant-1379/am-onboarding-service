-- -----------------------------------------------------
-- ENUM `package_security_options_enum`
-- -----------------------------------------------------

create type package_security_options_enum as ENUM('OPTION_1', 'OPTION_2', 'UNSIGNED');

ALTER TABLE app_packages
    ADD COLUMN package_security_option package_security_options_enum DEFAULT NULL;