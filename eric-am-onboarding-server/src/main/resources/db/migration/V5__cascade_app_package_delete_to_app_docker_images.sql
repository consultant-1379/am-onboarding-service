ALTER TABLE app_docker_images DROP CONSTRAINT app_docker_images_package_id_fkey;
AlTER TABLE app_docker_images ADD CONSTRAINT app_docker_images_package_id_fkey FOREIGN KEY (package_id) REFERENCES app_packages (id) ON DELETE CASCADE
 ON UPDATE CASCADE;
