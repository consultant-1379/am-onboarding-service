CREATE TABLE onboarding_heartbeat (
  pod_name VARCHAR UNIQUE,
  latest_update_time TIMESTAMP NOT NULL,
  PRIMARY KEY (pod_name)
);
