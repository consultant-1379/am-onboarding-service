CREATE TABLE app_package_artifacts (
  id VARCHAR UNIQUE,
  package_id VARCHAR NOT NULL,
  artifact_path VARCHAR DEFAULT NULL,
  artifact BYTEA DEFAULT NULL,
  FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE CASCADE
);

