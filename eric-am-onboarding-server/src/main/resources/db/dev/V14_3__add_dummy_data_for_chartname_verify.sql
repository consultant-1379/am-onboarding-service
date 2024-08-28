INSERT INTO app_packages(package_id,descriptor_id,descriptor_version,provider,product_name,software_version,onboarding_state, usage_state, operational_state)
VALUES ('3e0f5f6a-bcc2-4279-82ef-b3a11a14d555','3e0f5f6a-bcc2-4279-82ef-b3a11a14d555','cxp9025898_4r81e08','Ericsson','SGSN-MME','1.20 (CXS101289_R81E08)','CREATED', 'NOT_IN_USE', 'ENABLED');

INSERT INTO chart_urls(id, package_id, charts_registry_url, priority)
VALUES ('3e0f5f6a-bcc2-4279-82ef-b3a11a14d555','3e0f5f6a-bcc2-4279-82ef-b3a11a14d555','http://10.210.53.96:31028/onboarded/charts/test-scale-chart-0.1.0.tgz','1');