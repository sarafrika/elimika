CREATE TABLE class_session_templates
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID                     NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    class_definition_uuid UUID                     NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    template_order        INTEGER                  NOT NULL,
    start_time            TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time              TIMESTAMP WITH TIME ZONE NOT NULL,
    recurrence_type       VARCHAR(32),
    interval_value        INTEGER,
    days_of_week          VARCHAR(128),
    day_of_month          INTEGER,
    end_date              DATE,
    occurrence_count      INTEGER,
    conflict_resolution   VARCHAR(32)              NOT NULL,
    created_date          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date          TIMESTAMP WITH TIME ZONE,
    created_by            VARCHAR(255)             NOT NULL,
    updated_by            VARCHAR(255),
    CONSTRAINT chk_class_session_templates_time_valid
        CHECK (start_time < end_time),
    CONSTRAINT chk_class_session_templates_template_order_non_negative
        CHECK (template_order >= 0),
    CONSTRAINT chk_class_session_templates_recurrence_type
        CHECK (recurrence_type IS NULL OR recurrence_type IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_class_session_templates_interval_positive
        CHECK (interval_value IS NULL OR interval_value > 0),
    CONSTRAINT chk_class_session_templates_day_of_month_valid
        CHECK (day_of_month IS NULL OR day_of_month BETWEEN 1 AND 31),
    CONSTRAINT chk_class_session_templates_occurrence_count_positive
        CHECK (occurrence_count IS NULL OR occurrence_count > 0),
    CONSTRAINT chk_class_session_templates_conflict_resolution
        CHECK (conflict_resolution IN ('FAIL', 'SKIP', 'ROLLOVER')),
    CONSTRAINT uq_class_session_templates_class_order
        UNIQUE (class_definition_uuid, template_order)
);

CREATE INDEX idx_class_session_templates_class_definition
    ON class_session_templates (class_definition_uuid);

COMMENT ON TABLE class_session_templates IS 'Stores the original session templates used to generate scheduled instances for a class definition';
COMMENT ON COLUMN class_session_templates.class_definition_uuid IS 'Class definition that owns this session template';
COMMENT ON COLUMN class_session_templates.template_order IS 'Stable ordering of templates within the class definition payload';
COMMENT ON COLUMN class_session_templates.start_time IS 'UTC start date-time for the first occurrence';
COMMENT ON COLUMN class_session_templates.end_time IS 'UTC end date-time for the first occurrence';
COMMENT ON COLUMN class_session_templates.conflict_resolution IS 'Conflict handling strategy used when applying this template';
