-- A training method (format x location) the applicant does not offer is stored as NULL in all three
-- bases. Zero used to stand in for "not offered" and could satisfy a rate check it never priced.

ALTER TABLE course_training_applications
    ALTER COLUMN private_online_hourly_rate DROP NOT NULL,
    ALTER COLUMN private_inperson_hourly_rate DROP NOT NULL,
    ALTER COLUMN group_online_hourly_rate DROP NOT NULL,
    ALTER COLUMN group_inperson_hourly_rate DROP NOT NULL;

ALTER TABLE program_training_applications
    ALTER COLUMN private_online_hourly_rate DROP NOT NULL,
    ALTER COLUMN private_inperson_hourly_rate DROP NOT NULL,
    ALTER COLUMN group_online_hourly_rate DROP NOT NULL,
    ALTER COLUMN group_inperson_hourly_rate DROP NOT NULL;

UPDATE course_training_applications
SET private_online_hourly_rate    = NULLIF(private_online_hourly_rate, 0),
    private_inperson_hourly_rate  = NULLIF(private_inperson_hourly_rate, 0),
    group_online_hourly_rate      = NULLIF(group_online_hourly_rate, 0),
    group_inperson_hourly_rate    = NULLIF(group_inperson_hourly_rate, 0),
    private_online_session_rate   = NULLIF(private_online_session_rate, 0),
    private_inperson_session_rate = NULLIF(private_inperson_session_rate, 0),
    group_online_session_rate     = NULLIF(group_online_session_rate, 0),
    group_inperson_session_rate   = NULLIF(group_inperson_session_rate, 0),
    private_online_daily_rate     = NULLIF(private_online_daily_rate, 0),
    private_inperson_daily_rate   = NULLIF(private_inperson_daily_rate, 0),
    group_online_daily_rate       = NULLIF(group_online_daily_rate, 0),
    group_inperson_daily_rate     = NULLIF(group_inperson_daily_rate, 0)
WHERE 0 IN (private_online_hourly_rate, private_inperson_hourly_rate, group_online_hourly_rate,
            group_inperson_hourly_rate, private_online_session_rate, private_inperson_session_rate,
            group_online_session_rate, group_inperson_session_rate, private_online_daily_rate,
            private_inperson_daily_rate, group_online_daily_rate, group_inperson_daily_rate);

UPDATE program_training_applications
SET private_online_hourly_rate    = NULLIF(private_online_hourly_rate, 0),
    private_inperson_hourly_rate  = NULLIF(private_inperson_hourly_rate, 0),
    group_online_hourly_rate      = NULLIF(group_online_hourly_rate, 0),
    group_inperson_hourly_rate    = NULLIF(group_inperson_hourly_rate, 0),
    private_online_session_rate   = NULLIF(private_online_session_rate, 0),
    private_inperson_session_rate = NULLIF(private_inperson_session_rate, 0),
    group_online_session_rate     = NULLIF(group_online_session_rate, 0),
    group_inperson_session_rate   = NULLIF(group_inperson_session_rate, 0),
    private_online_daily_rate     = NULLIF(private_online_daily_rate, 0),
    private_inperson_daily_rate   = NULLIF(private_inperson_daily_rate, 0),
    group_online_daily_rate       = NULLIF(group_online_daily_rate, 0),
    group_inperson_daily_rate     = NULLIF(group_inperson_daily_rate, 0)
WHERE 0 IN (private_online_hourly_rate, private_inperson_hourly_rate, group_online_hourly_rate,
            group_inperson_hourly_rate, private_online_session_rate, private_inperson_session_rate,
            group_online_session_rate, group_inperson_session_rate, private_online_daily_rate,
            private_inperson_daily_rate, group_online_daily_rate, group_inperson_daily_rate);

-- With zero gone, a stored rate is either absent or a real, positive price.
ALTER TABLE course_training_applications
    DROP CONSTRAINT IF EXISTS chk_course_training_rate_card_modalities_non_negative;
ALTER TABLE course_training_applications
    ADD CONSTRAINT chk_course_training_application_rates_positive CHECK (
        (private_online_hourly_rate IS NULL OR private_online_hourly_rate > 0)
        AND (private_inperson_hourly_rate IS NULL OR private_inperson_hourly_rate > 0)
        AND (group_online_hourly_rate IS NULL OR group_online_hourly_rate > 0)
        AND (group_inperson_hourly_rate IS NULL OR group_inperson_hourly_rate > 0)
        AND (private_online_session_rate IS NULL OR private_online_session_rate > 0)
        AND (private_inperson_session_rate IS NULL OR private_inperson_session_rate > 0)
        AND (group_online_session_rate IS NULL OR group_online_session_rate > 0)
        AND (group_inperson_session_rate IS NULL OR group_inperson_session_rate > 0)
        AND (private_online_daily_rate IS NULL OR private_online_daily_rate > 0)
        AND (private_inperson_daily_rate IS NULL OR private_inperson_daily_rate > 0)
        AND (group_online_daily_rate IS NULL OR group_online_daily_rate > 0)
        AND (group_inperson_daily_rate IS NULL OR group_inperson_daily_rate > 0)
    );

ALTER TABLE program_training_applications
    DROP CONSTRAINT IF EXISTS chk_program_training_rate_card_modalities_non_negative;
ALTER TABLE program_training_applications
    ADD CONSTRAINT chk_program_training_application_rates_positive CHECK (
        (private_online_hourly_rate IS NULL OR private_online_hourly_rate > 0)
        AND (private_inperson_hourly_rate IS NULL OR private_inperson_hourly_rate > 0)
        AND (group_online_hourly_rate IS NULL OR group_online_hourly_rate > 0)
        AND (group_inperson_hourly_rate IS NULL OR group_inperson_hourly_rate > 0)
        AND (private_online_session_rate IS NULL OR private_online_session_rate > 0)
        AND (private_inperson_session_rate IS NULL OR private_inperson_session_rate > 0)
        AND (group_online_session_rate IS NULL OR group_online_session_rate > 0)
        AND (group_inperson_session_rate IS NULL OR group_inperson_session_rate > 0)
        AND (private_online_daily_rate IS NULL OR private_online_daily_rate > 0)
        AND (private_inperson_daily_rate IS NULL OR private_inperson_daily_rate > 0)
        AND (group_online_daily_rate IS NULL OR group_online_daily_rate > 0)
        AND (group_inperson_daily_rate IS NULL OR group_inperson_daily_rate > 0)
    );
