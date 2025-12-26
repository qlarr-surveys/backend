-- liquibase formatted sql
-- preconditions onFail:HALT onError:HALT
-- changeset master:3

ALTER TABLE auto_complete
ADD COLUMN filename VARCHAR(255) NOT NULL DEFAULT '';

CREATE INDEX idx_auto_complete_survey_filename ON auto_complete(survey_id, filename);
