-- Create instructor_reviews table to store student feedback about instructors

CREATE TABLE instructor_reviews (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    instructor_uuid UUID NOT NULL REFERENCES instructors (uuid) ON DELETE CASCADE,
    student_uuid UUID NOT NULL REFERENCES students (uuid) ON DELETE CASCADE,
    enrollment_uuid UUID NOT NULL REFERENCES enrollments (uuid) ON DELETE CASCADE,

    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    headline VARCHAR(255),
    comments TEXT,

    -- Optional quality dimensions for future analytics
    clarity_rating INTEGER CHECK (clarity_rating BETWEEN 1 AND 5),
    engagement_rating INTEGER CHECK (engagement_rating BETWEEN 1 AND 5),
    punctuality_rating INTEGER CHECK (punctuality_rating BETWEEN 1 AND 5),

    is_anonymous BOOLEAN NOT NULL DEFAULT false,

    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255)
);

CREATE INDEX idx_instructor_reviews_instructor ON instructor_reviews (instructor_uuid);
CREATE INDEX idx_instructor_reviews_student ON instructor_reviews (student_uuid);
CREATE INDEX idx_instructor_reviews_enrollment ON instructor_reviews (enrollment_uuid);

ALTER TABLE instructor_reviews
    ADD CONSTRAINT uq_instructor_review_per_enrollment UNIQUE (instructor_uuid, enrollment_uuid);

COMMENT ON TABLE instructor_reviews IS 'Student reviews and ratings for instructors, scoped to enrollments.';
COMMENT ON COLUMN instructor_reviews.rating IS 'Overall star rating (1-5) provided by the student.';
COMMENT ON COLUMN instructor_reviews.is_anonymous IS 'When true, public displays should hide the student identity.';

