ALTER TABLE data_source
    ADD user_id INT NULL;

ALTER TABLE data_source
    MODIFY user_id INT NOT NULL;

DROP TABLE data_source_seq;

DROP TABLE data_type_seq;

ALTER TABLE data_source
    DROP COLUMN type;