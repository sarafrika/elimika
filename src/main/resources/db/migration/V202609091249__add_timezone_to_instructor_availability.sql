-- Migration: Add timezone column to instructor_availability
-- Author: Wilfred Njuguna
-- Date: 2026-09-09

-- start_time/end_time are a wall clock in the instructor's own zone, but nothing recorded which
-- zone that was, so every reader treated them as UTC and published 09:00 Nairobi as 09:00Z.

-- 'UTC' is the only backfill that changes nothing: readers already assumed UTC, so stamping it
-- makes the new conversion the identity and leaves existing rows deciding what they decided
-- yesterday. Instructors state their real zone on the next save.
ALTER TABLE instructor_availability
    ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC';

COMMENT ON COLUMN instructor_availability.timezone IS 'IANA timezone the start_time/end_time wall clock was authored in; UTC for rows recorded before the zone was captured';
