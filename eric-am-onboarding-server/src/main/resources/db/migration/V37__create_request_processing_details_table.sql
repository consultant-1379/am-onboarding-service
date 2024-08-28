-- -----------------------------------------------------
-- ENUM `processing_state`
-- -----------------------------------------------------
CREATE TYPE processing_state_enum
AS ENUM ('STARTED', 'FINISHED');

-- -----------------------------------------------------
-- Table `request_processing_details`
-- -----------------------------------------------------
CREATE TABLE request_processing_details (
  request_id VARCHAR UNIQUE,
  request_hash VARCHAR NOT NULL,
  response_code INTEGER,
  response_headers VARCHAR,
  response_body VARCHAR,
  processing_state processing_state_enum NOT NULL,
  retry_after INTEGER NOT NULL,
  creation_time TIMESTAMP NOT NULL,

  PRIMARY KEY (request_id)
);