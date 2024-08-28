CREATE TABLE operation_detail
(
    id             VARCHAR UNIQUE,
    operation_name VARCHAR NOT NULL,
    supported      BOOLEAN NOT NULL,
    error_message  VARCHAR,
    package_id     VARCHAR NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE CASCADE ON UPDATE RESTRICT
);