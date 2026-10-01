-- Remove duplicate course training requirements and stop new ones.
--
-- A creator editing a published course writes into its pending shadow draft, which the editor's
-- requirement list (read from the live course) does not show. A requirement re-submitted because it
-- seemed not to have saved landed in the draft a second time, and approval inserted every unlinked
-- draft row into the live course, so learners saw each such requirement twice.
--
-- Exact duplicates are rows of one course with the same name, type, quantity and provider side.
-- The oldest (lowest id) is kept. The table has no soft-delete column, and requirements carry no
-- learner data, so the extras are deleted. Two things point at a requirement by uuid and are moved
-- onto the kept row first:
--   * draft rows' source_requirement_uuid (the live row a draft copy promotes onto);
--   * training application answers ("do you have this?"), keeping one answer per application.

CREATE TEMP TABLE ctr_duplicate_map ON COMMIT DROP AS
SELECT d.uuid AS duplicate_uuid, k.uuid AS kept_uuid
FROM (SELECT id,
             uuid,
             FIRST_VALUE(id) OVER (PARTITION BY course_uuid, name, requirement_type, quantity, provided_by
                                   ORDER BY id) AS kept_id
      FROM course_training_requirements) d
         JOIN course_training_requirements k ON k.id = d.kept_id
WHERE d.id <> d.kept_id;

UPDATE course_training_requirements r
SET source_requirement_uuid = m.kept_uuid
FROM ctr_duplicate_map m
WHERE r.source_requirement_uuid = m.duplicate_uuid;

UPDATE training_application_requirement_answers a
SET requirement_uuid = m.kept_uuid
FROM ctr_duplicate_map m
WHERE a.requirement_uuid = m.duplicate_uuid
  AND NOT EXISTS (SELECT 1
                  FROM training_application_requirement_answers other
                  WHERE other.application_type = a.application_type
                    AND other.application_uuid = a.application_uuid
                    AND other.requirement_uuid = m.kept_uuid);

DELETE
FROM training_application_requirement_answers a
    USING ctr_duplicate_map m
WHERE a.requirement_uuid = m.duplicate_uuid;

DELETE
FROM course_training_requirements r
    USING ctr_duplicate_map m
WHERE r.uuid = m.duplicate_uuid;

-- One row per requirement per course. NULLS NOT DISTINCT so a missing quantity or provider still
-- counts as the same value. Deferred to commit because draft promotion updates rows in place and
-- inserts new ones in one transaction, and Hibernate flushes inserts before updates and deletes:
-- a rename-and-re-add would otherwise trip the check mid-flush on a state that is valid at commit.
ALTER TABLE course_training_requirements
    ADD CONSTRAINT uq_course_training_requirements_identity
        UNIQUE NULLS NOT DISTINCT (course_uuid, name, requirement_type, quantity, provided_by)
        DEFERRABLE INITIALLY DEFERRED;
