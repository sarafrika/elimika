-- Organisation student groups (cohorts / streams) for the org dashboard "Groups" page.
-- A group is an org-scoped, named collection of students. Membership is a simple
-- join table keyed by student user UUID. Both tables follow the BaseEntity audit shape.

CREATE TABLE student_groups
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    organisation_uuid UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    description       TEXT,
    group_type        VARCHAR(100),

    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_by        VARCHAR(255)
);

CREATE INDEX idx_student_groups_organisation ON student_groups (organisation_uuid);

COMMENT ON TABLE student_groups IS
    'Org-scoped named collections of students (cohorts/streams) shown on the organisation Groups page.';

CREATE TABLE student_group_members
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    group_uuid    UUID                     NOT NULL REFERENCES student_groups (uuid) ON DELETE CASCADE,
    student_uuid  UUID                     NOT NULL,

    created_date  TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date  TIMESTAMP WITH TIME ZONE,
    created_by    VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_by    VARCHAR(255)
);

CREATE UNIQUE INDEX uq_student_group_member ON student_group_members (group_uuid, student_uuid);
CREATE INDEX idx_student_group_members_group ON student_group_members (group_uuid);

COMMENT ON TABLE student_group_members IS
    'Membership rows linking a student (by user UUID) to a student_group.';
