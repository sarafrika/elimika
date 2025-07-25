CREATE OR REPLACE FUNCTION generate_instructor_full_name_from_user()
    RETURNS TRIGGER AS $$
DECLARE
    user_first_name VARCHAR(50);
    user_middle_name VARCHAR(50);
    user_last_name VARCHAR(50);
BEGIN
    -- Get user names from users table
    SELECT first_name, middle_name, last_name
    INTO user_first_name, user_middle_name, user_last_name
    FROM users
    WHERE uuid = NEW.user_uuid;

    -- Generate full name from user data
    NEW.full_name = TRIM(
            CONCAT(
                    COALESCE(user_first_name, ''),
                    CASE
                        WHEN user_middle_name IS NOT NULL AND user_middle_name != ''
                            THEN ' ' || user_middle_name
                        ELSE ''
                        END,
                    CASE
                        WHEN user_last_name IS NOT NULL AND user_last_name != ''
                            THEN ' ' || user_last_name
                        ELSE ''
                        END
            )
                    );

    -- Handle edge case
    IF NEW.full_name = '' OR NEW.full_name IS NULL THEN
        NEW.full_name = COALESCE(user_first_name, 'Unknown');
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for students table using user data
CREATE TRIGGER trigger_generate_instructor_full_name_from_user
    BEFORE INSERT OR UPDATE ON students
    FOR EACH ROW
EXECUTE FUNCTION generate_instructor_full_name_from_user();

-- Update students.full_name when users table is updated
CREATE OR REPLACE FUNCTION update_instructor_full_name_on_user_change()
    RETURNS TRIGGER AS $$
BEGIN
    -- Update all students linked to this user
    UPDATE instructors
    SET full_name = TRIM(
            CONCAT(
                    COALESCE(NEW.first_name, ''),
                    CASE
                        WHEN NEW.middle_name IS NOT NULL AND NEW.middle_name != ''
                            THEN ' ' || NEW.middle_name
                        ELSE ''
                        END,
                    CASE
                        WHEN NEW.last_name IS NOT NULL AND NEW.last_name != ''
                            THEN ' ' || NEW.last_name
                        ELSE ''
                        END
            )
                    ),
        updated_date = CURRENT_TIMESTAMP,
        updated_by = NEW.updated_by
    WHERE user_uuid = NEW.uuid;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update student full_name when user names change
CREATE TRIGGER trigger_update_instructor_full_name_on_user_change
    AFTER UPDATE OF first_name, middle_name, last_name ON users
    FOR EACH ROW
EXECUTE FUNCTION update_instructor_full_name_on_user_change();