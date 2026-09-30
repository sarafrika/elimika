-- Nightly course aggregates for recommendations and catalogue ranking, rewritten in full by
-- CourseFeatureRefreshJob (01:00 UTC) and read by the 01:30 search rebuild. Non-personal only:
-- no row here names a learner.

CREATE TABLE course_learning_stats
(
    course_uuid             UUID         PRIMARY KEY REFERENCES courses (uuid) ON DELETE CASCADE,
    enrolment_count         INTEGER      NOT NULL DEFAULT 0,
    active_count            INTEGER      NOT NULL DEFAULT 0,
    completed_count         INTEGER      NOT NULL DEFAULT 0,
    dropped_count           INTEGER      NOT NULL DEFAULT 0,
    -- completed / all enrolments; NULL while the course has none.
    completion_rate         NUMERIC(5, 4),
    avg_progress            NUMERIC(5, 2),
    median_days_to_complete NUMERIC(10, 2),
    enrolments_30d          INTEGER      NOT NULL DEFAULT 0,
    -- (C*m + sum of ratings) / (C + n), C = 5, m = the global mean rating; NULL while nothing is rated.
    rating_bayes            NUMERIC(5, 4),
    computed_at             TIMESTAMPTZ  NOT NULL
);

-- Course pairs sharing learners (active or completed enrolments). Both directions are stored.
-- A pair is kept only with at least 5 shared learners, or 10 when any shared learner is a minor.
CREATE TABLE course_co_enrolments
(
    course_uuid           UUID           NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    neighbour_course_uuid UUID           NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    shared_learners       INTEGER        NOT NULL,
    jaccard               NUMERIC(6, 5)  NOT NULL,
    lift                  NUMERIC(12, 4) NOT NULL,
    includes_minors       BOOLEAN        NOT NULL DEFAULT false,
    computed_at           TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_course_co_enrolments PRIMARY KEY (course_uuid, neighbour_course_uuid),
    CONSTRAINT chk_course_co_enrolments_not_self CHECK (course_uuid <> neighbour_course_uuid)
);
