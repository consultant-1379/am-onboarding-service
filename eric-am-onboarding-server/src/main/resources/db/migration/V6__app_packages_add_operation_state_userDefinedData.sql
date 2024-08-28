-- -----------------------------------------------------
-- ENUM `operational_state_enum`
-- -----------------------------------------------------

create type operational_state_enum as ENUM('ENABLED', 'DISABLED');

ALTER TABLE app_packages ADD operational_state operational_state_enum;

ALTER TABLE app_packages ADD user_defined_data VARCHAR;

-- -----------------------------------------------------
-- ENUM `onboarding_state_enum`
-- -----------------------------------------------------

create type onboarding_state_enum as ENUM('CREATED', 'UPLOADING', 'PROCESSING', 'ONBOARDED');

ALTER TABLE app_packages ALTER COLUMN onboarding_state SET DATA TYPE onboarding_state_enum USING onboarding_state::onboarding_state_enum;

ALTER TABLE app_packages DROP COLUMN id CASCADE;

ALTER TABLE app_packages ADD CONSTRAINT descriptor_unique UNIQUE (descriptor_id);
