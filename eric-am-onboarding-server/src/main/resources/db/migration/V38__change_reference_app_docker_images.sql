alter table app_docker_images
    ADD COLUMN package_id VARCHAR;

update app_docker_images
set package_id = app_packages.package_id
from app_packages
where app_packages.descriptor_id = app_docker_images.descriptor_id;

alter table app_docker_images
    drop constraint app_docker_images_fkey;

alter table app_docker_images
    add constraint app_docker_images_package_id_fkey
        foreign key (package_id) references app_packages (package_id) on delete cascade on update cascade;

alter table app_docker_images
    drop descriptor_id;

