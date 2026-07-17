-- Extend draft-over-live editing to a course's assessments.
--
-- Like lessons, assessments and their line items are referenced by learner data — assessment
-- scores (RESTRICT) and line-item scores and rubric evaluations (CASCADE). Promotion must
-- therefore update matched rows in place, preserving their uuids, and deactivate removed rows
-- rather than deleting them. Requirements and training requirements carry no learner data, so
-- they are replaced wholesale on promotion and need no source link.

ALTER TABLE course_assessments
    ADD COLUMN source_assessment_uuid UUID;

-- course_assessments has no visibility flag of its own; add one so a removed assessment can be
-- deactivated instead of deleted, keeping its scores valid.
ALTER TABLE course_assessments
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_course_assessments_source ON course_assessments (source_assessment_uuid);

COMMENT ON COLUMN course_assessments.source_assessment_uuid IS
    'On a draft assessment, the live assessment it will be promoted onto. NULL means the edit adds it.';
COMMENT ON COLUMN course_assessments.active IS
    'False on an assessment removed by an approved edit; kept rather than deleted so learner scores stay valid.';

ALTER TABLE course_assessment_line_items
    ADD COLUMN source_line_item_uuid UUID;

CREATE INDEX idx_course_assessment_line_items_source ON course_assessment_line_items (source_line_item_uuid);

COMMENT ON COLUMN course_assessment_line_items.source_line_item_uuid IS
    'On a draft line item, the live line item it will be promoted onto. NULL means the edit adds it.';

-- Requirements carry no learner data, so a removed one can simply be deleted on promotion.
-- They still get a source link so that editing a live requirement while a draft is open is
-- routed to the draft's copy of it, matching how lessons and assessments behave.
ALTER TABLE course_requirements
    ADD COLUMN source_requirement_uuid UUID;

CREATE INDEX idx_course_requirements_source ON course_requirements (source_requirement_uuid);

COMMENT ON COLUMN course_requirements.source_requirement_uuid IS
    'On a draft requirement, the live requirement it will be promoted onto. NULL means the edit adds it.';

ALTER TABLE course_training_requirements
    ADD COLUMN source_requirement_uuid UUID;

CREATE INDEX idx_course_training_requirements_source ON course_training_requirements (source_requirement_uuid);

COMMENT ON COLUMN course_training_requirements.source_requirement_uuid IS
    'On a draft training requirement, the live one it will be promoted onto. NULL means the edit adds it.';
