ALTER TABLE class_definitions
    ADD COLUMN academic_period_start_date DATE,
    ADD COLUMN academic_period_end_date DATE,
    ADD COLUMN registration_period_start_date DATE,
    ADD COLUMN registration_period_end_date DATE,
    ADD COLUMN class_reminder_minutes INTEGER,
    ADD COLUMN class_color VARCHAR(7);

ALTER TABLE class_definitions
    ADD CONSTRAINT chk_class_definitions_academic_period_valid
        CHECK (
            academic_period_start_date IS NULL
            OR academic_period_end_date IS NULL
            OR academic_period_start_date <= academic_period_end_date
        ),
    ADD CONSTRAINT chk_class_definitions_registration_period_valid
        CHECK (
            registration_period_start_date IS NULL
            OR registration_period_end_date IS NULL
            OR registration_period_start_date <= registration_period_end_date
        ),
    ADD CONSTRAINT chk_class_definitions_class_reminder_minutes_non_negative
        CHECK (class_reminder_minutes IS NULL OR class_reminder_minutes >= 0),
    ADD CONSTRAINT chk_class_definitions_class_color_valid
        CHECK (class_color IS NULL OR class_color ~ '^#[0-9A-Fa-f]{6}$');

COMMENT ON COLUMN class_definitions.academic_period_start_date IS 'Optional academic period start date for the class lifecycle';
COMMENT ON COLUMN class_definitions.academic_period_end_date IS 'Optional academic period end date for the class lifecycle';
COMMENT ON COLUMN class_definitions.registration_period_start_date IS 'Optional registration period start date for class enrollment';
COMMENT ON COLUMN class_definitions.registration_period_end_date IS 'Optional registration period end date for class enrollment';
COMMENT ON COLUMN class_definitions.class_reminder_minutes IS 'Optional number of minutes before class start when reminders should be sent';
COMMENT ON COLUMN class_definitions.class_color IS 'Optional hex color code used to visually distinguish the class in UI surfaces';
