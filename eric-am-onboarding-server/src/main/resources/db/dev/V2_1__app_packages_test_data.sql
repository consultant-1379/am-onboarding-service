INSERT INTO app_packages(package_id,descriptor_id,descriptor_version,provider,product_name,software_version,onboarding_state,charts_registry_url)
VALUES ('b3def1ce-4cf4-477c-aab3-21cb04e6a379','b3def1ce-4cf4-477c-aab3-21cb04e6a379','cxp9025898_4r81e08','Ericsson','SGSN-MME','1.20 (CXS101289_R81E08)','CREATED','http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.20'),
('e3def1ce-4cf4-477c-aab3-21cb04e6a379','e3def1ce-4cf4-477c-aab3-21cb04e6a379','cxp9025898_4r81e08','Ericsson','SGSN-MME','1.20 (CXS101289_R81E08)','CREATED','http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.20'),
('3e0f5f6a-bcc2-4279-82ef-b3a11a14d456','3e0f5f6a-bcc2-4279-82ef-b3a11a14d456','cxp9025898_4r81e08','Ericsson','SGSN-MME','1.20 (CXS101289_R81E08)','CREATED','http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.24'),
('1','1','cxp9025898_4r81e08','Ericsson','SGSN-MME','1.20 (CXS101289_R81E08)','CREATED','http://10.210.53.96:31028/api/onboarded/charts/Ericsson.SGSN-MME/1.20');
INSERT INTO app_docker_images (package_id, image_id)
VALUES (1, 'docker.io/justwatch/elasticsearch_exporter:1.0.2');

INSERT INTO app_docker_images (package_id, image_id)
VALUES (1, 'armdocker.rnd.ericsson.se/proj-nose/adp-ns:0.0.1-222');

INSERT INTO app_docker_images (package_id, image_id)
VALUES (1, 'armdocker.rnd.ericsson.se/proj-nose/adp-ns-gs:0.0.1-222');
