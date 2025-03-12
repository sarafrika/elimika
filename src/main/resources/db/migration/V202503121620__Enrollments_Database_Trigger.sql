CREATE OR REPLACE FUNCTION handle_class_enrollment()
    RETURNS TRIGGER AS
$$
DECLARE
    enrollment_count INT;
    waiting_position INT;
BEGIN
    -- Get current enrollment count
    SELECT current_enrollment_count
    INTO enrollment_count
    FROM classes
    WHERE uuid = NEW.class_uuid;

    -- If class has space, allow enrollment
    IF enrollment_count < (SELECT capacity_limit FROM classes WHERE uuid = NEW.class_uuid) THEN
        -- Increment enrollment count
        UPDATE classes
        SET current_enrollment_count = current_enrollment_count + 1
        WHERE uuid = NEW.class_uuid;

        RETURN NEW;
    ELSE
        -- Determine waiting list position
        SELECT COALESCE(MAX(position), 0) + 1
        INTO waiting_position
        FROM waiting_list
        WHERE class_uuid = NEW.class_uuid;

        -- Insert into waiting list instead
        INSERT INTO waiting_list (student_uuid, class_uuid, position, created_date, created_by)
        VALUES (NEW.student_uuid, NEW.class_uuid, waiting_position, NOW(), NEW.created_by);

        -- Increment waiting list count
        UPDATE classes
        SET waiting_list_count = waiting_list_count + 1
        WHERE uuid = NEW.class_uuid;

        -- Prevent actual insertion into enrollments
        RETURN NULL;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_class_capacity
    BEFORE INSERT
    ON enrollments
    FOR EACH ROW
EXECUTE FUNCTION handle_class_enrollment();
