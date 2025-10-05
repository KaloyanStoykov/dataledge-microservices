CREATE TABLE data_type
(
    id            BIGINT NOT NULL,
    name          VARCHAR(255) NULL,
    `description` VARCHAR(255) NULL,
    CONSTRAINT pk_datatype PRIMARY KEY (id)
);

ALTER TABLE data_source
    ADD type_id BIGINT NULL;

ALTER TABLE data_source
    MODIFY type_id BIGINT NOT NULL;

ALTER TABLE data_source
    ADD CONSTRAINT FK_DATASOURCE_ON_TYPE FOREIGN KEY (type_id) REFERENCES data_type (id);

DROP TABLE data_source_seq;

ALTER TABLE data_source
DROP
COLUMN type;