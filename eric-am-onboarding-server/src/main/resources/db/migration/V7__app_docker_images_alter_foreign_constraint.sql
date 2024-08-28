ALTER TABLE app_docker_images RENAME COLUMN package_id TO descriptor_id;

ALTER TABLE app_docker_images ALTER COLUMN descriptor_id SET DATA TYPE VARCHAR;

ALTER TABLE app_docker_images ADD CONSTRAINT app_docker_images_fkey FOREIGN KEY (descriptor_id) REFERENCES app_packages (descriptor_id) ON DELETE CASCADE ON UPDATE CASCADE;
