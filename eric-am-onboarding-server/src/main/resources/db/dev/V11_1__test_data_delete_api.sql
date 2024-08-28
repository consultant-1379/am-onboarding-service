INSERT INTO app_packages(package_id,descriptor_id,descriptor_version,provider,product_name,software_version,onboarding_state, usage_state, operational_state)
VALUES ('f3def1ce-4cf4-477c-aab3-21cb04e6a381','h3def1ce-4cf4-477c-aab3-21cb04e6a381','cxp9025898_4r81e08','Ericsson','SGSN','1.21 (CXS101289_R81E08)','CREATED', 'NOT_IN_USE', 'ENABLED'),
('f3def1ce-4cf4-477c-aab3-21cb04e6a382','h3def1ce-4cf4-477c-aab3-21cb04e6a382','cxp9025898_4r81e08','Ericsson1','vSGSN','1.23 (CXS101289_R81E08)','CREATED', 'NOT_IN_USE', 'ENABLED'),
('f3def1ce-4cf4-477c-aab3-21cb04e6a383','h3def1ce-4cf4-477c-aab3-21cb04e6a383','cxp9025898_4r81e08','Ericsson2','SAPC','1.26 (CXS101289_R81E08)','CREATED', 'IN_USE', 'ENABLED'),
('f3def1ce-4cf4-477c-aab3-21cb04e6a384','h3def1ce-4cf4-477c-aab3-21cb04e6a384','cxp9025898_4r81e08','Ericsson3','vMRF','1.21 (CXS101289_R81E08)','CREATED', 'IN_USE', 'ENABLED');

INSERT INTO chart_urls(id, package_id, charts_registry_url, priority)
VALUES ('hf1ce-4cf4-477c-aab3-21c454e6a371', 'f3def1ce-4cf4-477c-aab3-21cb04e6a381', 'http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.25', '1'),
('hf1ce-4cf4-477c-aab3-21c454e6a372', 'f3def1ce-4cf4-477c-aab3-21cb04e6a382', 'http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.20', '1'),
('hf1ce-4cf4-477c-aab3-21c454e6a373', 'f3def1ce-4cf4-477c-aab3-21cb04e6a383', 'http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.25', '1'),
('hf1ce-4cf4-477c-aab3-21c454e6a374', 'f3def1ce-4cf4-477c-aab3-21cb04e6a384', 'http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.20', '1');
