-- A job is contracted per hour, per session or per day, so an instructor's rate card has to be able
-- to answer in whichever unit the contract used. The existing four columns were always per hour;
-- they are renamed to say so, because the unit being implicit is what let revenue and cost drift
-- apart in the first place.

ALTER TABLE course_training_applications
    RENAME COLUMN private_online_rate TO private_online_hourly_rate;
ALTER TABLE course_training_applications
    RENAME COLUMN private_inperson_rate TO private_inperson_hourly_rate;
ALTER TABLE course_training_applications
    RENAME COLUMN group_online_rate TO group_online_hourly_rate;
ALTER TABLE course_training_applications
    RENAME COLUMN group_inperson_rate TO group_inperson_hourly_rate;

ALTER TABLE program_training_applications
    RENAME COLUMN private_online_rate TO private_online_hourly_rate;
ALTER TABLE program_training_applications
    RENAME COLUMN private_inperson_rate TO private_inperson_hourly_rate;
ALTER TABLE program_training_applications
    RENAME COLUMN group_online_rate TO group_online_hourly_rate;
ALTER TABLE program_training_applications
    RENAME COLUMN group_inperson_rate TO group_inperson_hourly_rate;

-- The new bases are nullable on purpose. Every card on file today holds only hourly rates, and a
-- NOT NULL here would invalidate every approved training application at once. They are required by
-- the API on create and update; a card without them simply cannot be matched to a per-session or
-- per-day job until its owner fills them in.
ALTER TABLE course_training_applications
    ADD COLUMN IF NOT EXISTS private_online_session_rate   NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS private_inperson_session_rate NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS group_online_session_rate     NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS group_inperson_session_rate   NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS private_online_daily_rate     NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS private_inperson_daily_rate   NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS group_online_daily_rate       NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS group_inperson_daily_rate     NUMERIC(12, 4);

ALTER TABLE program_training_applications
    ADD COLUMN IF NOT EXISTS private_online_session_rate   NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS private_inperson_session_rate NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS group_online_session_rate     NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS group_inperson_session_rate   NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS private_online_daily_rate     NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS private_inperson_daily_rate   NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS group_online_daily_rate       NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS group_inperson_daily_rate     NUMERIC(12, 4);

COMMENT ON COLUMN course_training_applications.private_online_hourly_rate
    IS 'Rate per learner per hour. Renamed from private_online_rate; the value is unchanged because it always meant this.';
COMMENT ON COLUMN course_training_applications.private_online_session_rate
    IS 'Rate per learner per session, whatever its length. Null until the instructor prices per-session work.';
COMMENT ON COLUMN course_training_applications.private_online_daily_rate
    IS 'Rate per learner per calendar day, however many sessions fall in it. Null until the instructor prices per-day work.';
