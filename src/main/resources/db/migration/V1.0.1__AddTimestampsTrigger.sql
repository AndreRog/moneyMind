-- V2__Add_Timestamps_Trigger.sql

-- Create a function to set timestamps
CREATE OR REPLACE FUNCTION set_timestamps()
RETURNS TRIGGER AS $$
BEGIN
  -- Set created_at and updated_at on INSERT
  IF TG_OP = 'INSERT' THEN
    NEW.created_at := CURRENT_TIMESTAMP;
    NEW.updated_at := CURRENT_TIMESTAMP;
  -- Set updated_at on UPDATE
  ELSE
    NEW.updated_at := CURRENT_TIMESTAMP;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create the trigger
CREATE TRIGGER set_timestamps_trigger
BEFORE INSERT OR UPDATE ON bank_transaction
FOR EACH ROW EXECUTE FUNCTION set_timestamps();