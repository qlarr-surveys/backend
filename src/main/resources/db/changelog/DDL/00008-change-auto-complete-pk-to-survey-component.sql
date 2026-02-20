-- liquibase formatted sql
-- changeset qlarr:00008

-- Drop the existing primary key (survey_id, filename)
ALTER TABLE auto_complete DROP CONSTRAINT auto_complete_pkey;

-- Drop the unique constraint on (survey_id, component_id) since it becomes the PK
ALTER TABLE auto_complete DROP CONSTRAINT uk_auto_complete_survey_component;

-- Create composite primary key on (survey_id, component_id)
ALTER TABLE auto_complete ADD PRIMARY KEY (survey_id, component_id);