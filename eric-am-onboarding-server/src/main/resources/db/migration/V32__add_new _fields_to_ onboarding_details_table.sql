ALTER TABLE onboarding_details
    ADD COLUMN onboarding_phase VARCHAR,
    ADD COLUMN onboarding_heartbeat_id VARCHAR,
    ADD COLUMN package_upload_context VARCHAR,
    ADD FOREIGN KEY (onboarding_heartbeat_id) REFERENCES onboarding_heartbeat (pod_name);