-- Per-session pricing is retired: rate cards keep hourly and daily rates only.
-- Dropping a column also drops any multi-column CHECK naming it, so the positivity checks are rebuilt over the eight remaining rates.

ALTER TABLE course_training_applications
    DROP CONSTRAINT IF EXISTS chk_course_training_application_rates_positive;
ALTER TABLE course_training_applications
    DROP COLUMN IF EXISTS private_online_session_rate,
    DROP COLUMN IF EXISTS private_inperson_session_rate,
    DROP COLUMN IF EXISTS group_online_session_rate,
    DROP COLUMN IF EXISTS group_inperson_session_rate;
ALTER TABLE course_training_applications
    ADD CONSTRAINT chk_course_training_application_rates_positive CHECK (
        (private_online_hourly_rate IS NULL OR private_online_hourly_rate > 0)
        AND (private_inperson_hourly_rate IS NULL OR private_inperson_hourly_rate > 0)
        AND (group_online_hourly_rate IS NULL OR group_online_hourly_rate > 0)
        AND (group_inperson_hourly_rate IS NULL OR group_inperson_hourly_rate > 0)
        AND (private_online_daily_rate IS NULL OR private_online_daily_rate > 0)
        AND (private_inperson_daily_rate IS NULL OR private_inperson_daily_rate > 0)
        AND (group_online_daily_rate IS NULL OR group_online_daily_rate > 0)
        AND (group_inperson_daily_rate IS NULL OR group_inperson_daily_rate > 0)
    );

ALTER TABLE program_training_applications
    DROP CONSTRAINT IF EXISTS chk_program_training_application_rates_positive;
ALTER TABLE program_training_applications
    DROP COLUMN IF EXISTS private_online_session_rate,
    DROP COLUMN IF EXISTS private_inperson_session_rate,
    DROP COLUMN IF EXISTS group_online_session_rate,
    DROP COLUMN IF EXISTS group_inperson_session_rate;
ALTER TABLE program_training_applications
    ADD CONSTRAINT chk_program_training_application_rates_positive CHECK (
        (private_online_hourly_rate IS NULL OR private_online_hourly_rate > 0)
        AND (private_inperson_hourly_rate IS NULL OR private_inperson_hourly_rate > 0)
        AND (group_online_hourly_rate IS NULL OR group_online_hourly_rate > 0)
        AND (group_inperson_hourly_rate IS NULL OR group_inperson_hourly_rate > 0)
        AND (private_online_daily_rate IS NULL OR private_online_daily_rate > 0)
        AND (private_inperson_daily_rate IS NULL OR private_inperson_daily_rate > 0)
        AND (group_online_daily_rate IS NULL OR group_online_daily_rate > 0)
        AND (group_inperson_daily_rate IS NULL OR group_inperson_daily_rate > 0)
    );

ALTER TABLE course_training_rate_updates
    DROP CONSTRAINT IF EXISTS chk_course_training_rate_updates_rates_positive;
ALTER TABLE course_training_rate_updates
    DROP COLUMN IF EXISTS private_online_session_rate,
    DROP COLUMN IF EXISTS private_inperson_session_rate,
    DROP COLUMN IF EXISTS group_online_session_rate,
    DROP COLUMN IF EXISTS group_inperson_session_rate;
ALTER TABLE course_training_rate_updates
    ADD CONSTRAINT chk_course_training_rate_updates_rates_positive CHECK (
        (private_online_hourly_rate IS NULL OR private_online_hourly_rate > 0)
        AND (private_inperson_hourly_rate IS NULL OR private_inperson_hourly_rate > 0)
        AND (group_online_hourly_rate IS NULL OR group_online_hourly_rate > 0)
        AND (group_inperson_hourly_rate IS NULL OR group_inperson_hourly_rate > 0)
        AND (private_online_daily_rate IS NULL OR private_online_daily_rate > 0)
        AND (private_inperson_daily_rate IS NULL OR private_inperson_daily_rate > 0)
        AND (group_online_daily_rate IS NULL OR group_online_daily_rate > 0)
        AND (group_inperson_daily_rate IS NULL OR group_inperson_daily_rate > 0)
    );

ALTER TABLE program_training_rate_updates
    DROP CONSTRAINT IF EXISTS chk_program_training_rate_updates_rates_positive;
ALTER TABLE program_training_rate_updates
    DROP COLUMN IF EXISTS private_online_session_rate,
    DROP COLUMN IF EXISTS private_inperson_session_rate,
    DROP COLUMN IF EXISTS group_online_session_rate,
    DROP COLUMN IF EXISTS group_inperson_session_rate;
ALTER TABLE program_training_rate_updates
    ADD CONSTRAINT chk_program_training_rate_updates_rates_positive CHECK (
        (private_online_hourly_rate IS NULL OR private_online_hourly_rate > 0)
        AND (private_inperson_hourly_rate IS NULL OR private_inperson_hourly_rate > 0)
        AND (group_online_hourly_rate IS NULL OR group_online_hourly_rate > 0)
        AND (group_inperson_hourly_rate IS NULL OR group_inperson_hourly_rate > 0)
        AND (private_online_daily_rate IS NULL OR private_online_daily_rate > 0)
        AND (private_inperson_daily_rate IS NULL OR private_inperson_daily_rate > 0)
        AND (group_online_daily_rate IS NULL OR group_online_daily_rate > 0)
        AND (group_inperson_daily_rate IS NULL OR group_inperson_daily_rate > 0)
    );
