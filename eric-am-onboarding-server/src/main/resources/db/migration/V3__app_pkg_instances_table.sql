-- -----------------------------------------------------
-- ENUM `app_usage_state`
-- -----------------------------------------------------
create type app_usage_state as ENUM('IN_USE', 'NOT_IN_USE');

ALTER TABLE app_packages ADD usage_state app_usage_state;

CREATE table app_pkg_instances (
  id SERIAL UNIQUE,
  package_id VARCHAR NOT NULL,
  instance_id VARCHAR UNIQUE,
  FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
