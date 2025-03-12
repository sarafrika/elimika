-- Rename Course Table To Courses
ALTER TABLE course
    RENAME TO courses;

-- Classes Table
CREATE TABLE classes
(
    id                       BIGSERIAL PRIMARY KEY,
    uuid                     UUID                                                               NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid              UUID                                                               NOT NULL,
    trainer_uuid             UUID                                                               NOT NULL,
    start_date               TIMESTAMP                                                          NOT NULL,
    end_date                 TIMESTAMP                                                          NOT NULL,
    class_mode               VARCHAR(20) CHECK (class_mode IN ('ONLINE', 'IN_PERSON'))          NOT NULL,
    location                 VARCHAR(255),
    meeting_link             TEXT,
    schedule                 TEXT,
    capacity_limit           INT                                                                NOT NULL CHECK (capacity_limit > 0),
    current_enrollment_count INT                                                                                DEFAULT 0 CHECK (current_enrollment_count >= 0),
    waiting_list_count       INT                                                                                DEFAULT 0 CHECK (waiting_list_count >= 0),
    group_or_individual      VARCHAR(20) CHECK (group_or_individual IN ('GROUP', 'INDIVIDUAL')) NOT NULL,
    created_date             TIMESTAMP                                                          NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by               VARCHAR(50)                                                        NOT NULL,
    updated_date             TIMESTAMP                                                          NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by               VARCHAR(50),
    deleted                  BOOLEAN                                                            NOT NULL        DEFAULT FALSE
);

CREATE INDEX idx_classes_course_uuid ON classes (course_uuid);
CREATE INDEX idx_classes_trainer_uuid ON classes (trainer_uuid);

-- Waiting List Table
CREATE TABLE waiting_list
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    student_uuid UUID        NOT NULL,
    class_uuid   UUID        NOT NULL,
    position     INT         NOT NULL CHECK (position > 0),
    added_date   TIMESTAMPTZ                 DEFAULT NOW(),
    created_date TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50) NOT NULL,
    updated_date TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50)
);

CREATE INDEX idx_waiting_list_student_uuid ON waiting_list (student_uuid);
CREATE INDEX idx_waiting_list_class_uuid ON waiting_list (class_uuid);

-- Enrollments Table
CREATE TABLE enrollments
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID                                                                          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    student_uuid    UUID                                                                          NOT NULL,
    class_uuid      UUID                                                                          NOT NULL,
    enrollment_date TIMESTAMPTZ                                                                                   DEFAULT NOW(),
    status          VARCHAR(50) CHECK (status IN ('ENROLLED', 'WAITING', 'COMPLETED', 'DROPPED')) NOT NULL,
    created_date    TIMESTAMPTZ                                                                   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)                                                                   NOT NULL,
    updated_date    TIMESTAMPTZ                                                                   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50)
);

CREATE INDEX idx_enrollments_student_uuid ON enrollments (student_uuid);
CREATE INDEX idx_enrollments_class_uuid ON enrollments (class_uuid);

-- Assessments Table
CREATE TABLE class_assessments
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID                                                                  NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    class_uuid    UUID                                                                  NOT NULL,
    type          VARCHAR(50) CHECK (type IN ('QUIZ', 'ASSIGNMENT', 'EXAM', 'PROJECT')) NOT NULL,
    max_score     DECIMAL(10, 4)                                                        NOT NULL CHECK (max_score > 0),
    passing_score DECIMAL(10, 4)                                                        NOT NULL CHECK (passing_score > 0 AND passing_score <= max_score),
    due_date      TIMESTAMPTZ                                                           NOT NULL,
    created_date  TIMESTAMPTZ                                                           NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50)                                                           NOT NULL,
    updated_date  TIMESTAMPTZ                                                           NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50)
);

CREATE INDEX idx_class_assessments_class_uuid ON class_assessments (class_uuid);
CREATE INDEX idx_class_type ON class_assessments (type);

-- Student Submissions Table
CREATE TABLE student_submissions
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    student_uuid    UUID        NOT NULL,
    assessment_uuid UUID        NOT NULL,
    submission      TEXT        NOT NULL,
    score           INT CHECK (score >= 0),
    submitted_at    TIMESTAMPTZ                 DEFAULT NOW(),
    created_date    TIMESTAMPTZ NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    updated_date    TIMESTAMPTZ NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50)
);

CREATE INDEX idx_student_submissions_student_uuid ON student_submissions (student_uuid);
CREATE INDEX idx_student_submissions_assessment_uuid ON student_submissions (assessment_uuid);

-- Certifications Table
CREATE TABLE certifications
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    student_uuid UUID        NOT NULL,
    course_uuid  UUID        NOT NULL,
    issued_at    TIMESTAMPTZ                 DEFAULT NOW(),
    created_date TIMESTAMPTZ NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50) NOT NULL,
    updated_date TIMESTAMPTZ NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50)
);

CREATE INDEX idx_certifications_student_uuid ON certifications (student_uuid);
CREATE INDEX idx_certifications_course_uuid ON certifications (course_uuid);