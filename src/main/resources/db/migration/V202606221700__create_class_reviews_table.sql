-- Create class_reviews table to store student feedback about classes

CREATE TABLE class_reviews (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    class_definition_uuid UUID NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
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

CREATE INDEX idx_class_reviews_class_definition ON class_reviews (class_definition_uuid);
CREATE INDEX idx_class_reviews_student ON class_reviews (student_uuid);

ALTER TABLE class_reviews
    ADD CONSTRAINT uq_class_review_per_student UNIQUE (class_definition_uuid, student_uuid);

COMMENT ON TABLE class_reviews IS 'Student reviews and ratings for class definitions.';
COMMENT ON COLUMN class_reviews.rating IS 'Overall star rating (1-5) provided by the student.';
COMMENT ON COLUMN class_reviews.is_anonymous IS 'When true, public displays should hide the student identity.';
