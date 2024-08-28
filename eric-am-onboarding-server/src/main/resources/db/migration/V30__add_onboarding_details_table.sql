CREATE TABLE onboarding_details (
  id SERIAL UNIQUE,
  expired_onboarding_time TIMESTAMP NOT NULL,
  package_id VARCHAR NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE CASCADE ON UPDATE RESTRICT
);
