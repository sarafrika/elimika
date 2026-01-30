-- Create course_reviews table to store student feedback about courses

CREATE TABLE course_reviews (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    course_uuid UUID NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    student_uuid UUID NOT NULL REFERENCES students (uuid) ON DELETE CASCADE,

    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    headline VARCHAR(255),
    comments TEXT,

    is_anonymous BOOLEAN NOT NULL DEFAULT false,

    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255)
);

CREATE INDEX idx_course_reviews_course ON course_reviews (course_uuid);
CREATE INDEX idx_course_reviews_student ON course_reviews (student_uuid);

ALTER TABLE course_reviews
    ADD CONSTRAINT uq_course_review_per_student UNIQUE (course_uuid, student_uuid);

COMMENT ON TABLE course_reviews IS 'Student reviews and ratings for courses.';
COMMENT ON COLUMN course_reviews.rating IS 'Overall star rating (1-5) provided by the student.';
COMMENT ON COLUMN course_reviews.is_anonymous IS 'When true, public displays should hide the student identity.';
