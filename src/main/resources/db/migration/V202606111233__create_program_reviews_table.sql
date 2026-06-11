-- Create program_reviews table to store student feedback about training programs

CREATE TABLE program_reviews (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    program_uuid UUID NOT NULL REFERENCES training_programs (uuid) ON DELETE CASCADE,
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

CREATE INDEX idx_program_reviews_program ON program_reviews (program_uuid);
CREATE INDEX idx_program_reviews_student ON program_reviews (student_uuid);

ALTER TABLE program_reviews
    ADD CONSTRAINT uq_program_review_per_student UNIQUE (program_uuid, student_uuid);

COMMENT ON TABLE program_reviews IS 'Student reviews and ratings for training programs.';
COMMENT ON COLUMN program_reviews.rating IS 'Overall star rating (1-5) provided by the student.';
COMMENT ON COLUMN program_reviews.is_anonymous IS 'When true, public displays should hide the student identity.';
