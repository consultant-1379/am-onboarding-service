INSERT INTO app_docker_images (descriptor_id, image_id)
VALUES ('b3def1ce-4cf4-477c-aab3-21cb04e6a379', 'docker.io/justwatch/elasticsearch_exporter:1.0.2');

INSERT INTO app_docker_images (descriptor_id, image_id)
VALUES ('b3def1ce-4cf4-477c-aab3-21cb04e6a379', 'armdocker.rnd.ericsson.se/proj-nose/adp-ns:0.0.1-222');

INSERT INTO app_docker_images (descriptor_id, image_id)
VALUES ('b3def1ce-4cf4-477c-aab3-21cb04e6a379', 'armdocker.rnd.ericsson.se/proj-nose/adp-ns-gs:0.0.1-222');

INSERT INTO app_docker_images (descriptor_id, image_id)
VALUES ('b3def1ce-4cf4-477c-aab3-21cb04e6a380', 'docker.io/justwatch/elasticsearch_exporter:1.0.2');

INSERT INTO app_docker_images (descriptor_id, image_id)
VALUES ('b3def1ce-4cf4-477c-aab3-21cb04e6a380', 'armdocker.rnd.ericsson.se/proj-nose/adp-ns:0.0.1-222');

INSERT INTO app_docker_images (descriptor_id, image_id)
VALUES ('b3def1ce-4cf4-477c-aab3-21cb04e6a380', 'armdocker.rnd.ericsson.se/proj-nose/adp-ns-gs:0.0.1-222');

INSERT INTO app_docker_images (descriptor_id, image_id)
VALUES ('1', 'docker.io/unique/non-shareable-image:1.0.0');

UPDATE app_packages
SET operational_state = 'DISABLED', usage_state = 'NOT_IN_USE';
