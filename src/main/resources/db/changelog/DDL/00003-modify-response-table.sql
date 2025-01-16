-- Add the survey_response_index column to the table
ALTER TABLE responses ADD COLUMN survey_response_index INTEGER;

-- Function to dynamically assign a unique index per survey_id
CREATE OR REPLACE FUNCTION assign_survey_response_index()
RETURNS TRIGGER AS $$
DECLARE
    seq_name TEXT := 'response_seq_' || NEW.survey_id;
BEGIN
    -- Create the sequence if it does not already exist
    PERFORM 1 FROM pg_class WHERE relname = seq_name;
    IF NOT FOUND THEN
        EXECUTE format('CREATE SEQUENCE %I START 1', seq_name);
    END IF;

    -- Fetch the next value from the sequence
    EXECUTE format('SELECT nextval(%L)', seq_name) INTO NEW.survey_response_index;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to set survey_response_index before insert
CREATE TRIGGER trigger_assign_survey_response_index
BEFORE INSERT ON responses
FOR EACH ROW EXECUTE FUNCTION assign_survey_response_index();

-- Ensure survey_id and survey_response_index are unique together
CREATE UNIQUE INDEX idx_responses_survey_response_index
ON responses (survey_id, survey_response_index);
