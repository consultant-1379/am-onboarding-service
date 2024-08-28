INSERT INTO app_packages(package_id,descriptor_id,descriptor_version,provider,product_name,software_version,onboarding_state, usage_state, operational_state)
VALUES ('f3def1ce-4cf4-477c-123','h3defabc','cxp9025898_4r81e08','SGSN.1_21_CXS101289_R81E08','Ericsson','1.21 (CXS101289_R81E08)','ONBOARDED',
'NOT_IN_USE', 'ENABLED');
INSERT INTO app_packages(package_id,descriptor_id,descriptor_version,provider,product_name,software_version,onboarding_state, usage_state, operational_state)
VALUES ('f3def1ce-4cf4-477c-456','h3defdef','cxp9025898_4r81e08','SGSN.1_21_CXS101289_R81E08','Ericsson','1.21 (CXS101289_R81E08)','ONBOARDED',
'NOT_IN_USE', 'ENABLED');
INSERT INTO app_packages(package_id,descriptor_id,descriptor_version,provider,product_name,software_version,onboarding_state, usage_state, operational_state)
VALUES ('f3def1ce-4cf4-477c-789','h3defghi','cxp9025898_4r81e08','SGSN.1_21_CXS101289_R81E08','Ericsson','1.21 (CXS101289_R81E08)','ONBOARDED',
'NOT_IN_USE', 'ENABLED');

INSERT INTO app_package_artifacts(id, package_id, artifact_path, artifact)
VALUES ('1', 'f3def1ce-4cf4-477c-123','Definitions/OtherTemplates/sample-vnf-0.1.2.tgz','Sample zip file');
INSERT INTO app_package_artifacts(id, package_id, artifact_path, artifact)
VALUES ('2', 'f3def1ce-4cf4-477c-456','TOSCA-Metadata/TOSCA.meta','Sample text/plain file content');
INSERT INTO app_package_artifacts(id, package_id, artifact_path, artifact)
VALUES ('3', 'f3def1ce-4cf4-477c-789','Definitions/OtherTemplates/sample-vnf-0.1.2.tgz','Sample zip file');
INSERT INTO app_package_artifacts(id, package_id, artifact_path, artifact)
VALUES ('4', 'c2def1ce-4cf4-477c','TOSCA-Metadata/TOSCA.meta','Entry-Definitions: Definitions/cnf_vnfd.yaml');

