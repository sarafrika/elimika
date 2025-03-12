-- 1. First create the sequence
CREATE SEQUENCE organisation_code_seq
    START WITH 100000
    INCREMENT BY 1
    MINVALUE 100000
    NO MAXVALUE
    CACHE 1;

-- 2. Create the function to generate unique codes
CREATE OR REPLACE FUNCTION generate_unique_organisation_code()
    RETURNS TEXT AS
$$
DECLARE
    next_code BIGINT;
BEGIN
    -- Get the next value from the sequence
    next_code := nextval('organisation_code_seq');

    -- Return the formatted organization code (6 digits with leading zeros)
    RETURN LPAD(next_code::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql;

-- 3. Create the trigger function
CREATE OR REPLACE FUNCTION set_organisation_code()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Set the code using the generated unique organization code
    NEW.code := generate_unique_organisation_code();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 4. Finally, create the trigger
CREATE TRIGGER set_organisation_code_before_insert
    BEFORE INSERT
    ON organisation
    FOR EACH ROW
EXECUTE FUNCTION set_organisation_code();