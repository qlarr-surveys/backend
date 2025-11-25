-- liquibase formatted sql
-- preconditions onFail:HALT onError:HALT
-- changeset master:2

CREATE TABLE auto_complete (
    id              UUID            NOT NULL PRIMARY KEY,
    survey_id       UUID            NOT NULL,
    component_id    VARCHAR(255)    NOT NULL,
    data            JSONB           NOT NULL,
    CONSTRAINT fk_auto_complete_survey
        FOREIGN KEY (survey_id)
        REFERENCES surveys(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_auto_complete_survey_component
        UNIQUE (survey_id, component_id)
);