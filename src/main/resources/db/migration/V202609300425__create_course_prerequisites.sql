-- Structured course-to-course prerequisites, edited by the course's author in the course editor.
-- is_mandatory = true is a required prior course; false is a recommended one.
-- A row whose course_uuid is a shadow draft is part of a pending edit and is promoted with it.

CREATE TABLE course_prerequisites
(
    id                       BIGSERIAL PRIMARY KEY,
    uuid                     UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid              UUID         NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    prerequisite_course_uuid UUID         NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    is_mandatory             BOOLEAN      NOT NULL DEFAULT true,
    created_date             TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date             TIMESTAMP,
    created_by               VARCHAR(255) NOT NULL,
    updated_by               VARCHAR(255),
    CONSTRAINT uq_course_prerequisites_pair UNIQUE (course_uuid, prerequisite_course_uuid),
    CONSTRAINT chk_course_prerequisites_not_self CHECK (course_uuid <> prerequisite_course_uuid)
);

CREATE INDEX idx_course_prerequisites_prerequisite ON course_prerequisites (prerequisite_course_uuid);

-- Backfill from program sequences: a course that follows another inside any program requires it.
-- Shadow drafts are skipped on both sides, and a pair whose reverse is also declared by some program
-- is left out rather than written as a two-course cycle.
INSERT INTO course_prerequisites (course_uuid, prerequisite_course_uuid, is_mandatory, created_by)
SELECT DISTINCT pc.course_uuid, pc.prerequisite_course_uuid, true, 'system:program-backfill'
FROM program_courses pc
         JOIN courses c ON c.uuid = pc.course_uuid AND c.parent_course_uuid IS NULL
         JOIN courses p ON p.uuid = pc.prerequisite_course_uuid AND p.parent_course_uuid IS NULL
WHERE pc.prerequisite_course_uuid IS NOT NULL
  AND pc.prerequisite_course_uuid <> pc.course_uuid
  AND NOT EXISTS (SELECT 1
                  FROM program_courses r
                  WHERE r.course_uuid = pc.prerequisite_course_uuid
                    AND r.prerequisite_course_uuid = pc.course_uuid)
ON CONFLICT (course_uuid, prerequisite_course_uuid) DO NOTHING;
