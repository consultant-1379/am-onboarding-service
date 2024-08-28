CREATE TABLE app_os_container
(
    id                                 VARCHAR,
    package_id                         VARCHAR UNIQUE NOT NULL,
    os_container_deployable_unit_assoc VARCHAR DEFAULT NULL,
    requested_cpu_resources            VARCHAR DEFAULT NULL,
    cpu_resource_limit                 VARCHAR DEFAULT NULL,
    requested_memory_resources         VARCHAR DEFAULT NULL,
    memory_resource_limit              VARCHAR DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE TABLE app_vb_storage
(
    id                                 VARCHAR,
    package_id                         VARCHAR UNIQUE NOT NULL,
    os_container_deployable_unit_assoc VARCHAR DEFAULT NULL,
    size_of_storage                    VARCHAR DEFAULT NULL,
    rdma_enabled                       BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (id),
    FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE RESTRICT ON UPDATE RESTRICT
);