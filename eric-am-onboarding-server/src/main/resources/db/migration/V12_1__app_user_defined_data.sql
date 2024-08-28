CREATE TABLE app_user_defined_data (
  id VARCHAR UNIQUE NOT NULL,
  package_id VARCHAR NOT NULL,
  user_key VARCHAR NOT NULL,
  user_value VARCHAR,
  FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE CASCADE
);