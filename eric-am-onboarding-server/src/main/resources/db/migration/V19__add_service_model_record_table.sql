CREATE TABLE service_model_record (
id VARCHAR UNIQUE,
service_model_id VARCHAR DEFAULT NULL,
package_id VARCHAR DEFAULT NULL,
descriptor_id VARCHAR DEFAULT NULL,
service_model_name VARCHAR DEFAULT NULL,
PRIMARY KEY (id),
FOREIGN KEY (package_id) REFERENCES app_packages (package_id) ON DELETE RESTRICT ON UPDATE RESTRICT)