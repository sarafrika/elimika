-- Create content_moderation_history table to audit admin approval decisions for courses and programs

CREATE TABLE content_moderation_history (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    content_type VARCHAR(20) NOT NULL,
    content_uuid UUID NOT NULL,

    action VARCHAR(20) NOT NULL,
    reason TEXT,
    moderator_uuid UUID,

    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255)
);

CREATE INDEX idx_cmh_content ON content_moderation_history (content_type, content_uuid, created_date DESC);

COMMENT ON TABLE content_moderation_history IS 'Audit trail of admin moderation decisions for courses and training programs.';
COMMENT ON COLUMN content_moderation_history.content_type IS 'Type of moderated content: course or training_program.';
COMMENT ON COLUMN content_moderation_history.content_uuid IS 'UUID of the moderated course or training program (no FK; spans multiple tables).';
COMMENT ON COLUMN content_moderation_history.action IS 'Moderation decision: approved, rejected or revoked.';
COMMENT ON COLUMN content_moderation_history.moderator_uuid IS 'Internal user UUID of the admin who made the decision.';
