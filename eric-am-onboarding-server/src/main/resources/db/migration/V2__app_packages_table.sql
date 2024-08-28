CREATE TABLE app_packages (
  id SERIAL UNIQUE,
  package_id VARCHAR UNIQUE NOT NULL,
  descriptor_id VARCHAR DEFAULT NULL,
  descriptor_version VARCHAR DEFAULT NULL,
  descriptor_model TEXT DEFAULT NULL,
  provider VARCHAR DEFAULT NULL,
  product_name VARCHAR DEFAULT NULL,
  software_version VARCHAR DEFAULT NULL,
  onboarding_state VARCHAR NOT NULL,
  charts_registry_url VARCHAR DEFAULT NULL,
  files BYTEA DEFAULT NULL
);

CREATE TABLE app_docker_images (
  id SERIAL UNIQUE,
  package_id INTEGER NOT NULL,
  image_id VARCHAR NOT NULL,
  FOREIGN KEY (package_id) REFERENCES app_packages (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
