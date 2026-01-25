-- liquibase formatted sql
-- changeset master:6

-- Add filename column (nullable initially)
ALTER TABLE auto_complete ADD COLUMN filename VARCHAR(255);

-- Copy id values to filename for existing records
UPDATE auto_complete SET filename = id::text;

-- Make filename NOT NULL
ALTER TABLE auto_complete ALTER COLUMN filename SET NOT NULL;

-- Drop the existing primary key constraint (id column)
ALTER TABLE auto_complete DROP CONSTRAINT auto_complete_pkey;

-- Drop the id column
ALTER TABLE auto_complete DROP COLUMN id;

-- Create composite primary key on (survey_id, filename)
ALTER TABLE auto_complete ADD PRIMARY KEY (survey_id, filename);
