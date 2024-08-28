-- -----------------------------------------------------
-- ENUM `chart_type_enum`
-- -----------------------------------------------------

create type chart_type_enum as ENUM('CNF', 'CRD');

ALTER TABLE chart_urls ADD chart_type chart_type_enum DEFAULT 'CNF';