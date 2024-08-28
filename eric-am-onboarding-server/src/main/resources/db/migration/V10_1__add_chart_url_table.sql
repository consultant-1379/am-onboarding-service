CREATE TABLE chart_urls (
  id VARCHAR UNIQUE NOT NULL,
  package_id VARCHAR NOT NULL,
  charts_registry_url VARCHAR NOT NULL,
  priority INTEGER NOT NULL,
  FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE CASCADE
);
