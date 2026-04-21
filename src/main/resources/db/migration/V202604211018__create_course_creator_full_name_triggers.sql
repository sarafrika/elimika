CREATE OR REPLACE FUNCTION generate_course_creator_full_name_from_user()
    RETURNS TRIGGER AS $$
DECLARE
    user_first_name VARCHAR(50);
    user_middle_name VARCHAR(50);
    user_last_name VARCHAR(50);
BEGIN
    SELECT first_name, middle_name, last_name
    INTO user_first_name, user_middle_name, user_last_name
    FROM users
    WHERE uuid = NEW.user_uuid;

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

    IF NEW.full_name = '' OR NEW.full_name IS NULL THEN
        NEW.full_name = COALESCE(user_first_name, 'Unknown');
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generate_course_creator_full_name_from_user
    BEFORE INSERT OR UPDATE ON course_creators
    FOR EACH ROW
    EXECUTE FUNCTION generate_course_creator_full_name_from_user();

CREATE OR REPLACE FUNCTION update_course_creator_full_name_on_user_change()
    RETURNS TRIGGER AS $$
BEGIN
    UPDATE course_creators
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

CREATE TRIGGER trigger_update_course_creator_full_name_on_user_change
    AFTER UPDATE OF first_name, middle_name, last_name ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_course_creator_full_name_on_user_change();
