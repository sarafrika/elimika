-- Review state for a draft-over-live course edit.
--
-- Kept in its own table rather than on courses so the course lifecycle columns
-- (status, active, admin_approved) keep their existing meaning. A live course with a
-- pending edit stays status='published' and admin_approved=true throughout review.

CREATE TABLE course_pending_edits
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    course_uuid       UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    draft_course_uuid UUID REFERENCES courses (uuid) ON DELETE SET NULL,

    status            VARCHAR(20)              NOT NULL CHECK (status IN ('pending', 'approved', 'rejected', 'withdrawn')),
    submitted_by_uuid UUID,
    submitted_at      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    reviewed_by_uuid  UUID,
    reviewed_at       TIMESTAMP WITH TIME ZONE,
    review_reason     TEXT,

    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255)
);

-- At most one edit awaiting review per course. Resolved rows (approved/rejected/withdrawn)
-- are retained, so this table doubles as the edit-submission history.
CREATE UNIQUE INDEX uq_cpe_one_pending_per_course
    ON course_pending_edits (course_uuid) WHERE status = 'pending';

CREATE INDEX idx_cpe_course ON course_pending_edits (course_uuid, submitted_at DESC);
CREATE INDEX idx_cpe_status ON course_pending_edits (status, submitted_at DESC);
CREATE INDEX idx_cpe_draft_course ON course_pending_edits (draft_course_uuid);

COMMENT ON TABLE course_pending_edits IS
    'Tracks course edits awaiting admin review. The edit itself lives on the draft course row referenced by draft_course_uuid.';
COMMENT ON COLUMN course_pending_edits.course_uuid IS 'The live course the edit applies to.';
COMMENT ON COLUMN course_pending_edits.draft_course_uuid IS
    'Shadow course row holding the proposed content. Nulled once the edit is resolved and the draft is deleted.';
COMMENT ON COLUMN course_pending_edits.status IS 'pending until an admin approves or rejects it, or the creator withdraws it.';
