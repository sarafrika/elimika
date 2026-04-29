CREATE TABLE lesson_practice_activities
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    lesson_uuid       UUID                     NOT NULL REFERENCES lessons (uuid) ON DELETE CASCADE,
    title             VARCHAR(255)             NOT NULL,
    instructions      TEXT                     NOT NULL,
    activity_type     VARCHAR(32)              NOT NULL        DEFAULT 'EXERCISE',
    grouping          VARCHAR(32)              NOT NULL        DEFAULT 'INDIVIDUAL',
    estimated_minutes INTEGER,
    materials         TEXT[],
    expected_output   TEXT,
    display_order     INTEGER                  NOT NULL        DEFAULT 1,
    status            VARCHAR(20)              NOT NULL        DEFAULT 'draft',
    active            BOOLEAN                  NOT NULL        DEFAULT false,
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255),
    CONSTRAINT chk_lesson_practice_activity_type
        CHECK (activity_type IN ('EXERCISE', 'DISCUSSION', 'CASE_STUDY', 'ROLE_PLAY', 'REFLECTION', 'HANDS_ON')),
    CONSTRAINT chk_lesson_practice_activity_grouping
        CHECK (grouping IN ('INDIVIDUAL', 'PAIR', 'SMALL_GROUP', 'WHOLE_CLASS')),
    CONSTRAINT chk_lesson_practice_activity_estimated_minutes
        CHECK (estimated_minutes IS NULL OR estimated_minutes > 0),
    CONSTRAINT chk_lesson_practice_activity_display_order
        CHECK (display_order > 0),
    CONSTRAINT chk_lesson_practice_activity_status
        CHECK (status IN ('draft', 'in_review', 'published', 'archived')),
    CONSTRAINT chk_lesson_practice_activity_active_published
        CHECK (active = false OR (active = true AND status = 'published'))
);

CREATE INDEX idx_lesson_practice_activities_uuid ON lesson_practice_activities (uuid);
CREATE INDEX idx_lesson_practice_activities_lesson_uuid ON lesson_practice_activities (lesson_uuid);
CREATE INDEX idx_lesson_practice_activities_display_order ON lesson_practice_activities (display_order);
CREATE INDEX idx_lesson_practice_activities_status ON lesson_practice_activities (status);
CREATE INDEX idx_lesson_practice_activities_active ON lesson_practice_activities (active);
CREATE INDEX idx_lesson_practice_activities_type ON lesson_practice_activities (activity_type);
CREATE INDEX idx_lesson_practice_activities_grouping ON lesson_practice_activities (grouping);
