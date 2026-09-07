-- Turns the class registration window into a real rule: both dates become mandatory, and the
-- enrolment gate refuses a seat outside them.
--
-- The one thing this backfill must not do is close a class that is taking enrolments today. The
-- window has never been enforced, so every existing class is enrollable right now whatever these two
-- columns happen to say, and a backfill that invents a window already in the past would shut a live
-- class with nothing in the product to explain it.
--
-- Two rules follow from that, and every statement below is one of them:
--   1. A date an operator actually stated is never rewritten. Only the half that was left blank is
--      invented.
--   2. An invented half is placed so that today falls inside the window, as far as the stated half
--      allows it to.
--
-- The deliberate exception is a stated closing date in the past. "Registration closed on 7 June" is a
-- sentence an operator wrote and the class listing already shows; honouring it is the whole point of
-- this change, so such a class does close here even though the unenforced window meant it was still
-- enrollable yesterday. Pushing that date out instead would keep the class open at the cost of
-- discarding the only half of the window anybody ever stated, which is the wrong side to give way on.
-- The same reading applies to a stated opening date in the future: it is honoured, and the class is
-- not yet open.
--
-- "Today" is read as (now() AT TIME ZONE 'UTC')::date rather than CURRENT_DATE, so the backfill lands
-- on the same day boundary the gate judges against and does not depend on the session time zone of
-- whoever runs the migration. LEAST and GREATEST ignore NULL arguments in PostgreSQL, so each
-- expression below is non-null on the strength of the one argument that always is.

-- Dropped first, before anything is written. Each UPDATE fills one half of a pair whose other half
-- may still be NULL at that moment, and the old constraint rejects exactly the rows the backfill
-- exists to repair -- an inverted pair mid-backfill would abort the migration, mark it failed in
-- flyway_schema_history and stop the application from starting until someone repaired that by hand.
ALTER TABLE class_definitions
    DROP CONSTRAINT IF EXISTS chk_class_definitions_registration_period_valid;

-- A stated closing date and no opening date. The close is kept exactly as written; the open is pulled
-- back far enough to cover both today and that close, so a class still taking enrolments keeps taking
-- them and one the operator closed stays closed.
UPDATE class_definitions
SET registration_period_start_date = LEAST(
        COALESCE(academic_period_start_date, (created_date AT TIME ZONE 'UTC')::date),
        (now() AT TIME ZONE 'UTC')::date,
        registration_period_end_date
    )
WHERE registration_period_start_date IS NULL
  AND registration_period_end_date IS NOT NULL;

-- A stated opening date and no closing date. The open is kept as written; the close is pushed out to
-- the academic period end, and never lands before today. An academic period that ended before
-- registration even opens says nothing about when registration closes, so those rows fall through to
-- the far-future sentinel along with the rows that have no academic period at all -- which leaves
-- them exactly as open as they are today. Either branch is on or after the opening day, so the pair
-- cannot come out inverted.
UPDATE class_definitions
SET registration_period_end_date = GREATEST(
        CASE
            WHEN academic_period_end_date >= registration_period_start_date THEN academic_period_end_date
            ELSE DATE '2099-12-31'
        END,
        (now() AT TIME ZONE 'UTC')::date
    )
WHERE registration_period_end_date IS NULL
  AND registration_period_start_date IS NOT NULL;

-- Nothing stated at all. The academic period is the best available guess at what was meant, clamped
-- so today is inside the window either way: a class selling seats for a term that starts in October
-- must not be shut for the weeks until then, and one whose term has already ended must not be shut
-- retroactively.
UPDATE class_definitions
SET registration_period_start_date = LEAST(
        COALESCE(academic_period_start_date, (created_date AT TIME ZONE 'UTC')::date),
        (now() AT TIME ZONE 'UTC')::date
    ),
    registration_period_end_date = GREATEST(
        COALESCE(academic_period_end_date, DATE '2099-12-31'),
        (now() AT TIME ZONE 'UTC')::date
    )
WHERE registration_period_start_date IS NULL
  AND registration_period_end_date IS NULL;

-- Rows that stated both halves are left untouched above, and the dropped constraint kept those pairs
-- in order, so this should find nothing. It is here so the constraint added below cannot abort on a
-- row that arrived some other way, and it gives way on the opening date because a stated close is the
-- half a class is actually run by.
UPDATE class_definitions
SET registration_period_start_date = registration_period_end_date
WHERE registration_period_start_date > registration_period_end_date;

ALTER TABLE class_definitions
    ALTER COLUMN registration_period_start_date SET NOT NULL,
    ALTER COLUMN registration_period_end_date SET NOT NULL;

-- Recreated without the NULL branches, which no longer describe anything the column can hold.
ALTER TABLE class_definitions
    ADD CONSTRAINT chk_class_definitions_registration_period_valid
        CHECK (registration_period_start_date <= registration_period_end_date);

COMMENT ON COLUMN class_definitions.registration_period_start_date IS
    'First day, inclusive, on which a student may enrol in this class';
COMMENT ON COLUMN class_definitions.registration_period_end_date IS
    'Last day, inclusive, on which a student may enrol in this class';
